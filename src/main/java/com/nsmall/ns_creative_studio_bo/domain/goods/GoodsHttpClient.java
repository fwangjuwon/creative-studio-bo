package com.nsmall.ns_creative_studio_bo.domain.goods;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Component
public class GoodsHttpClient implements Closeable {
    private final CloseableHttpClient client;

    public GoodsHttpClient() {
        var connections = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(new PublicImageDnsResolver())
                .setMaxConnTotal(12).setMaxConnPerRoute(4)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(5)).setSocketTimeout(Timeout.ofSeconds(20)).build())
                .build();
        client = HttpClients.custom().setConnectionManager(connections)
                .setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(Timeout.ofSeconds(5))
                        .setResponseTimeout(Timeout.ofSeconds(20)).build())
                .disableRedirectHandling().disableAutomaticRetries().disableCookieManagement()
                .setUserAgent("Mozilla/5.0 NSCreativeStudio/1.0").build();
    }

    public Response get(URI uri) throws IOException {
        // Validate again for redirects and direct callers, before any network operation.
        if (NsmallGoodsService.trustedImageUri(uri.toString()) == null) throw new IOException("Invalid image URL");
        HttpGet request = new HttpGet(uri);
        request.setHeader("Accept", "application/json,image/jpeg,image/png,image/webp");
        request.setHeader("Origin", "https://m.nsmall.com");
        request.setHeader("Referer", "https://m.nsmall.com/");
        ClassicHttpResponse response = client.executeOpen(null, request, null);
        try {
            Header location = response.getFirstHeader("Location");
            InputStream body = response.getEntity() == null ? InputStream.nullInputStream() : response.getEntity().getContent();
            return new Response(response.getCode(), location == null ? "" : location.getValue(), body, response);
        } catch (IOException | RuntimeException e) {
            response.close();
            throw e;
        }
    }

    @Override
    public void close() throws IOException { client.close(); }

    public record Response(int statusCode, String location, InputStream body, Closeable resource) implements Closeable {
        @Override
        public void close() throws IOException { resource.close(); }
    }
}
