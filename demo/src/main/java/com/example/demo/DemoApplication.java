package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
	// Add the controller.
}

@RestController
class HelloWorldController {
	@GetMapping("/")
	public String hello() {
		String directoryName = System.getProperty("user.dir");
		System.out.println("Current Working Directory is = " +directoryName);
		return "Directiorio: " + directoryName;
	}
}