package com.github.rodbate.uid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ID生成服务入口
 * <p>
 * User: jiangsongsong
 * Date: 2018/12/8
 * Time: 15:50
 */
@SpringBootApplication
public class IdGeneratorApplication {

    /**
     * 服务主程序入口
     *
     * @param args command args
     */
    public static void main(String[] args) {
        SpringApplication.run(IdGeneratorApplication.class, args).registerShutdownHook();
    }

}
