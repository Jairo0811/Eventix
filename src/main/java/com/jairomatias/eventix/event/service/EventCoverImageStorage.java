package com.jairomatias.eventix.event.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jairomatias.eventix.shared.exception.BusinessRuleException;

@Service
public class EventCoverImageStorage {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
    private static final String PUBLIC_PREFIX = "/events/media/";
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp");

    private final Path storagePath;

    public EventCoverImageStorage(
            @Value("${app.event-cover.storage-path:data/event-covers}")
            String storagePath) {
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        validate(file);
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String filename = UUID.randomUUID() + EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        Path target = resolve(filename);

        try {
            Files.createDirectories(storagePath);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return PUBLIC_PREFIX + filename;
        } catch (IOException exception) {
            throw new BusinessRuleException("No fue posible guardar la portada del evento.");
        }
    }

    public StoredEventCover load(String filename) {
        Path file = resolve(filename);
        if (!Files.isRegularFile(file)) {
            throw new BusinessRuleException("La portada solicitada no existe.");
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            return new StoredEventCover(resource, mediaTypeFor(filename));
        } catch (IOException exception) {
            throw new BusinessRuleException("No fue posible leer la portada del evento.");
        }
    }

    public void deleteManaged(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }

        String filename = publicUrl.substring(PUBLIC_PREFIX.length());
        try {
            Files.deleteIfExists(resolve(filename));
        } catch (IOException ignored) {
            // La limpieza de una portada anterior no debe invalidar la operación principal.
        }
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("La portada no puede superar 5 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null
                || !EXTENSIONS_BY_CONTENT_TYPE.containsKey(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException("La portada debe ser JPG, PNG o WEBP.");
        }
        if (!hasExpectedSignature(file, contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException("El archivo no es una imagen válida del tipo indicado.");
        }
    }

    /** El Content-Type lo controla el cliente: se comprueban los primeros bytes reales. */
    private boolean hasExpectedSignature(MultipartFile file, String contentType) {
        byte[] header = new byte[12];
        int read;
        try (InputStream input = file.getInputStream()) {
            read = input.readNBytes(header, 0, header.length);
        } catch (IOException exception) {
            return false;
        }
        return switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE -> read >= 3
                    && startsWith(header, 0, new int[] {0xFF, 0xD8, 0xFF});
            case MediaType.IMAGE_PNG_VALUE -> read >= 8
                    && startsWith(header, 0, new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "image/webp" -> read >= 12
                    && startsWith(header, 0, new int[] {0x52, 0x49, 0x46, 0x46})
                    && startsWith(header, 8, new int[] {0x57, 0x45, 0x42, 0x50});
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, int offset, int[] expected) {
        for (int i = 0; i < expected.length; i++) {
            if ((data[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private Path resolve(String filename) {
        Path resolved = storagePath.resolve(filename).normalize();
        if (!resolved.startsWith(storagePath)) {
            throw new BusinessRuleException("Nombre de portada inválido.");
        }
        return resolved;
    }

    private MediaType mediaTypeFor(String filename) {
        String normalized = filename.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (normalized.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }

    public record StoredEventCover(Resource resource, MediaType mediaType) {
    }
}
