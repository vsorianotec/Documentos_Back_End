package com.document.validator.documentsmicroservice.controller;

import com.document.validator.documentsmicroservice.dto.GenericResponseDTO;
import com.document.validator.documentsmicroservice.dto.ValidateResponseDTO;
import com.document.validator.documentsmicroservice.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/document/")
@CrossOrigin
public class DocumentController {
    @Autowired
    DocumentService documentService;


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

        if(responseDTO.getStatus()==0)
            return ResponseEntity.ok(responseDTO);
        else
            return ResponseEntity.badRequest().body(responseDTO);
    }

    @RequestMapping("/download/{fileName:.+}")
    public void downloadPDFResource(HttpServletRequest request, HttpServletResponse response,
                                    @PathVariable("fileName") String fileName) throws IOException {

        documentService.download(fileName,response);
    }

}
