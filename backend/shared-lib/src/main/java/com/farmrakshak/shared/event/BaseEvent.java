package com.farmrakshak.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String eventType;
    private String source;

    @Builder.Default
    private String timestamp = Instant.now().toString();

    private Object payload;
}
