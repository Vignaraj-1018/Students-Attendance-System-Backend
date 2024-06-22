package com.attendancesystem.students.attendancesystem.Controller;

import com.attendancesystem.students.attendancesystem.Model.AttendanceDetails;
import com.attendancesystem.students.attendancesystem.Service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping("")
    public String welcomeFunction() {
        return "Welcome to Attendance System";
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateAttendance(@RequestBody AttendanceDetails attendanceDetails){
        return attendanceService.updateAttendance(attendanceDetails);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAttendance(@RequestParam String attendanceId ){
        return attendanceService.deleteAttendance(attendanceId);
    }

    @GetMapping("/get")
    public ResponseEntity<?> getSingleAttendance(@RequestParam String attendanceId){
        return attendanceService.getSingleAttendance(attendanceId);
    }

    @GetMapping("/get/all")
    public ResponseEntity<?> getSingleUserAllAttendance(@RequestParam String userId){
        return attendanceService.getSingleUserAllAttendance(userId);
    }
}
