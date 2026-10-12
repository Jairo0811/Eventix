package com.jairomatias.eventix.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.jairomatias.eventix.shared.exception.BusinessRuleException;

class EventCoverImageStorageTest {

    private static final byte[] PNG = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] WEBP = {
        0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50};

    @TempDir
    Path directory;

    private EventCoverImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new EventCoverImageStorage(directory.toString());
    }

    @Test
    void storesImagesWhoseContentMatchesTheDeclaredType() {
        assertThat(storage.store(file("image/png", PNG))).endsWith(".png");
        assertThat(storage.store(file("image/jpeg", JPEG))).endsWith(".jpg");
        assertThat(storage.store(file("image/webp", WEBP))).endsWith(".webp");
    }

    @Test
    void rejectsNonImageContentDeclaredAsPng() throws Exception {
        byte[] html = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> storage.store(file("image/png", html)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no es una imagen válida");
        try (var files = Files.list(directory)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void rejectsContentWhoseSignatureDoesNotMatchTheDeclaredType() {
        assertThatThrownBy(() -> storage.store(file("image/png", JPEG)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> storage.store(file("image/jpeg", WEBP)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsFilesShorterThanTheSignature() {
        assertThatThrownBy(() -> storage.store(file("image/webp", new byte[] {0x52, 0x49})))
                .isInstanceOf(BusinessRuleException.class);
    }

    private MockMultipartFile file(String contentType, byte[] content) {
        return new MockMultipartFile("cover", "cover", contentType, content);
    }
}
