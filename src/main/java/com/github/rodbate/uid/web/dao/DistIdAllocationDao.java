package com.github.rodbate.uid.web.dao;


/**
 * User: jiangsongsong
 * Date: 2018/12/10
 * Time: 16:55
 */
public interface DistIdAllocationDao {

    /**
     * increment allocation id segment by specified step
     *
     * @param bizType business type
     * @param step step
     * @return lower segment id(exclusive) eg (a, b], return a
     */
    long incr(String bizType, int step);

    /**
     * query the id segment allocation by the bizType
     *
     * @param bizType business type
     * @return max id for the bizType
     */
    long getMaxIdByBizType(String bizType);

}
