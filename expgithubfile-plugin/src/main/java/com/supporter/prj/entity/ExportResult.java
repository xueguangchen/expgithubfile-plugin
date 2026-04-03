package com.supporter.prj.entity;

/**
 * 导出结果实体类
 * 用于返回导出的统计信息
 *
 * @author xueguangchen
 * @version 1.0.0
 */
public class ExportResult {
    private int fileCount;
    private long totalSize;

    public ExportResult() {
        this.fileCount = 0;
        this.totalSize = 0;
    }

    public ExportResult(int fileCount, long totalSize) {
        this.fileCount = fileCount;
        this.totalSize = totalSize;
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
}
