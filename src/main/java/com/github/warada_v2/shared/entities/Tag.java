package com.github.warada_v2.shared.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tags")
public class Tag extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tag_id_seq")
  @SequenceGenerator(name = "tag_id_seq", sequenceName = "tag_id_seq", allocationSize = 1)
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "normalized_name", nullable = false, unique = true)
  private String normalizedName;

  @Column(name = "comic_count", nullable = false)
  private Integer comicCount = 0;

  @Column(name = "updated_at", nullable = false)
  @UpdateTimestamp
  private LocalDateTime updatedAt;

  @Column(name = "created_at", updatable = false, nullable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;

  @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
  @JsonIgnore
  private List<Comic> comics = new ArrayList<>();

  public Tag() {}

  public Tag(
      Long id,
      String name,
      String normalizedName,
      Integer comicCount,
      LocalDateTime updatedAt,
      LocalDateTime createdAt,
      List<Comic> comics) {
    this.id = id;
    this.name = name;
    this.normalizedName = normalizedName;
    this.comicCount = comicCount != null ? comicCount : 0;
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
    this.comics = comics != null ? comics : new ArrayList<>();
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNormalizedName() {
    return normalizedName;
  }

  public void setNormalizedName(String normalizedName) {
    this.normalizedName = normalizedName;
  }

  public Integer getComicCount() {
    return comicCount;
  }

  public void setComicCount(Integer comicCount) {
    this.comicCount = comicCount;
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

  public List<Comic> getComics() {
    return comics;
  }

  public void setComics(List<Comic> comics) {
    this.comics = comics;
  }

  @Override
  public String toString() {
    return "Tag{"
        + "id="
        + id
        + ", name='"
        + name
        + '\''
        + ", normalizedName='"
        + normalizedName
        + '\''
        + ", comicCount="
        + comicCount
        + '}';
  }

  public static final class Builder {
    private Long id;
    private String name;
    private String normalizedName;
    private Integer comicCount = 0;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private List<Comic> comics = new ArrayList<>();

    private Builder() {}

    public Builder id(Long id) {
      this.id = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder normalizedName(String normalizedName) {
      this.normalizedName = normalizedName;
      return this;
    }

    public Builder comicCount(Integer comicCount) {
      this.comicCount = comicCount;
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

    public Builder comics(List<Comic> comics) {
      this.comics = comics;
      return this;
    }

    public Tag build() {
      return new Tag(id, name, normalizedName, comicCount, updatedAt, createdAt, comics);
    }
  }
}
