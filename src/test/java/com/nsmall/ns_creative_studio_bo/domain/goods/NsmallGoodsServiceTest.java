package com.nsmall.ns_creative_studio_bo.domain.goods;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NsmallGoodsServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private GoodsHttpClient client;
    private NsmallGoodsService service;
    private final ArrayDeque<GoodsHttpClient.Response> responses = new ArrayDeque<>();

    @BeforeEach
    void setup() throws Exception {
        client = mock(GoodsHttpClient.class);
        service = new NsmallGoodsService(mapper, client);
        doAnswer(invocation -> responses.remove()).when(client).get(any());
    }

    private String json(String html) throws Exception {
        return mapper.writeValueAsString(Map.of("data", Map.of("resultCode", "0000", "resultData", Map.of(
                "detailInfo", Map.of("goodsCd", 36149436, "productNm", "완도 약산 미역 5봉", "goodsSpcsHtml", html,
                        "photoList", List.of(Map.of("photoPath", "https://product-image.prod-nsmall.com/new/36149436_0.jpg")))))));
    }

    private void response(int status, byte[] body, Map<String, List<String>> headers) {
        InputStream input = new ByteArrayInputStream(body);
        responses.add(new GoodsHttpClient.Response(status, headers.getOrDefault("location", List.of("")).get(0), input, input));
    }

    private void goodsResponse() throws Exception {
        response(200, json("<img src='http://image.nsmall.com/ec_descimages/test.jpg'>").getBytes(java.nio.charset.StandardCharsets.UTF_8), Map.of());
    }

    @Test
    void extractsGalleryAndDetailWithoutRenderingSupplierHtml() throws Exception {
        GoodsImages goods = service.parseGoods("36149436", mapper.readTree(json("""
                <script>alert(1)</script><IMG SRC='http://image.nsmall.com/ec_descimages/detail.jpg?a=1&amp;b=2'>
                <img src='//image.nsmall.com/ec_descimages/detail.jpg?a=1&amp;b=2'>
                <img data-src='https://image.nsmall.com/ec_descimages/lazy.png'>
                <img src='http://127.0.0.1/private.jpg'><img src='data:image/png;base64,AA'>
                <iframe src='https://evil.example/'></iframe>
                """)));
        assertThat(goods.productName()).isEqualTo("완도 약산 미역 5봉");
        assertThat(goods.images()).hasSize(3);
        assertThat(goods.images().get(1).sourceUrl()).isEqualTo("https://image.nsmall.com/ec_descimages/detail.jpg?a=1&b=2");
        assertThat(goods.images()).extracting(GoodsImages.Image::kind).containsExactly("GALLERY", "DETAIL", "DETAIL");
        assertThat(goods.unsupportedDetailImageCount()).isEqualTo(2);
    }

    @Test
    void includesSupplierHostedDetailImagesFromEsmplus() throws Exception {
        // Reproduces goods 35918024: detail images live on supplier CDNs, not NSmall's image host.
        GoodsImages goods = service.parseGoods("36149436", mapper.readTree(json("""
                <center>
                <img src="https://gi.esmplus.com/sharon1981/g/brandstory_new.jpg">
                <img src="https://ai.esmplus.com/sharon1981/gokmulier/notice/gmle_brand.jpg">
                <img src="https://gi.esmplus.com/sharon1981/gijang/kijang_01_500gx3_k.jpg">
                <img src="//gi.esmplus.com/sharon1981/gijang/kijang_02_k.jpg">
                </center>
                """)));
        assertThat(goods.images()).hasSize(5);
        assertThat(goods.images()).filteredOn(i -> i.kind().equals("DETAIL"))
                .extracting(GoodsImages.Image::sourceUrl).containsExactly(
                        "https://gi.esmplus.com/sharon1981/g/brandstory_new.jpg",
                        "https://ai.esmplus.com/sharon1981/gokmulier/notice/gmle_brand.jpg",
                        "https://gi.esmplus.com/sharon1981/gijang/kijang_01_500gx3_k.jpg",
                        "https://gi.esmplus.com/sharon1981/gijang/kijang_02_k.jpg");
        assertThat(goods.unsupportedDetailImageCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://127.0.0.1/a.jpg", "http://[::1]/a.jpg", "http://2130706433/a.jpg", "http://localhost/a.jpg",
            "file:///etc/passwd", "https://user@image.nsmall.com/a.jpg", "https://image.nsmall.com:8443/a.jpg", "javascript:alert(1)",
            "https://images.local/a.jpg", "https://images.internal/a.jpg", "https://user@ai.esmplus.com/a.jpg"})
    void rejectsUnsafeImageAddressSyntax(String url) {
        assertThat(NsmallGoodsService.trustedImageUri(url)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0", "../123", "36149436?x=1", "https://m.nsmall.com/goods/36149436", "123456789012345"})
    void rejectsInvalidCodesBeforeNetwork(String code) {
        assertThatThrownBy(() -> service.getImages(code)).isInstanceOf(BizException.class);
        verifyNoInteractions(client);
    }

    @Test
    void metadataUsesFixedEndpointAndIsCached() throws Exception {
        goodsResponse();
        assertThat(service.getImages("36149436")).isSameAs(service.getImages("36149436"));
        verify(client).get(URI.create("https://mapi.nsmall.com/md/api/v1/product/info/36149436?deviceChnId=INT"));
    }

    @Test
    void refusesUnknownImageIdsWithoutFetchingArbitraryImage() throws Exception {
        goodsResponse();
        assertThatThrownBy(() -> service.getImage("36149436", "https://evil.test/"))
                .isInstanceOf(BizException.class);
        verify(client, times(1)).get(any());
    }

    @Test
    void refusesRedirectToLiteralPrivateAddress() throws Exception {
        goodsResponse();
        GoodsImages goods = service.getImages("36149436");
        response(302, new byte[0], Map.of("location", List.of("http://127.0.0.1/private")));
        assertThatThrownBy(() -> service.getImage("36149436", goods.images().get(0).id()))
                .isInstanceOf(BizException.class);
        verify(client, times(2)).get(any());
    }

    @Test
    void includesPublicSupplierImagesWithoutRegisteringEveryHost() throws Exception {
        GoodsImages goods = service.parseGoods("36149436", mapper.readTree(json("""
                <center><img src="http://foraimg.godohosting.com/images/salmon_180308_01.jpg">
                <img data-original="https://images.another-supplier.com/product.jpg">
                </center>
                """)));
        assertThat(goods.images()).hasSize(3);
        assertThat(goods.unsupportedDetailImageCount()).isZero();
        assertThat(goods.images().get(1).sourceUrl()).isEqualTo("http://foraimg.godohosting.com/images/salmon_180308_01.jpg");
    }

    @Test
    void checksImageBytesRatherThanTrustingContentType() throws Exception {
        goodsResponse();
        GoodsImages goods = service.getImages("36149436");
        response(200, "<html>blocked</html>".getBytes(), Map.of("content-type", List.of("image/jpeg")));
        assertThatThrownBy(() -> service.getImage("36149436", goods.images().get(0).id()))
                .isInstanceOf(BizException.class);
    }

    @Test
    void returnsActualJpegBytes() throws Exception {
        goodsResponse();
        GoodsImages goods = service.getImages("36149436");
        byte[] jpeg = {(byte)255, (byte)216, (byte)255, 0};
        response(200, jpeg, Map.of());
        assertThat(service.getImage("36149436", goods.images().get(0).id()).bytes()).isEqualTo(jpeg);
    }

    @Test
    void handlesMissingProductAndMismatchedCodes() throws Exception {
        assertThatThrownBy(() -> service.parseGoods("36149436", mapper.readTree("{\"data\":{\"resultCode\":\"7020\"}}")))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.parseGoods("999", mapper.readTree(json(""))))
                .isInstanceOf(BizException.class);
    }
}
