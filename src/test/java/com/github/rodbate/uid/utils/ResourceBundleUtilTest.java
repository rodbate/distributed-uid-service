package com.github.rodbate.uid.utils;

import com.github.rodbate.uid.BaseTest;
import org.junit.Test;

import java.util.Locale;

/**
 * User: jiangsongsong
 * Date: 2018/12/11
 * Time: 12:15
 */
public class ResourceBundleUtilTest extends BaseTest {


    @Test
    public void test() {
        assertEquals("zh_cn", ResourceBundleUtil.getMessage("test", Locale.SIMPLIFIED_CHINESE, "10"));
        assertEquals("en_us", ResourceBundleUtil.getMessage("test", Locale.US, "10"));
        assertEquals("", ResourceBundleUtil.getMessage("test1", Locale.US, "10"));
        assertEquals("zh_cn", ResourceBundleUtil.getMessage("test", Locale.TAIWAN, "10"));
        assertEquals("", ResourceBundleUtil.getMessage("test", Locale.TAIWAN, "20"));
        assertEquals("", ResourceBundleUtil.getMessage("test", Locale.US, "20"));
    }

}
