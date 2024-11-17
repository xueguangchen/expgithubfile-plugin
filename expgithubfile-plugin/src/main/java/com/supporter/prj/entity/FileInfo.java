package com.supporter.prj.entity;

import org.eclipse.jgit.diff.DiffEntry;

/**
 * @author xueguangchen
 * @version 1.0.0
 * @ClassName com.supporter.prj.entity.FileInfo.java
 * @Description 文件信息实体
 * @createTime 2024年11月17日 13:25:00
 */
public class FileInfo {
    private String filePath;//路径
    private String rootPath;//根路径
    private String sourcePath;//绝对路径
    private DiffEntry.ChangeType fileType;//文件类型

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public DiffEntry.ChangeType getFileType() {
        return fileType;
    }

    public void setFileType(DiffEntry.ChangeType fileType) {
        this.fileType = fileType;
    }

    public FileInfo(String filePath, String rootPath, String sourcePath, DiffEntry.ChangeType fileType) {
        this.filePath = filePath;
        this.rootPath = rootPath;
        this.sourcePath = sourcePath;
        this.fileType = fileType;
    }

    public FileInfo() {
    }
}
