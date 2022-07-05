package com.example.demo.controller;

import com.example.demo.dto.GenericResponseDTO;
import com.example.demo.dto.LogonRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authentication/")
@CrossOrigin
public class DemoController {

    @PostMapping("/logon")
    public ResponseEntity<GenericResponseDTO> logon(@RequestBody LogonRequestDTO request){
        GenericResponseDTO response=new GenericResponseDTO();
        response.setStatus(0);
        response.setCodeError("DEMO000");
        response.setMsgError("OK");
        if(response.getStatus()==0)
            return ResponseEntity.ok(response);
        else
            return ResponseEntity.badRequest().body(response);
    }
}
