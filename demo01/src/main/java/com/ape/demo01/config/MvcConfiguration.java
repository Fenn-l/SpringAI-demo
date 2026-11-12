package com.ape.demo01.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置（课件 1.3.3）
 *
 * <p>前端 Vite 跑在 5173，后端跑在 8080，属于跨域，需要在服务端放开规则。
 */
@Configuration
public class MvcConfiguration implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 匹配后端所有接口路径，对全部接口启用跨域规则。
        registry.addMapping("/**")
                // Origin（前端来源）允许所有域名访问
                .allowedOrigins("*")
                // 允许前端发起的请求方法：GET 查、POST 新增、PUT 修改、DELETE 删除
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许请求携带任意请求头（token、自定义 header 等）。
                .allowedHeaders("*")
                // Content-Disposition一般用于文件下载场景（告诉浏览器弹出下载框、文件名）。
                // 不加这行，前端拿不到这个响应头，无法获取下载文件名。
                .exposedHeaders("Content-Disposition");
    }
}
