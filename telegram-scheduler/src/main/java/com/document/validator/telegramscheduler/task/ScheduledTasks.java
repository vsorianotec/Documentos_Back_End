package com.document.validator.telegramscheduler.task;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class ScheduledTasks {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Value("${app.microservice.domain}")
    private String domain;

    Logger logger = LogManager.getLogger(getClass());



    @Scheduled(fixedRate = 5000)
    public void reportCurrentTime() {
        logger.info("domain: " + domain );
        logger.info("The time is now " + dateFormat.format(new Date()));
    }
}