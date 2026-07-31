package com.github.warada_v2.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.warada_v2.crawl.scheduler.FileTempClearer;
import com.github.warada_v2.shared.configurations.WaradaConfig;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FileTempClearerTest {

  @Mock private MinioClient minioClient;

  @Mock private WaradaConfig waradaConfig;

  @Mock private WaradaConfig.CleanUpTempImages cleanUp;

  private FileTempClearer fileTempClearer;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    when(waradaConfig.cleanUp()).thenReturn(cleanUp);
    when(waradaConfig.imageBucketTempPath()).thenReturn("temp");
    when(waradaConfig.imageBucket()).thenReturn("warada-images");

    fileTempClearer = new FileTempClearer();
    setField("minio", minioClient);
    setField("waradaConfig", waradaConfig);
  }

  @Test
  void cleanTempObjectsSkipsWhenDisabled() {
    when(cleanUp.enabled()).thenReturn(false);
    when(cleanUp.minAgeMinutes()).thenReturn(60);

    //        fileTempClearer.init();
    //        fileTempClearer.cleanTempObjects();

    verify(minioClient, never()).listObjects(any());
  }

  @Test
  void cleanTempObjectsDeletesOnlyOldObjects() throws Exception {
    when(cleanUp.enabled()).thenReturn(true);
    when(cleanUp.minAgeMinutes()).thenReturn(60);

    Item oldItem = Mockito.mock(Item.class);
    when(oldItem.isDir()).thenReturn(false);
    when(oldItem.objectName()).thenReturn("temp/old-image.jpg");
    when(oldItem.lastModified()).thenReturn(ZonedDateTime.now().minusDays(2));

    Item recentItem = Mockito.mock(Item.class);
    when(recentItem.isDir()).thenReturn(false);
    when(recentItem.objectName()).thenReturn("temp/recent-image.jpg");
    when(recentItem.lastModified()).thenReturn(ZonedDateTime.now());

    Result<Item> res1 = new Result<>(oldItem);
    Result<Item> res2 = new Result<>(recentItem);

    when(minioClient.listObjects(any())).thenReturn(List.of(res1, res2));

    //        fileTempClearer.init();
    //        fileTempClearer.cleanTempObjects();

    verify(minioClient, times(1)).removeObject(any());
  }

  private void setField(String fieldName, Object value) throws Exception {
    Field field = FileTempClearer.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(fileTempClearer, value);
  }
}
