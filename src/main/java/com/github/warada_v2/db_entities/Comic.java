package com.github.warada_v2.db_entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.commons.enums.UpdateSchedule;
import com.github.warada_v2.converter.CrawlStatusConverter;
import com.github.warada_v2.converter.UpdateScheduleConverter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


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
    private String author;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "comic_tags",
            joinColumns = @JoinColumn(name = "comic_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

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

    @Convert(converter = UpdateScheduleConverter.class)
    @Column(name = "update_on", nullable = false)
    @Builder.Default
    private UpdateSchedule updateOn = UpdateSchedule.NONE;

    @Column(name = "fixed_time_to_next_update", nullable = false)
    @Builder.Default
    private Integer fixedTimeToNextUpdate = 0;

    @Column(name = "next_update_date")
    private LocalDateTime nextUpdateDate;

    @Column(name = "last_update_date")
    private LocalDateTime lastUpdateDate;

}

