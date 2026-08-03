package com.aiticket.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地开发时自动加载项目根目录或 backend 目录下的 {@code .env}，
 * 并将 Neon / libpq 风格连接串规范为 Spring + PG JDBC 可用的形式。
 * <p>
 * PG JDBC <strong>不接受</strong> {@code jdbc:postgresql://user:pass@host/db}（会把密码误解析为端口），
 * 因此会拆出 username/password，URL 只保留 host/db/query。
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";
    private static final String DATABASE_URL = "DATABASE_URL";
    private static final String SPRING_DATASOURCE_URL = "SPRING_DATASOURCE_URL";

    /**
     * 在 Spring Environment 构建早期注入 .env 中的键值，并规范化数据源。
     *
     * @param environment 可配置环境
     * @param application Spring 应用
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> dotenvValues = new LinkedHashMap<String, Object>();
        for (Path candidate : candidateEnvFiles()) {
            if (candidate != null && Files.isRegularFile(candidate)) {
                mergeFile(candidate, dotenvValues);
            }
        }

        Map<String, Object> toAdd = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : dotenvValues.entrySet()) {
            if (!StringUtils.hasText(environment.getProperty(entry.getKey()))) {
                toAdd.put(entry.getKey(), entry.getValue());
            }
        }
        if (!toAdd.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, toAdd));
        }

        applyJdbcNormalization(environment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    /**
     * 候选 .env 路径：当前工作目录、其父目录（从 backend/ 启动时）。
     *
     * @return 候选路径列表
     */
    private static Path[] candidateEnvFiles() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        return new Path[]{
                cwd.resolve(".env"),
                cwd.getParent() != null ? cwd.getParent().resolve(".env") : null
        };
    }

    /**
     * 解析 .env 文件并合并进目标 Map（后者不覆盖已有键）。
     *
     * @param file   .env 文件
     * @param target 目标键值
     */
    private static void mergeFile(Path file, Map<String, Object> target) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = stripQuotes(line.substring(eq + 1).trim());
                if (!target.containsKey(key)) {
                    target.put(key, value);
                }
            }
        } catch (IOException ignored) {
            // 本地可选配置；读取失败时交由原有环境变量处理
        }
    }

    /**
     * 去掉首尾成对引号。
     *
     * @param value 原始值
     * @return 去引号后的值
     */
    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    /**
     * 将 DATABASE_URL / SPRING_DATASOURCE_* 规范为 Spring DataSource 可用属性。
     *
     * @param environment 环境
     */
    private static void applyJdbcNormalization(ConfigurableEnvironment environment) {
        String springUrl = environment.getProperty(SPRING_DATASOURCE_URL);
        String databaseUrl = environment.getProperty(DATABASE_URL);
        String primaryRaw = StringUtils.hasText(springUrl) ? springUrl : databaseUrl;
        if (!StringUtils.hasText(primaryRaw)) {
            return;
        }

        JdbcConnectionParts primary = parseJdbcConnection(primaryRaw);
        if (primary == null) {
            return;
        }

        // 主 URL 无 userinfo 时，尝试从 DATABASE_URL 提取账号
        if (!StringUtils.hasText(primary.username)
                && StringUtils.hasText(databaseUrl)
                && !databaseUrl.equals(primaryRaw)) {
            JdbcConnectionParts fromDb = parseJdbcConnection(databaseUrl);
            if (fromDb != null && StringUtils.hasText(fromDb.username)) {
                primary = new JdbcConnectionParts(primary.jdbcUrl, fromDb.username, fromDb.password);
            }
        }

        // 显式 SPRING_DATASOURCE_USERNAME / PASSWORD 优先
        String explicitUser = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_USERNAME"),
                environment.getProperty("spring.datasource.username"));
        String explicitPass = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
                environment.getProperty("spring.datasource.password"));
        if (StringUtils.hasText(explicitUser)) {
            primary = new JdbcConnectionParts(
                    primary.jdbcUrl,
                    explicitUser,
                    StringUtils.hasText(explicitPass) ? explicitPass : primary.password);
        }

        Map<String, Object> override = new LinkedHashMap<String, Object>();
        override.put("spring.datasource.url", primary.jdbcUrl);
        if (StringUtils.hasText(databaseUrl)) {
            JdbcConnectionParts dbParts = parseJdbcConnection(databaseUrl);
            if (dbParts != null) {
                override.put(DATABASE_URL, dbParts.jdbcUrl);
            }
        }
        if (StringUtils.hasText(springUrl)) {
            JdbcConnectionParts springParts = parseJdbcConnection(springUrl);
            if (springParts != null) {
                override.put(SPRING_DATASOURCE_URL, springParts.jdbcUrl);
            }
        }
        if (StringUtils.hasText(primary.username)) {
            override.put("spring.datasource.username", primary.username);
        }
        if (primary.password != null) {
            override.put("spring.datasource.password", primary.password);
        }

        environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME + "-jdbc-normalize", override));
    }

    /**
     * 返回第一个非空字符串。
     *
     * @param values 候选值
     * @return 第一个有文本的值，或 null
     */
    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 解析 libpq / JDBC 连接串，拆出无 userinfo 的 JDBC URL 与账号密码。
     *
     * @param url 原始 URL
     * @return 解析结果；无法识别时返回 null
     */
    static JdbcConnectionParts parseJdbcConnection(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("postgres://")) {
            trimmed = "jdbc:postgresql://" + trimmed.substring("postgres://".length());
        } else if (trimmed.startsWith("postgresql://")) {
            trimmed = "jdbc:" + trimmed;
        }
        if (!trimmed.startsWith("jdbc:postgresql://")) {
            return null;
        }

        String rest = trimmed.substring("jdbc:postgresql://".length());
        int slash = rest.indexOf('/');
        String authority = slash >= 0 ? rest.substring(0, slash) : rest;
        String pathAndQuery = slash >= 0 ? rest.substring(slash) : "";

        String username = null;
        String password = null;
        String hostPort = authority;
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            String userInfo = authority.substring(0, at);
            hostPort = authority.substring(at + 1);
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                username = urlDecode(userInfo.substring(0, colon));
                password = urlDecode(userInfo.substring(colon + 1));
            } else {
                username = urlDecode(userInfo);
            }
        }

        String jdbcUrl = stripQueryParam("jdbc:postgresql://" + hostPort + pathAndQuery, "channel_binding");
        return new JdbcConnectionParts(jdbcUrl, username, password);
    }

    /**
     * URL 解码（UTF-8）；失败时返回原文。
     *
     * @param value 编码值
     * @return 解码值
     */
    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    /**
     * 从 JDBC URL 中移除指定 query 参数。
     *
     * @param url   JDBC URL
     * @param param 参数名
     * @return 清理后的 URL
     */
    static String stripQueryParam(String url, String param) {
        int q = url.indexOf('?');
        if (q < 0) {
            return url;
        }
        String base = url.substring(0, q);
        String query = url.substring(q + 1);
        StringBuilder kept = new StringBuilder();
        for (String part : query.split("&")) {
            if (part.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            String name = eq >= 0 ? part.substring(0, eq) : part;
            if (param.equalsIgnoreCase(name)) {
                continue;
            }
            if (kept.length() > 0) {
                kept.append('&');
            }
            kept.append(part);
        }
        return kept.length() == 0 ? base : base + "?" + kept;
    }

    /**
     * JDBC 连接解析结果。
     */
    static final class JdbcConnectionParts {
        final String jdbcUrl;
        final String username;
        final String password;

        /**
         * @param jdbcUrl  无 userinfo 的 JDBC URL
         * @param username 用户名（可空）
         * @param password 密码（可空）
         */
        JdbcConnectionParts(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }
    }
}
