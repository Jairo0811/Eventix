package com.jairomatias.eventix.event.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.jairomatias.eventix.event.service.EventCoverImageStorage;
import com.jairomatias.eventix.event.service.EventCoverImageStorage.StoredEventCover;

@Controller
@RequestMapping("/events/media")
public class EventCoverImageController {

    private final EventCoverImageStorage storage;

    public EventCoverImageController(EventCoverImageStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<?> cover(@PathVariable String filename) {
        StoredEventCover stored = storage.load(filename);
        return ResponseEntity.ok()
                .contentType(stored.mediaType())
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(stored.resource());
    }
}
