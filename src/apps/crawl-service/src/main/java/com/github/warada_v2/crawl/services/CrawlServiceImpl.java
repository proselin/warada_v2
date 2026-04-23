package com.github.warada_v2.crawl.services;

import com.github.warada_v2.crawl.client.CoreServiceClient;
import com.github.warada_v2.crawl.configurations.WaradaConfig;
import com.github.warada_v2.crawl.models.*;
import com.github.warada_v2.crawl.producers.ChapterImagesExtractedProducer;
import com.github.warada_v2.crawl.producers.ComicExtractedProducer;
import com.github.warada_v2.shared.exceptions.ConflictException;
import com.github.warada_v2.shared.models.events.ChapterImagesExtractedEvent;
import com.github.warada_v2.shared.models.events.ChapterPendingEvent;
import com.github.warada_v2.shared.models.events.ComicExtractedEvent;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CrawlServiceImpl implements CrawlService {

    private static final Logger log = Logger.getLogger(CrawlServiceImpl.class);

    @Inject NettruyenExtractor nettruyenExtractor;
    @Inject MinioClient minioClient;
    @Inject WaradaConfig waradaConfig;
    @Inject ComicExtractedProducer comicExtractedProducer;
    @Inject ChapterImagesExtractedProducer chapterImagesExtractedProducer;
    @Inject CrawlProgressBroadcaster broadcaster;

    @Inject
    @RestClient
    CoreServiceClient coreServiceClient;

    @Override
    public void crawlNettruyenComic(String slugNId) throws Exception {
        log.infof("[CRAWL] START crawling comic slugNId=%s", slugNId);

        try {
            Response response = coreServiceClient.comicExists(slugNId);
            if (response.getStatus() == 200) {
                throw new ConflictException("Comic already exists: " + slugNId);
            }
        } catch (Exception e) {}

        var extracted = nettruyenExtractor.extractComicDetail(slugNId);
        var extractedComic = extracted.comic();
        var extractedThumb = extracted.thumbImage();

        String thumbPermanentPath = extractedComic.slug() + "/thumb/" + extractedThumb.fileName();
        renameObject(waradaConfig.imageBucket(), extractedThumb.objectTemp(), thumbPermanentPath);

        List<ComicExtractedEvent.ChapterInfo> chapterInfos = extracted.chapters().data().stream()
                .map(ch -> new ComicExtractedEvent.ChapterInfo(
                        ch.chapterName(),
                        ch.originPathParams(),
                        ch.originUrl(),
                        ch.chapterNum()))
                .toList();

        ComicExtractedEvent event = new ComicExtractedEvent(
                extractedComic.slug(),
                extractedComic.name(),
                extractedComic.author() != null ? extractedComic.author().name() : null,
                extractedComic.description(),
                extractedComic.comicId() != null ? extractedComic.comicId().toString() : null,
                extractedComic.url(),
                slugNId,
                "OnGoing",
                extractedComic.genre(),
                new ComicExtractedEvent.ThumbImageInfo(extractedThumb.originUrl(), extractedThumb.fileName(), thumbPermanentPath),
                chapterInfos
        );

        comicExtractedProducer.send(event);
        log.infof("[CRAWL] Published ComicExtractedEvent slug=%s chapters=%d", extractedComic.slug(), chapterInfos.size());
    }

    @Override
    public void crawlChapterById(Long chapterId) throws Exception {
        log.infof("[CRAWL] crawlChapterById is a no-op in new architecture; use ChapterPendingConsumer");
    }

    @Override
    public void retryFailedChapters(String comicSlug) {
        log.infof("[CRAWL] retryFailedChapters comicSlug=%s — not implemented in new architecture", comicSlug);
    }

    public void crawlChapterFromEvent(ChapterPendingEvent event) {
        log.infof("[CRAWL] Crawling images for chapterId=%d comicSlug=%s", event.chapterId(), event.comicSlug());

        var detail = nettruyenExtractor.extractChapterDetail(event.originPathParams());

        List<ChapterImagesExtractedEvent.ImageInfo> imageInfos = new ArrayList<>();

        detail.images().parallelStream().forEach(raw -> {
            try {
                String newPath = event.comicSlug() + "/chapters/" + event.chapterNum() + "/" + raw.fileName();
                renameObject(waradaConfig.imageBucket(), raw.objectTemp(), newPath);
                synchronized (imageInfos) {
                    imageInfos.add(new ChapterImagesExtractedEvent.ImageInfo(
                            raw.fileName(), newPath, raw.imageUrlPulled(), raw.position()
                    ));
                }
            } catch (Exception e) {
                log.errorf(e, "[CRAWL] Failed to process image for chapterId=%d fileName=%s", event.chapterId(), raw.fileName());
                throw new RuntimeException(e);
            }
        });

        ChapterImagesExtractedEvent imagesEvent = new ChapterImagesExtractedEvent(
                event.chapterId(), event.comicSlug(), event.chapterNum(), imageInfos
        );
        chapterImagesExtractedProducer.send(imagesEvent);
        log.infof("[CRAWL] Published ChapterImagesExtractedEvent chapterId=%d images=%d", event.chapterId(), imageInfos.size());
    }

    private void renameObject(String bucket, String sourcePath, String destPath) throws Exception {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucket)
                        .object(destPath)
                        .source(CopySource.builder().bucket(bucket).object(sourcePath).build())
                        .build()
        );
        minioClient.removeObject(
                RemoveObjectArgs.builder().bucket(bucket).object(sourcePath).build()
        );
    }
}
