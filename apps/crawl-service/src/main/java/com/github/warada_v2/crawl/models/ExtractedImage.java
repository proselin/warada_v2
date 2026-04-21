package com.github.warada_v2.crawl.models;

import java.util.List;

public record ExtractedImage(
        List<String> imageUrls,
        String fileName,
        String objectTemp,
        Integer position,
        String imageUrlPulled
) {
    public static Builder builder() {
        return new Builder();
    }

    public ExtractedImage withFileNameWithObjectWithImageUrlPulled(String fileName, String object, String imageUrlPulled) {
        return new ExtractedImage(imageUrls(), fileName, object, position(), imageUrlPulled);
    }

    public static final class Builder {
        private List<String> imageUrls;
        private String fileName;
        private String objectTemp;
        private Integer position;
        private String imageUrlPulled;

        private Builder() {}

        public Builder imageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder objectTemp(String objectTemp) { this.objectTemp = objectTemp; return this; }
        public Builder position(Integer position) { this.position = position; return this; }
        public Builder imageUrlPulled(String imageUrlPulled) { this.imageUrlPulled = imageUrlPulled; return this; }

        public ExtractedImage build() {
            return new ExtractedImage(imageUrls, fileName, objectTemp, position, imageUrlPulled);
        }
    }
}
