package com.document.validator.documentsmicroservice;

import nu.pattern.OpenCV;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocumentsMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentsMicroserviceApplication.class, args);
		OpenCV.loadShared();
	}

}
