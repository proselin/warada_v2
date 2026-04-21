package com.github.warada_v2.shared.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.warada_v2.shared.enums.ImageType;
import com.github.warada_v2.shared.converter.ImageTypeConverter;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "images")
public class Image extends PanacheEntityBase {

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

    public Image() {
    }

    public Image(Long id, LocalDateTime updatedAt, LocalDateTime createdAt, String fileName, String filePath,
                 Integer position, ImageType type, String originUrl, Comic comic, Chapter chapter) {
        this.id = id;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
        this.fileName = fileName;
        this.filePath = filePath;
        this.position = position;
        this.type = type;
        this.originUrl = originUrl;
        this.comic = comic;
        this.chapter = chapter;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public ImageType getType() {
        return type;
    }

    public void setType(ImageType type) {
        this.type = type;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public Comic getComic() {
        return comic;
    }

    public void setComic(Comic comic) {
        this.comic = comic;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    @Override
    public String toString() {
        return "Image{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", position=" + position +
                ", type=" + type +
                ", originUrl='" + originUrl + '\'' +
                '}';
    }

    public static final class Builder {
        private Long id;
        private LocalDateTime updatedAt;
        private LocalDateTime createdAt;
        private String fileName;
        private String filePath;
        private Integer position;
        private ImageType type;
        private String originUrl;
        private Comic comic;
        private Chapter chapter;

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

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder position(Integer position) {
            this.position = position;
            return this;
        }

        public Builder type(ImageType type) {
            this.type = type;
            return this;
        }

        public Builder originUrl(String originUrl) {
            this.originUrl = originUrl;
            return this;
        }

        public Builder comic(Comic comic) {
            this.comic = comic;
            return this;
        }

        public Builder chapter(Chapter chapter) {
            this.chapter = chapter;
            return this;
        }

        public Image build() {
            return new Image(id, updatedAt, createdAt, fileName, filePath, position, type, originUrl, comic, chapter);
        }
    }
}
