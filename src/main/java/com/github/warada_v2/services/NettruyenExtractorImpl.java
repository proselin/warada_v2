package com.github.warada_v2.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.warada_v2.configurations.WaradaConfig;
import com.github.warada_v2.models.*;
import com.github.warada_v2.rest.client.NettruyenRestService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.warada_v2.commons.Utils.getExtensionFromUrl;

@ApplicationScoped
public class NettruyenExtractorImpl implements NettruyenExtractor {

    private static final Logger log = Logger.getLogger(NettruyenExtractorImpl.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern LD_JSON_PATTERN = Pattern.compile("<script[^>]*type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SLUG_P = Pattern.compile("gOpts\\.comicSlug\\s*=\\s*(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ID_P = Pattern.compile("gOpts\\.comicId\\s*=\\s*(['\"])?(\\d+)\\1?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern IMG_SERVER_P = Pattern.compile("data-sv1=['\"]([^'\"]*)['\"][^>]*data-sv2=['\"]([^'\"]*)['\"]");

    @Inject
    @RestClient
    NettruyenRestService nettruyenRestService;

    @Inject
    MinioClient minioClient;

    @Inject
    WaradaConfig waradaConfig;

    public ExtractedComicDetail extractComicDetail(String slug) {
        log.info("Extracting Nettruyen from URL: " + slug);
        String content = getComicContent(slug);
        try {
            var comic = extractComicHtmlContent(content);
            log.infof("Extracted NettruyenComicContentDto: %s" + comic);
            var chapters = getChapterList(comic.comicId(), comic.slug());
            log.infof("Extracted NettruyenChaptersDto: %s ", chapters);
            var thumbImage = crawlThumbImage(comic.image());
            log.infof("Extracted NettruyenThumbImage: %s", thumbImage);
            return new ExtractedComicDetail(comic, chapters, thumbImage);
        } catch (Exception e) {
            log.error("Error extracting Nettruyen from URL: " + slug, e);
            throw e;
        }
    }

    public ExtractedChapters getChapterList(Long comicId, String slug) {
        log.info("Getting ChapterList with slug: " + slug + " from comicId: " + comicId);
        var response = nettruyenRestService.getComicChapterList(slug, comicId);
        var results = response.readEntity(ExtractedChapters.class);
        // Add Path Params field to each chapter
        results = results.withData(results.data().stream().map(chapter -> {
            var result = chapter
                    .withOriginPathParams(
                            generateChapterPathParams(slug, chapter.chapterSlug(), chapter.chapterId())
                    );
            return result.withOriginUrl(
                    generateChapterOriginUrl(
                            result.originPathParams()
                    )
            );
        }).toList());
        return results;
    }

    public ExtractedNettruyenComic extractComicHtmlContent(String html) {
        Matcher m = LD_JSON_PATTERN.matcher(html);

        String slug = findGroup(SLUG_P, html, 2);
        String idStr = findGroup(ID_P, html, 2);

        Long id = null;
        if (idStr != null) {
            try {
                id = Long.parseLong(idStr);
            } catch (NumberFormatException ignored) {
                throw new RuntimeException("Invalid ID: " + idStr);
            }
        }

        ExtractedNettruyenComic result = null;

        while (m.find()) {
            String json = m.group(1).trim();
            try {
                json = json.replaceAll("(?<=:[\\s]*\\\")([^\"]*?)(\\r?\\n)([^\"]*?)(?=\\\")", "$1\\\\n$3");
                json = json.replaceAll("(\\r|\\n)+", " ");
                JsonNode root = mapper.readTree(json);
                for (JsonNode node : flattenJsonLd(root)) {
                    if (hasType(node, "ComicSeries")) {
                        result = mapper.treeToValue(node, ExtractedNettruyenComic.class);
                    }
                }
            } catch (Exception exception) {
                throw new RuntimeException("Error parsing JSON: " + json, exception);
            }
        }

        assert result != null;
        result = result.withSlugAndComicId(slug, id);

        return result;
    }

    private boolean hasType(JsonNode n, String type) {
        JsonNode t = n.get("@type");
        if (t == null) return false;
        if (t.isTextual()) return type.equals(t.asText());
        if (t.isArray()) for (JsonNode e : t) if (type.equals(e.asText())) return true;
        return false;
    }

    private List<JsonNode> flattenJsonLd(JsonNode root) {
        List<JsonNode> nodes = new ArrayList<>();
        if (root.isArray()) {
            root.forEach(nodes::add);
        } else if (root.has("@graph") && root.get("@graph").isArray()) {
            root.get("@graph").forEach(nodes::add);
        } else {
            nodes.add(root);
        }
        return nodes;
    }

    private String getComicContent(String url) {
        log.info("[Client] request GET from URL: " + url);
        var response = nettruyenRestService.getContent(url);
        log.info("[Client] response GET from URL: " + url + " with code " + response.getStatus());
        return response.readEntity(String.class);
    }

    private String findGroup(Pattern p, String s, int groupIndex) {
        Matcher m = p.matcher(s);
        if (m.find()) return m.group(groupIndex);
        return null;
    }

    private String generateChapterPathParams(String comicSlug, String chapterSlug, Long chapterOriginId) {
        return comicSlug + "/" + chapterSlug + "/" + chapterOriginId;
    }

    private String generateChapterOriginUrl(String chapterSlug) {
        return waradaConfig.nettruyen().url() + "/truyen-tranh/" + chapterSlug;
    }

    private ExtractedThumbImage crawlThumbImage(String imageUrl) {
        log.info("Extracting thumbnail from URL: " + imageUrl);
        var downloadInfoMap = downloadNSaveImage(imageUrl, "thumbnail");
        return new ExtractedThumbImage(imageUrl, downloadInfoMap.get("fileName"), downloadInfoMap.get("objectTemp"));
    }

    private Map<String, String> downloadNSaveImage(String imageUrl, String uniqueName) {
        try (Response imgResponse = pullImage(imageUrl)) {

            log.info("Downloading N-Save image from URL: " + imageUrl);
            InputStream imageStream = imgResponse.readEntity(InputStream.class);

            String contentType = imgResponse.getHeaderString("Content-Type");
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String extension = getExtensionFromUrl(imageUrl);
            String filename = timestamp + '_' + uniqueName + extension;
            String object = waradaConfig.imageBucketTempPath() + "/" + filename;
            log.info("[Start] Minio saving image with file Name " + filename);
            this.minioClient.putObject(
                    PutObjectArgs.builder()
                            .contentType(contentType)
                            .object(object)
                            .bucket(waradaConfig.imageBucket())
                            .stream(imageStream, -1, 10485760) // -1 = unknown size, 10MB part size
                    .build()
            );
            log.info("[Done] Minio Saved image with file Name " + filename);
            var result = new HashMap<String, String>();
            result.put("fileName", filename);
            result.put("objectTemp", object);
            return result;

        } catch (Exception err) {
            throw new RuntimeException("Failed to download and save image", err);
        }
    }

    private Response pullImage(String imageUrl) {
        try (Client client = ClientBuilder.newClient()) {
            log.info("[Start][Client] Downloading N-Save image from URL: " + imageUrl);
            Response imgResponse = client.target(imageUrl)
                                        .request(MediaType.APPLICATION_OCTET_STREAM)
                                        .header("Content-Type", "application/octet-stream")
                                        .header("Accept", "*/*")
                                        .header("Origin", waradaConfig.nettruyen().url())
                                        .header("Referer", waradaConfig.nettruyen().url())
                                        .header("Access-Control-Allow-Origin", "*")
                                        .header("sec-fetch-mode", "cors")
                                        .header("sec-fetch-dest", "empty")
                                        .header("sec-fetch-site", "cross-site")
                                        .get();
            if (imgResponse.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new RuntimeException("HTTP " + imgResponse.getStatus() + ": " + imgResponse.readEntity(String.class));
            }
            log.info("[Done][Client] Downloaded N-Save image from URL: " + imageUrl);
            return imgResponse;
        } catch (Exception ex) {
            log.error("Failed to download and save image", ex);
            throw new RuntimeException(ex);
        }
    }

    private Response getChapterContent(String slug) {
        log.infof("[Client][Start] calling chapter content with slug: %s", slug);
        try (Client client = ClientBuilder.newClient()) {
           Response response =  client.target(generateChapterOriginUrl(slug))
                                      .request(MediaType.TEXT_HTML).get();
           if (response.getStatus() != Response.Status.OK.getStatusCode()) {
               throw new RuntimeException("HTTP " + response.getStatus() + ": " + response.readEntity(String.class));
           }
            log.infof("[Client][Done] calling chapter content with slug: %s ", slug);
            return response;
        } catch (Exception ex) {
            log.error("Failed to get Chapter detail content", ex);
            throw new RuntimeException(ex);
        }
    }

    public ExtractedChapterDetail extractChapterDetail(String slug) {
        String content = getChapterContent(slug).readEntity(String.class);
        Matcher matcher = IMG_SERVER_P.matcher(content);
        List<ExtractedImage> extractedImageInfos = new ArrayList<>();

        {
            AtomicInteger index = new AtomicInteger(0);
            while (matcher.find()) {
                List<String> imageUrls = new ArrayList<>();
                imageUrls.add(matcher.group(1) != null ? matcher.group(1) : "");
                imageUrls.add(matcher.group(2) != null ? matcher.group(2) : "");
                extractedImageInfos.add(ExtractedImage.builder().imageUrls(imageUrls).position(index.getAndIncrement()).build());
            }
        }

        extractedImageInfos = extractedImageInfos.parallelStream().map(extractedImageInfo -> {
            for (String imageUrl : extractedImageInfo.imageUrls()) {
                log.infof("START Crawl chapter image with URL: %s", imageUrl);
                try {
                    var downloadInfo = downloadNSaveImage(imageUrl, extractedImageInfo.position().toString());
                    return extractedImageInfo.withFileNameWithObjectWithImageUrlPulled(downloadInfo.get("fileName"), downloadInfo.get("objectTemp"), imageUrl);
                } catch (Exception e) {
                    log.errorf("Error fetching image from URL: %s", imageUrl, e);
                }
            }
            return null;
        }).filter(Objects::nonNull).toList();

        return new ExtractedChapterDetail(extractedImageInfos);
    }
}
