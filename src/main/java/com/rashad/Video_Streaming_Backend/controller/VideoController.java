package com.rashad.Video_Streaming_Backend.controller;

import com.rashad.Video_Streaming_Backend.dto.ChannelResponse;
import com.rashad.Video_Streaming_Backend.dto.VideoResponse;
import com.rashad.Video_Streaming_Backend.dto.VideoUploadRequest;
import com.rashad.Video_Streaming_Backend.entity.Channel;
import com.rashad.Video_Streaming_Backend.entity.Video;
import com.rashad.Video_Streaming_Backend.entity.enums.Visibility;
import com.rashad.Video_Streaming_Backend.service.ChannelService;
import com.rashad.Video_Streaming_Backend.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Value("${files.hls.video}")
    private String HLS_DIR;

    private final VideoService service;
    private final ChannelService channelService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@ModelAttribute VideoUploadRequest videoUploadRequest, Principal principal) {
        Video savedVideo = service.saveVideo(videoUploadRequest, principal);

        if (savedVideo != null) {
            return ResponseEntity.ok(savedVideo);
        } else {
            return ResponseEntity.internalServerError().body("Video not uploaded");
        }
    }

    @GetMapping("/{videoId}/metadata")
    public VideoResponse getVideoMetadata(@PathVariable Long videoId) {
        Video video = service.getVideoById(videoId);

        Channel channel = video.getChannel();

        // Build channel info
        ChannelResponse channelResponse = ChannelResponse
                .builder()
                .id(channel.getId())
                .name(channel.getName())
                .avatar(channel.getAvatar())
                .subscribers(channel.getSubscribers())
                .build();

        // Build video info
        VideoResponse videoResponse = new VideoResponse();
        videoResponse.setId(videoId);
        videoResponse.setTitle(video.getTitle());
        videoResponse.setDescription(video.getDescription());
        videoResponse.setHlsUrl("http://localhost:8080/video/" + videoId + "/master.m3u8"); // HLS URL
        videoResponse.setThumbnail("");
        videoResponse.setViews(0);
        videoResponse.setLikes(video.getLikes());
        videoResponse.setUploadDate(video.getCreatedAt());
        videoResponse.setTags(List.of(video.getTags()));
        videoResponse.setChannelResponse(channelResponse);

        System.out.println(videoResponse);

        return videoResponse;
    }

    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<Resource> serveMasterFile(@PathVariable String videoId) {

        Path path = Paths.get(HLS_DIR, videoId, "master.m3u8").toAbsolutePath();

        if (!Files.exists(path)) {
            System.out.println(path);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl"
                )
                .body(resource);
    }

    @GetMapping("/{videoId}/**")
    public ResponseEntity<Resource> serveSegments(
            @PathVariable String videoId,
            HttpServletRequest request) {

        String fullPath = request.getRequestURI();
        String prefix = "/video/" + videoId + "/";
        String segment = fullPath.substring(fullPath.indexOf(prefix) + prefix.length());

        logger.info("Request URI: {}", fullPath);
        logger.info("Extracted segment path: {}", segment);

        if (segment.contains("..")) {
            logger.warn("Blocked potential path traversal attack: {}", segment);
            return ResponseEntity.badRequest().build();
        }

        try {
            Path filePath = Paths.get(HLS_DIR, videoId).resolve(segment);
            logger.info("Resolved file path: {}", filePath.toAbsolutePath());

            if (!Files.exists(filePath)) {
                logger.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            boolean isPlaylist = segment.endsWith(".m3u8");
            boolean isSegment = segment.endsWith(".ts");

            String contentType = isPlaylist ? "application/vnd.apple.mpegurl"
                    : isSegment ? "video/MP2T"
                    : "application/octet-stream";

            String cacheControl = isSegment
                    ? "public, max-age=31536000, immutable"
                    : "no-cache, no-store";

            logger.info("Serving file with Content-Type: {}", contentType);

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Length", String.valueOf(Files.size(filePath)))
                    .header("Cache-Control", cacheControl)
                    .body(new InputStreamResource(Files.newInputStream(filePath)));

        } catch (Exception e) {
            logger.error("Error serving segment file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET /video?page=0&size=8&category=Music
    @GetMapping
    public ResponseEntity<?> getVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String category) {

        Page<Video> videos = service.getVideos(page, size, category);
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/channel/{channelId}")
    public Page<VideoResponse> like(@PathVariable String channelId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "8") int size) {
        return service.getVideosByChannel(channelId, page, size);
    }
}
