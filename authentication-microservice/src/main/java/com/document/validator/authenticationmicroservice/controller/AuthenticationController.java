package com.document.validator.authenticationmicroservice.controller;

import com.document.validator.authenticationmicroservice.dto.LogonRequestDTO;
import com.document.validator.authenticationmicroservice.dto.LogonResponseDTO;
import com.document.validator.authenticationmicroservice.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/authentication/")
@CrossOrigin
public class AuthenticationController {
    @Autowired
    AuthenticationService authenticationService;

    @PostMapping("/logon")
    public ResponseEntity<LogonResponseDTO> logon(@RequestBody LogonRequestDTO request){
        LogonResponseDTO response = authenticationService.logon(request);
        if(response.getStatus()==0)
            return ResponseEntity.ok(response);
        else
            return ResponseEntity.badRequest().body(response);
    }
}
