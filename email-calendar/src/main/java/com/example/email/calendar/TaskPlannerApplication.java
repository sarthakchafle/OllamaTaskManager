package com.example.email.calendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class TaskPlannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskPlannerApplication.class, args);
	}
	// Configure WebClient as a bean for making HTTP requests
	@Bean
	public WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}
}
