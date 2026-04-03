package com.supporter.prj.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.supporter.prj.view.ConfigManagePanel;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 配置管理工具类
 * 负责保存、加载和管理导出配置
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class ConfigManager {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.ExpGitFilePrj";
    private static final String CONFIG_FILE = CONFIG_DIR + "/configs.json";
    private static final String EXPORTED_CONFIG_DIR = CONFIG_DIR + "/exported_configs";

    private static ConfigManager instance;
    private List<ConfigManagePanel.ConfigItem> configs;

    private ConfigManager() {
        configs = new ArrayList<>();
        loadAllConfigsFromFile();
    }

    /**
     * 获取单例实例
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * 获取配置文件目录
     */
    private static File getConfigDir() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 保存配置
     */
    public void saveConfig(ConfigManagePanel.ConfigItem config) {
        // 检查是否已存在同名配置
        Optional<ConfigManagePanel.ConfigItem> existing = configs.stream()
                .filter(c -> c.getName().equals(config.getName()))
                .findFirst();

        if (existing.isPresent()) {
            // 更新现有配置
            int index = configs.indexOf(existing.get());
            configs.set(index, config);
        } else {
            // 添加新配置
            configs.add(config);
        }

        saveAllConfigs();
    }

    /**
     * 删除配置
     */
    public void deleteConfig(String configName) {
        configs.removeIf(c -> c.getName().equals(configName));
        saveAllConfigs();
    }

    /**
     * 获取所有配置
     */
    public List<ConfigManagePanel.ConfigItem> getAllConfigs() {
        return new ArrayList<>(configs);
    }

    /**
     * 根据名称获取配置
     */
    public ConfigManagePanel.ConfigItem getConfigByName(String name) {
        return configs.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 搜索配置
     */
    public List<ConfigManagePanel.ConfigItem> searchConfigs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllConfigs();
        }

        String lowerKeyword = keyword.toLowerCase();
        return configs.stream()
                .filter(c -> 
                    c.getName().toLowerCase().contains(lowerKeyword) ||
                    c.getRepoPath().toLowerCase().contains(lowerKeyword) ||
                    c.getTargetPath().toLowerCase().contains(lowerKeyword)
                )
                .collect(Collectors.toList());
    }

    /**
     * 加载配置（兼容旧方法）
     */
    @Deprecated
    public static void saveConfigStatic(ConfigManagePanel.ConfigItem config) {
        getInstance().saveConfig(config);
    }

    /**
     * 加载所有配置（兼容旧方法）
     */
    @Deprecated
    public static List<ConfigManagePanel.ConfigItem> loadAllConfigs() {
        return getInstance().getAllConfigs();
    }

    /**
     * 删除配置（兼容旧方法）
     */
    @Deprecated
    public static void deleteConfigStatic(String configName) {
        getInstance().deleteConfig(configName);
    }

    /**
     * 获取最近的配置（兼容旧方法）
     */
    @Deprecated
    public static ConfigManagePanel.ConfigItem getRecentConfig() {
        List<ConfigManagePanel.ConfigItem> allConfigs = getInstance().getAllConfigs();
        return allConfigs.isEmpty() ? null : allConfigs.get(0);
    }

    /**
     * 创建示例配置数据（兼容旧方法）
     */
    @Deprecated
    public static ConfigManagePanel.ConfigItem createSampleConfig(String name, String repoPath, String targetPath) {
        return new ConfigManagePanel.ConfigItem(
                name,
                repoPath,
                targetPath,
                "已提交",
                "最近20个提交",
                "*.java,*.yml",
                false,
                true
        );
    }

    /**
     * 从文件加载所有配置
     */
    private void loadAllConfigsFromFile() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            // 如果配置文件不存在，加载默认配置
            configs = getDefaultConfigs();
            return;
        }

        try {
            // 读取JSON格式配置
            configs = loadFromJson(configFile);
            if (configs.isEmpty()) {
                configs = getDefaultConfigs();
            }
        } catch (Exception e) {
            // 尝试加载旧格式配置
            try {
                configs = loadFromProperties(configFile);
            } catch (Exception ex) {
                configs = getDefaultConfigs();
            }
        }
    }

    /**
     * 保存所有配置
     */
    private void saveAllConfigs() {
        try {
            // 确保目录存在
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 保存为JSON格式
            saveToJson(new File(CONFIG_FILE), configs);
        } catch (Exception e) {
            throw new RuntimeException("保存配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从JSON文件加载配置
     */
    private List<ConfigManagePanel.ConfigItem> loadFromJson(File file) throws IOException {
        List<ConfigManagePanel.ConfigItem> result = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            String content = sb.toString().trim();
            if (content.isEmpty() || content.equals("[]")) {
                return result;
            }

            // 简单JSON解析（不依赖外部库）
            content = content.substring(1, content.length() - 1); // 移除 [ ]
            String[] items = content.split("\\},\\s*\\{");
            
            for (String item : items) {
                item = item.replace("{", "").replace("}", "").trim();
                if (item.isEmpty()) continue;
                
                ConfigManagePanel.ConfigItem config = parseConfigItem(item);
                if (config != null) {
                    result.add(config);
                }
            }
        }
        
        return result;
    }

    /**
     * 解析配置项
     */
    private ConfigManagePanel.ConfigItem parseConfigItem(String json) {
        try {
            String name = extractJsonValue(json, "name");
            String repoPath = extractJsonValue(json, "repoPath");
            String targetPath = extractJsonValue(json, "targetPath");
            String exportType = extractJsonValue(json, "exportType");
            String commitRange = extractJsonValue(json, "commitRange");
            String filterRule = extractJsonValue(json, "filterRule");
            boolean includeEmptyCommits = Boolean.parseBoolean(extractJsonValue(json, "includeEmptyCommits"));
            boolean preserveStructure = Boolean.parseBoolean(extractJsonValue(json, "preserveStructure"));
            boolean showFilePreview = Boolean.parseBoolean(extractJsonValue(json, "showFilePreview"));
            boolean filterTestFiles = Boolean.parseBoolean(extractJsonValue(json, "filterTestFiles"));
            boolean createExportLog = Boolean.parseBoolean(extractJsonValue(json, "createExportLog"));

            ConfigManagePanel.ConfigItem item = new ConfigManagePanel.ConfigItem(
                    name, repoPath, targetPath, exportType, 
                    commitRange, filterRule, includeEmptyCommits, preserveStructure,
                    showFilePreview, filterTestFiles, createExportLog
            );
            
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取JSON值
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return "";

        startIndex += searchKey.length();
        while (startIndex < json.length() && (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '"')) {
            startIndex++;
        }

        int endIndex = startIndex;
        while (endIndex < json.length() && json.charAt(endIndex) != ',' && json.charAt(endIndex) != '}') {
            endIndex++;
        }

        String value = json.substring(startIndex, endIndex).trim();
        if (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        
        // 反转义JSON字符
        return unescapeJson(value);
    }
    
    /**
     * JSON字符串反转义
     */
    private String unescapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\n", "\n")
                   .replace("\\r", "\r")
                   .replace("\\t", "\t")
                   .replace("\\\"", "\"")
                   .replace("\\\\", "\\");
    }

    /**
     * 保存为JSON文件
     */
    private void saveToJson(File file, List<ConfigManagePanel.ConfigItem> configs) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("[");
            for (int i = 0; i < configs.size(); i++) {
                ConfigManagePanel.ConfigItem config = configs.get(i);
                writer.println("  {");
                writer.println("    \"name\": \"" + escapeJson(config.getName()) + "\",");
                writer.println("    \"repoPath\": \"" + escapeJson(config.getRepoPath()) + "\",");
                writer.println("    \"targetPath\": \"" + escapeJson(config.getTargetPath()) + "\",");
                writer.println("    \"exportType\": \"" + escapeJson(config.getExportType()) + "\",");
                writer.println("    \"commitRange\": \"" + escapeJson(config.getCommitRange()) + "\",");
                writer.println("    \"filterRule\": \"" + escapeJson(config.getFilterRule()) + "\",");
                writer.println("    \"includeEmptyCommits\": " + config.isIncludeEmptyCommits() + ",");
                writer.println("    \"preserveStructure\": " + config.isPreserveStructure() + ",");
                writer.println("    \"showFilePreview\": " + config.isShowFilePreview() + ",");
                writer.println("    \"filterTestFiles\": " + config.isFilterTestFiles() + ",");
                writer.println("    \"createExportLog\": " + config.isCreateExportLog());
                if (i < configs.size() - 1) {
                    writer.println("  },");
                } else {
                    writer.println("  }");
                }
            }
            writer.println("]");
        }
    }

    /**
     * JSON字符串转义
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * 从Properties文件加载（兼容旧格式）
     */
    private List<ConfigManagePanel.ConfigItem> loadFromProperties(File configFile) throws IOException {
        List<ConfigManagePanel.ConfigItem> result = new ArrayList<>();
        Properties props = new Properties();
        
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        }

        int count = Integer.parseInt(props.getProperty("config.count", "0"));
        for (int i = 0; i < count; i++) {
            String prefix = "config." + i + ".";
            String name = props.getProperty(prefix + "name");
            if (name != null) {
                ConfigManagePanel.ConfigItem config = new ConfigManagePanel.ConfigItem(
                        props.getProperty(prefix + "name"),
                        props.getProperty(prefix + "repoPath"),
                        props.getProperty(prefix + "targetPath"),
                        props.getProperty(prefix + "exportType"),
                        props.getProperty(prefix + "commitRange"),
                        props.getProperty(prefix + "filterRule"),
                        Boolean.parseBoolean(props.getProperty(prefix + "includeEmptyCommits", "false")),
                        Boolean.parseBoolean(props.getProperty(prefix + "preserveStructure", "true"))
                );
                result.add(config);
            }
        }

        return result;
    }

    /**
     * 获取默认配置
     */
    private List<ConfigManagePanel.ConfigItem> getDefaultConfigs() {
        return new ArrayList<>();
    }

    // ========== 配置导出导入功能 ==========

    /**
     * 导出配置到文件
     */
    public void exportConfig(ConfigManagePanel.ConfigItem config, String filePath) throws IOException {
        File exportDir = new File(EXPORTED_CONFIG_DIR);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        List<ConfigManagePanel.ConfigItem> singleConfig = new ArrayList<>();
        singleConfig.add(config);
        saveToJson(new File(filePath), singleConfig);
    }

    /**
     * 导出所有配置
     */
    public void exportAllConfigs(String filePath) throws IOException {
        saveToJson(new File(filePath), configs);
    }

    /**
     * 从文件导入配置
     */
    public void importConfig(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("配置文件不存在: " + filePath);
        }

        List<ConfigManagePanel.ConfigItem> importedConfigs = loadFromJson(file);
        
        for (ConfigManagePanel.ConfigItem config : importedConfigs) {
            // 检查是否已存在同名配置
            Optional<ConfigManagePanel.ConfigItem> existing = configs.stream()
                    .filter(c -> c.getName().equals(config.getName()))
                    .findFirst();

            if (existing.isPresent()) {
                // 重命名导入的配置
                config.setName(config.getName() + "_导入_" + System.currentTimeMillis());
            }
            
            configs.add(config);
        }

        saveAllConfigs();
    }

    /**
     * 获取导出配置目录
     */
    public String getExportedConfigDir() {
        return EXPORTED_CONFIG_DIR;
    }
}
