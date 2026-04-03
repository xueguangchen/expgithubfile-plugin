package com.supporter.prj.entity;

import java.io.Serializable;
import java.util.List;

/**
 * 导出选项配置类
 * 封装导出操作的各种高级选项
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class ExportOptions implements Serializable {
    private static final long serialVersionUID = 1L;

    // 是否显示文件预览
    private boolean showFilePreview = true;
    
    // 是否过滤测试文件
    private boolean filterTestFiles = false;
    
    // 是否包含空提交
    private boolean includeEmptyCommits = false;
    
    // 是否保留文件结构
    private boolean preserveStructure = true;
    
    // 是否创建导出日志
    private boolean createExportLog = true;
    
    // 过滤规则列表
    private List<FilterRule> filterRules;

    public ExportOptions() {
    }

    // ========== Getters and Setters ==========

    public boolean isShowFilePreview() {
        return showFilePreview;
    }

    public void setShowFilePreview(boolean showFilePreview) {
        this.showFilePreview = showFilePreview;
    }

    public boolean isFilterTestFiles() {
        return filterTestFiles;
    }

    public void setFilterTestFiles(boolean filterTestFiles) {
        this.filterTestFiles = filterTestFiles;
    }

    public boolean isIncludeEmptyCommits() {
        return includeEmptyCommits;
    }

    public void setIncludeEmptyCommits(boolean includeEmptyCommits) {
        this.includeEmptyCommits = includeEmptyCommits;
    }

    public boolean isPreserveStructure() {
        return preserveStructure;
    }

    public void setPreserveStructure(boolean preserveStructure) {
        this.preserveStructure = preserveStructure;
    }

    public boolean isCreateExportLog() {
        return createExportLog;
    }

    public void setCreateExportLog(boolean createExportLog) {
        this.createExportLog = createExportLog;
    }

    public List<FilterRule> getFilterRules() {
        return filterRules;
    }

    public void setFilterRules(List<FilterRule> filterRules) {
        this.filterRules = filterRules;
    }

    /**
     * 检查文件是否应该被过滤（不导出）
     * @param filePath 文件路径
     * @return true表示应该过滤掉，false表示应该保留
     */
    public boolean shouldFilterFile(String filePath) {
        // 检查测试文件过滤
        if (filterTestFiles && isTestFile(filePath)) {
            return true;
        }
        
        // 检查自定义过滤规则
        // 多个规则取交集：文件必须匹配所有规则才保留
        if (filterRules != null && !filterRules.isEmpty()) {
            for (FilterRule rule : filterRules) {
                if (rule != null && !rule.matches(filePath)) {
                    // 只要有一个规则不匹配，就过滤掉
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * 判断文件是否为测试文件
     */
    private boolean isTestFile(String filePath) {
        String normalizedPath = filePath.replace("\\", "/").toLowerCase();
        
        // 检查测试目录
        if (normalizedPath.contains("/test/") ||
            normalizedPath.contains("/tests/") ||
            normalizedPath.contains("/__tests__/") ||
            normalizedPath.startsWith("test/") ||
            normalizedPath.startsWith("tests/")) {
            return true;
        }
        
        // 检查测试文件命名
        String fileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
        if (fileName.startsWith("test") ||
            fileName.endsWith("test.java") ||
            fileName.endsWith("test.js") ||
            fileName.endsWith("test.ts") ||
            fileName.endsWith("spec.js") ||
            fileName.endsWith("spec.ts") ||
            fileName.contains(".test.") ||
            fileName.contains(".spec.")) {
            return true;
        }
        
        return false;
    }

    @Override
    public String toString() {
        return "ExportOptions{" +
                "showFilePreview=" + showFilePreview +
                ", filterTestFiles=" + filterTestFiles +
                ", includeEmptyCommits=" + includeEmptyCommits +
                ", preserveStructure=" + preserveStructure +
                ", createExportLog=" + createExportLog +
                ", filterRules=" + (filterRules != null ? filterRules.size() + " rules" : "none") +
                '}';
    }
}
