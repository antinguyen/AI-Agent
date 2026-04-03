package com.sales.management.product;

import com.sales.management.common.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProductImageStorageService {

    private final Path productImageDir;

    public ProductImageStorageService(@Value("${app.upload.base-dir:uploads}") String uploadBaseDir) {
        this.productImageDir = Paths.get(uploadBaseDir, "products").toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessRuleException("Only image files are supported");
        }

        String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "image");
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0 && dot < originalName.length() - 1) {
            ext = originalName.substring(dot).toLowerCase(Locale.ROOT);
        }

        String storedName = UUID.randomUUID() + ext;
        Path target = productImageDir.resolve(storedName).normalize();

        try {
            Files.createDirectories(productImageDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessRuleException("Cannot store product image");
        }

        return "/uploads/products/" + storedName;
    }
}
