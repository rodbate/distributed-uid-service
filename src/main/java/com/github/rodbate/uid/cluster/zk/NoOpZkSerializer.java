package com.github.rodbate.uid.cluster.zk;

import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

/**
 * User: jiangsongsong
 * Date: 2018/12/24
 * Time: 11:51
 */
public class NoOpZkSerializer implements ZkSerializer {

    public static final NoOpZkSerializer INSTANCE = new NoOpZkSerializer();

    private NoOpZkSerializer() {
    }

    @Override
    public byte[] serialize(Object data) throws ZkMarshallingError {
        return (byte[]) data;
    }

    @Override
    public Object deserialize(byte[] bytes) throws ZkMarshallingError {
        return bytes;
    }
}
