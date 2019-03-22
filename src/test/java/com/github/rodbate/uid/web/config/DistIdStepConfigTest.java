package com.github.rodbate.uid.web.config;

import com.github.rodbate.uid.SpringApplicationBaseTest;
import com.github.rodbate.uid.enums.BizTypeEnum;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * User: jiangsongsong
 * Date: 2018/12/11
 * Time: 10:42
 */
@Ignore
@TestPropertySource(properties = { "dist-id.config.step.default = 1", "dist-id.config.step.cookie-id = 2",  "dist-id.config.step.test = 3"})
public class DistIdStepConfigTest extends SpringApplicationBaseTest {

    @Autowired
    private DistIdStepConfig distIdStepConfig;

    @Test
    public void test() {
        assertEquals(2, distIdStepConfig.getStep(BizTypeEnum.COOKIE_ID));

        boolean exFlag = false;
        try {
            distIdStepConfig.getStepWithCheckBizType("test");
        } catch (IllegalArgumentException ex) {
            //reach here
            exFlag = true;
        }
        assertTrue(exFlag);

        assertEquals(2, distIdStepConfig.getStepWithCheckBizType("cookie-id"));
        assertEquals(1, distIdStepConfig.getStepWithNotCheckBizType("test"));

    }
}
