package com.document.validator.documentsmicroservice.controller;

import com.document.validator.documentsmicroservice.dto.GenericResponseDTO;
import com.document.validator.documentsmicroservice.dto.ValidateResponseDTO;
import com.document.validator.documentsmicroservice.dto.VerifyImageQrResponseDTO;
import com.document.validator.documentsmicroservice.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@CrossOrigin(origins = "*",maxAge =3600, allowCredentials = "false")
@RequestMapping("/document/")

public class DocumentController {
    @Autowired
    DocumentService documentService;

    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    // Added for CORS -> Not working for application level
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**"); //.allowedOrigins("https://www.alypse.ml");
    }


    @PostMapping("/sign")
    public ResponseEntity<GenericResponseDTO> sign(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("description") String description,
                                                   @RequestParam("userId") int userId) throws Exception {

        GenericResponseDTO responseDTO= documentService.sign(file,description,userId);

        if(responseDTO.getStatus()==0)
            return ResponseEntity.ok(responseDTO);
        else
            return ResponseEntity.badRequest().body(responseDTO);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateResponseDTO> validate(@RequestParam("file") MultipartFile file) throws Exception {

        ValidateResponseDTO responseDTO= documentService.validate(file);
        return ResponseEntity.ok(responseDTO);
    }

    @RequestMapping("/download/{fileName:.+}")
    public void downloadPDFResource(HttpServletRequest request, HttpServletResponse response,
                                    @PathVariable("fileName") String fileName) throws IOException {

        documentService.download(fileName,response,null);
    }

    @RequestMapping("/download/{uuid}/{fileName:.+}")
    public void downloadPDFResource(HttpServletRequest request, HttpServletResponse response,
                                    @PathVariable("fileName") String fileName,@PathVariable("uuid") String uuid) throws IOException {

        documentService.download(fileName,response,uuid);
    }

    @PostMapping("/cropImage")
    public ResponseEntity<GenericResponseDTO> cropImage(@RequestParam("file") MultipartFile file) throws Exception {

        GenericResponseDTO responseDTO= documentService.cropImage(file);

        if(responseDTO.getStatus()==0)
            return ResponseEntity.ok(responseDTO);
        else
            return ResponseEntity.badRequest().body(responseDTO);
    }

    @PostMapping("/verifyImageQR")
    public ResponseEntity<GenericResponseDTO> verifyImageQR(@RequestParam("file") MultipartFile file
    ) throws Exception {

        VerifyImageQrResponseDTO responseDTO= documentService.verifyImageQR(file);

        if(responseDTO.getStatus()==0)
            return ResponseEntity.ok(responseDTO);
        else
            return ResponseEntity.badRequest().body(responseDTO);
    }
}
