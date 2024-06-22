package com.attendancesystem.students.attendancesystem.Controller;

import com.attendancesystem.students.attendancesystem.Model.EmailValidate;
import com.attendancesystem.students.attendancesystem.Model.UserDetails;
import com.attendancesystem.students.attendancesystem.Service.LoginService;
import com.attendancesystem.students.attendancesystem.Service.NotificationService;
import com.attendancesystem.students.attendancesystem.Service.SharedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SharedService sharedService;


    @GetMapping("/home")
    public ResponseEntity<?> welcomeFunction(){
        System.out.println("Hello from the Version 2 Application!");
        return new ResponseEntity<>("Ok", HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody UserDetails userDetails) {
        return loginService.loginUser(userDetails);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody UserDetails userDetails) {
        return loginService.signUpUser(userDetails);
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateUser(@RequestBody UserDetails userDetails) {
        return loginService.updateUser(userDetails);
    }

    @PostMapping("/validateOtp")
    public ResponseEntity<?> validateOtp(@RequestBody UserDetails userDetails){
        return loginService.validateOtp(userDetails);
    }

    @PostMapping("/resendOtp")
    public ResponseEntity<?> resendOtp(@RequestBody UserDetails userDetails){
        return loginService.resendOtp(userDetails);
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<?> forgotPasswordRequest(@RequestBody UserDetails userDetails){
        return loginService.forgotPasswordRequest(userDetails);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestBody UserDetails userDetails) {
        return loginService.resetPassword(userDetails);
    }

    @PostMapping("/enableNotification")
    public ResponseEntity<?> enableNotificationForUser(@RequestBody UserDetails userDetails){
        return notificationService.enableNotificationForUser(userDetails);
    }

    @PostMapping("/disableNotification")
    public ResponseEntity<?> disableNotificationForUser(@RequestBody UserDetails userDetails){
        return notificationService.disableNotificationForUser(userDetails);
    }

    @PostMapping("/contactMe")
    public ResponseEntity<?> contactMe(@RequestBody EmailValidate emailValidate){
        return sharedService.contactMe(emailValidate);
    }

}
