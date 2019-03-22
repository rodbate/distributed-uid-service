package com.github.rodbate.uid.cluster;

/**
 * User: jiangsongsong
 * Date: 2018/12/25
 * Time: 11:29
 */
public class NotLeaderException extends RuntimeException {

    private final NodeInfo nodeInfo;

    public NotLeaderException(NodeInfo nodeInfo) {
        super(String.format("node => %s, is not leader", nodeInfo.toString()));
        this.nodeInfo = nodeInfo;
    }

    public NotLeaderException(NodeInfo nodeInfo, Throwable cause) {
        super(String.format("node => %s, is not leader", nodeInfo.toString()), cause);
        this.nodeInfo = nodeInfo;
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }
}
