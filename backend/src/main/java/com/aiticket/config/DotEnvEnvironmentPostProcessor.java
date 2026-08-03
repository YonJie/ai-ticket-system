package com.aiticket.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地开发时自动加载项目根目录或 backend 目录下的 {@code .env}，
 * 并将 Neon 常见的 {@code postgresql://} 连接串规范为 JDBC URL。
 * <p>
 * 已由操作系统或启动参数注入的同名变量优先级更高，不会被覆盖。
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";
    private static final String DATABASE_URL = "DATABASE_URL";
    private static final String SPRING_DATASOURCE_URL = "SPRING_DATASOURCE_URL";

    /**
     * 在 Spring Environment 构建早期注入 .env 中的键值。
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
        if (dotenvValues.isEmpty()) {
            normalizeExistingJdbcUrls(environment);
            return;
        }

        normalizeDatabaseUrlInMap(dotenvValues);
        // 不覆盖已存在的系统/环境变量
        Map<String, Object> toAdd = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : dotenvValues.entrySet()) {
            if (!StringUtils.hasText(environment.getProperty(entry.getKey()))) {
                toAdd.put(entry.getKey(), entry.getValue());
            }
        }
        // 仅在提供时写入，避免空 username 覆盖 JDBC URL 内嵌凭据
        putIfPresent(toAdd, environment, "SPRING_DATASOURCE_USERNAME", "spring.datasource.username");
        putIfPresent(toAdd, environment, "SPRING_DATASOURCE_PASSWORD", "spring.datasource.password");
        if (!toAdd.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, toAdd));
        }
        normalizeExistingJdbcUrls(environment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
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
     * 将 Map 中的 DATABASE_URL / SPRING_DATASOURCE_URL 规范为可用的 JDBC URL。
     *
     * @param values 属性 Map
     */
    private static void normalizeDatabaseUrlInMap(Map<String, Object> values) {
        normalizeKeyInMap(values, DATABASE_URL);
        normalizeKeyInMap(values, SPRING_DATASOURCE_URL);
    }

    /**
     * 规范单个 URL 键。
     *
     * @param values 属性 Map
     * @param key    键名
     */
    private static void normalizeKeyInMap(Map<String, Object> values, String key) {
        Object raw = values.get(key);
        if (raw instanceof String) {
            String normalized = toJdbcUrl((String) raw);
            if (normalized != null) {
                values.put(key, normalized);
            }
        }
    }

    /**
     * 若 Environment 中已有非 JDBC / 含不兼容参数的连接串，则高优先级覆盖为可用 JDBC 形式。
     *
     * @param environment 环境
     */
    private static void normalizeExistingJdbcUrls(ConfigurableEnvironment environment) {
        Map<String, Object> override = new LinkedHashMap<String, Object>();
        putNormalizedOverride(override, environment, DATABASE_URL);
        putNormalizedOverride(override, environment, SPRING_DATASOURCE_URL);
        if (!override.isEmpty()) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource(PROPERTY_SOURCE_NAME + "-jdbc-normalize", override));
        }
    }

    /**
     * 比较并写入需要覆盖的规范化 URL。
     *
     * @param override    覆盖 Map
     * @param environment 环境
     * @param key         键名
     */
    private static void putNormalizedOverride(Map<String, Object> override,
                                              ConfigurableEnvironment environment,
                                              String key) {
        String raw = environment.getProperty(key);
        String normalized = toJdbcUrl(raw);
        if (normalized != null && !normalized.equals(raw)) {
            override.put(key, normalized);
        }
    }

    /**
     * 若环境中尚未设置目标属性，则从别名键复制到 Spring DataSource 属性。
     *
     * @param toAdd       待添加属性
     * @param environment 当前环境
     * @param sourceKey   源键（如 SPRING_DATASOURCE_USERNAME）
     * @param targetKey   目标键（如 spring.datasource.username）
     */
    private static void putIfPresent(Map<String, Object> toAdd, ConfigurableEnvironment environment,
                                     String sourceKey, String targetKey) {
        if (StringUtils.hasText(environment.getProperty(targetKey))) {
            return;
        }
        Object value = toAdd.get(sourceKey);
        if (value == null) {
            value = environment.getProperty(sourceKey);
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            toAdd.put(targetKey, value);
        }
    }

    /**
     * 将 {@code postgresql://...} 转为 {@code jdbc:postgresql://...}；已是 JDBC 则原样返回。
     *
     * @param url 原始 URL
     * @return JDBC URL；无法处理时返回 null
     */
    static String toJdbcUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("postgresql://") || trimmed.startsWith("postgres://")) {
            trimmed = "jdbc:" + trimmed.replaceFirst("^postgres://", "postgresql://");
        }
        if (!trimmed.startsWith("jdbc:")) {
            return null;
        }
        // Neon 控制台常带 channel_binding=require，旧版 PG JDBC 可能不兼容
        return stripQueryParam(trimmed, "channel_binding");
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
}
