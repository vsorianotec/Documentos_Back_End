package com.document.validator.usersmicroservice.service;

import com.document.validator.usersmicroservice.entity.User;
import com.document.validator.usersmicroservice.repository.UserRepository;
import com.document.validator.usersmicroservice.utils.CriptographySimuladorHsm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    CriptographySimuladorHsm cripto;


    public List<User> getAll(){
        return  userRepository.findAll();
    }

    public User getUserById(int id){
        return  userRepository.findById(id).orElse(null);
    }

    public User save(User user){
        user.setPassword(cripto.encriptar("Digital_123"));
        User userNew = userRepository.save(user);
        return userNew;
    }
}
