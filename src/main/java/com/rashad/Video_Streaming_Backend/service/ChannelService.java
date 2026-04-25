package com.rashad.Video_Streaming_Backend.service;
import com.rashad.Video_Streaming_Backend.dto.ChannelResponse;
import com.rashad.Video_Streaming_Backend.entity.Channel;
import com.rashad.Video_Streaming_Backend.entity.User;
import com.rashad.Video_Streaming_Backend.repo.ChannelRepository;
import com.rashad.Video_Streaming_Backend.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepo userRepo;

    public Channel getChannelById(String id) {
        return channelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Channel not found ❌"));
    }

    public Channel createChannel(Channel channel, String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ❗ Prevent duplicate handle
        channelRepository.findByHandleIgnoreCase(channel.getHandle().trim())
                .ifPresent(c -> {
                    throw new RuntimeException("Handle already exists ❌");
                });

        // ❗ One user = one channel
        if (channelRepository.existsByUser(user)) {
            throw new RuntimeException("User already has a channel ❌");
        }

        Channel savedChannel = Channel
                .builder()
                .name(channel.getName())
                .handle(channel.getHandle().trim())
                .description(channel.getDescription())
                .user(user)
                .avatar("")
                .banner("")
                .isVerified(true)
                .build();

        user.setChannel(savedChannel);

        System.out.println(savedChannel);

        return channelRepository.save(savedChannel);
    }
}