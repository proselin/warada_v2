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


@Getter
@Setter
@Table(name = "comics")
@Builder
@Entity
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Comic {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comic_id_seq")
    @SequenceGenerator(name = "comic_id_seq", sequenceName = "comic_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "title")
    private String title;

    @Column(name = "chapter_count")
    private Integer chapterCount;

    @Column(name = "origin_id", unique = true)
    private String originId;

    @Column(name = "origin_path_params", unique = true)
    private String originPathParams;

    @Column(nullable = false)
    private String status;

    @Column(name = "description")
    private String description;

    @Column(name = "origin_url", unique = true)
    private String originUrl;

    @Column()
    private String tags;

    @Column()
    private String author;

    @Column(name = "slug", unique = true, nullable = false, length = 200)
    private String slug;

    @Convert(converter = CrawlStatusConverter.class)
    @Column(name = "crawling_status", nullable = false)
    private CrawlStatus crawlingStatus;


    @JoinColumn(name = "thumb_image_id")
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Image thumbImage;

    @OneToMany(targetEntity = Chapter.class, mappedBy = "comic", orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Chapter> chapters;

}

