package com.attendancesystem.students.attendancesystem.Service;

import com.attendancesystem.students.attendancesystem.Model.AttendanceDetails;
import com.attendancesystem.students.attendancesystem.Model.UserDetails;
import com.mongodb.client.result.DeleteResult;
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
import java.util.Map;
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
            LOGGER.error("User Not Found: {}",attendanceDetails.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User Not Found"));
        } else if (userDetails.isAuthenticated()) {
            upsertAttendance(attendanceDetails);
            LOGGER.info("User Attendance Updated Successfully: {}", attendanceDetails.getUserId());
            return new ResponseEntity<>(attendanceDetails,HttpStatus.OK);
        }
        else {
            LOGGER.error("User Not Authenticated: {}", attendanceDetails.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("message", "User Not Authenticated"));
        }
    }

    public void upsertAttendance(AttendanceDetails attendanceDetails){
        AttendanceDetails attendanceDetailsDB = sharedService.checkForExistingAttendance(attendanceDetails.getUserId(), attendanceDetails.getAcademicYear(), attendanceDetails.getSemester());
        if(attendanceDetailsDB == null){
            attendanceDetails.setAttendanceId(UUID.randomUUID().toString());
            LOGGER.info("New Attendance Details for userId: {}", attendanceDetails.getUserId());
        }
        else{
            attendanceDetails.setAttendanceId(attendanceDetailsDB.getAttendanceId());
            LOGGER.info("Existing Attendance Details for userId: {}", attendanceDetails.getUserId());
        }
        Document document = new Document();
        mongoTemplate.getConverter().write(attendanceDetails, document);
        Update update = Update.fromDocument(document);
        mongoTemplate.upsert(sharedService.getAttendanceQuery(attendanceDetails.getAttendanceId(), attendanceDetails.getUserId(), attendanceDetails.getAcademicYear(),
                attendanceDetails.getSemester()),update, AttendanceDetails.class);
    }

    public ResponseEntity<?> deleteAttendance(String attendanceId) {
        System.out.println(attendanceId);
        DeleteResult deleteResult = sharedService.deleteSingleAttendance(attendanceId);
        if (deleteResult.getDeletedCount() == 1) {
            LOGGER.info("Attendance Details Deleted Successfully: {}", attendanceId);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Attendance Details Deleted Successfully"));
        } else {
            LOGGER.error("Error in Deleting Attendance Details: {}", attendanceId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error in Deleting Attendance Details"));
        }
    }

    public ResponseEntity<?> getSingleAttendance(String attendanceId) {
        AttendanceDetails attendanceDetails = sharedService.getSingleAttendance(attendanceId);
        if(attendanceDetails == null){
            LOGGER.error("Attendance Not Found: {}", attendanceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Attendance Not Found"));
        }
        else{
            LOGGER.info("Attendance Fetch Successful: {}", attendanceId);
            return new ResponseEntity<>(attendanceDetails,HttpStatus.OK);
        }
    }

    public ResponseEntity<?> getSingleUserAllAttendance(String userId) {
        UserDetails userDetails = sharedService.getUserByUserId(userId);
        if(userDetails == null){
            LOGGER.error("User Not Found: {}",userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User Not Found"));
        }
        List<AttendanceDetails> attendanceDetails = sharedService.getAttendanceByUserId(userId);
        if(attendanceDetails.isEmpty()){
            LOGGER.error("Attendance Not Found for User: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Attendance Not Found"));
        }
        else{
            LOGGER.info("Attendance Fetch Successful for User: {}", userId);
            return new ResponseEntity<>(attendanceDetails,HttpStatus.OK);
        }
    }

    public ResponseEntity<?> getSummaryAttendance(String userId) {
        try {
            LOGGER.info("Getting Attendance Summary: {}", userId);
            String query = "{'aggregate': 'attendance_details', 'pipeline':[" +
                    "{\"$match\":{\"userId\":\"?\"}},{\"$unwind\":\"$subjectList\"},{\"$project\":{\"_id\":\"$_id\",\"academicYear\":\"$academicYear\",\"semester\":\"$semester\",\"subjectId\":\"$subjectList.subjectId\",\"name\":\"$subjectList.name\",\"presentCount\":\"$subjectList.presentCount\",\"totalCount\":\"$subjectList.totalCount\",\"percentage\":{\"$multiply\":[{\"$divide\":[\"$subjectList.presentCount\",\"$subjectList.totalCount\"]},100]}}},{\"$group\":{\"_id\":[\"$_id\"],\"academicYear\":{\"$first\":\"$academicYear\"},\"semester\":{\"$first\":\"$semester\"},\"attendanceId\":{\"$first\":\"$_id\"},\"totalPercentage\":{\"$sum\":\"$percentage\"},\"total\":{\"$sum\":1},\"subjectList\":{\"$push\":{\"name\":\"$name\",\"presentCount\":\"$presentCount\",\"absentCount\":\"$absentCount\",\"percentage\":\"$percentage\",\"subjectId\":\"$subjectId\"}}}},{\"$group\":{\"_id\":[\"$_id\"],\"academicYear\":{\"$first\":\"$academicYear\"},\"semester\":{\"$first\":\"$semester\"},\"attendanceId\":{\"$first\":\"$attendanceId\"},\"averagePercentage\":{\"$first\":{\"$divide\":[\"$totalPercentage\",\"$total\"]}}}},{\"$group\":{\"_id\":[\"$academicYear\"],\"academicYear\":{\"$first\":\"$academicYear\"},\"semesterList\":{\"$push\":{\"semester\":\"$semester\",\"attendanceId\":\"$attendanceId\",\"averagePercentage\":\"$averagePercentage\"}},}},{\"$project\":{\"_id\":0,\"academicYear\":1,\"semestersList\":{\"$sortArray\":{\"input\":\"$semesterList\",\"sortBy\":{\"semester\":1}}}}},{\"$sort\":{\"academicYear\":1}}" +
                    "],'cursor':{}}";
            query = query.replaceAll("\\?", userId);
            Document summaryAttendance = mongoTemplate.executeCommand(query);
            Map<String, Object> cursor = (Map<String, Object>) summaryAttendance.get("cursor");
            List<?> firstBatch = (List<?>) cursor.get("firstBatch");
            LOGGER.info("Attendance Summary Fetch Successful");
            return new ResponseEntity<>(firstBatch, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.info("Cannot get User Summary: {}", e.getMessage());
            return new ResponseEntity<>(e, HttpStatus.NOT_FOUND);
        }

    }
}
