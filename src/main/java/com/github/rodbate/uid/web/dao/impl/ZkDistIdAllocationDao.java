package com.github.rodbate.uid.web.dao.impl;

import com.github.rodbate.uid.cluster.Node;
import com.github.rodbate.uid.web.dao.DistIdAllocationDao;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;

import static com.github.rodbate.uid.common.Constants.ID_GENERATOR_DATA_PATH;

/**
 * User: jiangsongsong
 * Date: 2018/12/24
 * Time: 12:11
 */
@Repository
public class ZkDistIdAllocationDao implements DistIdAllocationDao {

    private final Node node;

    public ZkDistIdAllocationDao(Node node) {
        this.node = node;
    }

    @Override
    public long incr(String bizType, int step) {
        final String idPath = getIdPath(bizType);
        this.node.createPersistentNodeIfNotExist(idPath);
        byte[] maxIdBytes = this.node.readData(idPath);
        long maxId = maxIdBytes == null ? 1 : Long.parseLong(new String(maxIdBytes, StandardCharsets.UTF_8));
        byte[] nextMaxId = Long.toString(maxId + step).getBytes(StandardCharsets.UTF_8);
        this.node.writeData(idPath, nextMaxId);
        return maxId;
    }

    @Override
    public long getMaxIdByBizType(String bizType) {
        byte[] maxIdBytes = this.node.readData(getIdPath(bizType));
        return Long.parseLong(new String(maxIdBytes, StandardCharsets.UTF_8));
    }


    private String getIdPath(String bizType) {
        return ID_GENERATOR_DATA_PATH + "/" + bizType;
    }
}
