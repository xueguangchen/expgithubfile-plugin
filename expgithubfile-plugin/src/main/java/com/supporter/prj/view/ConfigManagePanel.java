package com.supporter.prj.view;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.supporter.prj.util.ConfigManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 配置管理标签页
 * 用于管理导出配置
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class ConfigManagePanel {
    private Project project;
    private JPanel panel;
    private JBList<ConfigItem> configList;
    private DefaultListModel<ConfigItem> listModel;
    private JTextArea configDetailArea;
    private JButton newConfigButton;
    private JButton editConfigButton;
    private JButton deleteConfigButton;
    private JButton exportConfigButton;
    private JButton importConfigButton;
    private JButton applyConfigButton;
    private JTextField searchField;
    private JLabel statusLabel;

    private java.util.List<ConfigItem> allConfigs;
    private QuickExportPanel quickExportPanel;

    public ConfigManagePanel(Project project) {
        this.project = project;
        initialize();
    }

    public void setQuickExportPanel(QuickExportPanel quickExportPanel) {
        this.quickExportPanel = quickExportPanel;
    }

    /**
     * 刷新配置列表
     */
    public void refreshConfigList() {
        allConfigs = new ArrayList<>(ConfigManager.getInstance().getAllConfigs());
        updateConfigList();
        updateStatusLabel();
    }

    private void initialize() {
        // 创建主面板
        panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建左侧列表区域
        JPanel leftPanel = createLeftPanel();
        panel.add(leftPanel, BorderLayout.CENTER);

        // 右侧详情区域
        JPanel rightPanel = createRightPanel();
        panel.add(rightPanel, BorderLayout.EAST);

        // 底部操作区域
        JPanel bottomPanel = createBottomPanel();
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // 初始化配置数据
        initializeConfigs();
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 0, 0, 10));

        // 创建搜索框
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterConfigs();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterConfigs();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterConfigs();
            }
        });

        JPanel searchPanelInner = new JPanel(new BorderLayout());
        searchPanelInner.add(new JBLabel("搜索配置:"), BorderLayout.WEST);
        searchPanelInner.add(searchField, BorderLayout.CENTER);

        searchPanel.add(searchPanelInner, BorderLayout.CENTER);

        // 创建配置列表
        listModel = new DefaultListModel<>();
        configList = new JBList<>(listModel);
        configList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    showConfigDetail();
                }
            }
        });

        // 配置列表右键菜单
        configList.setCellRenderer(new ConfigListCellRenderer());
        configList.setComponentPopupMenu(createConfigPopupMenu());

        JScrollPane scrollPane = new JBScrollPane(configList);
        scrollPane.setPreferredSize(new Dimension(300, 500));

        // 顶部操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        newConfigButton = new JButton("➕ 新建");
        editConfigButton = new JButton("✏️ 编辑");
        deleteConfigButton = new JButton("🗑️ 删除");

        newConfigButton.addActionListener(e -> createNewConfig());
        editConfigButton.addActionListener(e -> editSelectedConfig());
        deleteConfigButton.addActionListener(e -> deleteSelectedConfig());

        buttonPanel.add(newConfigButton);
        buttonPanel.add(editConfigButton);
        buttonPanel.add(deleteConfigButton);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(400, 500));

        // 配置详情标题
        JLabel detailTitle = new JBLabel("配置详情");
        detailTitle.setFont(new Font(detailTitle.getFont().getName(), Font.BOLD, 14));
        panel.add(detailTitle, BorderLayout.NORTH);

        // 配置详情显示区域
        configDetailArea = new JTextArea();
        configDetailArea.setEditable(false);
        configDetailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        configDetailArea.setText("选择一个配置查看详情...");

        JScrollPane scrollPane = new JBScrollPane(configDetailArea);
        scrollPane.setPreferredSize(new Dimension(350, 400));
        panel.add(scrollPane, BorderLayout.CENTER);

        // 右侧操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        exportConfigButton = new JButton("导出");
        importConfigButton = new JButton("导入");
        applyConfigButton = new JButton("应用");

        exportConfigButton.addActionListener(e -> exportConfig());
        importConfigButton.addActionListener(e -> importConfig());
        applyConfigButton.addActionListener(e -> applyConfig());

        buttonPanel.add(exportConfigButton);
        buttonPanel.add(importConfigButton);
        buttonPanel.add(applyConfigButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // 统计信息
        statusLabel = new JBLabel("共 0 个配置");
        statusLabel.setForeground(new Color(100, 100, 100));
        panel.add(statusLabel, BorderLayout.WEST);

        return panel;
    }

    private void initializeConfigs() {
        // 从 ConfigManager 加载配置
        allConfigs = new ArrayList<>(ConfigManager.getInstance().getAllConfigs());

        // 更新列表
        updateConfigList();
        updateStatusLabel();
    }

    private void updateConfigList() {
        listModel.clear();
        String searchText = searchField.getText().toLowerCase();

        for (ConfigItem config : allConfigs) {
            if (searchText.isEmpty() ||
                config.getName().toLowerCase().contains(searchText) ||
                config.getRepoPath().toLowerCase().contains(searchText) ||
                config.getTargetPath().toLowerCase().contains(searchText)) {
                listModel.addElement(config);
            }
        }

        // 自动选择第一个
        if (!listModel.isEmpty()) {
            configList.setSelectedIndex(0);
        }
    }

    private void filterConfigs() {
        updateConfigList();
    }

    private void showConfigDetail() {
        ConfigItem selected = configList.getSelectedValue();
        if (selected != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            StringBuilder sb = new StringBuilder();
            sb.append("配置名称: ").append(selected.getName()).append("\n\n");
            sb.append("仓库路径: ").append(selected.getRepoPath()).append("\n");
            sb.append("目标路径: ").append(selected.getTargetPath()).append("\n");
            sb.append("导出类型: ").append(selected.getExportType()).append("\n");

            if (!selected.getExportType().equals("未提交")) {
                sb.append("提交范围: ").append(selected.getCommitRange()).append("\n");
            }

            sb.append("过滤规则: ").append(selected.getFilterRule()).append("\n\n");
            sb.append("高级选项:\n");
            sb.append("  显示文件预览: ").append(selected.isShowFilePreview() ? "是" : "否").append("\n");
            sb.append("  过滤测试文件: ").append(selected.isFilterTestFiles() ? "是" : "否").append("\n");
            sb.append("  包含空提交: ").append(selected.isIncludeEmptyCommits() ? "是" : "否").append("\n");
            sb.append("  保留文件结构: ").append(selected.isPreserveStructure() ? "是" : "否").append("\n");
            sb.append("  自动创建导出日志: ").append(selected.isCreateExportLog() ? "是" : "否").append("\n");
            
            // 导出格式
            sb.append("\n导出格式:\n");
            sb.append("  目录结构: ").append(selected.getDirectoryPattern() != null && !selected.getDirectoryPattern().isEmpty() 
                    ? selected.getDirectoryPattern() : "(默认)").append("\n");
            sb.append("  文件命名: ").append(selected.getFilePattern() != null && !selected.getFilePattern().isEmpty() 
                    ? selected.getFilePattern() : "(保留原名)").append("\n");
            sb.append("  README: ").append(selected.getReadmeTemplate() != null && !selected.getReadmeTemplate().isEmpty() 
                    ? "已配置" : "不生成").append("\n");
            if (selected.getTemplateVariables() != null && !selected.getTemplateVariables().isEmpty()) {
                sb.append("  自定义变量: ").append(selected.getTemplateVariables().replace("\n", ", ")).append("\n");
            }

            sb.append("\n创建时间: ").append(dateFormat.format(selected.getCreateTime())).append("\n");
            sb.append("最后修改: ").append(dateFormat.format(selected.getLastModifyTime()));

            configDetailArea.setText(sb.toString());
        } else {
            configDetailArea.setText("选择一个配置查看详情...");
        }
    }

    private JPopupMenu createConfigPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem newMenuItem = new JMenuItem("新建配置");
        newMenuItem.addActionListener(e -> createNewConfig());

        JMenuItem editMenuItem = new JMenuItem("编辑配置");
        editMenuItem.addActionListener(e -> editSelectedConfig());

        JMenuItem deleteMenuItem = new JMenuItem("删除配置");
        deleteMenuItem.addActionListener(e -> deleteSelectedConfig());

        JMenuItem exportMenuItem = new JMenuItem("导出配置");
        exportMenuItem.addActionListener(e -> exportConfig());

        JMenuItem applyMenuItem = new JMenuItem("应用配置");
        applyMenuItem.addActionListener(e -> applyConfig());

        popupMenu.add(newMenuItem);
        popupMenu.addSeparator();
        popupMenu.add(editMenuItem);
        popupMenu.add(deleteMenuItem);
        popupMenu.addSeparator();
        popupMenu.add(exportMenuItem);
        popupMenu.addSeparator();
        popupMenu.add(applyMenuItem);

        return popupMenu;
    }

    private void createNewConfig() {
        EditConfigDialog dialog = new EditConfigDialog(project, "新建配置");
        if (dialog.showAndGet()) {
            ConfigItem newConfig = dialog.getConfigItem();
            // 检查重名
            if (isConfigNameExists(newConfig.getName())) {
                JOptionPane.showMessageDialog(panel, 
                    "配置名称 '" + newConfig.getName() + "' 已存在，请使用其他名称！", 
                    "名称重复", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ConfigManager.getInstance().saveConfig(newConfig);
            allConfigs = new ArrayList<>(ConfigManager.getInstance().getAllConfigs());
            updateConfigList();
            updateStatusLabel();
            showStatus("配置创建成功: " + newConfig.getName());
        }
    }

    private void editSelectedConfig() {
        ConfigItem selected = configList.getSelectedValue();
        if (selected != null) {
            String originalName = selected.getName();
            EditConfigDialog dialog = new EditConfigDialog(project, "编辑配置", selected);
            if (dialog.showAndGet()) {
                ConfigItem updatedConfig = dialog.getConfigItem();
                // 如果修改了名称，检查是否与其他配置重名
                if (!updatedConfig.getName().equals(originalName) && 
                    isConfigNameExists(updatedConfig.getName())) {
                    JOptionPane.showMessageDialog(panel, 
                        "配置名称 '" + updatedConfig.getName() + "' 已存在，请使用其他名称！", 
                        "名称重复", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // 先删除旧配置（如果改名了）
                if (!updatedConfig.getName().equals(originalName)) {
                    ConfigManager.getInstance().deleteConfig(originalName);
                }
                ConfigManager.getInstance().saveConfig(updatedConfig);
                allConfigs = new ArrayList<>(ConfigManager.getInstance().getAllConfigs());
                updateConfigList();
                showStatus("配置更新成功: " + updatedConfig.getName());
            }
        }
    }
    
    /**
     * 检查配置名称是否已存在
     */
    private boolean isConfigNameExists(String name) {
        return allConfigs.stream().anyMatch(c -> c.getName().equals(name));
    }

    private void deleteSelectedConfig() {
        ConfigItem selected = configList.getSelectedValue();
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(
                    panel,
                    String.format("确定要删除配置 '%s' 吗？", selected.getName()),
                    "确认删除",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                ConfigManager.getInstance().deleteConfig(selected.getName());
                allConfigs = new ArrayList<>(ConfigManager.getInstance().getAllConfigs());
                updateConfigList();
                configDetailArea.setText("");
                updateStatusLabel();
                showStatus("配置删除成功: " + selected.getName());
            }
        }
    }

    private void exportConfig() {
        ConfigItem selected = configList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(panel, "请先选择一个配置", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出配置");
        fileChooser.setSelectedFile(new File(selected.getName() + ".json"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON配置文件 (*.json)", "json"));

        if (fileChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
            try {
                ConfigManager.getInstance().exportConfig(selected, fileChooser.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(panel, "配置导出成功！\n" + fileChooser.getSelectedFile().getAbsolutePath(), 
                        "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "导出失败: " + ex.getMessage(), 
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importConfig() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导入配置");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON配置文件 (*.json)", "json"));

        if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
            try {
                ConfigManager.getInstance().importConfig(fileChooser.getSelectedFile().getAbsolutePath());
                // 重新加载配置列表
                allConfigs = new ArrayList<>(ConfigManager.getInstance().getAllConfigs());
                updateConfigList();
                updateStatusLabel();
                JOptionPane.showMessageDialog(panel, "配置导入成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "导入失败: " + ex.getMessage(), 
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void applyConfig() {
        ConfigItem selected = configList.getSelectedValue();
        if (selected != null) {
            // 应用配置到快速导出面板
            if (quickExportPanel != null) {
                quickExportPanel.applyConfig(selected);
            }
            // 切换到快速导出标签页
            if (panel.getParent() instanceof JTabbedPane) {
                JTabbedPane tabbedPane = (JTabbedPane) panel.getParent();
                tabbedPane.setSelectedIndex(0); // 切换到第一个标签页（快速导出）
            }
            showStatus("配置已应用: " + selected.getName());
        }
    }

    private void updateStatusLabel() {
        statusLabel.setText("共 " + listModel.getSize() + " 个配置");
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(new Color(0, 128, 0));

        // 3秒后恢复默认状态
        new javax.swing.Timer(3000, e -> {
            updateStatusLabel();
            statusLabel.setForeground(new Color(100, 100, 100));
        }).start();
    }

    public JPanel getPanel() {
        return panel;
    }

    // 配置项数据模型
    public static class ConfigItem {
        private String name;
        private String repoPath;
        private String targetPath;
        private String exportType;
        private String commitRange;
        private String filterRule;
        private boolean includeEmptyCommits;
        private boolean preserveStructure;
        private boolean showFilePreview;
        private boolean filterTestFiles;
        private boolean createExportLog;
        private String directoryPattern;
        private String filePattern;
        private String readmeTemplate;
        private String templateVariables;
        private Date createTime;
        private Date lastModifyTime;

        public ConfigItem(String name, String repoPath, String targetPath,
                         String exportType, String commitRange, String filterRule,
                         boolean includeEmptyCommits, boolean preserveStructure) {
            this(name, repoPath, targetPath, exportType, commitRange, filterRule,
                 includeEmptyCommits, preserveStructure, true, false, true);
        }

        public ConfigItem(String name, String repoPath, String targetPath,
                         String exportType, String commitRange, String filterRule,
                         boolean includeEmptyCommits, boolean preserveStructure,
                         boolean showFilePreview, boolean filterTestFiles, boolean createExportLog) {
            this.name = name;
            this.repoPath = repoPath;
            this.targetPath = targetPath;
            this.exportType = exportType;
            this.commitRange = commitRange;
            this.filterRule = filterRule;
            this.includeEmptyCommits = includeEmptyCommits;
            this.preserveStructure = preserveStructure;
            this.showFilePreview = showFilePreview;
            this.filterTestFiles = filterTestFiles;
            this.createExportLog = createExportLog;
            this.createTime = new Date();
            this.lastModifyTime = new Date();
        }

        // Getters
        public String getName() { return name; }
        public String getRepoPath() { return repoPath; }
        public String getTargetPath() { return targetPath; }
        public String getExportType() { return exportType; }
        public String getCommitRange() { return commitRange; }
        public String getFilterRule() { return filterRule; }
        public boolean isIncludeEmptyCommits() { return includeEmptyCommits; }
        public boolean isPreserveStructure() { return preserveStructure; }
        public boolean isShowFilePreview() { return showFilePreview; }
        public boolean isFilterTestFiles() { return filterTestFiles; }
        public boolean isCreateExportLog() { return createExportLog; }
        public String getDirectoryPattern() { return directoryPattern; }
        public String getFilePattern() { return filePattern; }
        public String getReadmeTemplate() { return readmeTemplate; }
        public String getTemplateVariables() { return templateVariables; }
        public Date getCreateTime() { return createTime; }
        public Date getLastModifyTime() { return lastModifyTime; }

        // Setters
        public void setName(String name) { this.name = name; }
        public void setRepoPath(String repoPath) { this.repoPath = repoPath; }
        public void setTargetPath(String targetPath) { this.targetPath = targetPath; }
        public void setExportType(String exportType) { this.exportType = exportType; }
        public void setCommitRange(String commitRange) { this.commitRange = commitRange; }
        public void setFilterRule(String filterRule) { this.filterRule = filterRule; }
        public void setIncludeEmptyCommits(boolean includeEmptyCommits) { this.includeEmptyCommits = includeEmptyCommits; }
        public void setPreserveStructure(boolean preserveStructure) { this.preserveStructure = preserveStructure; }
        public void setShowFilePreview(boolean showFilePreview) { this.showFilePreview = showFilePreview; }
        public void setFilterTestFiles(boolean filterTestFiles) { this.filterTestFiles = filterTestFiles; }
        public void setCreateExportLog(boolean createExportLog) { this.createExportLog = createExportLog; }
        public void setDirectoryPattern(String directoryPattern) { this.directoryPattern = directoryPattern; }
        public void setFilePattern(String filePattern) { this.filePattern = filePattern; }
        public void setReadmeTemplate(String readmeTemplate) { this.readmeTemplate = readmeTemplate; }
        public void setTemplateVariables(String templateVariables) { this.templateVariables = templateVariables; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
        public void setLastModifyTime(Date lastModifyTime) { this.lastModifyTime = lastModifyTime; }

        @Override
        public String toString() {
            return name;
        }
    }

    // 配置列表单元格渲染器
    private static class ConfigListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ConfigItem) {
                ConfigItem config = (ConfigItem) value;
                setText(config.getName());

                // 选中时使用默认前景色，避免深色主题下看不清
                if (!isSelected) {
                    // 根据配置类型设置不同颜色
                    if (config.getExportType().equals("已提交")) {
                        setForeground(new Color(0, 100, 200));
                    } else if (config.getExportType().equals("未提交")) {
                        setForeground(new Color(0, 150, 0));
                    }
                }
            }

            return this;
        }
    }
}