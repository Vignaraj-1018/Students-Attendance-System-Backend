package com.attendancesystem.students.attendancesystem.Model;


import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Document(collection = "attendance_details")
@Data
@Builder
public class AttendanceDetails {
    @Id
    public String attendanceId;
    public String userId;
    public String academicYear;
    public int semester;
    public ArrayList<SubjectList> subjectList;
}
