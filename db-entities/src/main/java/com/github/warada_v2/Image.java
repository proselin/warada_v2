package com.github.warada_v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "images")
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

    @Column(name= "file_name")
    private String fileName;

    @Column()
    private Integer position;

    @Column()
    private String type;

    @Column(name = "origin_url")
    private String originUrl;

    @OneToOne(mappedBy = "thumbImage")
    @JsonIgnore
    private Comic comic;

    @JoinColumn(name = "chapter_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Chapter chapter;
}
