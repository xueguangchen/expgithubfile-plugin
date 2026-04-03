package com.supporter.prj.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 导出模板实体类
 * 用于保存自定义的导出配置模板
 *
 * @author xueguangchen
 * @version 3.0.0
 */
public class ExportTemplate implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String description;
    
    // 目录结构模板
    private String directoryPattern;  // 如: {date}/{env}/{project}
    
    // 文件命名模板
    private String filePattern;       // 如: {filename}_{date}
    
    // 说明文件模板
    private String readmeTemplate;
    
    // 模板变量
    private Map<String, String> variables;
    
    // 关联的过滤规则
    private String filterRuleIds;
    
    // 创建和修改时间
    private Date createTime;
    private Date lastModifyTime;
    
    // 是否为预设模板
    private boolean preset;

    public ExportTemplate() {
        this.variables = new HashMap<>();
        this.createTime = new Date();
        this.lastModifyTime = new Date();
    }

    public ExportTemplate(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    /**
     * 解析目录路径
     * @param basePath 基础路径
     * @param params 参数
     * @return 解析后的路径
     */
    public String resolveDirectoryPath(String basePath, Map<String, String> params) {
        if (directoryPattern == null || directoryPattern.isEmpty()) {
            return basePath;
        }
        
        String result = directoryPattern;
        Map<String, String> allParams = new HashMap<>(variables);
        if (params != null) {
            allParams.putAll(params);
        }
        
        // 添加默认变量
        if (!allParams.containsKey("date")) {
            allParams.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        }
        if (!allParams.containsKey("datetime")) {
            allParams.put("datetime", new java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date()));
        }
        
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return basePath + "/" + result;
    }

    /**
     * 解析文件名
     * @param originalName 原始文件名
     * @param params 参数
     * @return 解析后的文件名
     */
    public String resolveFileName(String originalName, Map<String, String> params) {
        if (filePattern == null || filePattern.isEmpty()) {
            return originalName;
        }
        
        String result = filePattern;
        Map<String, String> allParams = new HashMap<>(variables);
        if (params != null) {
            allParams.putAll(params);
        }
        
        // 添加默认变量
        allParams.put("filename", originalName);
        if (!allParams.containsKey("date")) {
            allParams.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        }
        
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return result;
    }

    /**
     * 生成 README 内容
     * @param exportInfo 导出信息
     * @return README 内容
     */
    public String generateReadmeContent(Map<String, Object> exportInfo) {
        if (readmeTemplate == null || readmeTemplate.isEmpty()) {
            return generateDefaultReadme(exportInfo);
        }
        
        String result = readmeTemplate;
        for (Map.Entry<String, Object> entry : exportInfo.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        
        return result;
    }

    private String generateDefaultReadme(Map<String, Object> exportInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 导出说明 ===\n\n");
        sb.append("导出时间: ").append(exportInfo.getOrDefault("exportTime", new Date())).append("\n");
        sb.append("仓库路径: ").append(exportInfo.getOrDefault("repoPath", "")).append("\n");
        sb.append("文件数量: ").append(exportInfo.getOrDefault("fileCount", 0)).append("\n");
        sb.append("提交ID: ").append(exportInfo.getOrDefault("commitIds", "")).append("\n");
        sb.append("\n=== 变更文件列表 ===\n");
        
        @SuppressWarnings("unchecked")
        java.util.List<String> files = (java.util.List<String>) exportInfo.get("files");
        if (files != null) {
            for (String file : files) {
                sb.append("- ").append(file).append("\n");
            }
        }
        
        return sb.toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDirectoryPattern() { return directoryPattern; }
    public void setDirectoryPattern(String directoryPattern) { this.directoryPattern = directoryPattern; }
    public String getFilePattern() { return filePattern; }
    public void setFilePattern(String filePattern) { this.filePattern = filePattern; }
    public String getReadmeTemplate() { return readmeTemplate; }
    public void setReadmeTemplate(String readmeTemplate) { this.readmeTemplate = readmeTemplate; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
    public String getFilterRuleIds() { return filterRuleIds; }
    public void setFilterRuleIds(String filterRuleIds) { this.filterRuleIds = filterRuleIds; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getLastModifyTime() { return lastModifyTime; }
    public void setLastModifyTime(Date lastModifyTime) { this.lastModifyTime = lastModifyTime; }
    public boolean isPreset() { return preset; }
    public void setPreset(boolean preset) { this.preset = preset; }

    @Override
    public String toString() {
        return name;
    }
}
