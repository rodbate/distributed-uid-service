package com.github.rodbate.uid.common;

import com.github.rodbate.uid.utils.InetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Objects;

/**
 * User: jiangsongsong
 * Date: 2018/12/11
 * Time: 16:23
 */
@Slf4j
public final class ImmutableConfig {

    private ImmutableConfig() { throw new IllegalStateException("NO INSTANCE"); }


    public volatile static boolean applicationClosed = false;

    /**
     * config app.env
     */
    public static final String APP_ENV_KEY = "app.env";
    public static final ApplicationEnv DEFAULT_APP_ENV = ApplicationEnv.DEV;
    public volatile static ApplicationEnv appEnv = DEFAULT_APP_ENV;

    /**
     * config cluster.transport.host
     */
    public static final String CLUSTER_TRANSPORT_HOST_KEY = "cluster.transport.host";
    public static final String DEFAULT_CLUSTER_TRANSPORT_HOST = InetUtil.getLocalhostLanAddress().getHostAddress();
    public volatile static String clusterTransportHost = DEFAULT_CLUSTER_TRANSPORT_HOST;

    /**
     * config cluster.transport.port
     */
    public static final String CLUSTER_TRANSPORT_PORT_KEY = "cluster.transport.port";
    public static final int DEFAULT_CLUSTER_TRANSPORT_PORT = 9000;
    public volatile static int clusterTransportPort = DEFAULT_CLUSTER_TRANSPORT_PORT;


    /**
     * config zookeeper.client.connect-string
     */
    public static final String ZOOKEEPER_CLIENT_CONNECT_STRING_KEY = "zookeeper.client.connect-string";
    public static final String DEFAULT_ZOOKEEPER_CLIENT_CONNECT_STRING = "localhost:2181";
    public volatile static String zookeeperClientConnectString = DEFAULT_ZOOKEEPER_CLIENT_CONNECT_STRING;

    /**
     * config zookeeper.client.connection-timeout
     */
    public static final String ZOOKEEPER_CLIENT_CONNECTION_TIMEOUT_KEY = "zookeeper.client.connection-timeout";
    public static final int DEFAULT_ZOOKEEPER_CLIENT_CONNECTION_TIMEOUT = 5000;
    public volatile static int zookeeperClientConnectionTimeout = DEFAULT_ZOOKEEPER_CLIENT_CONNECTION_TIMEOUT;

    /**
     * config zookeeper.client.session-timeout
     */
    public static final String ZOOKEEPER_CLIENT_SESSION_TIMEOUT_KEY = "zookeeper.client.session-timeout";
    public static final int DEFAULT_ZOOKEEPER_CLIENT_SESSION_TIMEOUT = 30000;
    public volatile static int zookeeperClientSessionTimeout = DEFAULT_ZOOKEEPER_CLIENT_SESSION_TIMEOUT;


    /**
     * process config settings
     *
     * @param environment application environment
     */
    public static void processConfig(ConfigurableEnvironment environment) {
        //config app.env
        final String env = Objects.requireNonNull(environment.getProperty(APP_ENV_KEY), String.format("config %s require not null", APP_ENV_KEY));
        ImmutableConfig.appEnv = ApplicationEnv.fromEnv(env);
        logConfig(APP_ENV_KEY, appEnv);

        //config cluster.transport.host
        final String clusterTransportHost = environment.getProperty(CLUSTER_TRANSPORT_HOST_KEY);
        if (StringUtils.isNotBlank(clusterTransportHost) && !InetUtil.isValidHostOrIp(clusterTransportHost)) {
            throwExIfInvalid(CLUSTER_TRANSPORT_HOST_KEY, clusterTransportHost);
        } else if (StringUtils.isNotBlank(clusterTransportHost)) {
            ImmutableConfig.clusterTransportHost = clusterTransportHost;
        }
        logConfig(CLUSTER_TRANSPORT_HOST_KEY, ImmutableConfig.clusterTransportHost);

        //config cluster.transport.port
        final Integer clusterTransportPort = environment.getProperty(CLUSTER_TRANSPORT_PORT_KEY, Integer.class);
        if (clusterTransportPort != null && (clusterTransportPort < 0 || clusterTransportPort >= 65536)) {
            throwExIfInvalid(CLUSTER_TRANSPORT_PORT_KEY, clusterTransportPort);
        } else if (clusterTransportPort != null) {
            ImmutableConfig.clusterTransportPort = clusterTransportPort;
        }
        logConfig(CLUSTER_TRANSPORT_PORT_KEY, ImmutableConfig.clusterTransportPort);

        //config zookeeper.client.connect-string
        final String zookeeperClientConnectString = environment.getProperty(ZOOKEEPER_CLIENT_CONNECT_STRING_KEY);
        if (StringUtils.isBlank(zookeeperClientConnectString)) {
            throwExIfInvalid(ZOOKEEPER_CLIENT_CONNECT_STRING_KEY, zookeeperClientConnectString);
        } else {
            ImmutableConfig.zookeeperClientConnectString = zookeeperClientConnectString;
        }
        logConfig(ZOOKEEPER_CLIENT_CONNECT_STRING_KEY, ImmutableConfig.zookeeperClientConnectString);

        //config zookeeper.client.connection-timeout
        final Integer zookeeperClientConnectionTimeout = environment.getProperty(ZOOKEEPER_CLIENT_CONNECTION_TIMEOUT_KEY, Integer.class);
        if (zookeeperClientConnectionTimeout != null && zookeeperClientConnectionTimeout <= 0) {
            throwExIfInvalid(ZOOKEEPER_CLIENT_CONNECTION_TIMEOUT_KEY, zookeeperClientConnectionTimeout);
        } else if (zookeeperClientConnectionTimeout != null) {
            ImmutableConfig.zookeeperClientConnectionTimeout = zookeeperClientConnectionTimeout;
        }
        logConfig(ZOOKEEPER_CLIENT_CONNECTION_TIMEOUT_KEY, ImmutableConfig.zookeeperClientConnectionTimeout);

        //config zookeeper.client.session-timeout
        final Integer zookeeperClientSessionTimeout = environment.getProperty(ZOOKEEPER_CLIENT_SESSION_TIMEOUT_KEY, Integer.class);
        if (zookeeperClientSessionTimeout != null && zookeeperClientSessionTimeout <= 0) {
            throwExIfInvalid(ZOOKEEPER_CLIENT_SESSION_TIMEOUT_KEY, zookeeperClientSessionTimeout);
        } else if (zookeeperClientSessionTimeout != null) {
            ImmutableConfig.zookeeperClientSessionTimeout = zookeeperClientSessionTimeout;
        }
        logConfig(ZOOKEEPER_CLIENT_SESSION_TIMEOUT_KEY, ImmutableConfig.zookeeperClientSessionTimeout);
    }

    private static void logConfig(final String key, final Object value) {
        log.info("set config property: {} = {}", key, value);
    }

    private static void throwExIfInvalid(final String key, final Object value) {
        throw new IllegalArgumentException(String.format("invalid config property: %s = %s", key, value));
    }
}
