package com.github.warada_v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@Table(name = "comics")
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

    @Column(name= "title")
    private String title;

    @Column(name="chapter_count")
    private Integer chapterCount;

    @Column(name = "origin_id")
    private String originId;

    @Column(nullable = false)
    private String status;

    @Column(name="description")
    private String description;

    @Column(name="origin_url")
    private String originUrl;

    @Column()
    private String tags;

    @Column()
    private String author;

    @Column(
            name = "slug",
            unique = true,
            nullable = false,
            length = 200
    )
    private String slug;

    @Column(
            name = "crawling_status",
            nullable = false
    )
    private String crawlingStatus;


    @JoinColumn(name="thumb_image_id")
    @OneToOne
    private Image thumbImage;

    @OneToMany(
            targetEntity = Chapter.class,
            mappedBy = "comic",
            orphanRemoval = true,
            cascade = CascadeType.ALL
    )
    @JsonIgnore
    private List<Chapter> chapters = new ArrayList<>();

}

