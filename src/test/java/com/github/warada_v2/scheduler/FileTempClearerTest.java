package com.github.warada_v2.scheduler;

import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class FileTempClearerTest {

    @InjectMock
    MinioClient minioClient;

    @Inject
    FileTempClearer fileTempClearer;

    @Test
    void testCleanTempObjects() throws Exception {
//        // Create mock results
//        Item oldItem = Mockito.mock(Item.class);
//        when(oldItem.isDir()).thenReturn(false);
//        when(oldItem.objectName()).thenReturn("old-image.jpg");
//        when(oldItem.lastModified()).thenReturn(ZonedDateTime.now().minusDays(2));
//
//        Item recentItem = Mockito.mock(Item.class);
//        when(recentItem.isDir()).thenReturn(false);
//        when(recentItem.objectName()).thenReturn("recent-image.jpg");
//        when(recentItem.lastModified()).thenReturn(ZonedDateTime.now());
//
//        Result<Item> res1 = new Result<>(oldItem);
//        Result<Item> res2 = new Result<>(recentItem);
//
//        when(minioClient.listObjects(any())).thenReturn(List.of(res1, res2));
//
//        // Let's invoke the method
//        fileTempClearer.cleanTempObjects();
//
//        // Verify removeObject was attempted
//        Mockito.verify(minioClient, Mockito.atLeastOnce()).removeObject(any());
    }
}
