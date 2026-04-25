package com.rashad.Video_Streaming_Backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rashad.Video_Streaming_Backend.entity.enums.ReactionType;
import com.rashad.Video_Streaming_Backend.entity.enums.VideoStatus;
import com.rashad.Video_Streaming_Backend.entity.enums.Visibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@Setter
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String title;
    private String description;

    @ManyToOne
    @JsonIgnore
    private User uploadedBy;

    @ManyToOne
    private Channel channel;

    private String tags;
//    private MultipartFile thumbnailUrl;
    private String videoUrl;
    private Visibility visibility;
    private String contentType;
    private String filePath;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private VideoStatus status;

    private int likes;
    private int disLike;

    @OneToMany(mappedBy = "video")
    private List<Comment> comments;

    private ReactionType reactionType;
}


