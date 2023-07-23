package com.document.validator.decodeqrmicroservice;

import nu.pattern.OpenCV;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

@SpringBootApplication
public class DecodeQrMicroserviceApplication {
    static Logger logger = LogManager.getLogger(DecodeQrMicroserviceApplication.class);

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
        SpringApplication app = new SpringApplication(DecodeQrMicroserviceApplication.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "9083"));
        app.run(args);
        OpenCV.loadShared();


        Runtime runtime = Runtime.getRuntime();

        final NumberFormat format = NumberFormat.getInstance();

        final long maxMemory = runtime.maxMemory();
        final long allocatedMemory = runtime.totalMemory();
        final long freeMemory = runtime.freeMemory();
        final long mb = 1024 * 1024;
        final String mega = " MB";

        logger.info("========================== Memory Info ==========================");
        logger.info("Free memory: " + format.format(freeMemory / mb) + mega);
        logger.info("Allocated memory: " + format.format(allocatedMemory / mb) + mega);
        logger.info("Max memory: " + format.format(maxMemory / mb) + mega);
        logger.info("Total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / mb) + mega);
        logger.info("=================================================================\n");
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("*").allowedOrigins("*");
            }
        };
    }
}
