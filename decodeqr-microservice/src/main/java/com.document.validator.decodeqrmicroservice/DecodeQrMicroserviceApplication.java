package com.document.validator.decodeqrmicroservice;

import org.opencv.core.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DecodeQrMicroserviceApplication {

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) {
        SpringApplication.run(DecodeQrMicroserviceApplication.class, args);
    }

}
