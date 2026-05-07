package com.bibliotech.storage;

import com.bibliotech.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageProperties props;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(props.getUploadDir()));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le répertoire de stockage", e);
        }
    }

    public String store(MultipartFile file, String isbn) {
        validateFile(file);
        try {
            String filename = isbn + "_" + UUID.randomUUID() + ".pdf";
            Path target = Paths.get(props.getUploadDir()).resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("PDF stocké : {}", filename);
            return target.toString();
        } catch (IOException e) {
            throw new BusinessException("Erreur lors du stockage du PDF : " + e.getMessage());
        }
    }

    public byte[] load(String filePath) {
        try {
            Path path = Paths.get(filePath).normalize();
            if (!Files.exists(path)) {
                throw new BusinessException("Fichier PDF introuvable");
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new BusinessException("Erreur lecture PDF : " + e.getMessage());
        }
    }

    public void delete(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath).normalize());
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier : {}", filePath);
        }
    }

    public long size(String filePath) {
        try {
            return Files.size(Paths.get(filePath).normalize());
        } catch (IOException e) {
            return 0L;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new BusinessException("Le fichier PDF est requis");
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf"))
            throw new BusinessException("Seuls les fichiers PDF sont acceptés");
        if (file.getSize() > 50L * 1024 * 1024)
            throw new BusinessException("Le fichier PDF ne peut pas dépasser 50 Mo");
    }
}