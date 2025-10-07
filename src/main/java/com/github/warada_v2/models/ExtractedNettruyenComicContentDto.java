package com.github.warada_v2.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedNettruyenComicContentDto {

    @JsonProperty("@context")
    private String context;

    @JsonProperty("@type")
    private String type;

    private String name;
    private String alternateName;
    private String url;
    private String image;
    private Author author;
    private List<String> genre;
    private String description;
    private String datePublished;
    private String dateModified;
    private Long comicId;
    private String slug;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        @JsonProperty("@type")
        public String type;
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Publisher {
        @JsonProperty("@type")
        public String type;
        public String name;
        public String url;
        public Logo logo;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Logo {
            @JsonProperty("@type")
            public String type;
            public String url;
        }
    }


    @Override
    public String toString() {
        return "ComicSeriesDTO{" +
                "name='" + name + '\'' +
                ", author=" + (author != null ? author.name : null) +
                ", genre=" + genre +
                ", url='" + url + '\'' +
                '}';
    }
}
