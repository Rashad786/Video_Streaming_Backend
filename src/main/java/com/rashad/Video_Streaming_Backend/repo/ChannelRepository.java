package com.rashad.Video_Streaming_Backend.repo;
import com.rashad.Video_Streaming_Backend.entity.Channel;
import com.rashad.Video_Streaming_Backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, String> {

    Optional<Channel> findByHandleIgnoreCase(String handle);
    Optional<Channel> findByUser(User user);
    boolean existsByUser(User user);
}
