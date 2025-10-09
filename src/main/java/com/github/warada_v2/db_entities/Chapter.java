package com.github.warada_v2.db_entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.converter.CrawlStatusConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Entity
@Table(name = "chapters")
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
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

    @Column(name = "chapter_num")
    private String chapterNumber;

    @Column(name = "source_url", unique = true)
    private String sourceUrl;

    @Column(name = "origin_path_params", unique = true)
    private String originPathParams;

    @Column(name = "type")
    private String type;

    @Column(name = "position")
    private Integer position;

    @Convert(converter = CrawlStatusConverter.class)
    @Column(name = "crawling_status", nullable = false)
    private CrawlStatus crawlingStatus;

    @JoinColumn(name = "comic_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Comic comic;

    @OneToMany(mappedBy = "chapter", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Image> images;
}




