package com.rashad.Video_Streaming_Backend.repo;

import com.rashad.Video_Streaming_Backend.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepo extends JpaRepository<Video, Long> {

    Page<Video> findByChannelId(String channelId, Pageable pageable);
}
