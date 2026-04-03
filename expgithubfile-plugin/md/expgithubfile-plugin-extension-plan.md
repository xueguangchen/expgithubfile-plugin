# ExpGitFilePrj 插件扩展规划

## 📊 现状分析

### 当前功能结构

**主要界面组件**:
- `ExpInfoInputFrame` - 主界面弹框（纯Swing手动编码）
  - Git仓库路径选择（文本框+浏览按钮）
  - 目标文件夹路径选择（文本框+浏览按钮）
  - 提交类型下拉框（已提交/未提交）
  - 提交ID文本域（仅已提交时显示，带选择按钮）
  - 导出按钮
  - 打开导出文件按钮
  - 进度条显示

- `GitCommitHistoryListDialog` - 提交历史选择弹窗
  - 提交人过滤
  - 关键字过滤
  - 开始/结束日期过滤（JDatePicker）
  - 提交记录表格（支持多选）
  - 分页功能（每页15条）
  - 确定/取消按钮

**核心工具类**:
- `ExpGitHubUtil` - Git文件导出核心逻辑
- `IdeaPluginGitUtil` - IDEA项目路径获取
- `GitRepositoryUtil` - Git仓库路径查找

### 当前痛点

1. **界面简陋** - 布局不够美观，用户体验较差
2. **缺少预览** - 导出前无法预览将要导出的文件列表
3. **无配置管理** - 无法保存常用的导出配置
4. **缺少过滤** - 无法按文件类型、路径等条件过滤
5. **无历史记录** - 无法查看和重用之前的导出记录
6. **信息不透明** - 导出过程中缺少详细的状态反馈

---

## 🚀 功能扩展方向（按优先级排序）

### 1. 文件预览功能 ⭐⭐⭐⭐⭐

**核心价值**: 在导出前预览将要导出的文件列表，避免误操作

**功能点**:
- 显示文件列表树形结构
- 标注文件变更类型（新增/修改/删除）
- 显示文件大小、变更行数
- 支持勾选/取消某些文件
- 统计信息（总文件数、各类型数量）
- 文件搜索功能

**实现要点**:
```java
// 建议数据结构
public class FilePreviewItem {
    private String filePath;
    private DiffEntry.ChangeType changeType;
    private long fileSize;
    private int addedLines;      // 新增行数
    private int deletedLines;     // 删除行数
    private boolean selected;     // 是否选中
}
```

### 2. 导出配置保存 ⭐⭐⭐⭐⭐

**核心价值**: 保存常用配置，提高重复操作效率

**功能点**:
- 保存当前导出配置（仓库路径、目标路径、提交选择、过滤规则等）
- 配置命名和管理（增删改查）
- 快速加载已保存的配置
- 配置导出/导入（便于团队共享）
- 最近使用的配置

**数据持久化方案**:
```properties
# 配置文件存储位置
# Windows: %APPDATA%/ExpGitFilePrj/configs/
# Mac/Linux: ~/.ExpGitFilePrj/configs/

# 配置文件格式（JSON）
{
  "name": "生产环境部署配置",
  "repoPath": "/project/repo",
  "targetPath": "/opt/deploy/prod",
  "exportType": "committed",
  "commitIds": ["abc123", "def456"],
  "filters": {
    "extensions": ["java", "yml"],
    "excludePaths": ["node_modules", ".git"],
    "regexPatterns": ["^src/main/"]
  },
  "advancedOptions": {
    "includeEmptyCommits": false,
    "preserveStructure": true,
    "createLog": true
  }
}
```

### 3. 文件过滤规则 ⭐⭐⭐⭐

**核心价值**: 精确控制导出哪些文件

**功能点**:
- 按文件扩展名过滤（如只导出 .java 文件）
- 按路径模式过滤（如排除 node_modules 目录）
- 支持正则表达式
- 预设常用过滤规则
- 自定义过滤规则组合

**预设规则示例**:
- "仅Java源码" - *.java
- "排除前端资源" - 排除 node_modules, dist, .git
- "生产环境配置" - *prod*.yml, *prod*.properties
- "后端API文件" - src/main/java/**/*Controller.java, src/main/java/**/*Service.java

### 4. 导出历史管理 ⭐⭐⭐

**核心价值**: 记录导出历史，方便追溯和重用

**功能点**:
- 记录每次导出的历史（时间、配置、文件数量）
- 导出历史列表展示
- 支持重新导出历史记录
- 导出历史搜索和筛选
- 导出记录导出（CSV/Excel格式）
- 历史记录清理（保留最近N条）

**数据结构**:
```java
public class ExportHistory {
    private String id;
    private Date exportTime;
    private String configName;
    private String repoPath;
    private String targetPath;
    private int fileCount;
    private long totalSize;
    private List<String> commitIds;
    private ExportFilter filter;
}
```

### 5. 导出模板 ⭐⭐⭐

**核心价值**: 自定义导出目录结构和命名规则

**功能点**:
- 自定义导出目录结构模板
- 自定义文件命名规则（支持变量替换）
- 自动生成导出说明文件（readme.txt）
- 多环境模板管理

**模板变量**:
- `{date}` - 当前日期
- `{datetime}` - 当前日期时间
- `{commit_count}` - 提交数量
- `{env}` - 环境标识

### 6. 差异对比视图 ⭐⭐⭐⭐

**核心价值**: 查看文件的具体变更内容

**功能点**:
- 显示文件的代码差异
- 变更行高亮显示（绿色新增，红色删除）
- 支持查看单个文件的完整变更
- 差异统计（总变更行数）
- 快速跳转到变更位置

**实现建议**:
- 使用 JGit 的 DiffFormatter 获取差异
- 使用 JDiff 或 JDiff 插件渲染差异视图

---

## 🎨 界面优化设计

### 主界面布局（Tab分页）

**标签页结构**:
1. **快速导出** - 主要导出功能
2. **文件预览** - 预览和筛选文件
3. **配置管理** - 管理导出配置
4. **导出历史** - 查看和重用导出记录
5. **差异对比** - 查看文件变更详情

### 快速导出标签页设计

**顶部操作区**:
```
┌──────────────────────────────────────────────────────────┐
│ Git仓库路径: [________________] [📁浏览] [🔄刷新]        │
│ 目标路径:     [________________] [📁浏览] [📂打开]        │
└──────────────────────────────────────────────────────────┘
```

**提交类型选择区**:
```
┌─── 提交类型 ─────────────────┐  ┌─── 高级选项 ───────────────┐
│ ○ 已提交的文件              │  │ ☑ 显示文件预览             │
│ ○ 未提交的文件              │  │ ☑ 过滤测试文件             │
│ ○ 指定分支对比              │  │ □ 包含空提交               │
└────────────────────────────┘  │ □ 保留文件结构             │
                               │ ☑ 自动创建导出日志         │
                               └──────────────────────────────┘
```

**提交选择区**（仅"已提交"时显示）:
```
提交范围:
  [最近20个提交▼] [选择提交...]
  [指定时间段    ▼]

提交记录 (可拖动排序):
  ┌──────────────────────────────────────┐
  │ □ abc123 - 修复登录bug               │
  │ □ def456 - 新增导出功能              │
  │ □ ghi789 - 优化性能                  │
  └──────────────────────────────────────┘
  [↓添加提交] [↑上移] [↓下移] [×删除]

快速操作:
  [最近10次提交] [今天的提交] [我的提交] [标记的提交]
```

**文件预览区**:
```
文件预览:
  ┌─────────────────────────────────────────────────┐
  │ □ com/service/UserService.java       [修改] 12KB │
  │ □ com/controller/UserController.java  [新增]  8KB │
  │ ☑ util/CommonUtil.java              [删除]  3KB │
  │ ☑ resources/application.yml         [修改]  2KB │
  │                                                 │
  │ [🔍全部选中] [☐取消全部] [📋复制列表]           │
  └─────────────────────────────────────────────────┘

📊 统计: 共4个文件 | 新增:1 | 修改:2 | 删除:1
```

**底部操作区**:
```
┌──────────────────────────────────────────────────────────┐
│  [保存配置] [加载配置]            [开始导出 ▶]             │
│  ████████████████████░░░░░░░  78%                        │
└──────────────────────────────────────────────────────────┘
```

### 配置管理标签页设计

```
┌──────────────────────────────────────────────────────────┐
│ 配置管理                                  [➕新建][🗑️删除] │
├──────────────────────────────────────────────────────────┤
│                                                          │
│ 已保存的配置:                                            │
│ ┌────────────────────────────────────────────────┐     │
│ │ 📋 生产环境部署配置                    [✏️][🗑️][📤]│     │
│ │    目标路径: /opt/deploy/prod                   │     │
│ │    提交: 最近10个                               │     │
│ │    过滤: *.java, *.yml                          │     │
│ ├────────────────────────────────────────────────┤     │
│ │ 📋 测试环境部署配置                    [✏️][🗑️][📤]│     │
│ │    目标路径: /opt/deploy/test                   │     │
│ │    提交: 未提交文件                             │     │
│ │    过滤: * (全部)                               │     │
│ └────────────────────────────────────────────────┘     │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### 导出历史标签页设计

```
┌──────────────────────────────────────────────────────────┐
│ 导出历史                    [清空历史] [导出记录][搜索]  │
├──────────────────────────────────────────────────────────┤
│                                                          │
│ 2024-04-01 14:30:25  生产环境部署  15个文件  📁/path  [📤] │
│ 2024-04-01 11:20:10  测试环境部署   8个文件  📁/path  [📤] │
│ 2024-04-01 09:15:33  热修复         3个文件  📁/path  [📤] │
│ 2024-03-31 18:45:22  生产环境部署  22个文件  📁/path  [📤] │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 🛠️ 技术选型建议

### UI框架选择

#### 方案A: 纯Swing改造 ⭐⭐⭐
**优点**:
- 无需额外依赖，兼容性好
- 与现有代码风格一致

**缺点**:
- 开发效率较低
- 样式和布局灵活性有限

**适用场景**: 小规模改动，保持技术栈简单

**推荐库**:
```gradle
// 无需额外依赖，使用Swing标准组件
```

#### 方案B: 使用MigLayout ⭐⭐⭐⭐
**优点**:
- 布局灵活，代码简洁
- 支持复杂的布局需求
- 学习曲线平缓

**缺点**:
- 需要引入新依赖
- 体积略有增加

**依赖配置**:
```gradle
implementation 'com.miglayout:miglayout-swing:11.3'
```

**使用示例**:
```java
JPanel panel = new JPanel(new MigLayout());
panel.add(new JLabel("仓库路径:"), "gap rel");
panel.add(repoPathField, "grow, push, wmin 300");
panel.add(browseButton, "wrap");

panel.add(new JLabel("目标路径:"), "gap rel");
panel.add(targetPathField, "grow, push, wmin 300");
panel.add(browseButton2, "wrap");
```

#### 方案C: 使用IntelliJ Platform UI Components ⭐⭐⭐⭐⭐（推荐）
**优点**:
- 与IDEA风格完全一致，用户体验最佳
- 丰富的组件库
- 自动适应IDEA主题
- 官方支持

**缺点**:
- 学习成本略高
- 版本兼容性需要注意

**依赖配置**:
```gradle
// 已经包含在IntelliJ Platform SDK中
// 无需额外添加
```

**常用组件**:
```java
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBList;
import com.intellij.ui.JBScrollPane;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.JBTabbedPane;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.openapi.ui.DialogWrapper;
```

**使用示例**:
```java
// 创建Tab面板
JBTabbedPane tabbedPane = new JBTabbedPane();
tabbedPane.addTab("快速导出", quickExportPanel);
tabbedPane.addTab("文件预览", filePreviewPanel);
tabbedPane.addTab("配置管理", configManagePanel);

// 创建带浏览按钮的文本框
TextFieldWithBrowseButton repoPathField = new TextFieldWithBrowseButton();
repoPathField.addBrowseFolderListener("选择Git仓库", "", project, FileChooserDescriptorFactory.createSingleFolderDescriptor());

// 创建列表
JBList<ConfigItem> configList = new JBList<>(listModel);
JBScrollPane scrollPane = new JBScrollPane(configList);
```

### 数据持久化方案

#### 配置文件存储
```
# 配置文件存储位置
# Windows: %APPDATA%/ExpGitFilePrj/configs/
# Mac/Linux: ~/.ExpGitFilePrj/configs/

# 配置文件格式
export_history.json    # 导出历史
configs/                # 配置文件夹
  └── prod_config.json
  └── test_config.json
settings.ini            # 插件设置
```

#### JSON序列化
```gradle
implementation 'com.google.code.gson:gson:2.10.1'
// 或
implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
```

### 日志记录
```gradle
implementation 'org.slf4j:slf4j-api:2.0.16'
testImplementation 'ch.qos.logback:logback-classic:1.5.12'
```

---

## 📅 实施路线图

### 第一阶段：核心体验提升 ✅ 已完成
**目标**: 提升用户使用体验，解决核心痛点

**任务清单**:
1. ✅ 优化主界面布局（使用IntelliJ Platform UI Components）
2. ✅ 添加文件预览功能（核心功能，实用价值高）
3. ✅ 改进提交选择界面（更直观的提交记录展示）
4. ✅ 添加配置保存和加载功能

**预估工时**: 3-5天

### 第二阶段：效率提升 ✅ 已完成
**目标**: 提高操作效率，减少重复工作

**任务清单**:
1. ✅ 实现文件过滤规则（按类型和路径过滤）
2. ✅ 添加预设过滤规则模板
3. ✅ 完善配置管理功能（增删改查）
4. ✅ 添加配置导出/导入功能

**预估工时**: 2-3天

### 第三阶段：高级功能 ✅ 已完成
**目标**: 提供高级特性，满足复杂需求

**任务清单**:
1. ✅ 实现导出历史管理
2. ✅ 添加差异对比视图
3. ✅ 实现导出模板功能
4. ✅ 添加批量导出功能

**预估工时**: 3-5天

---

## 💡 设计原则

### 1. 渐进式增强
- 保持现有功能稳定
- 新功能独立模块化
- 支持逐步迁移和迭代

### 2. 用户友好
- 提供清晰的视觉反馈
- 合理的默认设置
- 减少用户认知负担

### 3. 可扩展性
- 模块化设计
- 插件式架构
- 便于未来功能扩展

### 4. 性能优先
- 大文件列表使用虚拟滚动
- 异步处理耗时操作
- 合理缓存机制

---

## 📚 参考资料

### IntelliJ Platform 开发文档
- [IntelliJ Platform UI Components](https://plugins.jetbrains.com/docs/intellij/user-interface-components.html)
- [Dialog Wrapper](https://plugins.jetbrains.com/docs/intellij/dialog-wrapper.html)
- [Notifications](https://plugins.jetbrains.com/docs/intellij/notifications.html)

### JGit 文档
- [JGit User Guide](https://wiki.eclipse.org/JGit/User_Guide)
- [JGit API Documentation](https://download.eclipse.org/jgit/site/5.13.0.202109080850-r/apidocs/)

### Swing UI 最佳实践
- [Java Swing Tutorial](https://docs.oracle.com/javase/tutorial/uiswing/)
- [MigLayout Documentation](https://www.miglayout.com/)
