package com.rashad.Video_Streaming_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {

    private Long id;
    private String title;
    private String description;
    private String hlsUrl;
    private String thumbnail;
    private long views;
    private long likes;
    private LocalDateTime uploadDate;
    private List<String> tags;
    private LocalDateTime createdAt;
    private String channelId;
    private ChannelResponse channelResponse;
}
