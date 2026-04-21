package com.github.warada_v2.services;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.commons.enums.ImageType;
import com.github.warada_v2.configurations.WaradaConfig;
import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.db_entities.Image;
import com.github.warada_v2.db_entities.Tag;
import com.github.warada_v2.exceptions.ConflictException;
import com.github.warada_v2.models.ChapterCrawlMessage;
import com.github.warada_v2.repositories.ChapterRepository;
import com.github.warada_v2.repositories.ComicRepository;
import com.github.warada_v2.repositories.ImageRepository;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class CrawlServiceImpl implements CrawlService {

    private static final Logger log = Logger.getLogger(CrawlServiceImpl.class);

    @Inject
    private ComicRepository comicRepository;

    @Inject
    private NettruyenExtractor nettruyenExtractor;

    @Inject
    private MinioClient minioClient;

    @Inject
    private WaradaConfig waradaConfig;

    @Inject
    private ChapterRepository chapterRepository;

    @Inject
    private ImageRepository imageRepository;

    @Inject
    private TagService tagService;

    @Inject
    private ChapterCrawlProducer chapterCrawlProducer;

    @Inject
    private CrawlProgressBroadcaster broadcaster;

    @Inject
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    /**
     * @param slugNId slug and id of nettruyen comic
     */
    @Transactional
    public void crawlNettruyenComic(String slugNId) throws Exception {
        log.info("START Crawling Nettruyen Comic from " + slugNId);
        // Check is comic is existed or not
        if (comicRepository.existedBySlugAndComicId(slugNId)) {
            throw new ConflictException("Crawling comic from " + slugNId + " already exists");
        }

        try {
            var extracted = nettruyenExtractor.extractComicDetail(slugNId);

            var extractedComic = extracted.comic();

            // Resolve tags: normalize (lowercase), reuse existing or create new
            List<Tag> resolvedTags = tagService.resolveOrCreateTags(extractedComic.genre());

            Comic comic =
                    Comic.builder()
                            .slug(extractedComic.slug())
                            .author(extractedComic.author().name())
                            .title(extractedComic.name())
                            .description(extractedComic.description())
                            .originId(extractedComic.comicId().toString())
                            .status("OnGoing")
                            .originUrl(extractedComic.url())
                            .originPathParams(slugNId)
                            .crawlingStatus(CrawlStatus.FINISHED)
                            .tags(resolvedTags)
                            .build();

            // Init thumb Image
            var extractedImage = extracted.thumbImage();
            String newPath = generateNewThumbImageObject(extractedImage.fileName());

            // Rename objectTemp to newer path
            renameObject(waradaConfig.imageBucket(), extractedImage.objectTemp(), newPath);
            Image image =
                    Image.builder()
                            .comic(comic)
                            .filePath(newPath)
                            .fileName(extractedImage.fileName())
                            .originUrl(extractedImage.originUrl())
                            .position(0)
                            .type(ImageType.THUMB_IMAGE)
                            .build();

            comic.setThumbImage(image);

            List<Chapter> chapters =
                    extracted.chapters().data().stream()
                            .map(
                                    chapter ->
                                            Chapter.builder()
                                                    .crawlingStatus(CrawlStatus.INIT)
                                                    .originUrl(chapter.originUrl())
                                                    .chapterNumber(chapter.chapterName())
                                                    .position(chapter.chapterNum())
                                                    .originPathParams(chapter.originPathParams())
                                                    .comic(comic)
                                                    .build())
                            .toList();

            comic.setChapters(chapters);
            comic.setChapterCount(chapters.size());

            comicRepository.persistAndFlush(comic);

            // Update comic count on each resolved tag
            tagService.incrementComicCount(resolvedTags);

            List<ChapterCrawlMessage> messages = comic.getChapters().stream()
                    .map(ch -> new ChapterCrawlMessage(ch.getId(), slugNId))
                    .toList();
            queueChaptersAfterCommit(slugNId, messages);

            log.info("DONE Crawling Nettruyen Comic from " + slugNId);
        } catch (Exception e) {
            log.error("ERROR Crawling Nettruyen Comic from " + slugNId);
            throw e;
        }
    }

    @Transactional
    public void crawlNettruyenChapter(Long chapterId) {
        log.infof("[START] Crawling individual chapter with ID: %s", chapterId);
        Chapter chapter = chapterRepository.findByIdOptional(chapterId).orElse(null);

        if (chapter == null) {
            log.errorf("Chapter with id %s not found", chapterId);
            throw new RuntimeException("Chapter with id " + chapterId + " not found");
        }
        if (chapter.getCrawlingStatus() == CrawlStatus.FINISHED) {
            log.infof("Chapter with id %s is already crawled", chapterId);
            return;
        }
        crawlChapter(chapter);
        log.infof("[DONE] Crawling individual chapter with ID: %s", chapterId);
    }

    @Transactional
    public void crawlChapter(Chapter chapter) {
        if (chapter.getCrawlingStatus() != CrawlStatus.INIT) {
            log.warnf("Chapter with id %s is already crawled", chapter.getId());
            return;
        }
        // Set newer status
        chapter.setCrawlingStatus(CrawlStatus.STARTED);
        chapterRepository.persistOrUpdate(chapter);
        var detail = nettruyenExtractor.extractChapterDetail(chapter.getOriginPathParams());

        log.infof("Pull chapter detail:= %s", detail);

        var images =
                detail.images().stream()
                        .map(
                                raw -> {
                                    try {
                                        String newPath = generateNewImageObject(raw.fileName());
                                        renameObject(waradaConfig.imageBucket(), raw.objectTemp(), newPath);
                                        return Image.builder()
                                                .chapter(chapter)
                                                .filePath(newPath)
                                                .fileName(raw.fileName())
                                                .originUrl(raw.imageUrlPulled())
                                                .position(raw.position())
                                                .type(ImageType.CHAPTER_IMAGE)
                                                .build();

                                    } catch (Exception e) {
                                        log.errorf(e, "ERROR Crawling image Chapter from " + raw);
                                        throw new RuntimeException(e);
                                    }
                                })
                        .collect(Collectors.toList());

        chapter.setImages(images);
        chapter.setCrawlingStatus(CrawlStatus.FINISHED);
        imageRepository.persist(images);
        chapterRepository.persistOrUpdate(chapter);
    }

    private void renameObject(String bucket, String oldName, String newName) throws Exception {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucket)
                        .object(newName)
                        .source(CopySource.builder().bucket(bucket).object(oldName).build())
                        .build());
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(oldName).build());
    }

    private String generateNewThumbImageObject(String fileName) {
        return waradaConfig.imageBucketImagesPath() + "/" + fileName;
    }

    private String generateNewImageObject(String fileName) {
        return waradaConfig.imageBucketImagesPath() + "/" + fileName;
    }

    @Override
    @Transactional
    public void retryFailedChapters(String comicSlug) {
        log.infof("[START] Retrying failed chapters for comic: %s", comicSlug);

        if (broadcaster.isActive(comicSlug)) {
            throw new ConflictException("Comic " + comicSlug + " already has an active crawl job in the queue");
        }

        Comic comic = comicRepository.findBySlugAndComicId(comicSlug).orElse(null);
        if (comic == null) {
            throw new NotFoundException("Comic not found: " + comicSlug);
        }

        List<Chapter> pendingChapters = chapterRepository.findByComicSlugAndStatusIn(
                comicSlug, CrawlStatus.INIT, CrawlStatus.STARTED);

        if (pendingChapters.isEmpty()) {
            log.infof("No failed/pending chapters to retry for comic: %s", comicSlug);
            return;
        }

        // Reset STARTED chapters back to INIT for re-processing
        for (Chapter chapter : pendingChapters) {
            if (chapter.getCrawlingStatus() == CrawlStatus.STARTED) {
                chapter.setCrawlingStatus(CrawlStatus.INIT);
                chapterRepository.persistOrUpdate(chapter);
            }
        }

        List<ChapterCrawlMessage> messages = pendingChapters.stream()
                .map(ch -> new ChapterCrawlMessage(ch.getId(), comicSlug))
                .toList();

        queueChaptersAfterCommit(comicSlug, messages);
        log.infof("[DONE] Queued %d chapters for retry for comic: %s", messages.size(), comicSlug);
    }

    private void queueChaptersAfterCommit(String comicSlug, List<ChapterCrawlMessage> messages) {
        if (messages.isEmpty()) {
            chapterCrawlProducer.sendAll(comicSlug, messages);
            return;
        }

        try {
            transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == Status.STATUS_COMMITTED) {
                        chapterCrawlProducer.sendAll(comicSlug, messages);
                        return;
                    }

                    log.warnf("Skipping queue publish for comic %s because transaction completed with status %d",
                            comicSlug, status);
                }
            });
        } catch (IllegalStateException e) {
            log.warnf("No active transaction found while queueing comic %s, publishing immediately", comicSlug);
            chapterCrawlProducer.sendAll(comicSlug, messages);
        }
    }
}
