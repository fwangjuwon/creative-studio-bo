package com.nsmall.ns_creative_studio_bo.domain.goods;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/** Optional read-only smoke check against public product data; no Gemini calls or database access. */
@EnabledIfEnvironmentVariable(named = "NSMALL_LIVE_CHECK", matches = "true")
class NsmallGoodsLiveTest {
    private GoodsHttpClient client;
    @BeforeEach void openClient() { client = new GoodsHttpClient(); }
    @AfterEach void closeClient() throws Exception { client.close(); }
    @Test
    void fetchesProductAndOriginalDetailImage() {
        NsmallGoodsService service = new NsmallGoodsService(new ObjectMapper(), client);
        GoodsImages goods = service.getImages("36149436");
        assertThat(goods.productName()).isEqualTo("완도 약산 미역 5봉");
        GoodsImages.Image detail = goods.images().stream().filter(i -> i.kind().equals("DETAIL")).findFirst().orElseThrow();
        assertThat(detail.sourceUrl()).endsWith("9159546552097424.jpg");
        NsmallGoodsService.ImageData image = service.getImage("36149436", detail.id());
        assertThat(image.contentType()).isEqualTo("image/jpeg");
        assertThat(image.bytes().length).isGreaterThan(1000);
        assertThat(service.getImages("36233797").images()).isNotEmpty();
    }

    @Test
    void fetchesSupplierHostedDetailImagesFor35918024() {
        NsmallGoodsService service = new NsmallGoodsService(new ObjectMapper(), client);
        GoodsImages goods = service.getImages("35918024");
        assertThat(goods.images()).filteredOn(i -> i.kind().equals("GALLERY")).hasSize(3);
        assertThat(goods.images()).filteredOn(i -> i.kind().equals("DETAIL")).isNotEmpty();
        assertThat(goods.unsupportedDetailImageCount()).isZero();
        GoodsImages.Image detail = goods.images().stream()
                .filter(i -> i.sourceUrl().contains("kijang_01_500gx3_k.jpg")).findFirst().orElseThrow();
        NsmallGoodsService.ImageData image = service.getImage("35918024", detail.id());
        assertThat(image.contentType()).isEqualTo("image/jpeg");
        assertThat(image.bytes().length).isGreaterThan(1000);
        GoodsImages.Image notice = goods.images().stream()
                .filter(i -> i.sourceUrl().startsWith("https://ai.esmplus.com/")).findFirst().orElseThrow();
        assertThat(service.getImage("35918024", notice.id()).contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void fetchesSupplierHostedDetailImagesFor36311160() {
        NsmallGoodsService service = new NsmallGoodsService(new ObjectMapper(), client);
        GoodsImages goods = service.getImages("36311160");
        assertThat(goods.images()).filteredOn(i -> i.kind().equals("GALLERY")).hasSize(3);
        assertThat(goods.images()).filteredOn(i -> i.kind().equals("DETAIL")).hasSize(9);
        assertThat(goods.unsupportedDetailImageCount()).isZero();
        GoodsImages.Image detail = goods.images().stream()
                .filter(i -> i.sourceUrl().endsWith("salmon_180308_01.jpg")).findFirst().orElseThrow();
        assertThat(service.getImage("36311160", detail.id()).contentType()).isEqualTo("image/jpeg");
    }
}
