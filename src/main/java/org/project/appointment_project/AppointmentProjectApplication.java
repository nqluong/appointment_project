package org.project.appointment_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppointmentProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppointmentProjectApplication.class, args);
	}

}
