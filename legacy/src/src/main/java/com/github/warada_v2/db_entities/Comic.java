package com.github.warada_v2.db_entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.converter.CrawlStatusConverter;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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

@Entity
@Table(name = "comics")
public class Comic extends PanacheEntityBase {

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

    public Comic() {
    }

    public Comic(Long id, LocalDateTime updatedAt, LocalDateTime createdAt, String title, Integer chapterCount,
                 String originId, String originPathParams, String status, String description, String originUrl,
                 String author, List<Tag> tags, String slug, CrawlStatus crawlingStatus, Image thumbImage,
                 List<Chapter> chapters) {
        this.id = id;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
        this.title = title;
        this.chapterCount = chapterCount;
        this.originId = originId;
        this.originPathParams = originPathParams;
        this.status = status;
        this.description = description;
        this.originUrl = originUrl;
        this.author = author;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.slug = slug;
        this.crawlingStatus = crawlingStatus;
        this.thumbImage = thumbImage;
        this.chapters = chapters;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(Integer chapterCount) {
        this.chapterCount = chapterCount;
    }

    public String getOriginId() {
        return originId;
    }

    public void setOriginId(String originId) {
        this.originId = originId;
    }

    public String getOriginPathParams() {
        return originPathParams;
    }

    public void setOriginPathParams(String originPathParams) {
        this.originPathParams = originPathParams;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public CrawlStatus getCrawlingStatus() {
        return crawlingStatus;
    }

    public void setCrawlingStatus(CrawlStatus crawlingStatus) {
        this.crawlingStatus = crawlingStatus;
    }

    public Image getThumbImage() {
        return thumbImage;
    }

    public void setThumbImage(Image thumbImage) {
        this.thumbImage = thumbImage;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }

    @Override
    public String toString() {
        return "Comic{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", chapterCount=" + chapterCount +
                ", originId='" + originId + '\'' +
                ", originPathParams='" + originPathParams + '\'' +
                ", status='" + status + '\'' +
                ", description='" + description + '\'' +
                ", originUrl='" + originUrl + '\'' +
                ", author='" + author + '\'' +
                ", slug='" + slug + '\'' +
                ", crawlingStatus=" + crawlingStatus +
                '}';
    }

    public static final class Builder {
        private Long id;
        private LocalDateTime updatedAt;
        private LocalDateTime createdAt;
        private String title;
        private Integer chapterCount;
        private String originId;
        private String originPathParams;
        private String status;
        private String description;
        private String originUrl;
        private String author;
        private List<Tag> tags = new ArrayList<>();
        private String slug;
        private CrawlStatus crawlingStatus;
        private Image thumbImage;
        private List<Chapter> chapters;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder chapterCount(Integer chapterCount) {
            this.chapterCount = chapterCount;
            return this;
        }

        public Builder originId(String originId) {
            this.originId = originId;
            return this;
        }

        public Builder originPathParams(String originPathParams) {
            this.originPathParams = originPathParams;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder originUrl(String originUrl) {
            this.originUrl = originUrl;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder crawlingStatus(CrawlStatus crawlingStatus) {
            this.crawlingStatus = crawlingStatus;
            return this;
        }

        public Builder thumbImage(Image thumbImage) {
            this.thumbImage = thumbImage;
            return this;
        }

        public Builder chapters(List<Chapter> chapters) {
            this.chapters = chapters;
            return this;
        }

        public Comic build() {
            return new Comic(id, updatedAt, createdAt, title, chapterCount, originId, originPathParams, status,
                    description, originUrl, author, tags, slug, crawlingStatus, thumbImage, chapters);
        }
    }
}

