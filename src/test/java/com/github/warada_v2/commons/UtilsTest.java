package com.github.warada_v2.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.warada_v2.shared.ez.Utils;
import org.junit.jupiter.api.Test;

class UtilsTest {

  @Test
  void testGetExtensionFromUrl() {
    assertEquals(".png", Utils.getExtensionFromUrl("http://example.com/image.png"));
    assertEquals(".jpg", Utils.getExtensionFromUrl("http://example.com/image.jpg?width=100"));
    assertEquals(".jpeg", Utils.getExtensionFromUrl("image.jpeg"));
    assertEquals(".com/image", Utils.getExtensionFromUrl("http://example.com/image"));
    assertEquals(".jpg", Utils.getExtensionFromUrl("no_extension")); // default
  }
}
