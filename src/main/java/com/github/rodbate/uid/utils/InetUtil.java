package com.github.rodbate.uid.utils;

import java.net.*;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * User: jiangsongsong
 * Date: 2018/12/11
 * Time: 16:49
 */
public final class InetUtil {

    private InetUtil() {
        throw new IllegalStateException("NO INSTANCE");
    }

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");


    /**
     * check ip v4 address validity
     *
     * @param hostOrIp ip address
     * @return if true valid else invalid
     */
    public static boolean isValidHostOrIp(final String hostOrIp) {
        try {
            InetAddress.getByName(hostOrIp);
            return true;
        } catch (UnknownHostException ignored) {
        }
        return false;
    }

    /**
     * get my inetaddress
     *
     * @return inetaddress
     */
    public static InetAddress getLocalhostLanAddress() {
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            InetAddress candidateAddress = null;
            while (nifs.hasMoreElements()) {
                NetworkInterface nif = nifs.nextElement();
                if (nif.isLoopback() || !nif.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = nif.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress instanceof Inet6Address) {
                        continue;
                    }
                    //local address
                    if (inetAddress.isSiteLocalAddress()) {
                        return inetAddress;
                    } else if (candidateAddress == null) {
                        candidateAddress = inetAddress;
                    }
                }
            }

            if (candidateAddress != null) {
                return candidateAddress;
            }
        } catch (SocketException e) {
            throw new RuntimeException("encounter exception while get network interfaces", e);
        }

        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException("encounter exception while get localhost", e);
        }
    }


}
