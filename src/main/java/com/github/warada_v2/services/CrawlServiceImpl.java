package com.github.warada_v2.services;

import com.github.warada_v2.commons.Constant;
import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.commons.enums.ImageType;
import com.github.warada_v2.configurations.WaradaConfig;
import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.db_entities.Image;
import com.github.warada_v2.exceptions.ConflictException;
import com.github.warada_v2.repositories.ChapterRepository;
import com.github.warada_v2.repositories.ComicRepository;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
@ApplicationScoped
public class CrawlServiceImpl implements CrawlService {

    @Inject
    ComicRepository comicRepository;

    @Inject
    NettruyenExtractor nettruyenExtractor;

    @Inject
    MinioClient minioClient;

    @Inject
    WaradaConfig waradaConfig;

    @Inject
    ChapterRepository chapterRepository;

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
            var extracted = nettruyenExtractor.extract(slugNId);

                var extractedComic = extracted.comic();
                Comic comic = Comic.builder()
                        .slug(extractedComic.slug())
                        .author(extractedComic.author().name())
                        .tags(Arrays.toString(extractedComic.genre().toArray()))
                        .title(extractedComic.name())
                        .description(extractedComic.description())
                        .originId(extractedComic.comicId().toString())
                        .status("OnGoing")
                        .originUrl(extractedComic.url())
                        .originPathParams(slugNId)
                        .crawlingStatus(CrawlStatus.FINISHED)
                        .build();

            // Init thumb Image
            var extractedImage = extracted.thumbImage();
            String newPath = generateNewThumbImageObject(extractedImage.fileName());

            //Rename object to newer path
            renameObject(
                    waradaConfig.imageBucket(),
                    extractedImage.objectTemp(),
                    newPath
            );
            Image image = Image.builder()
                    .comic(comic)
                    .filePath(newPath)
                    .fileName(extractedImage.fileName())
                    .originUrl(extractedImage.originUrl())
                    .position(0)
                    .type(ImageType.THUMB_IMAGE)
                    .build();

            comic.setThumbImage(image);

            List<Chapter> chapters = extracted.chapters().data().stream().map(
                    chapter -> Chapter.builder()
                            .crawlingStatus(CrawlStatus.INIT)
                            .sourceUrl(chapter.originPathParams())
                            .chapterNumber(chapter.chapterName())
                            .position(chapter.chapterNum())
                            .originPathParams(chapter.originPathParams())
                            .comic(comic)
                            .build()
            ).toList();

            comic.setChapters(chapters);
            comic.setChapterCount(chapters.size());

            comicRepository.persistAndFlush(comic);
            log.info("DONE Crawling Nettruyen Comic from " + slugNId);
        } catch (Exception e) {
            log.error("ERROR Crawling Nettruyen Comic from "+ slugNId);
            throw e;
        }

    }

    @Transactional
    public Chapter crawlNettruyenChapter(Long chapterId) {
        log.infof("[START] Crawling individual chapter with ID: {}", chapterId);
        Chapter chapter = chapterRepository.findByIdOptional(chapterId).orElse(null);

        if (chapter == null) {
            log.errorf("Chapter with id {} not found", chapterId);
            throw new RuntimeException("Chapter with id " + chapterId + " not found");
        }
        if(Objects.equals(chapter.getCrawlingStatus(), CrawlStatus.FINISHED.getCode())) {
            log.infof("Chapter with id {} is already crawled", chapterId);
            return chapter;
        }

//        var res =  crawlChapter(chapter);
        log.infof("[DONE] Crawling individual chapter with ID: {}", chapterId);
//        return res;
        return null;
    }



    private void renameObject(String bucket, String oldName, String newName) throws Exception {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucket)
                        .object(newName)
                        .source(
                                CopySource.builder()
                                        .bucket(bucket)
                                        .object(oldName)
                                        .build()
                        ).build()
        );
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(oldName)
                        .build()
        );
    }

    private String generateNewThumbImageObject(String fileName) {
        return waradaConfig.imageBucketImagesPath() + "/" + fileName;
    }

}
