package com.attendancesystem.students.attendancesystem.Model;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String jwtToken;
    private UserDetails userDetails;

    public LoginResponse(UserDetails userDetails, String jwtToken) {
        this.userDetails = userDetails;
        this.jwtToken = jwtToken;
    }
}
