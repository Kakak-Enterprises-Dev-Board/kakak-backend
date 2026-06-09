package com.kakak.kakak_backend.Files.fileEntity;


import com.kakak.kakak_backend.authentication.authEntity.AuthUsers;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigInteger;
import java.sql.Timestamp;

import java.util.UUID;

@Entity
@Table(name = "files")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class files {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @ManyToOne
        @JoinColumn(nullable = false)
        private AuthUsers uploaded_by;

        @Column(nullable = false)
        private String bucket_name;

        @Column(nullable = false)
        private String file_name;

        @Column(nullable = false, columnDefinition = "TEXT")
        private String file_key;

        @Column(nullable = false)
        private String mime_type;

        @Column(nullable = false)
        private BigInteger file_size;

        @Column(nullable = false)
        private String public_url;

        @Column(nullable = false)
        private String module;

        @CreationTimestamp
        @Column(nullable = false)
        private Timestamp uploaded_at;

}
