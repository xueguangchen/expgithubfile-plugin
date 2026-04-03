package com.supporter.prj.view;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.supporter.prj.entity.FilterRule;
import com.supporter.prj.view.ConfigManagePanel.ConfigItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 编辑配置对话框
 * 用于创建或编辑导出配置
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class EditConfigDialog extends DialogWrapper {
    private Project project;
    private String dialogTitle;
    private ConfigItem config;

    private JTextField nameField;
    private TextFieldWithBrowseButton repoPathField;
    private TextFieldWithBrowseButton targetPathField;
    private JComboBox<String> exportTypeComboBox;
    private JComboBox<String> commitRangeComboBox;
    private JLabel filterRuleLabel;
    private JButton filterRuleBtn;
    private List<FilterRule> selectedFilterRules;
    private JBCheckBox showFilePreviewCheckBox;
    private JBCheckBox filterTestFilesCheckBox;
    private JBCheckBox includeEmptyCommitsCheckBox;
    private JBCheckBox preserveStructureCheckBox;
    private JBCheckBox createLogCheckBox;

    public EditConfigDialog(Project project, String dialogTitle) {
        this(project, dialogTitle, null);
    }

    public EditConfigDialog(Project project, String dialogTitle, ConfigItem config) {
        super(project);
        this.project = project;
        this.dialogTitle = dialogTitle;
        this.config = config;
        this.selectedFilterRules = new ArrayList<>();
        init();
        setTitle(dialogTitle);
        setOKButtonText("保存");
        setCancelButtonText("取消");
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // 配置名称
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JBLabel("配置名称:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        nameField = new JTextField();
        nameField.setColumns(40);
        panel.add(nameField, gbc);
        row++;

        // 仓库路径
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JBLabel("仓库路径:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        repoPathField = new TextFieldWithBrowseButton();
        repoPathField.addBrowseFolderListener(
                new TextBrowseFolderListener(
                        FileChooserDescriptorFactory.createSingleFolderDescriptor(), project)
        );
        panel.add(repoPathField, gbc);
        row++;

        // 目标路径
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JBLabel("目标路径:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        targetPathField = new TextFieldWithBrowseButton();
        targetPathField.addBrowseFolderListener(
                new TextBrowseFolderListener(
                        FileChooserDescriptorFactory.createSingleFolderDescriptor(), project)
        );
        panel.add(targetPathField, gbc);
        row++;

        // 导出类型
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JBLabel("导出类型:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        exportTypeComboBox = new JComboBox<>();
        exportTypeComboBox.addItem("已提交");
        exportTypeComboBox.addItem("未提交");
        exportTypeComboBox.addActionListener(e -> updateVisibility());
        panel.add(exportTypeComboBox, gbc);
        row++;

        // 提交范围（仅当选择"已提交"时显示）
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        commitRangeComboBox = new JComboBox<>();
        commitRangeComboBox.addItem("最近20个提交");
        commitRangeComboBox.addItem("最近50个提交");
        commitRangeComboBox.addItem("最近100个提交");
        commitRangeComboBox.addItem("自定义时间范围");
        panel.add(new JBLabel("提交范围:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(commitRangeComboBox, gbc);
        row++;

        // 过滤规则
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JBLabel("过滤规则:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JPanel filterPanel = new JPanel(new BorderLayout(5, 0));
        filterRuleLabel = new JBLabel("未设置过滤规则");
        filterRuleLabel.setForeground(new Color(128, 128, 128));
        filterPanel.add(filterRuleLabel, BorderLayout.CENTER);
        
        filterRuleBtn = new JButton("设置过滤规则...");
        filterRuleBtn.addActionListener(e -> openFilterRuleDialog());
        filterPanel.add(filterRuleBtn, BorderLayout.EAST);
        
        panel.add(filterPanel, gbc);
        row++;

        // 高级选项
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(20, 10, 10, 10);
        panel.add(new JBLabel("高级选项:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel advancedOptionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

        showFilePreviewCheckBox = new JBCheckBox("显示文件预览");
        showFilePreviewCheckBox.setToolTipText("导出前显示文件预览");
        showFilePreviewCheckBox.setSelected(true);
        advancedOptionsPanel.add(showFilePreviewCheckBox);

        filterTestFilesCheckBox = new JBCheckBox("过滤测试文件");
        filterTestFilesCheckBox.setToolTipText("排除测试文件");
        advancedOptionsPanel.add(filterTestFilesCheckBox);

        includeEmptyCommitsCheckBox = new JBCheckBox("包含空提交");
        includeEmptyCommitsCheckBox.setToolTipText("是否包含空的提交记录");
        advancedOptionsPanel.add(includeEmptyCommitsCheckBox);

        preserveStructureCheckBox = new JBCheckBox("保留文件结构");
        preserveStructureCheckBox.setToolTipText("是否保持原有的目录结构");
        preserveStructureCheckBox.setSelected(true);
        advancedOptionsPanel.add(preserveStructureCheckBox);

        createLogCheckBox = new JBCheckBox("自动创建导出日志");
        createLogCheckBox.setToolTipText("导出完成后自动创建日志文件");
        createLogCheckBox.setSelected(true);
        advancedOptionsPanel.add(createLogCheckBox);

        panel.add(advancedOptionsPanel, gbc);
        row++;

        // 加载现有配置数据
        if (config != null) {
            loadConfigData();
        }

        return panel;
    }

    private void openFilterRuleDialog() {
        FilterRuleDialog dialog = new FilterRuleDialog(project, selectedFilterRules);
        if (dialog.showAndGet()) {
            selectedFilterRules = dialog.getSelectedRules();
            updateFilterRuleLabel();
        }
    }

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

    private void updateVisibility() {
        boolean isCommitted = "已提交".equals(exportTypeComboBox.getSelectedItem());
        commitRangeComboBox.setEnabled(isCommitted);
    }

    private void loadConfigData() {
        if (config != null) {
            nameField.setText(config.getName());
            repoPathField.setText(config.getRepoPath());
            targetPathField.setText(config.getTargetPath());
            exportTypeComboBox.setSelectedItem(config.getExportType());
            commitRangeComboBox.setSelectedItem(config.getCommitRange());
            
            // 解析过滤规则
            String filterRuleStr = config.getFilterRule();
            if (filterRuleStr != null && !filterRuleStr.isEmpty() && !filterRuleStr.equals("*")) {
                selectedFilterRules.clear();
                for (String ruleName : filterRuleStr.split(",")) {
                    FilterRule rule = findPresetRule(ruleName.trim());
                    if (rule != null) {
                        selectedFilterRules.add(rule);
                    }
                }
            }
            updateFilterRuleLabel();
            
            showFilePreviewCheckBox.setSelected(config.isShowFilePreview());
            filterTestFilesCheckBox.setSelected(config.isFilterTestFiles());
            includeEmptyCommitsCheckBox.setSelected(config.isIncludeEmptyCommits());
            preserveStructureCheckBox.setSelected(config.isPreserveStructure());
            createLogCheckBox.setSelected(config.isCreateExportLog());
        }
    }

    private FilterRule findPresetRule(String name) {
        for (FilterRule rule : FilterRule.getPresetRules()) {
            if (rule.getName().equals(name)) {
                return rule;
            }
        }
        return null;
    }

    public ConfigItem getConfigItem() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("配置名称不能为空");
        }

        String filterRuleStr = selectedFilterRules.isEmpty() ? "" : 
                selectedFilterRules.stream()
                        .map(FilterRule::getName)
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");

        ConfigItem newConfig = new ConfigItem(
                name,
                repoPathField.getText().trim(),
                targetPathField.getText().trim(),
                (String) exportTypeComboBox.getSelectedItem(),
                (String) commitRangeComboBox.getSelectedItem(),
                filterRuleStr,
                includeEmptyCommitsCheckBox.isSelected(),
                preserveStructureCheckBox.isSelected(),
                showFilePreviewCheckBox.isSelected(),
                filterTestFilesCheckBox.isSelected(),
                createLogCheckBox.isSelected()
        );

        // 如果是编辑现有配置，保留创建时间
        if (config != null) {
            newConfig.setCreateTime(config.getCreateTime());
        }

        return newConfig;
    }

    @Override
    protected JComponent createSouthPanel() {
        return super.createSouthPanel();
    }

    @Override
    protected void doOKAction() {
        try {
            // 验证数据
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(getWindow(), "配置名称不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String repoPath = repoPathField.getText().trim();
            if (repoPath.isEmpty()) {
                JOptionPane.showMessageDialog(getWindow(), "仓库路径不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String targetPath = targetPathField.getText().trim();
            if (targetPath.isEmpty()) {
                JOptionPane.showMessageDialog(getWindow(), "目标路径不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            super.doOKAction();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(getWindow(), "保存配置失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
}