package com.attendancesystem.students.attendancesystem;

import com.attendancesystem.students.attendancesystem.Model.UserDetails;
import com.attendancesystem.students.attendancesystem.Service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class Controller {

    @Autowired
    private LoginService loginService;

    @GetMapping("/")
    public ResponseEntity<?> welcomeFunction(){
        System.out.println("Hello from the Welcome Function!");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/secured")
    public ResponseEntity<?> securedFunction(){
        System.out.println("Hello from the Secured Function!");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authUser(@RequestBody UserDetails userDetails){
        return loginService.authUser(userDetails);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> createUser(@RequestBody UserDetails userDetails){
        return loginService.createUser(userDetails);
    }

    @PostMapping("/validate/otp")
    public ResponseEntity<?> validateOtp(@RequestBody UserDetails userDetails){
        return loginService.validateOtp(userDetails);
    }

    @PostMapping("/resend/otp")
    public ResponseEntity<?> resendOtp(@RequestBody UserDetails userDetails){
        return loginService.resendOtp(userDetails);
    }
}
