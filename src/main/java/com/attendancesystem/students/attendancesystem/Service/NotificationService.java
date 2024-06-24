package com.attendancesystem.students.attendancesystem.Service;

import com.attendancesystem.students.attendancesystem.Model.EmailValidate;
import com.attendancesystem.students.attendancesystem.Model.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private SharedService sharedService;

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    public ResponseEntity<?> triggerRemainderScheduler(){
        try{
            Thread backgroundThread = new Thread(this::dailyNotification);
            backgroundThread.start();
            LOGGER.info("Scheduler Triggered for Sending Remainder");
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Successfully Triggered Scheduler"));
        }
        catch (Exception e){
            LOGGER.info("Error in Triggering the Scheduler: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error in Triggering the Scheduler"));
        }
    }

    public ResponseEntity<?> triggerNotificationToAllUser(EmailValidate notificationInfo) {
        try {
            Thread backgroundThread = new Thread(() -> sendNotificationToAllUser(notificationInfo));
            backgroundThread.start();
            LOGGER.info("Method to Send Notification to All Users Triggered Successfully");
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Method to Send Notification to All Users Triggered Successfully"));
        } catch (Exception e) {
            LOGGER.error("Failed to Trigger Method to Send Notification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to Trigger Method to Send Notification"));
        }
    }

    public void sendNotificationToAllUser(EmailValidate notificationInfo){
        try{
    //        UserDetails allUsers = sharedService.getUserByUserEmail("vignaraj03@gmail.com");
            List<UserDetails> allUsers = sharedService.getAllUsers();
            LOGGER.info("Starting to Send Notification to All Users");
            allUsers.forEach(user->{
                EmailValidate emailMsg = EmailValidate.builder()
                        .name(user.getName())
                        .mail(user.getUserEmail())
                        .subject(notificationInfo.getSubject())
                        .message("<div><span style='font-size: 20px;'> Hello, <span style='font-size: 20px; font-weight: bold;'>"+user.getName()+"</span></span></div>"+notificationInfo.getMessage())
                        .toOther(true)
                        .build();
                sharedService.sendEmail(emailMsg);
            });
            LOGGER.info("Notification Sent to All Users Successfully");
        }
        catch (Exception e){
            LOGGER.info("Error While Sending Notification {}", e.getMessage());
        }
    }

    public void dailyNotification(){
        LOGGER.info("Scheduler Triggered");
        List<UserDetails> allUsers = sharedService.getAllUsers();
//        UserDetails user = sharedService.getUserByUserEmail("vignaraj03@gmail.com");
        LOGGER.info("Starting to Send Notification to Enabled Users");
        allUsers.forEach(user->{
            if(Boolean.TRUE.equals(user.getNotificationEnabled())){
                EmailValidate emailMsg = EmailValidate.builder()
                        .name(user.getName())
                        .mail(user.getUserEmail())
                        .subject("Remainder: Attendance Update - "+LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                        .message("<div style='padding:20px;'><div><span style='font-size:20px;'> Hello, <span style='font-size:20px; font-weight: bold;'>"+user.getName()+"</span></span><h3 style='font-size:20px; font-weight: normal;'>This message is a reminder to update your attendance for today, <span style='font-size:20px; font-weight: bold;' >"+ LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))+"</span></h3><h3 style='font-size:20px; font-weight: normal;'>Click here to update: <a href='https://student-attendance-tracker.vercel.app/dashboard'>LINK</a></h3></div><div style='font-size:10px; border-width:1px 0px 0px 0px;border-style: solid; padding:5px; width:100%;margin-top:100px;'><span>Regards,<br/> Admin Student Attendance System</span><br/><span><a href='mailto:studattendsys@gmail.com' style='color:#FF6E31;'>studattendsys@gmail.com</a></span></div></div>")
                        .toOther(true)
                        .build();
                sharedService.sendEmail(emailMsg);
            }
        });
        LOGGER.info("Notification Sent to All Enabled Users Successfully");
    }

    public ResponseEntity<?> enableNotificationForUser(UserDetails userDetails){
        UserDetails userFromDB = sharedService.getUserByUserId(userDetails.getUserId());
        if(userFromDB == null){
            LOGGER.error("User Not Found: {}", userDetails.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("Message","User Not Found"));
        }
        else{
            userFromDB.setNotificationEnabled(true);
            sharedService.upsertUser(userFromDB);
            LOGGER.info("Notification Enabled Successfully {}", userDetails.getUserId());
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("Message","Notification Enabled Successfully"));
        }
    }

    public ResponseEntity<?> disableNotificationForUser(UserDetails userDetails){
        UserDetails userFromDB = sharedService.getUserByUserId(userDetails.getUserId());
        if(userFromDB == null){
            LOGGER.error("User Not Found: {}", userDetails.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("Message","User Not Found"));
        }
        else {
            userFromDB.setNotificationEnabled(false);
            sharedService.upsertUser(userFromDB);
            LOGGER.info("Notification Disabled Successfully: {}", userDetails.getUserId());
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("Message","Notification Disabled Successfully"));
        }
    }
}
