package com.github.rodbate.uid.cluster;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * User: jiangsongsong
 * Date: 2018/12/12
 * Time: 10:49
 */
@Getter
@Setter
public class NodeInfo {
    private String host;
    private int port;
    private long startUpTime = System.currentTimeMillis();

    public NodeInfo() {
    }

    public NodeInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return Objects.equals(this.host, nodeInfo.host) && this.port == nodeInfo.port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }


    @Override
    public String toString() {
        return "NodeInfo{" +
            "host='" + host + '\'' +
            ", port=" + port +
            ", startUpTime=" + startUpTime +
            '}';
    }
}
