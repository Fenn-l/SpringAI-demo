package com.ape.demo01;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类（项目入口）。
 *
 * <p>@SpringBootApplication 等价于 @Configuration + @EnableAutoConfiguration + @ComponentScan，
 * 会自动扫描本包及子包下的 Bean。
 * @MapperScan 指定 MyBatis-Plus 的 Mapper 接口扫描路径，让 Mapper 被注入容器。
 */
@SpringBootApplication
@MapperScan("com.ape.demo01.mapper")
public class Demo01Application {

    /** 程序入口：启动内嵌 Tomcat 并初始化 Spring 容器。 */
    public static void main(String[] args) {
        SpringApplication.run(Demo01Application.class, args);
    }

}
