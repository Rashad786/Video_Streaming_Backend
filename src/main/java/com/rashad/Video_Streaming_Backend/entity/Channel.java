package com.rashad.Video_Streaming_Backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;


import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "channels")
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    private String handle;

    private String avatar;

    private String banner;

    @Builder.Default
    private long subscribers = 0;

    @Builder.Default
    private long totalViews = 0;

    @Column(length = 1000)
    private String description;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "channel")
    @JsonIgnore
    private List<Video> videos;

    @CreationTimestamp
    private LocalDate joinedDate;

    private boolean isVerified;
}
