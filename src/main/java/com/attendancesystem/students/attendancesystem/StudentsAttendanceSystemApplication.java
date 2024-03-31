package com.attendancesystem.students.attendancesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StudentsAttendanceSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudentsAttendanceSystemApplication.class, args);
	}

}
