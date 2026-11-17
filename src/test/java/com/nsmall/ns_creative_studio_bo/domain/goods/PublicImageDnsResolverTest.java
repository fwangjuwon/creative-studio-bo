package com.nsmall.ns_creative_studio_bo.domain.goods;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import static org.assertj.core.api.Assertions.*;

class PublicImageDnsResolverTest {
    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "10.1.2.3", "127.0.0.1", "169.254.169.254", "100.100.100.200",
            "172.16.0.1", "192.168.1.1", "192.0.0.9", "198.18.0.1", "192.0.2.1", "198.51.100.1", "203.0.113.1",
            "224.0.0.1", "255.255.255.255", "::1", "::", "fc00::1", "fe80::1", "::ffff:127.0.0.1",
            "2001:db8::1", "2002:7f00:1::1", "64:ff9b::7f00:1"})
    void rejectsLocalMetadataAndSpecialUseAddresses(String ip) throws Exception {
        assertThat(PublicImageDnsResolver.isPublic(InetAddress.getByName(ip))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "1.1.1.1", "2001:4860:4860::8888", "2606:4700:4700::1111"})
    void acceptsGlobalUnicastAddresses(String ip) throws Exception {
        assertThat(PublicImageDnsResolver.isPublic(InetAddress.getByName(ip))).isTrue();
    }

    @Test
    void refusesLoopbackAtConnectionResolution() {
        assertThatThrownBy(() -> new PublicImageDnsResolver().resolve("localhost")).isInstanceOf(UnknownHostException.class);
    }
}
