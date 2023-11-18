package com.attendancesystem.students.attendancesystem;

import com.attendancesystem.students.attendancesystem.Model.AttendanceDetails;
import com.attendancesystem.students.attendancesystem.Model.UserDetails;
import com.attendancesystem.students.attendancesystem.Service.AttendanceService;
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

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping("/")
    public ResponseEntity<?> welcomeFunction(){
        System.out.println("Hello from the Welcome Function!");
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

    @PostMapping("/request/forgotPassword")
    public ResponseEntity<?> forgotPasswordRequest(@RequestBody UserDetails userDetails){
        return loginService.forgotPasswordRequest(userDetails);
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestBody UserDetails userDetails) {
        return loginService.resetPassword(userDetails);
    }

    @PutMapping("/update/attendance")
    public ResponseEntity<?> updateAttendance(@RequestBody AttendanceDetails attendanceDetails){
        return attendanceService.updateAttendance(attendanceDetails);
    }

    @DeleteMapping("/delete/attendance")
    public ResponseEntity<?> deleteAttendance(@RequestParam String attendanceId ){
        return attendanceService.deleteAttendance(attendanceId);
    }

    @GetMapping("/get/attendance")
    public ResponseEntity<?> getSingleAttendance(@RequestParam String attendanceId){
        return attendanceService.getSingleAttendance(attendanceId);
    }

    @GetMapping("/get/user/attendance")
    public ResponseEntity<?> getSingleUserAllAttendance(@RequestParam String userId){
        return attendanceService.getSingleUserAllAttendance(userId);
    }

}
