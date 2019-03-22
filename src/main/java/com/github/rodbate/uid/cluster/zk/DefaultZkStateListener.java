package com.github.rodbate.uid.cluster.zk;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkStateListener;
import org.apache.zookeeper.Watcher;


/**
 * User: jiangsongsong
 * Date: 2018/12/12
 * Time: 9:09
 */
@Slf4j
public class DefaultZkStateListener implements IZkStateListener {

    public static final DefaultZkStateListener INSTANCE = new DefaultZkStateListener();

    private DefaultZkStateListener() {
    }

    @Override
    public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
        log.info("[{}] => zk state changed: {}", logPrefix(), state);
    }

    @Override
    public void handleNewSession() throws Exception {
        log.info("[{}] => zk handle new session", logPrefix());
    }

    @Override
    public void handleSessionEstablishmentError(Throwable error) throws Exception {
        log.error("[{}] => zk session establish error", logPrefix(), error);
    }

    private String logPrefix() {
        return "ZkStateListener";
    }
}
