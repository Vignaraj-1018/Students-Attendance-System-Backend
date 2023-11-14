package com.attendancesystem.students.attendancesystem.Model;


import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EmailValidate {
    public String name;
    public String mail;
    public String subject;
    public String message;
    public boolean toOther;

}
