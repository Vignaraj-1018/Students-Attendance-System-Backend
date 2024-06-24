package com.attendancesystem.students.attendancesystem.Service;

import com.attendancesystem.students.attendancesystem.Config.Jwt.JwtUtils;
import com.attendancesystem.students.attendancesystem.Model.EmailValidate;
import com.attendancesystem.students.attendancesystem.Model.LoginResponse;
import com.attendancesystem.students.attendancesystem.Model.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.security.SecureRandom;
import java.util.*;

@Service
public class LoginService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginService.class);

    @Autowired
    private SharedService sharedService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    public ResponseEntity<?> loginUser(UserDetails userDetails){
        String jwtToken = authenticateUser(userDetails);
        if (!Objects.equals(jwtToken, "AUTH_FAILED")) {
            UserDetails userFromDB = sharedService.getUserByUserEmail(userDetails.getUserEmail());
            LoginResponse loginResponse = new LoginResponse(userFromDB, jwtToken);
            return new ResponseEntity<>(loginResponse, HttpStatus.OK);
        }
        else{
            LOGGER.error("User Login Failed: {}", userDetails.getUserEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User Login Failed"));
        }
    }

    public String authenticateUser(UserDetails userDetails){
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDetails.getUserEmail(), userDetails.getPassword()));
        } catch (AuthenticationException exception) {
            return "AUTH_FAILED";
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        org.springframework.security.core.userdetails.UserDetails user = (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtils.generateTokenFromUsername(user);
        LOGGER.info("User Authentication Successful: {}", user.getUsername());
        return jwtToken;
    }

    public ResponseEntity<?> signUpUser(UserDetails userDetails){
        Map<String, Object> userCredentials= new HashMap<>();
        userCredentials.put("userEmail",userDetails.getUserEmail());
        userCredentials.put("password",userDetails.getPassword());
        ResponseEntity<?> response = createUser(userDetails);
        if(response.getStatusCode() == HttpStatus.CREATED){
            userDetails.setPassword(userCredentials.get("password").toString());
            return loginUser(userDetails);
        }
        else{
            return response;
        }

    }

    public ResponseEntity<?> createUser(UserDetails userDetails) {
        try {
            if (sharedService.getUserByUserEmail(userDetails.getUserEmail()) == null) {
                Map<String, Object> response = sendEmailOTP(userDetails);
                if (response.get("result").equals("SUCCESS")) {
                    return addUser(userDetails);
                } else {
                    Exception e = (Exception) response.get("Exception");
                    if (e instanceof ResourceAccessException) {
                        LOGGER.error("Email Service Down: {}", userDetails.getUserEmail());
                        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(Map.of("message", "Email Service Down"));
                    } else {
                        LOGGER.error("Error in Sending the OTP: {}", userDetails.getUserEmail());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error in Sending the OTP"));
                    }
                }
            } else {
                LOGGER.error("User Already Exist: {}", userDetails.getUserEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "User Already Exist"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in Creating the User: {}", userDetails.getUserEmail());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error in Creating the User"));
        }
    }

    public ResponseEntity<?> updateUser(UserDetails userDetails) {
        UserDetails userFromDB = sharedService.getUserByUserId(userDetails.getUserId());
        if (userFromDB == null) {
            LOGGER.info("User Not Found: {}", userDetails.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User Not Found"));
        } else {
            if (userDetails.getName() != null) {
                userFromDB.setName(userDetails.getName());
            }
            sharedService.upsertUser(userFromDB);
            LOGGER.info("User Updated Successfully: {}", userDetails.getUserId());
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "User Updated Successfully"));
        }
    }

    public ResponseEntity<?> addUser(UserDetails userDetails){
        userDetails.setUserId(UUID.randomUUID().toString());
        userDetails.setPassword(encryptPassword(userDetails.getPassword()));
        userDetails.setRole(Collections.singleton("ROLE_USER"));
        sharedService.upsertUser(userDetails);
        LOGGER.info("User Created Successfully: {}", userDetails.getUserEmail());
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
        EmailValidate emailMsg = generateEmailMsg(userDetails);

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

    public EmailValidate generateEmailMsg(UserDetails userDetails){
        return EmailValidate.builder()
                .name(userDetails.getName())
                .mail(userDetails.getUserEmail())
                .subject("Welcome to Student Attendance System")
                .message("<div style='padding:20px;'><div><span style='font-size:20px;'> Hello, <span style='font-size:20px; font-weight: bold;'>"+userDetails.getName()+"</span></span><h3 style='font-size:20px; font-weight: normal;'>Here's Your OTP Code for Email Validation: <span style='font-size:20px; font-weight: bold;' >"+userDetails.getOTP()+"</span></h3><h2></h2></div><div style='font-size:10px; border-width:1px 0px 0px 0px;border-style: solid; padding:5px; width:100%; margin-top:100px;'><span>Regards,<br/> Admin Student Attendance System</span><br/><span><a href='mailto:studattendsys@gmail.com' style='color:#FF6E31;'>studattendsys@gmail.com</a></span></div></div>")
                .toOther(true)
                .build();
    }

    public ResponseEntity<?> validateOtp(UserDetails userDetails) {
        UserDetails userFromDB = sharedService.getUserByUserEmail(userDetails.getUserEmail());

        if (userFromDB == null) {
            LOGGER.error("User Not Found: {}", userDetails.getUserEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User Not Found"));
        }

        if (userFromDB.getOTP().equals(userDetails.getOTP())) {
            if (userFromDB.isAuthenticated()) {
                LOGGER.error("User Already Authenticated: {}", userFromDB.getUserId());
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("message", "User Already Authenticated"));
            }
            userFromDB.setAuthenticated(true);
            sharedService.upsertUser(userFromDB);
            LOGGER.info("OTP Validation Successful: {}", userFromDB.getUserId());
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "OTP Validation Successful"));
        } else {
            LOGGER.error("User OTP Mismatch: {}", userFromDB.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("message", "OTP Mismatch"));
        }
    }

    public ResponseEntity<?> resendOtp(UserDetails userDetails) {
        UserDetails userFromDB = sharedService.getUserByUserEmail(userDetails.getUserEmail());
        if (userFromDB == null) {
            LOGGER.info("User Not Found: {}", userDetails.getUserEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User Not Found"));
        }
        if (userFromDB.isAuthenticated()) {
            LOGGER.info("User Already Authenticated: {}", userFromDB.getUserId());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "User Already Authenticated"));
        }
        Map<String, Object> response = sendEmailOTP(userFromDB);
        if (response.get("result").equals("SUCCESS")) {
            sharedService.upsertUser(userFromDB);
            LOGGER.info("Resend OTP Successful: {}", userFromDB.getUserId());
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Resend OTP Successful"));
        } else {
            Exception e = (Exception) response.get("Exception");
            if (e instanceof ResourceAccessException) {
                LOGGER.error("Email Service Down: {}", userFromDB.getUserEmail());
                return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(Map.of("message", "Email Service Down"));
            } else {
                LOGGER.error("Error in Resending the OTP: {}", userFromDB.getUserEmail());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error in Resending the OTP"));
            }
        }
    }

    public ResponseEntity<?> forgotPasswordRequest(UserDetails userDetails) {
        UserDetails userFromDB = sharedService.getUserByUserEmail(userDetails.getUserEmail());
        if (userFromDB == null) {
            LOGGER.info("User Not Found: {}", userDetails.getUserEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User Not Found"));
        } else {
            Map<String, Object> response = sendEmailOTP(userFromDB);
            if (response.get("result").equals("SUCCESS")) {
                sharedService.upsertUser(userFromDB);
                LOGGER.info("OTP Sent Successfully: {}", userDetails.getUserEmail());
                return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "OTP Sent Successfully, Validate Email to Continue"));
            } else {
                Exception e = (Exception) response.get("Exception");
                if (e instanceof ResourceAccessException) {
                    LOGGER.error("Email Service Down: {}", userFromDB.getUserEmail());
                    return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(Map.of("message", "Email Service Down"));
                } else {
                    LOGGER.error("Error in Sending the OTP: {}", userFromDB.getUserEmail());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error in Sending the OTP"));
                }
            }
        }
    }

    public ResponseEntity<?> resetPassword(UserDetails userDetails) {
        UserDetails userFromDB = sharedService.getUserByUserEmail(userDetails.getUserEmail());
        if (userFromDB == null) {
            LOGGER.info("User Not Found: {}", userDetails.getUserEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User Not Found"));
        } else {
            if (userFromDB.isAuthenticated()) {
                userFromDB.setPassword(encryptPassword(userDetails.getPassword()));
                sharedService.upsertUser(userFromDB);

                LOGGER.info("Reset Password Successful: {}", userDetails.getUserId());
                return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Reset Password Successful"));
            } else {
                LOGGER.info("User is Not Authenticated: {}", userDetails.getUserId());
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("message", "User is not Authenticated"));
            }
        }
    }
}
