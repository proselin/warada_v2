package com.github.warada_v2.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.warada_v2.models.ExtractedChapterListDto;
import com.github.warada_v2.models.ExtractedNettruyenComicContentDto;
import com.github.warada_v2.rest.client.NettruyenRestService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JBossLog
@ApplicationScoped
public class NettruyenExtractor {
    @Inject
    @RestClient
    NettruyenRestService nettruyenRestService;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Pattern LD_JSON_PATTERN = Pattern.compile(
            "<script[^>]*type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SLUG_P =
            Pattern.compile("gOpts\\.comicSlug\\s*=\\s*(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ID_P =
            Pattern.compile("gOpts\\.comicId\\s*=\\s*(['\"])?(\\d+)\\1?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);


    public Object extract(String url) {
        log.info("Extracting Nettruyen from URL: " + url);
        String content  = getComicContent(url);
        try {
            ExtractedNettruyenComicContentDto dto = extractComicHtmlContent(content);
            log.info("Extracted NettruyenComicContentDto: " + dto);
            var chapters = getChapterList(dto.getComicId(), dto.getSlug());

        } catch (Exception e) {
            log.error("Error extracting Nettruyen from URL: " + url, e);
        }
        return null;
    }

    public Object getChapterList(Long comicId, String slug) {
        log.info("Getting ChapterList with slug: " + slug + " from comicId: " + comicId);
        var response = nettruyenRestService.getComicChapterList(slug, comicId);
        var responseBody = response.readEntity(ExtractedChapterListDto.class);
        var data = responseBody.data;
        return data;
    }

    public ExtractedNettruyenComicContentDto extractComicHtmlContent(String html) {
        Matcher m = LD_JSON_PATTERN.matcher(html);

        String slug = findGroup(SLUG_P, html,2);
        String idStr = findGroup(ID_P, html, 2);

        Long id = null;
        if (idStr != null) {
            try { id = Long.parseLong(idStr); } catch (NumberFormatException ignored) {
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
        log.info("[Client] response GET from URL: "+ url + " with code " + response.getStatus() );
        return response.readEntity(String.class);
    }


    private String find(Pattern p, String s) {
        return findGroup(p, s, 2);
    }

    private String findGroup(Pattern p, String s, int groupIndex) {
        Matcher m = p.matcher(s);
        if (m.find()) return m.group(groupIndex);
        return null;
    }
}
