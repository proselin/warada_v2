package com.github.warada_v2.commons;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UtilsTest {

    @Test
    void testGetExtensionFromUrl() {
        assertEquals(".png", Utils.getExtensionFromUrl("http://example.com/image.png"));
        assertEquals(".jpg", Utils.getExtensionFromUrl("http://example.com/image.jpg?width=100"));
        assertEquals(".jpeg", Utils.getExtensionFromUrl("image.jpeg"));
        assertEquals(".com/image", Utils.getExtensionFromUrl("http://example.com/image")); 
        assertEquals(".jpg", Utils.getExtensionFromUrl("no_extension")); // default
    }

    @Test
    void testConstant() {
        Constant constant = new Constant();
        assertNotNull(constant);
    }
}
