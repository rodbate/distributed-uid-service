package com.github.rodbate.uid.cluster.zk;

import com.alibaba.fastjson.JSON;
import com.github.rodbate.uid.cluster.NodeInfo;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

/**
 * User: jiangsongsong
 * Date: 2018/12/12
 * Time: 9:01
 */
@Slf4j
public class NodeZkSerializer implements ZkSerializer {

    public static final NodeZkSerializer INSTANCE = new NodeZkSerializer();

    private NodeZkSerializer() {
    }

    @Override
    public byte[] serialize(Object data) throws ZkMarshallingError {
        return JSON.toJSONBytes(data);
    }

    @Override
    public Object deserialize(byte[] bytes) throws ZkMarshallingError {
        return JSON.parseObject(bytes, NodeInfo.class);
    }

}
