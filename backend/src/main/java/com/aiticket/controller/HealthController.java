package com.aiticket.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 健康检查接口。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 返回后端服务健康状态。
     *
     * @return 状态信息
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "UP");
    }
}
