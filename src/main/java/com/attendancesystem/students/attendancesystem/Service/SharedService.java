package com.attendancesystem.students.attendancesystem.Service;


import com.attendancesystem.students.attendancesystem.Model.AttendanceDetails;
import com.attendancesystem.students.attendancesystem.Model.EmailValidate;
import com.attendancesystem.students.attendancesystem.Model.UserDetails;
import com.mongodb.BasicDBObject;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class SharedService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${spring.smtp.host}")
    private String mailHost;

    @Value("${spring.smtp.port}")
    private int mailPort;

    @Value("${spring.smtp.username}")
    private String mailUsername;

    @Value("${spring.smtp.password}")
    private String mailPassword;

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedService.class);



    public List<UserDetails> getAllUsers(){
        return mongoTemplate.findAll(UserDetails.class);
    }

    public UserDetails getUserByUserEmail(String userEmail){
        return mongoTemplate.findOne(new Query(Criteria.where("userEmail").is(userEmail)),UserDetails.class);
    }

    public UserDetails getUserByUserId(String userId){
        return mongoTemplate.findOne(new Query(Criteria.where("userId").is(userId)),UserDetails.class);
    }

    public List<AttendanceDetails> getAttendanceByUserId(String userId){
        return mongoTemplate.find(new Query(Criteria.where("userId").is(userId)),AttendanceDetails.class);
    }

    public AttendanceDetails getSingleAttendance(String attendanceId){
        // Match operation
        AggregationOperation match = Aggregation.match(Criteria.where("_id").is(attendanceId));

        // Unwind operation
        AggregationOperation unwind = Aggregation.unwind("subjectList");

        // Project operation
        AggregationOperation project = Aggregation.project("_id", "academicYear", "semester", "lastModifiedDate")
                .and("subjectList.subjectId").as("subjectId")
                .and("subjectList.name").as("name")
                .and("subjectList.presentCount").as("presentCount")
                .and("subjectList.totalCount").as("totalCount")
                .and(
                        ConditionalOperators.when(
                                        ComparisonOperators.valueOf("subjectList.totalCount").equalToValue(0)
                                )
                                .then(0)
                                .otherwise(
                                        ArithmeticOperators.Multiply.valueOf(
                                                ArithmeticOperators.Divide.valueOf("subjectList.presentCount")
                                                        .divideBy("subjectList.totalCount")
                                        ).multiplyBy(100)
                                )
                ).as("percentage");

        // Group operation
        AggregationOperation group = Aggregation.group("_id")
                .first("academicYear").as("academicYear")
                .first("semester").as("semester")
                .first("_id").as("attendanceId")
                .sum("percentage").as("totalPercentage")
                .count().as("total")
                .first("lastModifiedDate").as("lastModifiedDate")
                .push(new BasicDBObject("name", "$name")
                        .append("presentCount", "$presentCount")
                        .append("totalCount", "$totalCount")
                        .append("percentage", "$percentage")
                        .append("subjectId", "$subjectId")
                ).as("subjectList");

        // Final Project operation
        AggregationOperation finalProject = Aggregation.project()
                .and("academicYear").as("academicYear")
                .and("semester").as("semester")
                .and("attendanceId").as("attendanceId")
                .andExpression("totalPercentage / total").as("averagePercentage")
                .and("lastModifiedDate").as("lastModifiedDate")
                .and("subjectList").as("subjectList");

        // Build the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(match, unwind, project, group, finalProject);

        // Execute the aggregation
        AggregationResults<AttendanceDetails> result = mongoTemplate.aggregate(aggregation, "attendance_details", AttendanceDetails.class);

        return result.getUniqueMappedResult();
//        return mongoTemplate.findOne(new Query(Criteria.where("attendanceId").is(attendanceId)),AttendanceDetails.class);
    }

    public AttendanceDetails checkForExistingAttendance(String userId, String academicYear, int semester){
        return mongoTemplate.findOne(getAttendanceQuery(null,userId,academicYear,semester), AttendanceDetails.class);
    }

    public DeleteResult deleteSingleAttendance(String attendanceId){
        Query query = new Query(Criteria.where("attendanceId").is(attendanceId));
        return mongoTemplate.remove(query, AttendanceDetails.class);
    }

    public Query getAttendanceQuery(String attendanceId, String userId, String academicYear, int semester){
        List<Criteria> criteriaList = new ArrayList<>();
        if(attendanceId != null){
            criteriaList.add(Criteria.where("attendanceId").is(attendanceId));
        }
        criteriaList.add(Criteria.where("userId").is(userId));
        criteriaList.add(Criteria.where("academicYear").is(academicYear));
        criteriaList.add(Criteria.where("semester").is(semester));
        Criteria criteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        return new Query(criteria);
    }

    public void upsertUser(UserDetails userDetails){
        Query query = new Query(Criteria.where("userId").is(userDetails.getUserId()).andOperator(Criteria.where("userEmail").is(userDetails.getUserEmail())));
        Document document = new Document();
        mongoTemplate.getConverter().write(userDetails, document);
        Update update = Update.fromDocument(document);
        mongoTemplate.upsert(query, update, UserDetails.class);
    }

    public void sendEmail(EmailValidate emailMsg){

        LOGGER.info("Sending Email to {}", emailMsg.getMail());
        try{
            MimeMessage mimeMessage = getJavaMailSender().createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            mimeMessage.setContent(emailMsg.getMessage(), "text/html;charset=utf-8");
            messageHelper.setFrom("noreply@baeldung.com");
            messageHelper.setSubject(emailMsg.getSubject());
            messageHelper.setTo(emailMsg.getMail());
            getJavaMailSender().send(mimeMessage);

            LOGGER.info("Email Sent Successfully: {}", emailMsg.getMail());
        }
        catch (Exception e){
            LOGGER.info("Exception in Sending Message: {}", e.getMessage());
        }

    }

    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);

        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }

    public ResponseEntity<?> contactMe(EmailValidate emailValidate) {
        LOGGER.info("Sending Email to me...");
        try{
            MimeMessage mimeMessage = getJavaMailSender().createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            mimeMessage.setContent("<div style='padding:20px;'><div><span style='font-size:20px;'> Hello from, <span style='font-size:20px; font-weight: bold;'>"+emailValidate.getName()+"("+emailValidate.getMail()+")</span></span><h3 style='font-size:20px; font-weight: normal;'>"+emailValidate.getMessage()+"</h3></div><div style='font-size:10px; border-width:1px 0px 0px 0px;border-style: solid; padding:5px; width:100%;margin-top:100px;'><span>Regards,<br/> Admin Student Attendance System</span><br/><span><a href='mailto:studattendsys@gmail.com' style='color:#FF6E31;'>studattendsys@gmail.com</a></span></div></div>", "text/html;charset=utf-8");
            messageHelper.setFrom("noreply@baeldung.com");
            messageHelper.setSubject(emailValidate.getSubject());
            messageHelper.setTo(mailUsername);
            getJavaMailSender().send(mimeMessage);

            LOGGER.info("Email Sent Successfully: ");
            return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Email Sent Successfully"));
        }
        catch (Exception e){
            LOGGER.info("Exception in Sending Message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(Map.of("message", "Exception in Sending Message"));
        }
    }
}
