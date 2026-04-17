package com.github.warada_v2.db_entities;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.converter.CrawlStatusConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "chapters")
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

    @Column(name = "origin_url", unique = true)
    private String originUrl;

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

    public Chapter() {
    }

    public Chapter(Long id, LocalDateTime updatedAt, LocalDateTime createdAt, String chapterNumber, String originUrl,
                   String originPathParams, String type, Integer position, CrawlStatus crawlingStatus, Comic comic,
                   List<Image> images) {
        this.id = id;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
        this.chapterNumber = chapterNumber;
        this.originUrl = originUrl;
        this.originPathParams = originPathParams;
        this.type = type;
        this.position = position;
        this.crawlingStatus = crawlingStatus;
        this.comic = comic;
        this.images = images;
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

    public String getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(String chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public String getOriginPathParams() {
        return originPathParams;
    }

    public void setOriginPathParams(String originPathParams) {
        this.originPathParams = originPathParams;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public CrawlStatus getCrawlingStatus() {
        return crawlingStatus;
    }

    public void setCrawlingStatus(CrawlStatus crawlingStatus) {
        this.crawlingStatus = crawlingStatus;
    }

    public Comic getComic() {
        return comic;
    }

    public void setComic(Comic comic) {
        this.comic = comic;
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    @Override
    public String toString() {
        return "Chapter{" +
                "id=" + id +
                ", chapterNumber='" + chapterNumber + '\'' +
                ", originUrl='" + originUrl + '\'' +
                ", originPathParams='" + originPathParams + '\'' +
                ", type='" + type + '\'' +
                ", position=" + position +
                ", crawlingStatus=" + crawlingStatus +
                '}';
    }

    public static final class Builder {
        private Long id;
        private LocalDateTime updatedAt;
        private LocalDateTime createdAt;
        private String chapterNumber;
        private String originUrl;
        private String originPathParams;
        private String type;
        private Integer position;
        private CrawlStatus crawlingStatus;
        private Comic comic;
        private List<Image> images;

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

        public Builder chapterNumber(String chapterNumber) {
            this.chapterNumber = chapterNumber;
            return this;
        }

        public Builder originUrl(String originUrl) {
            this.originUrl = originUrl;
            return this;
        }

        public Builder originPathParams(String originPathParams) {
            this.originPathParams = originPathParams;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder position(Integer position) {
            this.position = position;
            return this;
        }

        public Builder crawlingStatus(CrawlStatus crawlingStatus) {
            this.crawlingStatus = crawlingStatus;
            return this;
        }

        public Builder comic(Comic comic) {
            this.comic = comic;
            return this;
        }

        public Builder images(List<Image> images) {
            this.images = images;
            return this;
        }

        public Chapter build() {
            return new Chapter(id, updatedAt, createdAt, chapterNumber, originUrl, originPathParams, type, position,
                    crawlingStatus, comic, images);
        }
    }
}

