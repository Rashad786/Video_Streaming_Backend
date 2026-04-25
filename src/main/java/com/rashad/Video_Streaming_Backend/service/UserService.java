package com.rashad.Video_Streaming_Backend.service;

import com.rashad.Video_Streaming_Backend.entity.User;
import com.rashad.Video_Streaming_Backend.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepo repo;

    public User findByEmail(String username) {
        System.out.println("Looking for user with username: '" + username + "'");
        return repo.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not Found..."));
    }
}
