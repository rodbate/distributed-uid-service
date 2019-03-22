package com.github.rodbate.uid;

import com.github.rodbate.uid.common.ApplicationEnv;
import com.github.rodbate.uid.common.ImmutableConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * User: jiangsongsong
 * Date: 2018/12/10
 * Time: 14:39
 */
@SpringBootTest(classes = IdGeneratorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {"cluster.transport.port = 0"})
public class SpringApplicationBaseTest extends Assert {

    static {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }

    @Test
    public void baseTest() throws Exception {
        assertEquals(ApplicationEnv.DEV, ImmutableConfig.appEnv);
    }

}
