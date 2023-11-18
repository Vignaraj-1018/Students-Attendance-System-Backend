package com.attendancesystem.students.attendancesystem.Service;


import com.attendancesystem.students.attendancesystem.Model.AttendanceDetails;
import com.attendancesystem.students.attendancesystem.Model.UserDetails;
import com.mongodb.client.result.DeleteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SharedService {

    @Autowired
    private MongoTemplate mongoTemplate;

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
        return mongoTemplate.findOne(new Query(Criteria.where("attendanceId").is(attendanceId)),AttendanceDetails.class);
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

}
