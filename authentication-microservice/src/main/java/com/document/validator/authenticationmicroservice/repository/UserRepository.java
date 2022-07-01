package com.document.validator.authenticationmicroservice.repository;

import com.document.validator.authenticationmicroservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {

    User findByEmail(String email);

}
