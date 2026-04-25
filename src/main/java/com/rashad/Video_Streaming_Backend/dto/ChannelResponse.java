package com.rashad.Video_Streaming_Backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChannelResponse {

    private String id;
    private String name;
    private String avatar;
    private long subscribers;
}
