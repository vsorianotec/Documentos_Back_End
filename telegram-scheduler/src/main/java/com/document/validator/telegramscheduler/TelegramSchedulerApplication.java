package com.document.validator.telegramscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@SpringBootApplication
public class TelegramSchedulerApplication {

	public static void main(String[] args) {
		// Logger with diferente ID
		Date date = Calendar.getInstance().getTime();
		DateFormat dateFormat = new SimpleDateFormat("HHmmssSSS");
		String strDate = dateFormat.format(date);
		System.setProperty("time_ini",strDate);
		dateFormat = new SimpleDateFormat("yyyyMMdd");
		strDate = dateFormat.format(date);
		System.setProperty("date_init",strDate);

		SpringApplication.run(TelegramSchedulerApplication.class, args);
	}

}
