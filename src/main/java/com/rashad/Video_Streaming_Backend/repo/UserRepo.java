package com.rashad.Video_Streaming_Backend.repo;

import com.rashad.Video_Streaming_Backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String userName);
    Optional<User> findByEmail(String email);
}
