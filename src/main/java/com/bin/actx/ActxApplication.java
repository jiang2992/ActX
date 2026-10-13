package com.bin.actx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.DependsOn;

/**
 * Actx入口
 *
 * @author Bin
 * @since 2026/5/12 15:20
 */
@DependsOn({"springUtils", "appProperties"})
@SpringBootApplication
public class ActxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActxApplication.class, args);
    }

}
