package com.github.warada_v2.crawl.services.impls;

import com.github.warada_v2.crawl.services.CrawlProgressBroadcaster;
import com.github.warada_v2.crawl.services.CrawlService;
import com.github.warada_v2.crawl.services.NettruyenExtractor;
import com.github.warada_v2.shared.services.TagService;
import com.github.warada_v2.crawl.models.CrawlProgressEvent;
import com.github.warada_v2.crawl.models.ExtractedNettruyenComic;
import com.github.warada_v2.crawl.models.ExtractedThumbImage;
import com.github.warada_v2.shared.configurations.WaradaConfig;
import com.github.warada_v2.shared.entities.Chapter;
import com.github.warada_v2.shared.entities.Comic;
import com.github.warada_v2.shared.entities.Image;
import com.github.warada_v2.shared.entities.Tag;
import com.github.warada_v2.shared.enums.CrawlStatus;
import com.github.warada_v2.shared.enums.ImageType;
import com.github.warada_v2.shared.exceptions.ConflictException;
import com.github.warada_v2.shared.repositories.ChapterRepository;
import com.github.warada_v2.shared.repositories.ComicRepository;
import com.github.warada_v2.shared.repositories.ImageRepository;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CrawlServiceImpl implements CrawlService {

  private static final Logger log = Logger.getLogger(CrawlServiceImpl.class);

  @Inject NettruyenExtractor nettruyenExtractor;
  @Inject MinioClient minioClient;
  @Inject WaradaConfig waradaConfig;
  @Inject ComicRepository comicRepository;
  @Inject ChapterRepository chapterRepository;
  @Inject ImageRepository imageRepository;
  @Inject TagService tagService;
  @Inject CrawlProgressBroadcaster broadcaster;

  @Override
  public void crawlNettruyenComic(String slugNId) throws Exception {
    log.infof("[CRAWL] START crawling comic slugNId=%s", slugNId);

    if (comicRepository.existsByOriginPathParams(slugNId)) {
      throw new ConflictException("Comic already exists: " + slugNId);
    }

    var extracted = nettruyenExtractor.extractComicDetail(slugNId);
    var extractedComic = extracted.comic();
    var extractedThumb = extracted.thumbImage();

    String thumbPermanentPath = extractedComic.slug() + "/thumb/" + extractedThumb.fileName();
    renameObject(waradaConfig.imageBucket(), extractedThumb.objectTemp(), thumbPermanentPath);

    List<ComicChapterInfo> chapterInfos =
        extracted.chapters().data().stream()
            .map(
                ch ->
                    new ComicChapterInfo(
                        ch.chapterName(), ch.originPathParams(), ch.originUrl(), ch.chapterNum()))
            .toList();

    List<ChapterStub> chapterStubs =
        QuarkusTransaction.requiringNew()
            .call(
                () ->
                    persistComic(
                        extractedComic, extractedThumb, thumbPermanentPath, chapterInfos, slugNId));

    log.infof(
        "[CRAWL] Persisted comic slug=%s, crawling %d chapters",
        extractedComic.slug(), chapterStubs.size());

    for (ChapterStub stub : chapterStubs) {
      broadcaster.emit(
          extractedComic.slug(),
          new CrawlProgressEvent(
              CrawlProgressEvent.EventType.PROCESSING,
              "Processing chapter " + stub.chapterNumber(),
              new CrawlProgressEvent.ChapterProgress(
                  stub.chapterId(), stub.chapterNumber(), null, extractedComic.slug())));
      try {
        crawlChapterInternal(stub);
        broadcaster.emit(
            extractedComic.slug(),
            new CrawlProgressEvent(
                CrawlProgressEvent.EventType.COMPLETED,
                "Chapter crawled successfully",
                new CrawlProgressEvent.ChapterProgress(
                    stub.chapterId(), stub.chapterNumber(), null, extractedComic.slug())));
      } catch (Exception e) {
        log.errorf(e, "[CRAWL] Failed to crawl chapter chapterId=%d", stub.chapterId());
        broadcaster.emit(
            extractedComic.slug(),
            new CrawlProgressEvent(
                CrawlProgressEvent.EventType.FAILED,
                "Failed: " + e.getMessage(),
                new CrawlProgressEvent.ChapterProgress(
                    stub.chapterId(), stub.chapterNumber(), null, extractedComic.slug())));
      }
    }
  }

  @Override
  public void crawlChapterById(Long chapterId) throws Exception {
    Chapter chapter = chapterRepository.findByIdWithComic(chapterId).orElse(null);
    if (chapter == null) {
      log.warnf("[CRAWL] Chapter not found: chapterId=%d", chapterId);
      return;
    }
    crawlChapterInternal(
        new ChapterStub(
            chapter.getId(),
            chapter.getComic().getSlug(),
            chapter.getChapterNumber(),
            chapter.getOriginPathParams()));
  }

  @Override
  public void retryFailedChapters(String comicSlug) {
    log.infof("[CRAWL] retryFailedChapters comicSlug=%s", comicSlug);
    Comic comic = comicRepository.findBySlug(comicSlug).orElse(null);
    if (comic == null) {
      log.warnf("[CRAWL] Comic not found for retry: slug=%s", comicSlug);
      return;
    }
    List<Chapter> failedChapters =
        chapterRepository.list("comic = ?1 AND crawlingStatus = ?2", comic, CrawlStatus.INIT);
    log.infof("[CRAWL] Found %d chapters to retry for comic=%s", failedChapters.size(), comicSlug);
    for (Chapter chapter : failedChapters) {
      try {
        crawlChapterInternal(
            new ChapterStub(
                chapter.getId(),
                comicSlug,
                chapter.getChapterNumber(),
                chapter.getOriginPathParams()));
      } catch (Exception e) {
        log.errorf(e, "[CRAWL] Retry failed for chapterId=%d", chapter.getId());
      }
    }
  }

  private void crawlChapterInternal(ChapterStub stub) {
    log.infof(
        "[CRAWL] Crawling images for chapterId=%d comicSlug=%s",
        stub.chapterId(), stub.comicSlug());

    var detail = nettruyenExtractor.extractChapterDetail(stub.originPathParams());

    List<ImageInfo> imageInfos = new ArrayList<>();

    detail.images().parallelStream()
        .forEach(
            raw -> {
              try {
                String newPath =
                    stub.comicSlug() + "/chapters/" + stub.chapterNumber() + "/" + raw.fileName();
                renameObject(waradaConfig.imageBucket(), raw.objectTemp(), newPath);
                synchronized (imageInfos) {
                  imageInfos.add(
                      new ImageInfo(raw.fileName(), newPath, raw.imageUrlPulled(), raw.position()));
                }
              } catch (Exception e) {
                log.errorf(
                    e,
                    "[CRAWL] Failed to process image for chapterId=%d fileName=%s",
                    stub.chapterId(),
                    raw.fileName());
                throw new RuntimeException(e);
              }
            });

    QuarkusTransaction.requiringNew().run(() -> persistChapterImages(stub.chapterId(), imageInfos));
    log.infof(
        "[CRAWL] Chapter finished chapterId=%d images=%d", stub.chapterId(), imageInfos.size());
  }

  private List<ChapterStub> persistComic(
      ExtractedNettruyenComic extractedComic,
      ExtractedThumbImage extractedThumb,
      String thumbPermanentPath,
      List<ComicChapterInfo> chapterInfos,
      String originPathParams) {

    if (comicRepository.existsByOriginPathParams(originPathParams)) {
      log.warnf("[CRAWL] Comic already persisted, skipping: slug=%s", extractedComic.slug());
      return List.of();
    }

    List<Tag> tags = tagService.resolveOrCreateTags(extractedComic.genre());

    Comic comic =
        Comic.builder()
            .slug(extractedComic.slug())
            .title(extractedComic.name())
            .author(extractedComic.author() != null ? extractedComic.author().name() : null)
            .description(extractedComic.description())
            .originId(extractedComic.comicId() != null ? extractedComic.comicId().toString() : null)
            .originUrl(extractedComic.url())
            .originPathParams(originPathParams)
            .status("OnGoing")
            .crawlingStatus(CrawlStatus.FINISHED)
            .tags(tags)
            .chapterCount(chapterInfos.size())
            .build();

    if (extractedThumb != null) {
      Image thumb =
          Image.builder()
              .fileName(extractedThumb.fileName())
              .filePath(thumbPermanentPath)
              .originUrl(extractedThumb.originUrl())
              .position(0)
              .type(ImageType.THUMB_IMAGE)
              .comic(comic)
              .build();
      comic.setThumbImage(thumb);
    }

    List<Chapter> chapters =
        chapterInfos.stream()
            .map(
                ch ->
                    Chapter.builder()
                        .chapterNumber(ch.chapterName())
                        .position(ch.chapterNum())
                        .originUrl(ch.originUrl())
                        .originPathParams(ch.originPathParams())
                        .crawlingStatus(CrawlStatus.INIT)
                        .comic(comic)
                        .build())
            .toList();
    comic.setChapters(chapters);

    comicRepository.persistAndFlush(comic);
    tagService.incrementComicCount(tags);

    log.infof(
        "[CRAWL] Persisted comic id=%d slug=%s with %d chapters",
        comic.getId(), comic.getSlug(), chapters.size());

    return chapters.stream()
        .map(
            ch ->
                new ChapterStub(
                    ch.getId(), comic.getSlug(), ch.getChapterNumber(), ch.getOriginPathParams()))
        .toList();
  }

  private void persistChapterImages(Long chapterId, List<ImageInfo> imageInfos) {
    Chapter chapter = chapterRepository.findByIdOptional(chapterId).orElse(null);
    if (chapter == null) {
      log.errorf("[CRAWL] Chapter not found chapterId=%d", chapterId);
      throw new IllegalStateException("Chapter not found: " + chapterId);
    }

    if (chapter.getCrawlingStatus() == CrawlStatus.FINISHED) {
      log.warnf("[CRAWL] Chapter already finished, skipping chapterId=%d", chapterId);
      return;
    }

    for (ImageInfo imgInfo : imageInfos) {
      Image img =
          Image.builder()
              .chapter(chapter)
              .fileName(imgInfo.fileName())
              .filePath(imgInfo.filePath())
              .originUrl(imgInfo.originUrl())
              .position(imgInfo.position())
              .type(ImageType.CHAPTER_IMAGE)
              .build();
      imageRepository.persist(img);
    }

    chapter.setCrawlingStatus(CrawlStatus.FINISHED);
    chapterRepository.persistOrUpdate(chapter);

    log.infof("[CRAWL] Chapter finished chapterId=%d images=%d", chapterId, imageInfos.size());
  }

  private void renameObject(String bucket, String sourcePath, String destPath) throws Exception {
    minioClient.copyObject(
        CopyObjectArgs.builder()
            .bucket(bucket)
            .object(destPath)
            .source(CopySource.builder().bucket(bucket).object(sourcePath).build())
            .build());
    minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(sourcePath).build());
  }

  // Internal helper records
  private record ChapterStub(
      Long chapterId, String comicSlug, String chapterNumber, String originPathParams) {}

  private record ComicChapterInfo(
      String chapterName, String originPathParams, String originUrl, int chapterNum) {}

  private record ImageInfo(String fileName, String filePath, String originUrl, int position) {}
}
