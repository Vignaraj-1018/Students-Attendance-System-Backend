package com.attendancesystem.students.attendancesystem.Service;

import com.attendancesystem.students.attendancesystem.Model.AttendanceDetails;
import com.attendancesystem.students.attendancesystem.Model.UserDetails;
import com.mongodb.client.result.DeleteResult;
import org.apache.catalina.User;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AttendanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttendanceService.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SharedService sharedService;

    public ResponseEntity<?> updateAttendance(AttendanceDetails attendanceDetails){
        UserDetails userDetails = sharedService.getUserByUserId(attendanceDetails.getUserId());
        if(userDetails == null){
            LOGGER.error("User Not Found"+attendanceDetails.getUserId());
            return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);
        } else if (userDetails.isAuthenticated()) {
            upsertAttendance(attendanceDetails);
            LOGGER.info("User Attendance Updated Successfully: "+attendanceDetails.getUserId());
            return new ResponseEntity<>(attendanceDetails,HttpStatus.OK);
        }
        else {
            LOGGER.error("User Not Authenticated: "+attendanceDetails.getUserId());
            return new ResponseEntity<>("User Not Authenticated",HttpStatus.NOT_ACCEPTABLE);
        }
    }

    public void upsertAttendance(AttendanceDetails attendanceDetails){
        AttendanceDetails attendanceDetailsDB = sharedService.checkForExistingAttendance(attendanceDetails.getUserId(), attendanceDetails.getAcademicYear(), attendanceDetails.getSemester());
        if(attendanceDetailsDB == null){
            attendanceDetails.setAttendanceId(UUID.randomUUID().toString());
            LOGGER.info("New Attendance Details for userId: "+attendanceDetails.getUserId());
        }
        else{
            attendanceDetails.setAttendanceId(attendanceDetailsDB.getAttendanceId());
            LOGGER.info("Existing Attendance Details for userId: "+attendanceDetails.getUserId());
        }
        Document document = new Document();
        mongoTemplate.getConverter().write(attendanceDetails, document);
        Update update = Update.fromDocument(document);
        mongoTemplate.upsert(sharedService.getAttendanceQuery(attendanceDetails.getAttendanceId(), attendanceDetails.getUserId(), attendanceDetails.getAcademicYear(),
                attendanceDetails.getSemester()),update, AttendanceDetails.class);
    }

    public ResponseEntity<?> deleteAttendance(String attendanceId) {
        System.out.println(attendanceId);
        DeleteResult  deleteResult= sharedService.deleteSingleAttendance(attendanceId);
        if(deleteResult.getDeletedCount() == 1){
            LOGGER.info("Attendance Details Deleted Successfully: "+attendanceId);
            return new ResponseEntity<>("Attendance Details Deleted Successfully",HttpStatus.OK);
        }
        else{
            LOGGER.error("Error in Deleting Attendance Details: "+attendanceId);
            return new ResponseEntity<>("Error in Deleting Attendance Details",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> getSingleAttendance(String attendanceId) {
        AttendanceDetails attendanceDetails = sharedService.getSingleAttendance(attendanceId);
        if(attendanceDetails == null){
            LOGGER.error("Attendance Not Found"+attendanceId);
            return new ResponseEntity<>("Attendance Not Found",HttpStatus.NOT_FOUND);
        }
        else{
            LOGGER.info("Attendance Fetch Successful"+attendanceId);
            return new ResponseEntity<>(attendanceDetails,HttpStatus.OK);
        }
    }

    public ResponseEntity<?> getSingleUserAllAttendance(String userId) {
        UserDetails userDetails = sharedService.getUserByUserId(userId);
        if(userDetails == null){
            LOGGER.error("User Not Found: "+userId);
            return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);
        }
        List<AttendanceDetails> attendanceDetails = sharedService.getAttendanceByUserId(userId);
        if(attendanceDetails.isEmpty()){
            LOGGER.error("Attendance Not Found for User: "+userId);
            return new ResponseEntity<>("Attendance Not Found",HttpStatus.NOT_FOUND);
        }
        else{
            LOGGER.info("Attendance Fetch Successful for User"+userId);
            return new ResponseEntity<>(attendanceDetails,HttpStatus.OK);
        }
    }
}
