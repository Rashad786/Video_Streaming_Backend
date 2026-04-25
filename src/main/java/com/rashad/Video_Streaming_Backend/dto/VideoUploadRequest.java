package com.rashad.Video_Streaming_Backend.dto;

import com.rashad.Video_Streaming_Backend.entity.Channel;
import com.rashad.Video_Streaming_Backend.entity.User;
import com.rashad.Video_Streaming_Backend.entity.enums.Visibility;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VideoUploadRequest {

    private String title;
    private String description;
    private Visibility visibility;
    private User user;
    private Channel channel;
    private MultipartFile videoFile;
    private String tags;
    private MultipartFile thumbnailFile;
}
