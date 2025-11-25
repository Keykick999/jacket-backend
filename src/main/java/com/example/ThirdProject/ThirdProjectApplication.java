package com.example.ThirdProject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ThirdProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThirdProjectApplication.class, args);
	}

}
