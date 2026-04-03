package com.supporter.prj.entity;

/**
 * 文件差异条目
 * 用于差异对比视图
 *
 * @author xueguangchen
 * @version 3.0.0
 */
public class DiffEntryItem {
    private String filePath;
    private String changeType;      // ADD, MODIFY, DELETE, RENAME
    private String oldContent;      // 修改前的内容
    private String newContent;      // 修改后的内容
    private int addedLines;         // 新增行数
    private int deletedLines;       // 删除行数
    private int modifiedLines;      // 修改行数
    private String oldFilePath;     // 重命名前的路径（仅 RENAME 类型）
    
    public DiffEntryItem() {}
    
    public DiffEntryItem(String filePath, String changeType) {
        this.filePath = filePath;
        this.changeType = changeType;
    }

    /**
     * 获取差异摘要
     */
    public String getDiffSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(getChangeTypeIcon()).append(" ");
        sb.append(filePath);
        sb.append(" (+").append(addedLines).append(" -").append(deletedLines).append(")");
        return sb.toString();
    }
    
    /**
     * 获取变更类型图标
     */
    public String getChangeTypeIcon() {
        switch (changeType) {
            case "ADD": return "[+]";
            case "DELETE": return "[-]";
            case "MODIFY": return "[~]";
            case "RENAME": return "[R]";
            default: return "[ ]";
        }
    }
    
    /**
     * 获取变更类型描述
     */
    public String getChangeTypeDesc() {
        switch (changeType) {
            case "ADD": return "新增";
            case "DELETE": return "删除";
            case "MODIFY": return "修改";
            case "RENAME": return "重命名";
            default: return "未知";
        }
    }

    // Getters and Setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public String getOldContent() { return oldContent; }
    public void setOldContent(String oldContent) { this.oldContent = oldContent; }
    public String getNewContent() { return newContent; }
    public void setNewContent(String newContent) { this.newContent = newContent; }
    public int getAddedLines() { return addedLines; }
    public void setAddedLines(int addedLines) { this.addedLines = addedLines; }
    public int getDeletedLines() { return deletedLines; }
    public void setDeletedLines(int deletedLines) { this.deletedLines = deletedLines; }
    public int getModifiedLines() { return modifiedLines; }
    public void setModifiedLines(int modifiedLines) { this.modifiedLines = modifiedLines; }
    public String getOldFilePath() { return oldFilePath; }
    public void setOldFilePath(String oldFilePath) { this.oldFilePath = oldFilePath; }
}
