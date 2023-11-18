package com.attendancesystem.students.attendancesystem.Model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectList {
    public String subjectId;
    public String name;
    public int presentCount;
    public int totalCount;
}
