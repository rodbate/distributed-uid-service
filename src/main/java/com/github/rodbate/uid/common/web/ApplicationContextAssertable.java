package com.github.rodbate.uid.common.web;

import com.github.rodbate.uid.utils.WebUtil;

/**
 * User: jiangsongsong
 * Date: 2019/1/5
 * Time: 11:16
 */
public abstract class ApplicationContextAssertable {

    public ApplicationContextAssertable() {
        WebUtil.assertApplicationContext();
    }
}
