package com.nsmall.ns_creative_studio_bo.domain.goods;

import org.apache.hc.client5.http.DnsResolver;
import java.net.InetAddress;
import java.net.UnknownHostException;

/** Validates the exact addresses handed to the connection, avoiding a separate check/fetch DNS lookup. */
final class PublicImageDnsResolver implements DnsResolver {
    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(host);
        for (InetAddress address : addresses) {
            if (!isPublic(address)) throw new UnknownHostException("Non-public image destination is not allowed");
        }
        return addresses;
    }

    @Override
    public String resolveCanonicalHostname(String host) {
        return host;
    }

    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] bytes = address.getAddress();
        int a = bytes[0] & 255, b = bytes[1] & 255;
        if (bytes.length == 4) {
            int c = bytes[2] & 255;
            return a != 0 && a != 10 && a != 127 && a < 224
                    && !(a == 100 && b >= 64 && b <= 127)
                    && !(a == 169 && b == 254) && !(a == 172 && b >= 16 && b <= 31)
                    && !(a == 192 && (b == 168 || (b == 0 && (c == 0 || c == 2))))
                    && !(a == 198 && (b == 18 || b == 19 || (b == 51 && c == 100)))
                    && !(a == 203 && b == 0 && c == 113);
        }
        // Only global unicast IPv6; exclude documentation, transition/tunneling and special-use space.
        if (bytes.length != 16 || (a & 0xe0) != 0x20) return false;
        if (a == 0x20 && b == 0x02) return false; // 6to4
        if (a == 0x20 && b == 0x01 && ((bytes[2] & 255) < 2
                || ((bytes[2] & 255) == 0x0d && (bytes[3] & 255) == 0xb8))) return false;
        return !(a == 0x3f && b == 0xff && (bytes[2] & 255) < 0x10);
    }
}
