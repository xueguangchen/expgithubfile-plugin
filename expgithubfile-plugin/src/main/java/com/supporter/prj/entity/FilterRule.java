package com.supporter.prj.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 过滤规则实体类
 * 用于定义文件过滤规则
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class FilterRule implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private List<String> includeExtensions;  // 包含的文件扩展名
    private List<String> excludeExtensions;  // 排除的文件扩展名
    private List<String> includePaths;       // 包含的路径模式
    private List<String> excludePaths;       // 排除的路径模式
    private List<String> regexPatterns;      // 正则表达式模式
    private boolean enabled;

    public FilterRule() {
        this.includeExtensions = new ArrayList<>();
        this.excludeExtensions = new ArrayList<>();
        this.includePaths = new ArrayList<>();
        this.excludePaths = new ArrayList<>();
        this.regexPatterns = new ArrayList<>();
        this.enabled = true;
    }

    public FilterRule(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    /**
     * 检查文件是否匹配过滤规则
     * @param filePath 文件路径
     * @return true表示文件应该被包含，false表示文件应该被排除
     */
    public boolean matches(String filePath) {
        if (!enabled) {
            return true;
        }

        String normalizedPath = filePath.replace("\\", "/");

        // 1. 检查排除的路径模式（优先级最高）
        for (String excludePath : excludePaths) {
            if (matchesPathPattern(normalizedPath, excludePath)) {
                return false;
            }
        }

        // 2. 检查排除的扩展名
        String extension = getFileExtension(normalizedPath);
        if (extension != null && excludeExtensions.contains(extension.toLowerCase())) {
            return false;
        }

        // 3. 如果有包含路径模式，检查是否匹配
        if (!includePaths.isEmpty()) {
            boolean matched = false;
            for (String includePath : includePaths) {
                if (matchesPathPattern(normalizedPath, includePath)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }

        // 4. 如果有包含扩展名，检查是否匹配
        if (!includeExtensions.isEmpty()) {
            if (extension == null || !includeExtensions.contains(extension.toLowerCase())) {
                return false;
            }
        }

        // 5. 检查正则表达式
        if (!regexPatterns.isEmpty()) {
            boolean matched = false;
            for (String regex : regexPatterns) {
                try {
                    if (Pattern.matches(regex, normalizedPath)) {
                        matched = true;
                        break;
                    }
                } catch (Exception e) {
                    // 忽略无效的正则表达式
                }
            }
            if (!matched) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查路径是否匹配模式
     */
    private boolean matchesPathPattern(String filePath, String pattern) {
        String normalizedPattern = pattern.replace("\\", "/");

        // 支持通配符 * 和 **
        String regex = normalizedPattern
                .replace(".", "\\.")
                .replace("**", ".*")
                .replace("*", "[^/]*");

        try {
            return Pattern.matches(regex, filePath) ||
                   filePath.contains(normalizedPattern);
        } catch (Exception e) {
            return filePath.contains(normalizedPattern);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));

        if (lastDot > lastSlash && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }

    // ========== 预设过滤规则 ==========

    /**
     * 创建"仅Java源码"过滤规则
     */
    public static FilterRule createJavaOnlyRule() {
        FilterRule rule = new FilterRule("仅Java源码", "只包含 .java 文件");
        rule.getIncludeExtensions().add("java");
        return rule;
    }

    /**
     * 创建"排除前端资源"过滤规则
     */
    public static FilterRule createExcludeFrontendRule() {
        FilterRule rule = new FilterRule("排除前端资源", "排除 node_modules, dist, build 目录");
        rule.getExcludePaths().add("node_modules");
        rule.getExcludePaths().add("dist");
        rule.getExcludePaths().add("build");
        rule.getExcludePaths().add(".git");
        return rule;
    }

    /**
     * 创建"后端API文件"过滤规则
     */
    public static FilterRule createBackendApiRule() {
        FilterRule rule = new FilterRule("后端API文件", "只包含 Controller, Service, Repository 文件");
        rule.getIncludeExtensions().add("java");
        rule.getRegexPatterns().add(".*Controller\\.java$");
        rule.getRegexPatterns().add(".*Service\\.java$");
        rule.getRegexPatterns().add(".*Repository\\.java$");
        rule.getRegexPatterns().add(".*Dao\\.java$");
        return rule;
    }

    /**
     * 创建"配置文件"过滤规则
     */
    public static FilterRule createConfigFilesRule() {
        FilterRule rule = new FilterRule("配置文件", "只包含配置文件 yml, properties, xml");
        rule.getIncludeExtensions().add("yml");
        rule.getIncludeExtensions().add("yaml");
        rule.getIncludeExtensions().add("properties");
        rule.getIncludeExtensions().add("xml");
        return rule;
    }

    /**
     * 创建"排除测试文件"过滤规则
     */
    public static FilterRule createExcludeTestRule() {
        FilterRule rule = new FilterRule("排除测试文件", "排除测试目录和测试文件");
        rule.getExcludePaths().add("src/test");
        rule.getExcludePaths().add("test");
        rule.getExcludePaths().add("tests");
        rule.getExcludeExtensions().add("test.js");
        rule.getExcludeExtensions().add("spec.js");
        return rule;
    }

    /**
     * 创建"Web资源"过滤规则
     */
    public static FilterRule createWebResourcesRule() {
        FilterRule rule = new FilterRule("Web资源", "只包含前端资源文件");
        rule.getIncludeExtensions().add("html");
        rule.getIncludeExtensions().add("css");
        rule.getIncludeExtensions().add("js");
        rule.getIncludeExtensions().add("vue");
        rule.getIncludeExtensions().add("jsx");
        rule.getIncludeExtensions().add("ts");
        rule.getIncludeExtensions().add("tsx");
        return rule;
    }

    /**
     * 创建"SQL和数据库文件"过滤规则
     */
    public static FilterRule createSqlFilesRule() {
        FilterRule rule = new FilterRule("SQL和数据库文件", "只包含 SQL 和数据库相关文件");
        rule.getIncludeExtensions().add("sql");
        rule.getIncludeExtensions().add("ddl");
        rule.getIncludeExtensions().add("dml");
        return rule;
    }

    /**
     * 获取所有预设过滤规则
     */
    public static List<FilterRule> getPresetRules() {
        List<FilterRule> rules = new ArrayList<>();
        rules.add(createJavaOnlyRule());
        rules.add(createExcludeFrontendRule());
        rules.add(createBackendApiRule());
        rules.add(createConfigFilesRule());
        rules.add(createExcludeTestRule());
        rules.add(createWebResourcesRule());
        rules.add(createSqlFilesRule());
        return rules;
    }

    // ========== Getters and Setters ==========

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getIncludeExtensions() {
        return includeExtensions;
    }

    public void setIncludeExtensions(List<String> includeExtensions) {
        this.includeExtensions = includeExtensions;
    }

    public List<String> getExcludeExtensions() {
        return excludeExtensions;
    }

    public void setExcludeExtensions(List<String> excludeExtensions) {
        this.excludeExtensions = excludeExtensions;
    }

    public List<String> getIncludePaths() {
        return includePaths;
    }

    public void setIncludePaths(List<String> includePaths) {
        this.includePaths = includePaths;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public List<String> getRegexPatterns() {
        return regexPatterns;
    }

    public void setRegexPatterns(List<String> regexPatterns) {
        this.regexPatterns = regexPatterns;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return name + (description != null ? " - " + description : "");
    }
}
