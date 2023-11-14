package com.attendancesystem.students.attendancesystem.Service;

import com.attendancesystem.students.attendancesystem.Model.EmailValidate;
import com.attendancesystem.students.attendancesystem.Model.UserDetails;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Random;
import java.util.UUID;

@Service
public class LoginService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginService.class);

    @Value("${spring.emailService.url}")
    private String emailServiceUrl;

    @Autowired
    private MongoTemplate mongoTemplate;

    public ResponseEntity<?> authUser(UserDetails userDetails){
        UserDetails userFromDB = getUserByUserEmail(userDetails.getUserEmail());
        if( userFromDB == null){
            LOGGER.error("User Not Found" + userDetails.toString());
            return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);
        }
        else{
            if(userFromDB.getPassword().equals(userDetails.getPassword())){
                LOGGER.info("User Login Successful" + userDetails.toString());
                return new ResponseEntity<>(userFromDB,HttpStatus.OK);
            }
            else {
                LOGGER.error("User Password Wrong" + userDetails.toString());
                return new ResponseEntity<>("User Password Wrong",HttpStatus.UNAUTHORIZED);
            }
        }
    }

    public ResponseEntity<?> createUser(UserDetails userDetails) {
        try {
            if(getUserByUserEmail(userDetails.getUserEmail()) == null) {
                String response = sendEmailOTP(userDetails);
                if(response.equals("SUCCESS")){
                    return addUser(userDetails);
                }
                else{
                    return new ResponseEntity<>("Error in Creating the User",HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            else{
                LOGGER.error("User Already Exist" + userDetails.toString());
                return new ResponseEntity<>("User Already Exist",HttpStatus.CONFLICT);
            }
        }
        catch (Exception e){
            LOGGER.error("Error in Creating the User" + userDetails.toString());
            return new ResponseEntity<>("Error in Creating the User",HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    public ResponseEntity<?> addUser(UserDetails userDetails){
        userDetails.setUserId(UUID.randomUUID().toString());
        upsertUser(userDetails);
        LOGGER.info("User Created Successfully" + userDetails.toString());
        return new ResponseEntity<>(userDetails,HttpStatus.CREATED);
    }

    public void upsertUser(UserDetails userDetails){
        Query query = new Query(Criteria.where("userId").is(userDetails.getUserId()).andOperator(Criteria.where("userEmail").is(userDetails.getUserEmail())));
        Document document = new Document();
        mongoTemplate.getConverter().write(userDetails, document);
        Update update = Update.fromDocument(document);
        mongoTemplate.upsert(query, update, UserDetails.class);
    }

    public UserDetails getUserByUserEmail(String userEmail){
        return mongoTemplate.findOne(new Query(Criteria.where("userEmail").is(userEmail)),UserDetails.class);
    }

    public UserDetails getUserByUserId(String userId){
        return mongoTemplate.findOne(new Query(Criteria.where("userId").is(userId)),UserDetails.class);
    }

    public String sendEmailOTP(UserDetails userDetails){

        RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());
        String otp = generateOTP();
        userDetails.setOTP(otp);
        userDetails.setAuthenticated(false);
        EmailValidate postObj = generateEmailMsg(otp,userDetails);

        try{
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(emailServiceUrl,postObj, String.class);
            String responseBody = responseEntity.getBody();
            LOGGER.info("Response from external API: " + responseBody);
            return "SUCCESS";
        }
        catch (Exception e){
            LOGGER.error("Error in sending Email OTP to "+userDetails.getUserEmail());
            return "FAILURE";

        }

    }

    private SimpleClientHttpRequestFactory getClientHttpRequestFactory() {

        SimpleClientHttpRequestFactory clientHttpRequestFactory  = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(3000);
        clientHttpRequestFactory.setReadTimeout(3000);
        return clientHttpRequestFactory;
    }

    public static String generateOTP() {
        int otpLength = 6;

        Random random = new Random();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }

        return otp.toString();
    }

    public EmailValidate generateEmailMsg(String otp,UserDetails userDetails){
        return EmailValidate.builder()
                .name(userDetails.getName())
                .mail(userDetails.getUserEmail())
                .subject("Welcome to Student Attendance System")
                .message("Here's Your OTP Code for Email Validation: "+otp)
                .toOther(true)
                .build();
    }

    public ResponseEntity<?> validateOtp(UserDetails userDetails) {
        System.out.println(userDetails.toString());
        UserDetails userFromDB = getUserByUserId(userDetails.getUserId());

        if(userFromDB.getOTP().equals(userDetails.getOTP())){
            if(userFromDB.isAuthenticated()){
                LOGGER.error("User Already Authenticated: " + userFromDB.getUserId());
                return new ResponseEntity<>("User Already Authenticated",HttpStatus.NOT_ACCEPTABLE);
            }
            userFromDB.setAuthenticated(true);
            upsertUser(userFromDB);
            LOGGER.info("OTP Validation Successful: " + userFromDB.getUserId());
            return new ResponseEntity<>(userFromDB,HttpStatus.OK);
        }
        else {
            LOGGER.error("User OTP Mismatch: " + userFromDB.getUserId());
            return new ResponseEntity<>("OTP Mismatch",HttpStatus.NOT_ACCEPTABLE);
        }
    }

    public ResponseEntity<?> resendOtp(UserDetails userDetails) {
        UserDetails userFromDB = getUserByUserId(userDetails.getUserId());
        if(userFromDB.isAuthenticated()){
            LOGGER.info("User Already Authenticated: " + userFromDB.getUserId());
            return new ResponseEntity<>("User Already Authenticated",HttpStatus.CONFLICT);
        }
        String response = sendEmailOTP(userFromDB);
        upsertUser(userFromDB);
        if(response.equals("SUCCESS")){
            LOGGER.info("Resend OTP Successful: " + userFromDB.getUserId());
            return new ResponseEntity<>("Resend OTP Successful",HttpStatus.OK);
        }
        else{
            LOGGER.error("Error in Resending the OTP: " + userFromDB.getUserEmail());
            return new ResponseEntity<>("Error in Resending the OTP",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
