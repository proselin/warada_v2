package com.github.warada_v2.services;

import com.github.warada_v2.commons.enums.CrawlStatus;
import com.github.warada_v2.commons.enums.ImageType;
import com.github.warada_v2.configurations.WaradaConfig;
import com.github.warada_v2.db_entities.Chapter;
import com.github.warada_v2.db_entities.Comic;
import com.github.warada_v2.db_entities.Image;
import com.github.warada_v2.exceptions.ConflictException;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    /**
     * @param slugNId slug and id of nettruyen comic
     */
    @Transactional
    public void crawlNettruyenComic(String slugNId) throws Exception {
        log.info("START Crawling Nettruyen Comic from {}", slugNId);
        // Check is comic is existed or not
        if (comicRepository.existedBySlugAndComicId(slugNId)) {
            throw new ConflictException("Crawling comic from " + slugNId + " already exists");
        }

        try {
            var extracted = nettruyenExtractor.extract(slugNId);

            var extractedComic = extracted.getComic();
            Comic comic = Comic.builder()
                    .slug(extractedComic.getSlug())
                    .author(extractedComic.getAuthor().name)
                    .tags(Arrays.toString(extractedComic.getGenre().toArray()))
                    .title(extractedComic.getName())
                    .description(extractedComic.getDescription())
                    .originId(extractedComic.getComicId().toString())
                    .status("OnGoing")
                    .originUrl(extractedComic.getUrl())
                    .originPathParams(slugNId)
                    .crawlingStatus(CrawlStatus.FINISHED)
                    .build();

            // Init thumb Image
            var extractedImage = extracted.getThumbImage();
            String newPath = generateNewThumbImageObject(extractedImage.getFileName());

            //Rename object to newer path
            renameObject(
                    waradaConfig.imageBucket(),
                    extractedImage.getObjectTemp(),
                    newPath
            );
            Image image = Image.builder()
                    .comic(comic)
                    .filePath(newPath)
                    .fileName(extractedImage.getFileName())
                    .originUrl(extractedImage.getOriginUrl())
                    .position(0)
                    .type(ImageType.THUMB_IMAGE)
                    .build();

            comic.setThumbImage(image);

            List<Chapter> chapters = extracted.getChapters().data.stream().map(
                    chapter -> Chapter.builder()
                            .crawlingStatus(CrawlStatus.INIT)
                            .sourceUrl(chapter.originPathParams)
                            .chapterNumber(chapter.chapterName)
                            .position(chapter.chapterNum)
                            .originPathParams(chapter.originPathParams)
                            .comic(comic)
                            .build()
            ).toList();

            comic.setChapters(chapters);
            comic.setChapterCount(chapters.size());

            comicRepository.persistAndFlush(comic);
            log.info("DONE Crawling Nettruyen Comic from {}", slugNId);
        } catch (Exception e) {
            log.error("ERROR Crawling Nettruyen Comic from {}", slugNId);
            throw e;
        }

    }


    public void renameObject(String bucket, String oldName, String newName) throws Exception {
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

    public String generateNewThumbImageObject(String fileName) {
        return waradaConfig.imageBucketImagesPath() + "/" + fileName;
    }
}
