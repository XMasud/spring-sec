package com.example.auth_service.service;

import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.entity.User;
import com.example.auth_service.exceptions.EmailAlreadyExistsException;
import com.example.auth_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest registerRequest){

        if(userRepository.existsByEmail(registerRequest.email())){
            throw new EmailAlreadyExistsException(registerRequest.email());
        }

        User user = new User();
        user.setFullName(registerRequest.fullName());
        user.setEmail(registerRequest.email());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.password()));
        user.setRoles("USER");

        return userRepository.save(user);

    }
}
