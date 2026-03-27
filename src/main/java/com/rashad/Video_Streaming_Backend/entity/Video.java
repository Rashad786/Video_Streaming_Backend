package com.rashad.Video_Streaming_Backend.entity;

import com.rashad.Video_Streaming_Backend.entity.enums.ReactionType;
import com.rashad.Video_Streaming_Backend.entity.enums.VideoStatus;
import com.rashad.Video_Streaming_Backend.entity.enums.Visibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@RequiredArgsConstructor
@Data
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String title;
    private String description;

    @ManyToOne
    private User uploadedBy;

    private String tags;
    private String thumbnailUrl;
    private String videoUrl;
    private Visibility visibility;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private VideoStatus status;

    @OneToMany(mappedBy = "video")
    private List<Comment> comments;

    private ReactionType reactionType;
}


