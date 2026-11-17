package com.nsmall.ns_creative_studio_bo.domain.goods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.nsmall.ns_creative_studio_bo.domain.goods.GoodsImportErrorCode.*;

/** Reads the public product API used by m.nsmall.com. Never accepts a caller-supplied fetch URL. */
@Service
@Slf4j
public class NsmallGoodsService {
    private static final String API_BASE = "https://mapi.nsmall.com/md/api/v1/product/info/";
    private static final int MAX_JSON_BYTES = 2 * 1024 * 1024;
    private static final int MAX_IMAGE_BYTES = 32 * 1024 * 1024;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Set<String> HTTPS_IMAGE_HOSTS = Set.of(
            "image.nsmall.com", "product-image.prod-nsmall.com", "gi.esmplus.com", "ai.esmplus.com");
    private final ObjectMapper mapper;
    private final GoodsHttpClient client;
    // Metadata only: do not retain large image byte arrays on the application heap.
    private final Map<String, CachedGoods> cache = new LinkedHashMap<>();

    @Autowired
    public NsmallGoodsService(ObjectMapper mapper, GoodsHttpClient client) {
        this.mapper = mapper;
        this.client = client;
    }

    public GoodsImages getImages(String goodsCd) {
        validateGoodsCd(goodsCd);
        synchronized (cache) {
            CachedGoods entry = cache.get(goodsCd);
            if (entry != null && entry.expiresAt().isAfter(Instant.now())) return entry.goods();
        }
        try {
            GoodsImages goods;
            try (GoodsHttpClient.Response response = client.get(URI.create(API_BASE + goodsCd + "?deviceChnId=INT"))) {
                if (response.statusCode() == 404) throw new BizException(GOODS_NOT_FOUND);
                if (response.statusCode() != 200) {
                    log.warn("NSmall product API returned HTTP {} for goods {}", response.statusCode(), goodsCd);
                    throw new BizException(UPSTREAM_FAILED);
                }
                goods = parseGoods(goodsCd, mapper.readTree(readLimited(response.body(), MAX_JSON_BYTES, UPSTREAM_FAILED)));
            }
            synchronized (cache) {
                cache.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(Instant.now()));
                if (cache.size() >= 64) cache.remove(cache.keySet().iterator().next());
                cache.put(goodsCd, new CachedGoods(goods, Instant.now().plus(CACHE_TTL)));
            }
            return goods;
        } catch (IOException e) {
            log.warn("NSmall product API connection failed for goods {}: {}", goodsCd, e.toString());
            throw new BizException(UPSTREAM_FAILED);
        }
    }

    GoodsImages parseGoods(String goodsCd, JsonNode root) {
        JsonNode data = root.path("data");
        if (!"0000".equals(data.path("resultCode").asText())) throw new BizException(GOODS_NOT_FOUND);
        JsonNode detail = data.path("resultData").path("detailInfo");
        if (!goodsCd.equals(detail.path("goodsCd").asText()) || detail.path("productNm").asText().isBlank()) {
            throw new BizException(GOODS_NOT_FOUND);
        }
        Map<String, GoodsImages.Image> images = new LinkedHashMap<>();
        for (JsonNode photo : detail.path("photoList")) {
            String url = photo.path("photoPath").asText();
            URI uri = trustedImageUri(url);
            // Gallery thumbnails are displayed directly in the browser; only NSmall/CDN hosts here.
            if (uri != null && HTTPS_IMAGE_HOSTS.contains(uri.getHost())) addImage(images, url, "GALLERY", "대표 이미지");
        }
        // Parse only image attributes. Never expose or execute the supplier's HTML in our frontend.
        int unsupportedDetailImageCount = 0;
        for (Element img : Jsoup.parseBodyFragment(detail.path("goodsSpcsHtml").asText()).select("img")) {
            String src = img.attr("src");
            if (src.isBlank() || src.startsWith("data:")) {
                for (String attribute : List.of("data-src", "data-original", "data-lazy-src")) {
                    src = img.attr(attribute);
                    if (!src.isBlank()) break;
                }
            }
            if (trustedImageUri(src) == null) {
                unsupportedDetailImageCount++;
                continue;
            }
            addImage(images, src, "DETAIL", "상세 이미지");
        }
        if (images.isEmpty()) throw new BizException(NO_IMAGES);
        return new GoodsImages(goodsCd, detail.path("productNm").asText(),
                "https://m.nsmall.com/goods/" + goodsCd, List.copyOf(images.values()), unsupportedDetailImageCount);
    }

    private void addImage(Map<String, GoodsImages.Image> images, String rawUrl, String kind, String label) {
        if (images.size() >= 40) return;
        URI uri = trustedImageUri(rawUrl);
        if (uri == null || images.containsKey(uri.toString())) return;
        String url = uri.toString();
        images.put(url, new GoodsImages.Image(imageId(url), kind, label, url));
    }

    public ImageData getImage(String goodsCd, String imageId) {
        GoodsImages.Image image = getImages(goodsCd).images().stream()
                .filter(i -> i.id().equals(imageId)).findFirst().orElseThrow(() -> new BizException(INVALID_IMAGE));
        URI uri = trustedImageUri(image.sourceUrl());
        try {
            for (int redirect = 0; redirect <= 3; redirect++) {
                try (GoodsHttpClient.Response response = client.get(uri)) {
                    if (response.statusCode() >= 300 && response.statusCode() < 400) {
                        uri = trustedImageUri(uri.resolve(response.location()).toString());
                        if (uri == null) throw new BizException(INVALID_IMAGE);
                        continue;
                    }
                    if (response.statusCode() != 200) throw new BizException(INVALID_IMAGE);
                    byte[] bytes = readLimited(response.body(), MAX_IMAGE_BYTES, IMAGE_TOO_LARGE);
                    String type = imageType(bytes);
                    if (type == null) throw new BizException(INVALID_IMAGE);
                    return new ImageData(bytes, type);
                }
            }
            throw new BizException(INVALID_IMAGE);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("NSmall image download failed for goods {}: {}", goodsCd, e.toString());
            throw new BizException(INVALID_IMAGE);
        }
    }

    static void validateGoodsCd(String goodsCd) {
        if (goodsCd == null || !goodsCd.matches("[1-9][0-9]{0,13}")) throw new BizException(INVALID_GOODS_CODE);
    }

    static URI trustedImageUri(String raw) {
        try {
            String url = raw.trim();
            if (url.startsWith("//")) url = "https:" + url;
            URI uri = URI.create(url);
            if (!List.of("http", "https").contains(uri.getScheme()) || uri.getUserInfo() != null || uri.getFragment() != null) return null;
            if (uri.getPort() != -1 && uri.getPort() != ("https".equals(uri.getScheme()) ? 443 : 80)) return null;
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            // Reject literal IPs, short/local hostnames, credentials and special ports before DNS.
            // PublicImageDnsResolver validates the actual connection addresses, including redirects.
            if (!host.contains(".") || host.matches("[0-9.]+") || host.contains(":")
                    || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")) return null;
            String scheme = HTTPS_IMAGE_HOSTS.contains(host) ? "https" : uri.getScheme();
            return URI.create(scheme + "://" + host + uri.getRawPath()
                    + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()));
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private static String imageId(String url) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(url.getBytes(StandardCharsets.UTF_8))).substring(0, 20);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] readLimited(InputStream input, int maxBytes, GoodsImportErrorCode error) throws IOException {
        byte[] bytes = input.readNBytes(maxBytes + 1);
        if (bytes.length > maxBytes) throw new BizException(error);
        return bytes;
    }

    static String imageType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return "image/jpeg";
        if (bytes.length >= 8 && java.util.Arrays.equals(java.util.Arrays.copyOf(bytes, 8),
                new byte[]{(byte)137,80,78,71,13,10,26,10})) return "image/png";
        if (bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
        return null;
    }

    public record ImageData(byte[] bytes, String contentType) {}
    private record CachedGoods(GoodsImages goods, Instant expiresAt) {}
}
