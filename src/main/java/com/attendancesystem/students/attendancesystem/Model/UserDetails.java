package com.attendancesystem.students.attendancesystem.Model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_details")
@Data
@Builder
public class UserDetails {

    @Id
    public String userId;
    public String name;
    public String userEmail;
    public String password;
    public boolean authenticated;
    public String OTP;
}
