package com.github.warada_v2.db_entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.warada_v2.commons.enums.ImageType;
import com.github.warada_v2.converter.ImageTypeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "images")
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "image_id_seq")
    @SequenceGenerator(name = "image_id_seq", sequenceName = "image_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "file_name", unique = false, nullable = false)
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column()
    private Integer position;

    @Convert(converter = ImageTypeConverter.class)
    @Column(name = "type")
    private ImageType type;

    @Column(name = "origin_url", unique = true)
    private String originUrl;

    @OneToOne(mappedBy = "thumbImage")
    @JsonIgnore
    private Comic comic;

    @JoinColumn(name = "chapter_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Chapter chapter;
}

