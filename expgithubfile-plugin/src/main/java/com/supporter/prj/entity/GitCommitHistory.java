package com.supporter.prj.entity;

import java.util.Date;

/**
 * @author xueguangchen
 * @version 1.0.0
 * @ClassName com.supporter.prj.entity.GitCommitHistory.java
 * @Description 提交记录实体类
 * @createTime 2024年11月16日 14:11:00
 */
public class GitCommitHistory {
    private String commitId;
    private String authorName;
    private String emailAddress;
    private Date date;
    private String message;

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public GitCommitHistory(String commitId, String authorName, String emailAddress, Date date, String message) {
        this.commitId = commitId;
        this.authorName = authorName;
        this.emailAddress = emailAddress;
        this.date = date;
        this.message = message;
    }

    public GitCommitHistory() {
    }
}
