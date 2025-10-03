package com.github.warada_v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "chapters")
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chapter_id_seq")
    @SequenceGenerator(name = "chapter_id_seq", sequenceName = "chapter_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(
            name= "chapter_num"
    )
    private String chapterNumber;

    @Column(
            name = "source_url"
    )
    private String sourceUrl;

    @Column(
            name="type"
    )
    private String type;

    @Column(name = "position")
    private Integer position;

    @Column(name="crawling_status")
    private String crawlingStatus;

    @JoinColumn(name="comic_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Comic comic;

    @OneToMany(mappedBy = "chapter", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Image> images = new ArrayList<>();
}




