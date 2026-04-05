package com.supporter.prj.view;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.supporter.prj.entity.ExportHistory;
import com.supporter.prj.entity.ExportOptions;
import com.supporter.prj.entity.ExportTemplate;
import com.supporter.prj.entity.FilterRule;
import com.supporter.prj.util.ConfigManager;
import com.supporter.prj.util.ExpGitHubUtil;
import com.supporter.prj.util.ExportHistoryManager;
import com.supporter.prj.util.GitRepositoryUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 快速导出标签页
 * 使用 IntelliJ Platform UI Components 实现
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class QuickExportPanel {
    private Project project;
    private JPanel panel;
    private TextFieldWithBrowseButton repoPathField;
    private TextFieldWithBrowseButton targetPathField;
    private JComboBox<String> typeComboBox;
    private JComboBox<String> scopeComboBox;
    private JButton selectCommitIdsBtn;
    private JTextArea commitIdsArea;
    private JCheckBox showFilePreviewCheckBox;
    private JCheckBox filterTestFilesCheckBox;
    private JCheckBox includeEmptyCommitsCheckBox;
    private JCheckBox preserveStructureCheckBox;
    private JCheckBox createLogCheckBox;
    private RoundedProgressBar progressBar;
    private JLabel statusLabel;
    private JPanel progressPanel;
    private JButton exportBtn;
    private JButton openExpFileBtn;
    private JLabel commitIdsLabel;
    private JLabel scopeLabel;
    private JPanel commitPanel;
    private String gitRepoPath;
    private boolean targetFolderPathIsInited = true;
    private boolean isInitialized = false; // 标记初始化是否完成
    
    // 过滤规则相关
    private JButton filterRuleBtn;
    private JLabel filterRuleLabel;
    private List<FilterRule> selectedFilterRules;
    
    // 配置管理相关
    private JButton saveConfigBtn;
    private JButton loadConfigBtn;
    
    // 文件预览相关
    private FilePreviewPanel filePreviewPanel;
    private ExportHistoryPanel exportHistoryPanel;
    private ConfigManagePanel configManagePanel;
    private JButton previewBtn;
    
    public QuickExportPanel(Project project) {
        this.project = project;
        this.selectedFilterRules = new ArrayList<>();
        initialize();
    }

    // 标签统一宽度
    private static final int LABEL_WIDTH = 80;
    
    private void initialize() {
        // 创建主面板 - 使用BorderLayout
        panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 上部内容区
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // 创建各行的面板
        addRow(contentPanel, createRepoPathRow());
        addRow(contentPanel, createTargetPathRow());
        addRow(contentPanel, createTypeRow());
        addRow(contentPanel, createOptionsRow());
        
        // 提交选择区域（可能隐藏）
        commitScopeRowPanel = createCommitScopeRow();
        commitAreaRowPanel = createCommitAreaRow();
        commitScopeRowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        commitAreaRowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(commitScopeRowPanel);
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(commitAreaRowPanel);
        
        addRow(contentPanel, createFilterRuleRow());

        // 底部区域（按钮 + 进度条）
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        addRow(bottomPanel, createButtonRow());
        addRow(bottomPanel, createProgressRow());
        
        panel.add(contentPanel, BorderLayout.NORTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // 初始化默认值
        initializeDefaults();

        // 初始化组件可见性
        updateVisibility();
    }
    
    private void addRow(JPanel parent, JPanel row) {
        parent.add(Box.createVerticalStrut(8));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(row);
    }
    
    private JPanel createRepoPathRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        JLabel label = new JBLabel("Git仓库路径:");
        label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
        row.add(label);
        
        repoPathField = new TextFieldWithBrowseButton();
        repoPathField.addBrowseFolderListener(
                new TextBrowseFolderListener(
                        FileChooserDescriptorFactory.createSingleFolderDescriptor(), project)
        );
        repoPathField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { onRepoPathChanged(); }
            @Override
            public void removeUpdate(DocumentEvent e) { onRepoPathChanged(); }
            @Override
            public void changedUpdate(DocumentEvent e) { onRepoPathChanged(); }
        });
        row.add(repoPathField);
        
        row.add(Box.createHorizontalStrut(5));
        
        JButton refreshButton = new JButton("刷新");
        refreshButton.setToolTipText("验证仓库路径并清空提交记录");
        refreshButton.addActionListener(e -> {
            String path = repoPathField.getText().trim();
            if (StringUtils.isBlank(path)) {
                JOptionPane.showMessageDialog(panel, "请先设置本地仓库路径！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            File gitDir = new File(path, ".git");
            if (!gitDir.exists()) {
                JOptionPane.showMessageDialog(panel, 
                    "指定的路径不是有效的 Git 仓库！\n路径: " + path, 
                    "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            commitIdsArea.setText("");
            JOptionPane.showMessageDialog(panel, 
                "仓库路径验证通过！\n可以点击\"选择提交...\"按钮选择提交记录。",
                "成功", JOptionPane.INFORMATION_MESSAGE);
        });
        row.add(refreshButton);
        
        row.add(Box.createHorizontalGlue());
        
        return row;
    }
    
    private JPanel createTargetPathRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        JLabel label = new JBLabel("目标路径:");
        label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
        row.add(label);
        
        targetPathField = new TextFieldWithBrowseButton();
        targetPathField.addBrowseFolderListener(
                new TextBrowseFolderListener(
                        FileChooserDescriptorFactory.createSingleFolderDescriptor(), project)
        );
        row.add(targetPathField);
        
        row.add(Box.createHorizontalStrut(5));
        
        JButton openButton = new JButton("打开");
        openButton.setToolTipText("打开目标文件夹");
        openButton.addActionListener(e -> {
            String path = targetPathField.getText().trim();
            if (StringUtils.isNotBlank(path)) {
                try {
                    Desktop.getDesktop().open(new java.io.File(path));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, "无法打开文件夹：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(panel, "路径为空！", "提示", JOptionPane.WARNING_MESSAGE);
            }
        });
        row.add(openButton);
        
        row.add(Box.createHorizontalGlue());
        
        return row;
    }
    
    private JPanel createTypeRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel label = new JBLabel("提交类型:");
        label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
        row.add(label);
        
        typeComboBox = new JComboBox<>();
        typeComboBox.addItem("已提交");
        typeComboBox.addItem("未提交");
        typeComboBox.addActionListener(e -> updateVisibility());
        row.add(typeComboBox);
        
        row.add(Box.createHorizontalGlue());
        
        return row;
    }
    
    private JPanel createOptionsRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel label = new JBLabel("高级选项:");
        label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
        row.add(label);
        
        showFilePreviewCheckBox = new JBCheckBox("显示文件预览");
        showFilePreviewCheckBox.setSelected(true);
        row.add(showFilePreviewCheckBox);
        row.add(Box.createHorizontalStrut(10));

        filterTestFilesCheckBox = new JBCheckBox("过滤测试文件");
        row.add(filterTestFilesCheckBox);
        row.add(Box.createHorizontalStrut(10));

        includeEmptyCommitsCheckBox = new JBCheckBox("包含空提交");
        row.add(includeEmptyCommitsCheckBox);
        row.add(Box.createHorizontalStrut(10));

        preserveStructureCheckBox = new JBCheckBox("保留文件结构");
        preserveStructureCheckBox.setSelected(true);
        row.add(preserveStructureCheckBox);
        row.add(Box.createHorizontalStrut(10));

        createLogCheckBox = new JBCheckBox("创建导出日志");
        createLogCheckBox.setSelected(true);
        row.add(createLogCheckBox);

        row.add(Box.createHorizontalGlue());
        
        return row;
    }
    
    private JPanel commitScopeRowPanel;
    private JPanel commitAreaRowPanel;
    
    private JPanel createCommitScopeRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        scopeLabel = new JBLabel("提交范围:");
        scopeLabel.setPreferredSize(new Dimension(LABEL_WIDTH, scopeLabel.getPreferredSize().height));
        row.add(scopeLabel);
        
        scopeComboBox = new JComboBox<>();
        scopeComboBox.addItem("自定义选择");
        scopeComboBox.addItem("最近10个提交");
        scopeComboBox.addItem("最近20个提交");
        scopeComboBox.addItem("最近50个提交");
        scopeComboBox.addItem("今天提交");
        scopeComboBox.addItem("本周提交");
        scopeComboBox.addActionListener(e -> {
            String selected = (String) scopeComboBox.getSelectedItem();
            resetProgressBar();
            if ("自定义选择".equals(selected)) {
                openSelectCommitDialog();
            } else {
                quickSelectCommits(selected);
            }
        });
        row.add(scopeComboBox);
        
        row.add(Box.createHorizontalStrut(5));
        
        selectCommitIdsBtn = new JButton("选择提交...");
        selectCommitIdsBtn.addActionListener(e -> {
            resetProgressBar();
            openSelectCommitDialog();
        });
        row.add(selectCommitIdsBtn);
        
        row.add(Box.createHorizontalGlue());
        
        return row;
    }
    
    private JPanel createCommitAreaRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // 标签与"提交范围:"对齐
        commitIdsLabel = new JBLabel("已选提交记录:");
        commitIdsLabel.setPreferredSize(new Dimension(LABEL_WIDTH, commitIdsLabel.getPreferredSize().height));
        commitIdsLabel.setVisible(true);
        row.add(commitIdsLabel);
        
        commitPanel = new JPanel();
        commitPanel.setLayout(new BoxLayout(commitPanel, BoxLayout.Y_AXIS));
        commitPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        commitIdsArea = new JTextArea();
        commitIdsArea.setRows(4);
        commitIdsArea.setEnabled(false);
        JScrollPane scrollPane = new JScrollPane(commitIdsArea);
        scrollPane.setPreferredSize(new Dimension(600, 80));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        commitPanel.add(scrollPane);

        row.add(commitPanel);
        row.add(Box.createHorizontalGlue());
        
        return row;
    }
    
    private JPanel createFilterRuleRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel label = new JBLabel("过滤规则:");
        label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
        row.add(label);
        
        filterRuleLabel = new JBLabel("未设置过滤规则");
        filterRuleLabel.setForeground(new Color(128, 128, 128));
        row.add(filterRuleLabel);
        
        row.add(Box.createHorizontalStrut(10));
        
        filterRuleBtn = new JButton("设置过滤规则...");
        filterRuleBtn.addActionListener(e -> openFilterRuleDialog());
        row.add(filterRuleBtn);
        
        row.add(Box.createHorizontalGlue());
        
        return row;
    }
    
    private JPanel createButtonRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // 弹性空间将按钮推到右边
        row.add(Box.createHorizontalGlue());
        
        previewBtn = new JButton("预览文件");
        previewBtn.addActionListener(e -> previewFiles());
        row.add(previewBtn);
        row.add(Box.createHorizontalStrut(10));

        exportBtn = new JButton("开始导出");
        exportBtn.setFont(new Font(exportBtn.getFont().getName(), Font.BOLD, 12));
        exportBtn.setBackground(new Color(76, 175, 80));
        exportBtn.setForeground(Color.WHITE);
        exportBtn.addActionListener(e -> startExport());
        row.add(exportBtn);
        row.add(Box.createHorizontalStrut(10));

        openExpFileBtn = new JButton("打开导出文件夹");
        openExpFileBtn.addActionListener(e -> {
            String path = targetPathField.getText().trim();
            if (StringUtils.isNotBlank(path)) {
                try {
                    Desktop.getDesktop().open(new java.io.File(path));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(panel, "无法打开文件夹：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(panel, "路径为空！", "提示", JOptionPane.WARNING_MESSAGE);
            }
        });
        row.add(openExpFileBtn);
        
        row.add(Box.createHorizontalStrut(30));
        
        saveConfigBtn = new JButton("保存配置");
        saveConfigBtn.addActionListener(e -> saveCurrentConfig());
        row.add(saveConfigBtn);
        row.add(Box.createHorizontalStrut(10));
        
        loadConfigBtn = new JButton("加载配置");
        loadConfigBtn.addActionListener(e -> loadConfig());
        row.add(loadConfigBtn);

        return row;
    }
    
    private JPanel createProgressRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        // 弹性空间将进度条推到右边
        row.add(Box.createHorizontalGlue());
        
        progressPanel = new JPanel(new BorderLayout(5, 0));
        progressBar = new RoundedProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(300, 24));
        
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font(statusLabel.getFont().getName(), Font.BOLD, 12));
        statusLabel.setVisible(false);
        
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(statusLabel, BorderLayout.EAST);
        progressPanel.setVisible(false);
        
        row.add(progressPanel);
        
        return row;
    }

    /**
     * 打开过滤规则设置对话框
     */
    private void openFilterRuleDialog() {
        FilterRuleDialog dialog = new FilterRuleDialog(project, selectedFilterRules);
        if (dialog.showAndGet()) {
            selectedFilterRules = dialog.getSelectedRules();
            updateFilterRuleLabel();
        }
    }

    /**
     * 更新过滤规则标签显示
     */
    private void updateFilterRuleLabel() {
        if (selectedFilterRules == null || selectedFilterRules.isEmpty()) {
            filterRuleLabel.setText("未设置过滤规则");
            filterRuleLabel.setForeground(new Color(128, 128, 128));
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < selectedFilterRules.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(selectedFilterRules.get(i).getName());
            }
            filterRuleLabel.setText(sb.toString());
            filterRuleLabel.setForeground(new Color(0, 128, 0));
        }
    }

    /**
     * 保存当前配置
     */
    private void saveCurrentConfig() {
        String configName = JOptionPane.showInputDialog(
                panel,
                "请输入配置名称:",
                "保存配置",
                JOptionPane.QUESTION_MESSAGE
        );

        if (configName != null && !configName.trim().isEmpty()) {
            String finalConfigName = configName.trim();
            
            // 检查重名
            List<ConfigManagePanel.ConfigItem> existingConfigs = ConfigManager.getInstance().getAllConfigs();
            boolean nameExists = existingConfigs.stream().anyMatch(c -> c.getName().equals(finalConfigName));
            if (nameExists) {
                int result = JOptionPane.showConfirmDialog(
                    panel,
                    "配置名称 '" + finalConfigName + "' 已存在，是否覆盖？",
                    "名称重复",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            String filterRuleStr = selectedFilterRules.isEmpty() ? "" : 
                    selectedFilterRules.stream()
                            .map(FilterRule::getName)
                            .reduce((a, b) -> a + "," + b)
                            .orElse("");

            ConfigManagePanel.ConfigItem config = new ConfigManagePanel.ConfigItem(
                    finalConfigName,
                    repoPathField.getText(),
                    targetPathField.getText(),
                    (String) typeComboBox.getSelectedItem(),
                    "最近20个提交",
                    filterRuleStr,
                    includeEmptyCommitsCheckBox.isSelected(),
                    preserveStructureCheckBox.isSelected(),
                    showFilePreviewCheckBox.isSelected(),
                    filterTestFilesCheckBox.isSelected(),
                    createLogCheckBox.isSelected()
            );

            ConfigManager.getInstance().saveConfig(config);
            
            // 刷新配置管理面板
            if (configManagePanel != null) {
                configManagePanel.refreshConfigList();
            }
            
            JOptionPane.showMessageDialog(panel, "配置保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        List<ConfigManagePanel.ConfigItem> configs = ConfigManager.getInstance().getAllConfigs();
        if (configs.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "没有可用的配置", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ConfigManagePanel.ConfigItem selected = (ConfigManagePanel.ConfigItem) JOptionPane.showInputDialog(
                panel,
                "选择要加载的配置:",
                "加载配置",
                JOptionPane.QUESTION_MESSAGE,
                null,
                configs.toArray(),
                configs.get(0)
        );

        if (selected != null) {
            applyConfig(selected);
        }
    }

    /**
     * 应用配置
     */
    public void applyConfig(ConfigManagePanel.ConfigItem config) {
        System.out.println("[QuickExportPanel] applyConfig 被调用, config.getRepoPath() = " + config.getRepoPath());
        if (config.getRepoPath() != null) {
            repoPathField.setText(config.getRepoPath());
        }
        if (config.getTargetPath() != null) {
            targetPathField.setText(config.getTargetPath());
        }
        if (config.getExportType() != null) {
            typeComboBox.setSelectedItem(config.getExportType());
        }
        
        showFilePreviewCheckBox.setSelected(config.isShowFilePreview());
        filterTestFilesCheckBox.setSelected(config.isFilterTestFiles());
        includeEmptyCommitsCheckBox.setSelected(config.isIncludeEmptyCommits());
        preserveStructureCheckBox.setSelected(config.isPreserveStructure());
        createLogCheckBox.setSelected(config.isCreateExportLog());
        
        // 解析过滤规则
        String filterRuleStr = config.getFilterRule();
        if (filterRuleStr != null && !filterRuleStr.isEmpty()) {
            selectedFilterRules.clear();
            for (String ruleName : filterRuleStr.split(",")) {
                FilterRule rule = findPresetRule(ruleName.trim());
                if (rule != null) {
                    selectedFilterRules.add(rule);
                }
            }
            updateFilterRuleLabel();
        }

        JOptionPane.showMessageDialog(panel, "配置已加载: " + config.getName(), "成功", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 应用导出模板
     */
    public void applyExportTemplate(ExportTemplate template) {
        if (template == null) {
            return;
        }
        
        // 应用过滤规则
        String filterRuleIds = template.getFilterRuleIds();
        if (filterRuleIds != null && !filterRuleIds.isEmpty()) {
            selectedFilterRules.clear();
            for (String ruleName : filterRuleIds.split(",")) {
                FilterRule rule = findPresetRule(ruleName.trim());
                if (rule != null) {
                    selectedFilterRules.add(rule);
                }
            }
            updateFilterRuleLabel();
        }
        
        // 可以扩展：应用目录结构、文件命名等模板设置
        // 目前这些设置需要在导出时使用
    }

    /**
     * 查找预设过滤规则
     */
    private FilterRule findPresetRule(String name) {
        for (FilterRule rule : FilterRule.getPresetRules()) {
            if (rule.getName().equals(name)) {
                return rule;
            }
        }
        return null;
    }

    private void initializeDefaults() {
        System.out.println("[QuickExportPanel] initializeDefaults() 开始执行");
        
        // 使用 project 获取项目路径
        if (project != null) {
            String projectPath = project.getBasePath();
            System.out.println("[QuickExportPanel] projectPath = " + projectPath);
            
            if (projectPath != null) {
                // 使用 GitRepositoryUtil 获取 Git 仓库路径
                this.gitRepoPath = GitRepositoryUtil.findGitRepositoryComprehensively(projectPath);
                System.out.println("[QuickExportPanel] 找到的 gitRepoPath = " + gitRepoPath);
                
                if (StringUtils.isNotBlank(gitRepoPath)) {
                    System.out.println("[QuickExportPanel] initializeDefaults 设置 repoPathField: " + gitRepoPath);
                    repoPathField.setText(gitRepoPath);

                    // 获取当前日期时间
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                    String formattedDateTime = now.format(formatter);
                    targetPathField.setText(gitRepoPath + File.separator + ("exp_" + formattedDateTime));

                    // 更新导出历史面板的仓库路径（此时可能为null，后续设置）
                    if (exportHistoryPanel != null) {
                        exportHistoryPanel.setCurrentRepoPath(gitRepoPath);
                    }
                }
            }
        } else {
            System.out.println("[QuickExportPanel] project 为 null");
        }
        
        // 标记初始化完成
        isInitialized = true;
        System.out.println("[QuickExportPanel] 初始化完成，isInitialized = true");
    }

    private void updateVisibility() {
        String selectedValue = (String) typeComboBox.getSelectedItem();
        boolean isCommitted = "已提交".equals(selectedValue);

        resetProgressBar();

        // 使用 BoxLayout 时，直接隐藏整行即可（不会占用空间）
        commitScopeRowPanel.setVisible(isCommitted);
        commitAreaRowPanel.setVisible(isCommitted);

        if (!isCommitted) {
            commitIdsArea.setText("");
        }
        
        // 重新计算布局
        panel.revalidate();
        panel.repaint();
    }

    private void startExport() {
        // 验证输入
        String repoPathValue = repoPathField.getText().trim();
        String targetFolderPathValue = targetPathField.getText().trim();

        if (StringUtils.isBlank(repoPathValue)) {
            JOptionPane.showMessageDialog(panel, "本地仓库路径不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (StringUtils.isBlank(targetFolderPathValue)) {
            JOptionPane.showMessageDialog(panel, "目标文件路径不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selectedValue = (String) typeComboBox.getSelectedItem();
        String[] commitIds = null;

        if ("已提交".equals(selectedValue)) {
            String commitIdsValue = commitIdsArea.getText().trim();
            if (StringUtils.isBlank(commitIdsValue)) {
                JOptionPane.showMessageDialog(panel, "提交的SHA-1哈希值不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // 解析提交ID（支持逗号和换行分隔）
            commitIds = commitIdsValue.split("[,\\n\\r]+");
            for (int i = 0; i < commitIds.length; i++) {
                commitIds[i] = commitIds[i].trim();
            }
        }

        // 创建导出选项
        ExportOptions options = new ExportOptions();
        options.setShowFilePreview(showFilePreviewCheckBox.isSelected());
        options.setFilterTestFiles(filterTestFilesCheckBox.isSelected());
        options.setIncludeEmptyCommits(includeEmptyCommitsCheckBox.isSelected());
        options.setPreserveStructure(preserveStructureCheckBox.isSelected());
        options.setCreateExportLog(createLogCheckBox.isSelected());
        options.setFilterRules(selectedFilterRules);
        
        System.out.println("[QuickExportPanel] 导出选项: " + options.toString());

        // 开始导出
        exportBtn.setEnabled(false);
        progressPanel.setVisible(true);
        progressBar.setVisible(true);
        progressBar.setValue(0);
        progressBar.setString("导出中...");
        progressBar.setProgressColor(new Color(64, 158, 255)); // 默认蓝色
        statusLabel.setVisible(false);
        
        System.out.println("[QuickExportPanel] 准备开始导出...");
        System.out.println("[QuickExportPanel] 类型: " + selectedValue);
        System.out.println("[QuickExportPanel] 仓库: " + repoPathValue);
        System.out.println("[QuickExportPanel] 目标: " + targetFolderPathValue);

        final String[] finalCommitIds = commitIds;
        final String finalSelectedValue = selectedValue;
        final String finalRepoPath = repoPathValue;
        final String finalTargetPath = targetFolderPathValue;
        final ExportOptions finalOptions = options;
        
        new Thread(() -> {
            try {
                System.out.println("[QuickExportPanel] 导出线程启动");
                if (finalCommitIds != null) {
                    System.out.println("[QuickExportPanel] 提交ID数量: " + finalCommitIds.length);
                    for (int i = 0; i < Math.min(3, finalCommitIds.length); i++) {
                        System.out.println("[QuickExportPanel] 提交ID[" + i + "]: " + finalCommitIds[i]);
                    }
                }
                
                // 确保目标目录存在
                java.io.File targetDir = new java.io.File(finalTargetPath);
                if (!targetDir.exists()) {
                    boolean created = targetDir.mkdirs();
                    System.out.println("[QuickExportPanel] 创建目标目录: " + finalTargetPath + ", 结果: " + created);
                }

                com.supporter.prj.entity.ExportResult result = null;
                if ("已提交".equals(finalSelectedValue)) {
                    System.out.println("[QuickExportPanel] 执行已提交导出...");
                    result = ExpGitHubUtil.expCommittedFile(finalRepoPath, finalTargetPath, finalCommitIds, finalOptions);
                } else {
                    System.out.println("[QuickExportPanel] 执行未提交导出...");
                    result = ExpGitHubUtil.expUncommittedFiles(finalRepoPath, finalTargetPath, finalOptions);
                }
                System.out.println("[QuickExportPanel] 导出完成，文件数: " + result.getFileCount() + ", 总大小: " + result.getFormattedTotalSize());

                // 保存导出历史记录
                ExportHistory history = new ExportHistory();
                history.setConfigName("快速导出");
                history.setRepoPath(finalRepoPath);
                history.setTargetPath(finalTargetPath);
                history.setExportType(finalSelectedValue);
                history.setFileCount(result.getFileCount());
                history.setTotalSize(result.getTotalSize());
                history.setSuccess(true);
                if (finalCommitIds != null && finalCommitIds.length > 0) {
                    history.setCommitIds(java.util.Arrays.asList(finalCommitIds));
                }
                ExportHistoryManager.getInstance().addHistory(history);

                SwingUtilities.invokeLater(() -> {
                    System.out.println("[QuickExportPanel] SwingUtilities.invokeLater 执行中...");
                    exportBtn.setEnabled(true);
                    progressBar.setValue(100);
                    progressBar.setStringPainted(true);
                    progressBar.setString("完成");
                    progressBar.setProgressColor(new Color(76, 175, 80)); // 绿色
                    progressBar.setVisible(true);
                    // 显示绿色状态标签
                    statusLabel.setText("导出完成");
                    statusLabel.setForeground(new Color(46, 125, 50)); // 深绿色
                    statusLabel.setVisible(true);
                    System.out.println("[QuickExportPanel] UI更新完成");
                    
                    // 刷新导出历史面板
                    if (exportHistoryPanel != null) {
                        exportHistoryPanel.refreshHistory();
                    }
                    
                    // 如果启用了文件预览，更新文件预览面板的数据
                    if (finalOptions.isShowFilePreview() && filePreviewPanel != null) {
                        System.out.println("[QuickExportPanel] 更新文件预览面板数据");
                        filePreviewPanel.setPreviewSource(finalRepoPath, finalCommitIds, "已提交".equals(finalSelectedValue), finalOptions);
                    }
                    
                    JOptionPane.showMessageDialog(panel, "导出完成！\n目标路径: " + finalTargetPath, "成功", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception e) {
                System.err.println("[QuickExportPanel] 导出失败: " + e.getMessage());
                e.printStackTrace();
                
                // 保存失败的历史记录
                ExportHistory history = new ExportHistory();
                history.setConfigName("快速导出");
                history.setRepoPath(finalRepoPath);
                history.setTargetPath(finalTargetPath);
                history.setExportType(finalSelectedValue);
                history.setSuccess(false);
                history.setErrorMessage(e.getMessage());
                ExportHistoryManager.getInstance().addHistory(history);
                
                SwingUtilities.invokeLater(() -> {
                    exportBtn.setEnabled(true);
                    progressBar.setVisible(false);
                    statusLabel.setText("导出失败");
                    statusLabel.setForeground(new Color(244, 67, 54)); // 红色
                    statusLabel.setVisible(true);
                    
                    // 刷新导出历史面板
                    if (exportHistoryPanel != null) {
                        exportHistoryPanel.refreshHistory();
                    }
                    
                    JOptionPane.showMessageDialog(panel, "导出失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    public JPanel getPanel() {
        return panel;
    }

    /**
     * 重置进度条到初始状态
     */
    private void resetProgressBar() {
        progressBar.setValue(0);
        progressBar.setString("");
        progressBar.setProgressColor(new Color(64, 158, 255)); // 恢复默认蓝色
        progressBar.setVisible(false);
        statusLabel.setVisible(false);
        progressPanel.setVisible(false);
    }

    /**
     * 仓库路径变化时的处理
     */
    private void onRepoPathChanged() {
        // 初始化完成前不响应变化事件
        if (!isInitialized) {
            System.out.println("[QuickExportPanel] onRepoPathChanged() 初始化未完成，跳过");
            return;
        }
        
        System.out.println("[QuickExportPanel] onRepoPathChanged() 被调用");
        System.out.println("[QuickExportPanel] 调用堆栈: " + java.util.Arrays.toString(Thread.currentThread().getStackTrace()));
        
        resetProgressBar();
        String repoPath = repoPathField.getText().trim();
        System.out.println("[QuickExportPanel] onRepoPathChanged() repoPath = " + repoPath);

        // 更新导出历史面板的仓库路径过滤
        if (exportHistoryPanel != null) {
            System.out.println("[QuickExportPanel] onRepoPathChanged() 设置 exportHistoryPanel.setCurrentRepoPath: " + repoPath);
            exportHistoryPanel.setCurrentRepoPath(repoPath);
        } else {
            System.out.println("[QuickExportPanel] onRepoPathChanged() exportHistoryPanel 为 null，跳过设置");
        }

        // 检查仓库路径是否有效
        if (StringUtils.isBlank(repoPath)) {
            commitIdsArea.setText("");
            return;
        }

        File gitDir = new File(repoPath, ".git");
        boolean isValidRepo = gitDir.exists();

        // 检查提交范围是否为自定义选择
        String selectedScope = (String) scopeComboBox.getSelectedItem();
        if ("自定义选择".equals(selectedScope)) {
            // 自定义选择时，清空提交记录
            commitIdsArea.setText("");
        } else {
            // 非自定义选择时，自动获取新提交记录
            if (isValidRepo) {
                quickSelectCommitsSilent(selectedScope);
            } else {
                commitIdsArea.setText("");
            }
        }
    }

    /**
     * 静默快速选择提交记录（不显示弹窗）
     */
    private void quickSelectCommitsSilent(String selection) {
        String repoPath = repoPathField.getText().trim();
        if (StringUtils.isBlank(repoPath)) {
            return;
        }

        try {
            java.util.List<com.supporter.prj.entity.GitCommitHistory> commits = null;

            if (selection.startsWith("最近")) {
                int count = Integer.parseInt(selection.replaceAll("[^0-9]", ""));
                commits = ExpGitHubUtil.fetchGitCommitHistory(repoPath, count, null, null, null, null);
            } else if (selection.equals("今天提交")) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                java.util.Date startOfDay = cal.getTime();
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                cal.set(java.util.Calendar.MINUTE, 59);
                cal.set(java.util.Calendar.SECOND, 59);
                java.util.Date endOfDay = cal.getTime();
                commits = ExpGitHubUtil.fetchGitCommitHistory(repoPath, 0, null, null, startOfDay, endOfDay);
            } else if (selection.equals("本周提交")) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                java.util.Date startOfWeek = cal.getTime();
                commits = ExpGitHubUtil.fetchGitCommitHistory(repoPath, 0, null, null, startOfWeek, null);
            }

            if (commits != null && !commits.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (com.supporter.prj.entity.GitCommitHistory commit : commits) {
                    if (sb.length() > 0) {
                        sb.append(",\n");
                    }
                    sb.append(commit.getCommitId());
                }
                commitIdsArea.setText(sb.toString());
            } else {
                commitIdsArea.setText("");
            }
        } catch (Exception e) {
            commitIdsArea.setText("");
        }
    }

    public TextFieldWithBrowseButton getRepoPath() {
        return repoPathField;
    }

    public TextFieldWithBrowseButton getTargetFolderPath() {
        return targetPathField;
    }

    public JButton getExportBtn() {
        return exportBtn;
    }

    public JTextArea getCommitIds() {
        return commitIdsArea;
    }

    public JComboBox<String> getTypeComboBox() {
        return typeComboBox;
    }

    public RoundedProgressBar getProgressBar() {
        return progressBar;
    }

    public List<FilterRule> getSelectedFilterRules() {
        return selectedFilterRules;
    }

    /**
     * 设置文件预览面板引用
     */
    public void setFilePreviewPanel(FilePreviewPanel filePreviewPanel) {
        this.filePreviewPanel = filePreviewPanel;
    }
    
    /**
     * 设置导出历史面板引用
     */
    public void setExportHistoryPanel(ExportHistoryPanel exportHistoryPanel) {
        this.exportHistoryPanel = exportHistoryPanel;
        // 设置关联后，立即同步当前的仓库路径
        if (exportHistoryPanel != null && gitRepoPath != null) {
            System.out.println("[QuickExportPanel] setExportHistoryPanel 设置仓库路径: " + gitRepoPath);
            exportHistoryPanel.setCurrentRepoPath(gitRepoPath);
        }
    }
    
    /**
     * 同步仓库路径到所有关联面板
     * 在所有面板初始化完成后调用，确保正确的仓库路径被应用
     */
    public void syncRepoPath() {
        System.out.println("[QuickExportPanel] syncRepoPath() 被调用, gitRepoPath = " + gitRepoPath);
        if (gitRepoPath != null) {
            // 确保文本框显示正确的路径
            repoPathField.setText(gitRepoPath);
            // 更新导出历史面板
            if (exportHistoryPanel != null) {
                exportHistoryPanel.setCurrentRepoPath(gitRepoPath);
            }
        }
    }

    /**
     * 设置配置管理面板引用
     */
    public void setConfigManagePanel(ConfigManagePanel configManagePanel) {
        this.configManagePanel = configManagePanel;
    }

    /**
     * 预览将要导出的文件
     */
    private void previewFiles() {
        // 验证输入
        String repoPathValue = repoPathField.getText().trim();
        if (StringUtils.isBlank(repoPathValue)) {
            JOptionPane.showMessageDialog(panel, "请先设置本地仓库路径！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (filePreviewPanel == null) {
            JOptionPane.showMessageDialog(panel, "文件预览面板未初始化！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selectedValue = (String) typeComboBox.getSelectedItem();
        String[] commitIds = null;
        boolean isCommitted = "已提交".equals(selectedValue);

        if (isCommitted) {
            String commitIdsValue = commitIdsArea.getText().trim();
            if (StringUtils.isBlank(commitIdsValue)) {
                JOptionPane.showMessageDialog(panel, "请先选择要导出的提交记录！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // 解析提交ID（支持逗号和换行分隔）
            commitIds = commitIdsValue.split("[,\\n\\r]+");
            for (int i = 0; i < commitIds.length; i++) {
                commitIds[i] = commitIds[i].trim();
            }
        }

        // 创建导出选项（包含过滤规则）
        ExportOptions options = new ExportOptions();
        options.setFilterTestFiles(filterTestFilesCheckBox.isSelected());
        options.setFilterRules(selectedFilterRules);
        
        System.out.println("[QuickExportPanel] 预览选项: " + options.toString());

        // 设置预览数据源并刷新
        filePreviewPanel.setPreviewSource(repoPathValue, commitIds, isCommitted, options);
        // 同步目标路径到预览面板
        filePreviewPanel.setTargetPath(targetPathField.getText().trim());
        filePreviewPanel.refreshPreview();
        
        // 自动切换到文件预览标签页
        if (panel.getParent() instanceof JTabbedPane) {
            JTabbedPane tabbedPane = (JTabbedPane) panel.getParent();
            tabbedPane.setSelectedIndex(1); // 切换到文件预览标签页（索引1）
        }
    }

    /**
     * 打开提交选择对话框
     */
    private void openSelectCommitDialog() {
        String repoPath = repoPathField.getText().trim();
        if (StringUtils.isBlank(repoPath)) {
            JOptionPane.showMessageDialog(panel, "请先设置本地仓库路径！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SelectCommitDialog dialog = new SelectCommitDialog(project, repoPath, null);
        if (dialog.showAndGet()) {
            commitIdsArea.setText(dialog.getSelectedCommitIds());
        }
    }

    /**
     * 快速选择提交记录（静默模式，不显示提示框）
     */
    private void quickSelectCommits(String selection) {
        String repoPath = repoPathField.getText().trim();
        if (StringUtils.isBlank(repoPath)) {
            return;
        }

        try {
            java.util.List<com.supporter.prj.entity.GitCommitHistory> commits = null;

            if (selection.startsWith("最近")) {
                int count = Integer.parseInt(selection.replaceAll("[^0-9]", ""));
                commits = ExpGitHubUtil.fetchGitCommitHistory(repoPath, count, null, null, null, null);
            } else if (selection.equals("今天提交")) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                java.util.Date startOfDay = cal.getTime();
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                cal.set(java.util.Calendar.MINUTE, 59);
                cal.set(java.util.Calendar.SECOND, 59);
                java.util.Date endOfDay = cal.getTime();
                commits = ExpGitHubUtil.fetchGitCommitHistory(repoPath, 0, null, null, startOfDay, endOfDay);
            } else if (selection.equals("本周提交")) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                java.util.Date startOfWeek = cal.getTime();
                commits = ExpGitHubUtil.fetchGitCommitHistory(repoPath, 0, null, null, startOfWeek, null);
            }

            if (commits != null && !commits.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (com.supporter.prj.entity.GitCommitHistory commit : commits) {
                    if (sb.length() > 0) {
                        sb.append(",\n");
                    }
                    sb.append(commit.getCommitId());
                }
                commitIdsArea.setText(sb.toString());
            } else {
                commitIdsArea.setText("");
            }
        } catch (Exception e) {
            commitIdsArea.setText("");
        }
    }

    /**
     * 自定义圆角进度条组件
     */
    private static class RoundedProgressBar extends JComponent {
        private int minimum = 0;
        private int maximum = 100;
        private int value = 0;
        private String text = "";
        private boolean stringPainted = true;
        private Color progressColor = new Color(64, 158, 255); // 默认蓝色
        private Color backgroundColor = new Color(230, 230, 230);

        public RoundedProgressBar() {
            setPreferredSize(new Dimension(200, 24));
        }

        public void setMinimum(int min) { this.minimum = min; }
        public void setMaximum(int max) { this.maximum = max; }
        public void setValue(int val) { this.value = val; repaint(); }
        public void setString(String str) { this.text = str; repaint(); }
        public void setStringPainted(boolean painted) { this.stringPainted = painted; }
        public void setProgressColor(Color color) { this.progressColor = color; repaint(); }
        public void setBackgroundColor(Color color) { this.backgroundColor = color; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int arc = 12; // 圆角半径

            // 绘制背景
            g2d.setColor(backgroundColor);
            g2d.fill(new RoundRectangle2D.Double(0, 0, width, height, arc, arc));

            // 绘制进度
            double percent = (double)(value - minimum) / (maximum - minimum);
            int progressWidth = (int)(width * percent);
            if (progressWidth > 0) {
                g2d.setColor(progressColor);
                g2d.fill(new RoundRectangle2D.Double(0, 0, progressWidth, height, arc, arc));
            }

            // 绘制文字
            if (stringPainted && text != null && !text.isEmpty()) {
                g2d.setColor(Color.WHITE);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getAscent();
                int x = (width - textWidth) / 2;
                int y = (height + textHeight) / 2 - 2;
                g2d.drawString(text, x, y);
            }

            g2d.dispose();
        }
    }
}