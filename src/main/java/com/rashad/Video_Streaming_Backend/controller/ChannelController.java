package com.rashad.Video_Streaming_Backend.controller;

import com.rashad.Video_Streaming_Backend.entity.Channel;
import com.rashad.Video_Streaming_Backend.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/channels")
@CrossOrigin(origins = "*") // for frontend
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getChannel(@PathVariable String id) {
        try {
            System.out.println(id);
            Channel channel = channelService.getChannelById(id);
            return ResponseEntity.ok(channel);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Channel not found");
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createChannel(@RequestBody Channel channel, Principal principal) {
        try {
            System.out.println(channel);
            String email = principal.getName();
            return ResponseEntity.ok(channelService.createChannel(channel, email));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Channel not found");
        }
    }
}
