package com.aiticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 智能客服工单系统后端启动类。
 */
@SpringBootApplication
public class AiTicketBackendApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AiTicketBackendApplication.class, args);
    }
}
