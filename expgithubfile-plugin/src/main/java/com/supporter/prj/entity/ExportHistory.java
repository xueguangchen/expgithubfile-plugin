package com.supporter.prj.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 导出历史实体类
 * 用于记录每次导出的历史信息
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class ExportHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Date exportTime;
    private String configName;
    private String repoPath;
    private String targetPath;
    private String exportType;
    private int fileCount;
    private long totalSize;
    private List<String> commitIds;
    private String filterRuleName;
    private boolean success;
    private String errorMessage;
    private List<String> exportedFiles;

    public ExportHistory() {
        this.id = generateId();
        this.exportTime = new Date();
        this.commitIds = new ArrayList<>();
        this.exportedFiles = new ArrayList<>();
        this.success = true;
    }

    public ExportHistory(String configName, String repoPath, String targetPath) {
        this();
        this.configName = configName;
        this.repoPath = repoPath;
        this.targetPath = targetPath;
    }

    /**
     * 生成唯一ID
     */
    private String generateId() {
        return String.valueOf(System.currentTimeMillis()) + 
               String.valueOf((int)(Math.random() * 10000));
    }

    /**
     * 获取格式化的导出时间
     */
    public String getFormattedExportTime() {
        if (exportTime == null) {
            return "";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(exportTime);
    }

    /**
     * 获取格式化的文件大小
     */
    public String getFormattedTotalSize() {
        if (totalSize < 1024) {
            return totalSize + " B";
        } else if (totalSize < 1024 * 1024) {
            return String.format("%.2f KB", totalSize / 1024.0);
        } else if (totalSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", totalSize / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", totalSize / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 获取简要描述
     */
    public String getBriefDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFormattedExportTime());
        sb.append(" | ");
        sb.append(configName != null ? configName : "未命名配置");
        sb.append(" | ");
        sb.append(fileCount).append("个文件");
        sb.append(" | ");
        sb.append(getFormattedTotalSize());
        if (!success) {
            sb.append(" | 失败");
        }
        return sb.toString();
    }

    // ========== Getters and Setters ==========

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getExportTime() {
        return exportTime;
    }

    public void setExportTime(Date exportTime) {
        this.exportTime = exportTime;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getExportType() {
        return exportType;
    }

    public void setExportType(String exportType) {
        this.exportType = exportType;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public List<String> getCommitIds() {
        return commitIds;
    }

    public void setCommitIds(List<String> commitIds) {
        this.commitIds = commitIds;
    }

    public String getFilterRuleName() {
        return filterRuleName;
    }

    public void setFilterRuleName(String filterRuleName) {
        this.filterRuleName = filterRuleName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getExportedFiles() {
        return exportedFiles;
    }

    public void setExportedFiles(List<String> exportedFiles) {
        this.exportedFiles = exportedFiles;
    }

    @Override
    public String toString() {
        return getBriefDescription();
    }
}
