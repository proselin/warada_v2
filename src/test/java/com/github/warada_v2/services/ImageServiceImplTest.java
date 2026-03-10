package com.github.warada_v2.services;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class ImageServiceImplTest {

    @Inject
    ImageServiceImpl imageService;

    @Test
    void testImageServiceInit() {
        assertNotNull(imageService);
    }
}
