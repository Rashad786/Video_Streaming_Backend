package com.rashad.Video_Streaming_Backend.service;

import com.rashad.Video_Streaming_Backend.dto.AuthResponse;
import com.rashad.Video_Streaming_Backend.dto.VideoResponse;
import com.rashad.Video_Streaming_Backend.dto.VideoUploadRequest;
import com.rashad.Video_Streaming_Backend.entity.Channel;
import com.rashad.Video_Streaming_Backend.entity.User;
import com.rashad.Video_Streaming_Backend.entity.Video;
import com.rashad.Video_Streaming_Backend.entity.enums.VideoStatus;
import com.rashad.Video_Streaming_Backend.entity.enums.Visibility;
import com.rashad.Video_Streaming_Backend.repo.ChannelRepository;
import com.rashad.Video_Streaming_Backend.repo.UserRepo;
import com.rashad.Video_Streaming_Backend.repo.VideoRepo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

    private static final long[][] ladder = {
            { 1080, 5_000_000, 5_350_000, 7_500_000, 192_000 },
            {  720, 3_000_000, 3_210_000, 4_500_000, 128_000 },
            {  480, 1_500_000, 1_605_000, 2_250_000, 128_000 },
            {  360,   800_000,   856_000, 1_200_000,  96_000 },
    };

    @Value("${files.video}")
    String DIR;

    @Value("${files.hls.video}")
    String HLS_DIR;

    private final UserRepo userRepo;
    private final VideoRepo videoRepo;
    private final ChannelRepository channelRepo;

    @PostConstruct
    public void init() {
        File file = new File(DIR);

        try {
            Files.createDirectories(Paths.get(HLS_DIR));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(!file.exists()) {
            file.mkdir();
            System.out.println("Folder created: ");
        }else {
            System.out.println("Folder already created");
        }
    }

    public Video saveVideo(VideoUploadRequest req, Principal principal) {

        String email = principal.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Channel channel = channelRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        Video video = Video.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .visibility(req.getVisibility())
                .channel(channel)
                .uploadedBy(user)
                .tags(req.getTags())
//                .thumbnailUrl(req.getThumbnailFile())
                .status(VideoStatus.UPLOADING)
                .build();

        try {

            MultipartFile file = req.getVideoFile();
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();

            String cleanFileName = StringUtils.cleanPath(fileName);
            String cleanFolder = StringUtils.cleanPath(DIR);

            Path path = Paths.get(cleanFolder, cleanFileName);
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

            video.setContentType(contentType);
            video.setFilePath(path.toAbsolutePath().toString());

            Video savedVideo = videoRepo.save(video);

            processVideo(savedVideo.getId());

//            String thumbnailPath = generateThumbnail(video.getFilePath().toString(), HLS_DIR);

            return savedVideo;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Long processVideo(Long id) {

        Video video = videoRepo.findById(id).orElseThrow(() -> new RuntimeException("Video not Found"));
        String filePath = video.getFilePath();
        Path videoPath = Paths.get(filePath);

        try {

            Path outputPath = Paths.get(HLS_DIR, id.toString()).toAbsolutePath();
            Files.createDirectories(outputPath);

            List<String> cmd = new ArrayList<>();

            // ── Input ─────────────────────────────────────────────────────────────
            cmd.add("ffmpeg");
            cmd.add("-y");                           // overwrite output files
            cmd.add("-fflags");  cmd.add("+genpts"); // regenerate missing timestamps
            cmd.add("-i");       cmd.add(videoPath.toAbsolutePath().toString());

            // ── filter_complex — scale input to N resolutions ─────────────────────
            // Produces [v0], [v1], ... [vN] streams, one per ladder rung
            StringBuilder fc = new StringBuilder();
            for (int i = 0; i < ladder.length; i++) {
                // scale=-2:HEIGHT keeps aspect ratio and ensures even dimensions
                fc.append("[0:v]scale=-2:").append(ladder[i][0])
                        .append(":flags=lanczos[v").append(i).append("];");
            }
            fc.setLength(fc.length() - 1); // remove trailing semicolon
            cmd.add("-filter_complex"); cmd.add(fc.toString());

            // ── Per-rendition encoding ─────────────────────────────────────────────
            for (int i = 0; i < ladder.length; i++) {
                long[] r = ladder[i];

                cmd.add("-map"); cmd.add("[v" + i + "]"); // scaled video stream
                cmd.add("-map"); cmd.add("0:a");           // audio — same for all renditions

                // Video codec
                cmd.add("-c:v:" + i);       cmd.add("libx264");
                cmd.add("-preset:" + i);    cmd.add("veryfast");
                cmd.add("-pix_fmt");        cmd.add("yuv420p"); // iOS/Android compatibility

                // Bitrate control — target + ceiling + buffer
                cmd.add("-b:v:" + i);       cmd.add(r[1] + "");
                cmd.add("-maxrate:" + i);   cmd.add(r[2] + "");
                cmd.add("-bufsize:" + i);   cmd.add(r[3] + "");

                // Keyframe alignment — critical for seamless quality switching
                // At 24fps: g=48 → keyframe every 2s (= hls_time)
                cmd.add("-g:" + i);             cmd.add("48");
                cmd.add("-keyint_min:" + i);    cmd.add("48");
                cmd.add("-sc_threshold:" + i);  cmd.add("0"); // no scene-cut keyframes

                // Audio codec
                cmd.add("-c:a:" + i);   cmd.add("aac");
                cmd.add("-ar:" + i);    cmd.add("48000");
                cmd.add("-b:a:" + i);   cmd.add(r[4] + "");
            }

            // ── Timestamp normalization ───────────────────────────────────────────
            cmd.add("-vsync");             cmd.add("cfr");        // constant frame rate
            cmd.add("-avoid_negative_ts"); cmd.add("make_zero");  // shift timeline to 0

            // ── HLS muxer ─────────────────────────────────────────────────────────
            // var_stream_map binds each (video+audio) pair to one HLS output stream
            StringBuilder vsm = new StringBuilder();
            for (int i = 0; i < ladder.length; i++) {
                if (i > 0) vsm.append(" ");
                vsm.append("v:").append(i).append(",a:").append(i);
                vsm.append(",name:").append(ladder[i][0]).append("p"); // e.g. name:720p
            }

            cmd.add("-f");                  cmd.add("hls");
            cmd.add("-hls_time");           cmd.add("6");
            cmd.add("-hls_playlist_type");  cmd.add("vod");
            cmd.add("-hls_flags");          cmd.add("independent_segments");
            cmd.add("-hls_list_size");      cmd.add("0");  // all segments in playlist
            cmd.add("-master_pl_name");     cmd.add("master.m3u8");

            // %v → stream index (0,1,2...), %03d → segment counter
            cmd.add("-hls_segment_filename");
            cmd.add(outputPath.toAbsolutePath() + "/v%v/segment_%03d.ts");

            cmd.add("-var_stream_map");     cmd.add(vsm.toString());

            // Output pattern — creates v0/playlist.m3u8, v1/playlist.m3u8, etc.
            cmd.add(outputPath.toAbsolutePath() + "/v%v/playlist.m3u8");

            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Video processing failed with exit code: " + exitCode);
            }

            return id;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Video getVideoById(Long id) {
        return videoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not Found"));
    }

    public Page<Video> getVideos(int page, int size, String category) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

//        if (category != null && !category.isEmpty()) {
//            return videoRepo.findByCategory(category, pageable);
//        }
        return videoRepo.findAll(pageable);
    }

    public Page<VideoResponse> getVideosByChannel(String channelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Video> videos = videoRepo.findByChannelId(channelId, pageable);

        return videos.map(video -> VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
//                .thumbnail(video.getT())
                .createdAt(video.getCreatedAt())
                .channelId(video.getChannel().getId())
//                .channelAvatar(video.getChannel().getAvatar())
                .build());
    }


//    public String generateThumbnail(String videoPath, String outputDir) throws IOException, InterruptedException {
//
//        // Create output file path
//        String thumbnailPath = outputDir + File.separator + "thumb_" + System.currentTimeMillis() + ".jpg";
//
//        ProcessBuilder processBuilder = new ProcessBuilder(
//                "ffmpeg",
//                "-i", videoPath,
//                "-ss", "00:00:02",   // capture at 2 seconds
//                "-vframes", "1",
//                "-q:v", "2",         // high quality
//                thumbnailPath
//        );
//
//        processBuilder.redirectErrorStream(true);
//        Process process = processBuilder.start();
//
//        int exitCode = process.waitFor();
//
//        if (exitCode != 0) {
//            throw new RuntimeException("Failed to generate thumbnail. FFmpeg exit code: " + exitCode);
//        }
//
//        return thumbnailPath;
//    }

//    public void likeVideo(long id) {
//        Video video = videoRepo.findById(id)
//                .orElseThrow(() -> new BadCredentialsException("Video Not Found!!"));
//
//        video.setLike(video.getLike()+1);
//    }
}
