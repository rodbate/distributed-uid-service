package com.github.rodbate.uid.cluster;

import com.alibaba.fastjson.JSON;
import com.github.rodbate.uid.cluster.zk.DefaultZkStateListener;
import com.github.rodbate.uid.cluster.zk.NoOpZkSerializer;
import com.github.rodbate.uid.utils.CommonUtils;
import com.github.rodbate.uid.common.Constants;
import com.github.rodbate.uid.common.ImmutableConfig;
import com.github.rodbate.uid.common.web.ApplicationContextAssertable;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.rodbate.uid.common.Constants.ID_GENERATOR_CLUSTER_PATH;

/**
 * User: jiangsongsong
 * Date: 2018/12/11
 * Time: 18:31
 */
@Slf4j
@Component
public class Node extends ApplicationContextAssertable {

    private static final String ZK_PATH_SEPARATOR = "/";
    private static final String CLUSTER_NODE_PATH_PREFIX = "0";
    private ZkClient zkClient;
    private final NodeInfo myNode;
    private final AtomicReference<State> state = new AtomicReference<>(State.CANDIDATE);
    private String myZkNodeName;
    private volatile NodeInfo leader;

    public Node() {
        this.myNode = new NodeInfo(ImmutableConfig.clusterTransportHost, ImmutableConfig.clusterTransportPort);
    }


    @PostConstruct
    private void register() {
        log.info("node {} registering...", this.myNode.toString());
        this.zkClient = new ZkClient(ImmutableConfig.zookeeperClientConnectString,
            ImmutableConfig.zookeeperClientSessionTimeout, ImmutableConfig.zookeeperClientConnectionTimeout, NoOpZkSerializer.INSTANCE);
        //register zk state listener
        this.zkClient.subscribeStateChanges(DefaultZkStateListener.INSTANCE);
        this.myZkNodeName = initNode();
    }


    private String initNode() {
        createPersistentNodeIfNotExist(ID_GENERATOR_CLUSTER_PATH);

        //register child watcher
        this.zkClient.subscribeChildChanges(ID_GENERATOR_CLUSTER_PATH, new NodeElectionListener());

        //register self node
        String myNodePath = this.zkClient.createEphemeralSequential(Constants.ID_GENERATOR_CLUSTER_PATH + ZK_PATH_SEPARATOR + CLUSTER_NODE_PATH_PREFIX, toBytes(this.myNode));
        return myNodePath.substring(myNodePath.lastIndexOf(ZK_PATH_SEPARATOR) + 1);
    }


    /**
     * create node if not exist
     *
     * @param path node path
     */
    public void createPersistentNodeIfNotExist(final String path) {
        boolean exists = this.zkClient.exists(path);
        if (!exists) {
            this.zkClient.createPersistent(path, true);
        }
    }


    private byte[] toBytes(final NodeInfo nodeInfo) {
        return JSON.toJSONBytes(nodeInfo);
    }

    private NodeInfo parseNodeInfo(final byte[] data) {
        return JSON.parseObject(data, NodeInfo.class);
    }


    /**
     * invoked on nodes changed
     *
     * @param children node children
     */
    private void onNodesChange(final List<String> children) {
        if (children == null || children.size() == 0) {
            log.warn("id generator cluster nodes is empty");
            return;
        }
        Collections.sort(children);
        final String leaderNodeName = children.get(0);
        if (this.myZkNodeName.equals(leaderNodeName)) {
            this.state.set(State.LEADER);
            this.leader = this.myNode;
        } else {
            this.state.set(State.FOLLOWER);
            this.leader = parseNodeInfo(this.zkClient.readData(ID_GENERATOR_CLUSTER_PATH + ZK_PATH_SEPARATOR + leaderNodeName));
        }
        log.info("node[{}] become leader", JSON.toJSONString(this.leader));
        log.info("my node state {}", this.state.get());
    }


    /**
     * get cluster leader
     *
     * @return leader
     */
    public NodeInfo getLeader() {
        return this.leader;
    }

    /**
     * get my node
     *
     * @return my node
     */
    public NodeInfo getMyNode() {
        return this.myNode;
    }

    /**
     * current node is leader or not
     *
     * @return true if leader or false
     */
    public boolean isLeader() {
        return this.state.get() == State.LEADER;
    }


    /**
     * write data to specified path
     *
     * @param path            node path
     * @param data            node data
     * @param expectedVersion node expected version
     * @return Stat
     */
    public Stat writeData(final String path, final byte[] data, final int expectedVersion) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(data, "data");
        createPersistentNodeIfNotExist(path);
        return this.zkClient.writeDataReturnStat(path, data, expectedVersion);
    }

    /**
     * write data to specified path
     *
     * @param path node path
     * @param data node data
     * @return Stat
     */
    public Stat writeData(final String path, final byte[] data) {
        return writeData(path, data, -1);
    }


    /**
     * read node data
     *
     * @param path node path
     * @param stat node stat
     * @return node data bytes
     */
    public byte[] readData(final String path, final Stat stat) {
        Objects.requireNonNull(path, "path");
        return this.zkClient.readData(path, stat);
    }


    /**
     * read node data
     *
     * @param path node path
     * @return node data bytes
     */
    public byte[] readData(final String path) {
        return readData(path, null);
    }


    /**
     * close zk client
     */
    @PreDestroy
    private void close() {
        this.zkClient.close();
    }

    private enum State {
        LEADER, FOLLOWER, CANDIDATE;
    }

    private class NodeElectionListener implements IZkChildListener {
        @Override
        public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
            Node.log.info("[{}] zk children changed => parentPath: {}, current children: {}", "NodeElectionListener",
                parentPath, CommonUtils.collectionToString(currentChilds));
            Node.this.onNodesChange(currentChilds);
        }
    }

}
