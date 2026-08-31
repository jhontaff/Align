package com.jet.align;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlignApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlignApplication.class, args);
	}

}
