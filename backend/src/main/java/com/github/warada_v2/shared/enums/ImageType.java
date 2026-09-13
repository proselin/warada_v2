package com.github.warada_v2.shared.enums;

public enum ImageType {
  THUMB_IMAGE("THUMB"),
  CHAPTER_IMAGE("CHAPTER_IMAGE");

  private final String value;

  ImageType(String value) {
    this.value = value;
  }

  public static ImageType fromValue(String value) {
    for (ImageType t : values()) {
      if (t.value.equals(value)) return t;
    }
    throw new IllegalArgumentException("Unknown ImageType value: " + value);
  }

  public String getValue() {
    return value;
  }
}
