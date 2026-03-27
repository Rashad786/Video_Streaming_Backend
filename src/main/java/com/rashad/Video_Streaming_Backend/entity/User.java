package com.rashad.Video_Streaming_Backend.entity;

import com.rashad.Video_Streaming_Backend.entity.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)          // null for OAuth2 users
    private String password;

    private String imageUrl;          // profile pic from Google

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "uploadedBy")
    private List<Video> videos;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
