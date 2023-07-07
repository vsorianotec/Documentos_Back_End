package com.document.validator.documentsmicroservice;

import nu.pattern.OpenCV;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

@SpringBootApplication
public class DocumentsMicroserviceApplication {

	public static void main(String[] args) {
		// Logger with diferente ID
		Date date = Calendar.getInstance().getTime();
		DateFormat dateFormat = new SimpleDateFormat("HHmmssSSS");
		String strDate = dateFormat.format(date);
		System.setProperty("time_ini",strDate);
		dateFormat = new SimpleDateFormat("yyyyMMdd");
		strDate = dateFormat.format(date);
		System.setProperty("date_init",strDate);

		// Started the application
		SpringApplication app = new SpringApplication(DocumentsMicroserviceApplication.class);
		app.setDefaultProperties(Collections.singletonMap("server.port", "9082"));
		app.run(args);
		OpenCV.loadShared();
	}

}
