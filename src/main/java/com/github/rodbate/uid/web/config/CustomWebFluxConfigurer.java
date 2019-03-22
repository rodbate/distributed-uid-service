package com.github.rodbate.uid.web.config;

import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * User: jiangsongsong
 * Date: 2019/1/25
 * Time: 9:44
 */
//@Component
public class CustomWebFluxConfigurer implements WebFluxConfigurer {


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("*").allowedHeaders("*").allowedMethods("*");
    }

}
