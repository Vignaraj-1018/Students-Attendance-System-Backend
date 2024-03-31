package com.attendancesystem.students.attendancesystem.Service;

import com.attendancesystem.students.attendancesystem.Model.EmailValidate;
import com.attendancesystem.students.attendancesystem.Model.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.security.SecureRandom;
import java.util.*;

@Service
public class LoginService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginService.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SharedService sharedService;

    public ResponseEntity<?> authUser(UserDetails userDetails){
        if(userDetails.getPassword()==null){
            LOGGER.error("User Password Not Found: " + userDetails.getUserEmail());
            return new ResponseEntity<>("User Password Not Found",HttpStatus.NOT_FOUND);
        }
        UserDetails userFromDB = sharedService.getUserByUserEmail(userDetails.getUserEmail());
        if( userFromDB == null){
            LOGGER.error("User Not Found: " + userDetails.getUserEmail());
            return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);
        }
        else{
            if(checkPassword(userDetails.getPassword(),userFromDB.getPassword())){
                LOGGER.info("User Login Successful: " + userDetails.getUserEmail());
                return new ResponseEntity<>(userFromDB,HttpStatus.OK);
            }
            else {
                LOGGER.error("User Password Wrong: " + userDetails.getUserEmail());
                return new ResponseEntity<>("User Password Wrong",HttpStatus.UNAUTHORIZED);
            }
        }
    }

    public ResponseEntity<?> createUser(UserDetails userDetails) {
        try {
            if(sharedService.getUserByUserEmail(userDetails.getUserEmail()) == null) {
                Map<String, Object> response = sendEmailOTP(userDetails);
                if(response.get("result").equals("SUCCESS")){
                    return addUser(userDetails);
                }
                else{
                    Exception e = (Exception) response.get("Exception");
                    if(e instanceof ResourceAccessException){
                        LOGGER.error("Email Service Down: " + userDetails.getUserEmail());
                        return new ResponseEntity<>("Email Service Down",HttpStatus.GATEWAY_TIMEOUT);
                    }
                    else{
                        LOGGER.error("Error in Sending the OTP: " + userDetails.getUserEmail());
                        return new ResponseEntity<>("Error in Sending the OTP",HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }
            else{
                LOGGER.error("User Already Exist: " + userDetails.getUserEmail());
                return new ResponseEntity<>("User Already Exist",HttpStatus.CONFLICT);
            }
        }
        catch (Exception e){
            LOGGER.error("Error in Creating the User: " + userDetails.getUserEmail());
            return new ResponseEntity<>("Error in Creating the User",HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    public ResponseEntity<?> updateUser(UserDetails userDetails){
        UserDetails userFromDB = sharedService.getUserByUserId(userDetails.getUserId());
        if(userFromDB == null){
            LOGGER.info("User Not Found: " + userDetails.getUserId());
            return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);
        }
        else{
            if(userDetails.getName()!=null){
                userFromDB.setName(userDetails.getName());
            }
            sharedService.upsertUser(userFromDB);
            LOGGER.info("User Updated Successfully: " + userDetails.getUserId());
            return new ResponseEntity<>("User Updated Successfully",HttpStatus.OK);
        }
    }

    public ResponseEntity<?> addUser(UserDetails userDetails){
        userDetails.setUserId(UUID.randomUUID().toString());
        userDetails.setPassword(encryptPassword(userDetails.getPassword()));
        sharedService.upsertUser(userDetails);
        LOGGER.info("User Created Successfully: " + userDetails.getUserEmail());
        return new ResponseEntity<>(userDetails,HttpStatus.CREATED);
    }

    private String encryptPassword(String password) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(10, new SecureRandom());
        return bCryptPasswordEncoder.encode(password);
    }

    private boolean checkPassword(String password, String userPassword){
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(10, new SecureRandom());
        return bCryptPasswordEncoder.matches(password,userPassword);
    }

    public Map<String, Object> sendEmailOTP(UserDetails userDetails){

        String otp = generateOTP();
        userDetails.setOTP(otp);
        userDetails.setAuthenticated(false);
        EmailValidate emailMsg = generateEmailMsg(otp,userDetails);

        try{
            sharedService.sendEmail(emailMsg);
            Map<String, Object> returnObject = new HashMap<>();
            returnObject.put("result", "SUCCESS");
            return returnObject;
        }
        catch (Exception e){
            Map<String, Object> returnObject = new HashMap<>();
            returnObject.put("result", "FAILURE");
            returnObject.put("Exception",e);
            return returnObject;

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
                .message("<div style='padding:20px;'><div><span style='font-size:20px;'> Hello, <span style='font-size:20px; font-weight: bold;'>"+userDetails.getName()+"</span></span><h3 style='font-size:20px; font-weight: normal;'>Here's Your OTP Code for Email Validation: <span style='font-size:20px; font-weight: bold;' >"+userDetails.getOTP()+"</span></h3><h2></h2></div><div style='font-size:10px; border-width:1px 0px 0px 0px;border-style: solid; padding:5px; width:100%; margin-top:100px;'><span>Regards,<br/> Admin Student Attendance System</span><br/><span><a href='mailto:studattendsys@gmail.com' style='color:#FF6E31;'>studattendsys@gmail.com</a></span></div></div>")
                .toOther(true)
                .build();
    }

    public ResponseEntity<?> validateOtp(UserDetails userDetails) {
        UserDetails userFromDB = sharedService.getUserByUserId(userDetails.getUserId());

        if(userFromDB.getOTP().equals(userDetails.getOTP())){
            if(userFromDB.isAuthenticated()){
                LOGGER.error("User Already Authenticated: " + userFromDB.getUserId());
                return new ResponseEntity<>("User Already Authenticated",HttpStatus.NOT_ACCEPTABLE);
            }
            userFromDB.setAuthenticated(true);
            sharedService.upsertUser(userFromDB);
            LOGGER.info("OTP Validation Successful: " + userFromDB.getUserId());
            return new ResponseEntity<>(userFromDB,HttpStatus.OK);
        }
        else {
            LOGGER.error("User OTP Mismatch: " + userFromDB.getUserId());
            return new ResponseEntity<>("OTP Mismatch",HttpStatus.NOT_ACCEPTABLE);
        }
    }

    public ResponseEntity<?> resendOtp(UserDetails userDetails) {
        UserDetails userFromDB = sharedService.getUserByUserEmail(userDetails.getUserEmail());
        if(userFromDB == null){
            LOGGER.info("User Not Found: " + userDetails.getUserEmail());
            return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);
        }
        if(userFromDB.isAuthenticated()){
            LOGGER.info("User Already Authenticated: " + userFromDB.getUserId());
            return new ResponseEntity<>("User Already Authenticated",HttpStatus.CONFLICT);
        }
        Map<String, Object> response = sendEmailOTP(userFromDB);
        if(response.get("result").equals("SUCCESS")){
            sharedService.upsertUser(userFromDB);
            LOGGER.info("Resend OTP Successful: " + userFromDB.getUserId());
            return new ResponseEntity<>("Resend OTP Successful",HttpStatus.OK);
        }
        else{
            Exception e = (Exception) response.get("Exception");
            if(e instanceof ResourceAccessException){
                LOGGER.error("Email Service Down: " + userFromDB.getUserEmail());
                return new ResponseEntity<>("Email Service Down",HttpStatus.GATEWAY_TIMEOUT);
            }
            else{
                LOGGER.error("Error in Resending the OTP: " + userFromDB.getUserEmail());
                return new ResponseEntity<>("Error in Resending the OTP",HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    public ResponseEntity<?> forgotPasswordRequest(UserDetails userDetails) {
        UserDetails userFromDB = sharedService.getUserByUserEmail(userDetails.getUserEmail());
        if(userFromDB == null){
            LOGGER.info("User Not Found: " + userDetails.getUserEmail());
            return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);
        }
        else{
            Map<String, Object> response = sendEmailOTP(userFromDB);
            if(response.get("result").equals("SUCCESS")){
                sharedService.upsertUser(userFromDB);
                LOGGER.info("OTP Sent Successfully: " + userDetails.getUserEmail());
                return new ResponseEntity<>(userFromDB,HttpStatus.OK);
            }
            else{
                Exception e = (Exception) response.get("Exception");
                if(e instanceof ResourceAccessException){
                    LOGGER.error("Email Service Down: " + userFromDB.getUserEmail());
                    return new ResponseEntity<>("Email Service Down",HttpStatus.GATEWAY_TIMEOUT);
                }
                else{
                    LOGGER.error("Error in Sending the OTP: " + userFromDB.getUserEmail());
                    return new ResponseEntity<>("Error in Sending the OTP",HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }
    }

    public ResponseEntity<?> resetPassword(UserDetails userDetails) {
        UserDetails userFromDB = sharedService.getUserByUserId(userDetails.getUserId());
        if(userFromDB == null){
            LOGGER.info("User Not Found: " + userDetails.getUserEmail());
            return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);
        }
        else{
            if(userFromDB.isAuthenticated()){
                userFromDB.setPassword(encryptPassword(userDetails.getPassword()));
                sharedService.upsertUser(userFromDB);

                LOGGER.info("Reset Password Successful: " + userDetails.getUserId());
                return new ResponseEntity<>("Reset Password Successful",HttpStatus.OK);
            }
            else{
                LOGGER.info("User is Not Authenticated: " + userDetails.getUserId());
                return new ResponseEntity<>("User is not Authenticated",HttpStatus.NOT_ACCEPTABLE);
            }

        }
    }
}
