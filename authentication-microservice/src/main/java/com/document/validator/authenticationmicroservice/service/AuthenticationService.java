package com.document.validator.authenticationmicroservice.service;

import com.document.validator.authenticationmicroservice.dto.LogonRequestDTO;
import com.document.validator.authenticationmicroservice.dto.LogonResponseDTO;
import com.document.validator.authenticationmicroservice.entity.User;
import com.document.validator.authenticationmicroservice.repository.UserRepository;
import com.document.validator.authenticationmicroservice.utils.CriptographySimuladorHsm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    CriptographySimuladorHsm cripto;

    public LogonResponseDTO logon(LogonRequestDTO request){
        LogonResponseDTO response=new LogonResponseDTO();
        User user=userRepository.findByEmail(request.getEmail());
        if(user==null){
            response.setStatus(1);
            response.setCodeError("AUTH001");
            response.setMsgError("Usuario no existe");
            return response;
        }

        if(user.getPassword().equals(cripto.encriptar(request.getPassword()))){
            response.setUser(user);
            response.setStatus(0);
            response.setCodeError("AUTH000");
            response.setMsgError("OK");
        }else{
            response.setStatus(1);
            response.setCodeError("AUTH002");
            response.setMsgError("Password incorrecto");
        }
        return response;
    }

}
