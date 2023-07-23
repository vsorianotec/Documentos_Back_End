package com.document.validator.decodeqrmicroservice.controller;

import com.document.validator.decodeqrmicroservice.dto.GenericResponseDTO;
import com.document.validator.decodeqrmicroservice.dto.GetFirstImageVideoRequest;
import com.document.validator.decodeqrmicroservice.dto.SingVideoRequest;
import com.document.validator.decodeqrmicroservice.dto.VerifyImageQrResponseDTO;
import com.document.validator.decodeqrmicroservice.service.DecodeQrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@RestController
@CrossOrigin(origins = "*",maxAge =3600, allowCredentials = "false")
@RequestMapping("/decodeqr/")
public class DecodeQrController {

    @Autowired
    DecodeQrService decodeQrService;

    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    // Added for CORS -> Not working for application level
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**"); //.allowedOrigins("https://www.alypse.ml");
    }

    @PostMapping("/verifyImageQR")
    public ResponseEntity<GenericResponseDTO> verifyImageQR(@RequestParam("file") MultipartFile file
                                                   ) throws Exception {

        VerifyImageQrResponseDTO responseDTO= decodeQrService.verifyImageQR(file);
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/addQRVideo")
    public ResponseEntity<GenericResponseDTO> addQRVideo(@RequestBody SingVideoRequest request){
        GenericResponseDTO response = decodeQrService.addQRVideo(request);
        if(response.getStatus()==0)
            return ResponseEntity.ok(response);
        else
            return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/getFirstImageVideo")
    public ResponseEntity<GenericResponseDTO> getFirstImageVideo(@RequestBody GetFirstImageVideoRequest request){
        GenericResponseDTO response = decodeQrService.getFirstImageVideo(request);
        if(response.getStatus()==0)
            return ResponseEntity.ok(response);
        else
            return ResponseEntity.badRequest().body(response);
    }
}
