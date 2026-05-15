package com.Application.SocietyManagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableMongoRepositories
@EnableScheduling
@EnableAsync
public class SocietyManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(SocietyManagementApplication.class, args);
	}

}
