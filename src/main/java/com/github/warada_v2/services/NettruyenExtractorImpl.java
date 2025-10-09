package com.github.warada_v2.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.warada_v2.configurations.WaradaConfig;
import com.github.warada_v2.models.ExtractedChaptersDto;
import com.github.warada_v2.models.ExtractedNettruyenComicContentDto;
import com.github.warada_v2.models.ExtractedThumbImage;
import com.github.warada_v2.models.NettruyenExtractorResult;
import com.github.warada_v2.rest.client.NettruyenRestService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.warada_v2.commons.Utils.getExtensionFromUrl;

@JBossLog
@ApplicationScoped
public class NettruyenExtractorImpl implements NettruyenExtractor {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern LD_JSON_PATTERN = Pattern.compile("<script[^>]*type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SLUG_P = Pattern.compile("gOpts\\.comicSlug\\s*=\\s*(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ID_P = Pattern.compile("gOpts\\.comicId\\s*=\\s*(['\"])?(\\d+)\\1?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    @Inject
    @RestClient
    NettruyenRestService nettruyenRestService;
    @Inject
    MinioClient minioClient;
    @Inject
    WaradaConfig waradaConfig;

    public NettruyenExtractorResult extract(String url) {
        log.info("Extracting Nettruyen from URL: " + url);
        String content = getComicContent(url);
        var result = new NettruyenExtractorResult();
        try {
            ExtractedNettruyenComicContentDto comic = extractComicHtmlContent(content);

            result.setComic(comic);
            log.info("Extracted NettruyenComicContentDto: " + comic);
            result.setChapters(getChapterList(comic.getComicId(), comic.getSlug()));
            log.info("Extracted NettruyenChaptersDto: " + result);
            result.setThumbImage(crawlThumbImage(comic.getImage()));
            log.info("Extracted NettruyenThumbImage: " + result);

        } catch (Exception e) {
            log.error("Error extracting Nettruyen from URL: " + url, e);
            throw e;
        }
        return result;
    }

    public ExtractedChaptersDto getChapterList(Long comicId, String slug) {
        log.info("Getting ChapterList with slug: " + slug + " from comicId: " + comicId);
        var response = nettruyenRestService.getComicChapterList(slug, comicId);
        var results = response.readEntity(ExtractedChaptersDto.class);
        // Add Path Params field to each chapter
        results.data.forEach(chapter -> {
            chapter.originPathParams = generateChapterPathParams(slug, chapter.chapterSlug, chapter.chapterId);
        });
        return results;
    }

    public ExtractedNettruyenComicContentDto extractComicHtmlContent(String html) {
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

        ExtractedNettruyenComicContentDto result = null;

        while (m.find()) {
            String json = m.group(1).trim();
            try {
                JsonNode root = mapper.readTree(json);
                for (JsonNode node : flattenJsonLd(root)) {
                    if (hasType(node, "ComicSeries")) {
                        result = mapper.treeToValue(node, ExtractedNettruyenComicContentDto.class);
                    }
                }
            } catch (Exception exception) {
                throw new RuntimeException("Error parsing JSON: " + json, exception);
            }
        }

        assert result != null;
        result.setSlug(slug);
        result.setComicId(id);

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
        var response = nettruyenRestService.getComicHtmlContent(url);
        log.info("[Client] response GET from URL: " + url + " with code " + response.getStatus());
        return response.readEntity(String.class);
    }

    private String findGroup(Pattern p, String s, int groupIndex) {
        Matcher m = p.matcher(s);
        if (m.find()) return m.group(groupIndex);
        return null;
    }

    private String generateChapterPathParams(String comicSlug, String chapterSlug, Long chapterOriginId) {
        return "/" + comicSlug + "/" + chapterSlug + "/" + chapterOriginId;
    }

    private ExtractedThumbImage crawlThumbImage(String imageUrl) {
        log.info("Extracting thumbnail from URL: " + imageUrl);
        var result = new ExtractedThumbImage();
        result.setFileName(downloadNSaveImage(imageUrl));
        result.setOriginUrl(imageUrl);
        return result;
    }

    private String downloadNSaveImage(String imageUrl) {
        try (Response imgResponse = pullImage(imageUrl)) {

            log.info("Downloading N-Save image from URL: " + imageUrl);
            InputStream imageStream = imgResponse.readEntity(InputStream.class);

            String contentType = imgResponse.getHeaderString("Content-Type");
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String extension = getExtensionFromUrl(imageUrl);
            String filename = timestamp + extension;

            log.info("[Start] Minio saving image with file Name " + filename);
            this.minioClient.putObject(PutObjectArgs.builder().contentType(contentType).object(filename).bucket(waradaConfig.imageBucket()).stream(imageStream, -1, 10485760) // -1 = unknown size, 10MB part size
                    .build());
            log.info("[Done] Minio Saved image with file Name " + filename);
            return filename;

        } catch (Exception err) {
            throw new RuntimeException("Failed to download and save image", err);
        }

    }

    private Response pullImage(String imageUrl) {
        try (Client client = ClientBuilder.newClient()) {
            log.info("[Start][Client] Downloading N-Save image from URL: " + imageUrl);
            Response imgResponse = client.target(imageUrl).request(MediaType.APPLICATION_OCTET_STREAM).header("Content-Type", "application/octet-stream").header("Accept", "*/*").header("Origin", waradaConfig.nettruyen().url()).header("Referer", waradaConfig.nettruyen().url()).header("Access-Control-Allow-Origin", "*").header("sec-fetch-mode", "cors").header("sec-fetch-dest", "empty").header("sec-fetch-site", "cross-site").get();
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


}
