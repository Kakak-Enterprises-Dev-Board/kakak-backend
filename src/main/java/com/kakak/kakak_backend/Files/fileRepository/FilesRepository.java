package com.kakak.kakak_backend.Files.fileRepository;

import com.kakak.kakak_backend.Files.fileEntity.files;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FilesRepository extends JpaRepository<files, UUID> {
    @Query("select f from files f where f.file_key = :fileKey")
    Optional<files> findByFileKey(@Param("fileKey") String fileKey);

    @Query("select count(f) > 0 from files f where f.file_key = :fileKey")
    boolean existsByFileKey(@Param("fileKey") String fileKey);
}
