package com.github.warada_v2.crawl.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@RegisterForReflection
public record ExtractedNettruyenComic(
    @JsonProperty("@context") String context,
    @JsonProperty("@type") String type,
    String name,
    String alternateName,
    String url,
    String image,
    Author author,
    List<String> genre,
    String description,
    String datePublished,
    String dateModified,
    Long comicId,
    String slug) {
  public ExtractedNettruyenComic withSlugAndComicId(String slug, Long comicId) {
    return new ExtractedNettruyenComic(
        context(),
        type(),
        name(),
        alternateName(),
        url(),
        image(),
        author(),
        genre(),
        description(),
        datePublished(),
        dateModified(),
        comicId,
        slug);
  }

  @Override
  public String toString() {
    return "ExtractedNettruyenComic{"
        + "name='"
        + name
        + '\''
        + ", author="
        + (author != null ? author.name() : null)
        + ", genre="
        + genre
        + ", url='"
        + url
        + '\''
        + '}';
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Author(@JsonProperty("@type") String type, String name) {}
}
