# ExpGitFile 开发文档

## 项目结构

```
expgithubfile-plugin/
├── src/main/java/com/supporter/prj/
│   ├── action/
│   │   └── ExpGitSubmitFileAction.java    # 插件入口Action
│   ├── entity/
│   │   ├── ExportOptions.java             # 导出选项配置
│   │   ├── ExportResult.java              # 导出结果
│   │   ├── ExportHistory.java             # 导出历史记录
│   │   ├── ExportTemplate.java            # 导出模板
│   │   ├── FilterRule.java                # 过滤规则实体
│   │   ├── GitCommitHistory.java          # Git提交历史
│   │   └── DiffEntryItem.java             # 差异条目
│   ├── util/
│   │   ├── ExpGitHubUtil.java             # Git导出核心工具类
│   │   ├── ConfigManager.java             # 配置管理
│   │   └── ExportHistoryManager.java      # 导出历史管理
│   └── view/
│       ├── QuickExportPanel.java          # 快速导出主面板
│       ├── FilePreviewPanel.java          # 文件预览面板
│       ├── FilterRuleDialog.java          # 过滤规则对话框
│       ├── SelectCommitDialog.java        # 提交选择对话框
│       ├── DiffViewerPanel.java           # 差异对比面板
│       ├── ExportHistoryPanel.java        # 导出历史面板
│       └── ConfigManagePanel.java         # 配置管理面板
└── src/main/resources/META-INF/
    └── plugin.xml                          # 插件配置文件
```

## 核心类说明

### 1. QuickExportPanel - 主界面面板

主界面使用 BorderLayout + BoxLayout 组合布局：

```java
private void initialize() {
    panel = new JPanel(new BorderLayout());
    
    // 上部内容区 - BoxLayout.Y_AXIS
    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    addRow(contentPanel, createRepoPathRow());
    addRow(contentPanel, createTargetPathRow());
    // ...
    
    // 底部区域 - 按钮和进度条
    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
    
    panel.add(contentPanel, BorderLayout.NORTH);
    panel.add(bottomPanel, BorderLayout.SOUTH);
}
```

**关键技术点：**
- `LABEL_WIDTH = 80`：统一标签宽度实现对齐
- `Box.createHorizontalGlue()`：实现按钮右对齐
- `Box.createVerticalStrut(8)`：统一行间距
- `setVisible()` + `revalidate()/repaint()`：动态显示/隐藏组件

### 2. ExpGitHubUtil - Git导出工具类

核心导出方法：

```java
// 导出已提交文件
public static ExportResult expCommittedFile(
    String repoPath, 
    String targetPath, 
    String[] commitIds, 
    ExportOptions options
)

// 导出未提交文件
public static ExportResult expUncommittedFiles(
    String repoPath, 
    String targetPath, 
    ExportOptions options
)

// 获取Git提交历史
public static List<GitCommitHistory> fetchGitCommitHistory(
    String repoPath, 
    int maxCount, 
    String author, 
    String path, 
    Date since, 
    Date until
)
```

**注意事项：**
- 初始提交无父提交，需判断 `commit.getParentCount() > 0`
- 使用 `SwingUtilities.invokeLater()` 更新UI
- 进度更新需在UI线程执行

### 3. FilterRule - 过滤规则

预设规则定义：

```java
public static List<FilterRule> getPresetRules() {
    List<FilterRule> rules = new ArrayList<>();
    rules.add(new FilterRule("仅Java源码", ".*\\.java$", false));
    rules.add(new FilterRule("排除前端资源", ".*\\.(js|css|html|vue|jsx|tsx)$", true));
    // ...
    return rules;
}
```

规则应用：

```java
public boolean shouldFilterFile(String filePath) {
    Pattern pattern = Pattern.compile(patternRegex);
    Matcher matcher = pattern.matcher(filePath);
    return exclude ? !matcher.matches() : matcher.matches();
}
```

### 4. FilePreviewPanel - 文件预览

预览数据源设置：

```java
public void setPreviewSource(
    String repoPath, 
    String[] commitIds, 
    boolean isCommitted, 
    ExportOptions options
) {
    if (isCommitted) {
        loadCommittedFiles(repoPath, commitIds, options);
    } else {
        loadUncommittedFiles(repoPath, options);
    }
}
```

## 插件配置 (plugin.xml)

```xml
<idea-plugin>
    <id>com.supporter.prj.expGitFilePrj</id>
    <name>ExpGitFilePrj</name>
    <vendor>ExpGitFilePrj</vendor>
    <depends>com.intellij.modules.platform</depends>
    
    <actions>
        <action id="com.supporter.prj.action.ExpGitSubmitFileAction" 
                class="com.supporter.prj.action.ExpGitSubmitFileAction"
                text="ExpGitSubmitFile">
            <add-to-group group-id="ToolsMenu" anchor="last"/>
        </action>
    </actions>
</idea-plugin>
```

## 数据存储

### 配置存储路径
- 模板目录：`~/.expgithubfile/templates/`
- 配置文件：通过 `ConfigManager` 管理

### 导出历史存储
通过 `ExportHistoryManager` 单例管理，支持持久化。

## 常见问题处理

### 1. 导出卡住问题

**原因：** 初始提交无父提交，`commit.getParent(0)` 抛出异常

**解决：**
```java
if (commit.getParentCount() > 0) {
    RevCommit parentCommit = commit.getParent(0);
    // 比较差异
}
```

### 2. 进度条不更新

**原因：** 非UI线程更新Swing组件

**解决：**
```java
SwingUtilities.invokeLater(() -> {
    progressBar.setValue(50);
    progressBar.setString("处理中...");
});
```

### 3. 布局切换问题

**原因：** GridBagLayout隐藏组件仍占用空间

**解决：** 使用BoxLayout，隐藏组件自动收缩

```java
panel.setVisible(false);
panel.revalidate();
panel.repaint();
```

## 开发环境

- IntelliJ IDEA Plugin SDK
- Java 8+
- JGit (Git操作)
- Apache Commons Lang

## 构建发布

```bash
# 构建插件
./gradlew buildPlugin

# 运行测试IDE
./gradlew runIde

# 发布到插件仓库
./gradlew publishPlugin
```
