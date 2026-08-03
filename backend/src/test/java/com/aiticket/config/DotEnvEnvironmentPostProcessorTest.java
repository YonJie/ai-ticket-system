package com.aiticket.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link DotEnvEnvironmentPostProcessor} JDBC URL 解析单测。
 */
class DotEnvEnvironmentPostProcessorTest {

    /**
     * libpq 风格 user:pass@host 应拆成独立凭据，且 URL 不含 userinfo。
     */
    @Test
    void parsesLibpqStyleUrlWithEmbeddedCredentials() {
        DotEnvEnvironmentPostProcessor.JdbcConnectionParts parts =
                DotEnvEnvironmentPostProcessor.parseJdbcConnection(
                        "postgresql://neondb_owner:secret@ep-demo.neon.tech/neondb?sslmode=require&channel_binding=require");

        assertNotNull(parts);
        assertEquals(
                "jdbc:postgresql://ep-demo.neon.tech/neondb?sslmode=require",
                parts.jdbcUrl);
        assertEquals("neondb_owner", parts.username);
        assertEquals("secret", parts.password);
    }

    /**
     * 错误地写成 jdbc:...user:pass@host 时同样应拆分（修复 PG 误把密码当端口）。
     */
    @Test
    void stripsUserInfoFromJdbcUrl() {
        DotEnvEnvironmentPostProcessor.JdbcConnectionParts parts =
                DotEnvEnvironmentPostProcessor.parseJdbcConnection(
                        "jdbc:postgresql://neondb_owner:npg_secret@ep-demo.neon.tech/neondb?sslmode=require");

        assertNotNull(parts);
        assertEquals(
                "jdbc:postgresql://ep-demo.neon.tech/neondb?sslmode=require",
                parts.jdbcUrl);
        assertEquals("neondb_owner", parts.username);
        assertEquals("npg_secret", parts.password);
    }

    /**
     * 无凭据的 JDBC URL 保持不变。
     */
    @Test
    void keepsCredentialFreeJdbcUrl() {
        DotEnvEnvironmentPostProcessor.JdbcConnectionParts parts =
                DotEnvEnvironmentPostProcessor.parseJdbcConnection(
                        "jdbc:postgresql://ep-demo.neon.tech/neondb?sslmode=require");

        assertNotNull(parts);
        assertEquals(
                "jdbc:postgresql://ep-demo.neon.tech/neondb?sslmode=require",
                parts.jdbcUrl);
        assertNull(parts.username);
        assertNull(parts.password);
    }
}
