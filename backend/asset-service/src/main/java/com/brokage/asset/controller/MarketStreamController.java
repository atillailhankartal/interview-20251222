package com.brokage.asset.controller;

import com.brokage.asset.dto.PriceUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Slf4j
public class MarketStreamController {

    private final ObjectMapper objectMapper;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * SSE endpoint for real-time price updates
     * Clients connect once and receive price updates as they happen
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPrices() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE client disconnected, {} clients remaining", emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE client timeout, {} clients remaining", emitters.size());
        });

        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("SSE client error: {}, {} clients remaining", e.getMessage(), emitters.size());
        });

        emitters.add(emitter);
        log.debug("New SSE client connected, {} total clients", emitters.size());

        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\"}"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Listen for price update events and broadcast to all connected clients
     */
    @EventListener
    public void handlePriceUpdate(PriceUpdateEvent event) {
        if (emitters.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(event.getUpdates());

            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("price-update")
                            .data(json));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }

            emitters.removeAll(deadEmitters);

            if (!deadEmitters.isEmpty()) {
                log.debug("Removed {} dead SSE connections", deadEmitters.size());
            }
        } catch (Exception e) {
            log.error("Failed to broadcast price update", e);
        }
    }
}
