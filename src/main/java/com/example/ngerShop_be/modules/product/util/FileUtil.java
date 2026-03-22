package com.example.ngerShop_be.modules.product.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class FileUtil {

    private final Path rootDir;

    public FileUtil(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.rootDir = Paths.get(uploadDir);
        // Tự động tạo thư mục khi khởi chạy ứng dụng nếu chưa có
        try {
            if (!Files.exists(this.rootDir)) {
                Files.createDirectories(this.rootDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path dest = rootDir.resolve(filename);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename; // Trả về đường dẫn để lưu vào DB
        } catch (IOException e) {
            throw new RuntimeException("Upload failed", e);
        }
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            // Lấy tên file từ URL (Ví dụ: /uploads/abc.jpg -> abc.jpg)
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path filePath = rootDir.resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log lỗi nhưng không chặn luồng chính (tùy nhu cầu)
            System.err.println("Could not delete file: " + e.getMessage());
        }
    }
}
