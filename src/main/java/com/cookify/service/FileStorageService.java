package com.cookify.service;

import com.cookify.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded files under ./uploads/{category}/. Shared between
 * profile picture upload (Phase 2) and recipe image/video upload
 * (Phase 3) rather than duplicating multipart handling twice.
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of("mp4", "mov", "webm", "mkv");
    private static final Path UPLOAD_ROOT = Paths.get("uploads");

    public String storeImage(MultipartFile file, String category) {
        return store(file, category, ALLOWED_IMAGE_EXTENSIONS, "Error: Invalid image format");
    }

    public String storeVideo(MultipartFile file, String category) {
        return store(file, category, ALLOWED_VIDEO_EXTENSIONS, "Error: Invalid video format");
    }

    private String store(MultipartFile file, String category, Set<String> allowedExtensions, String errorMessage) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, errorMessage);
        }

        try {
            Path categoryDir = UPLOAD_ROOT.resolve(category);
            Files.createDirectories(categoryDir);
            String filename = UUID.randomUUID() + "." + extension.toLowerCase();
            Path target = categoryDir.resolve(filename);
            file.transferTo(target);
            return category + "/" + filename;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded file");
        }
    }
}
