package com.attendancesystem.students.attendancesystem.Controller;

import com.attendancesystem.students.attendancesystem.Model.EmailValidate;
import com.attendancesystem.students.attendancesystem.Service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class Controller {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("")
    public ResponseEntity<?> welcomeFunction(){
        System.out.println("Hello from the Welcome Function!");
        return new ResponseEntity<>("Welcome to Student Attendance API",HttpStatus.OK);
    }

    @PostMapping("/sendNotification")
    public ResponseEntity<?> sendNotificationForAllUser(@RequestBody EmailValidate emailValidate){
        return notificationService.triggerNotificationToAllUser(emailValidate);
    }

    @GetMapping("/sendRemainder")
    public ResponseEntity<?> dailyNotification(){
        return notificationService.triggerRemainderScheduler();
    }


}
