package com.supporter.prj.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.supporter.prj.entity.FilterRule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 过滤规则选择对话框
 * 用于选择和管理文件过滤规则
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class FilterRuleDialog extends DialogWrapper {
    private Project project;
    private JPanel mainPanel;
    private JBList<FilterRule> presetList;
    private DefaultListModel<FilterRule> presetListModel;
    private JList<FilterRule> selectedList;
    private DefaultListModel<FilterRule> selectedListModel;
    private JTextArea ruleDetailArea;
    private JTextField includeExtensionsField;
    private JTextField excludeExtensionsField;
    private JTextField includePathsField;
    private JTextField excludePathsField;
    private JTextField regexField;

    private List<FilterRule> selectedRules;
    private List<FilterRule> allPresetRules;

    public FilterRuleDialog(Project project) {
        super(project, false);
        this.project = project;
        this.selectedRules = new ArrayList<>();
        this.allPresetRules = FilterRule.getPresetRules();
        init();
        setTitle("选择过滤规则");
        setSize(800, 600);
        setResizable(true);
    }

    public FilterRuleDialog(Project project, List<FilterRule> existingRules) {
        this(project);
        if (existingRules != null) {
            this.selectedRules = new ArrayList<>(existingRules);
            updateSelectedList();
        }
    }

    @Override
    protected JComponent createCenterPanel() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建顶部提示
        JPanel topPanel = new JPanel(new BorderLayout());
        JBLabel tipLabel = new JBLabel("选择预设过滤规则或创建自定义规则");
        tipLabel.setFont(new Font(tipLabel.getFont().getName(), Font.BOLD, 14));
        topPanel.add(tipLabel, BorderLayout.WEST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 创建中间区域
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);

        // 左侧：预设规则列表和已选规则列表
        JPanel leftPanel = createLeftPanel();
        splitPane.setLeftComponent(leftPanel);

        // 右侧：规则详情和自定义规则
        JPanel rightPanel = createRightPanel();
        splitPane.setRightComponent(rightPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 初始化数据
        initializeData();

        return mainPanel;
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // 预设规则列表
        JPanel presetPanel = new JPanel(new BorderLayout(5, 5));
        presetPanel.setBorder(BorderFactory.createTitledBorder("预设规则"));

        presetListModel = new DefaultListModel<>();
        presetList = new JBList<>(presetListModel);
        presetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        presetList.setCellRenderer(new FilterRuleCellRenderer());
        presetList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showRuleDetail(presetList.getSelectedValue());
            }
        });

        JScrollPane presetScrollPane = new JScrollPane(presetList);
        presetScrollPane.setPreferredSize(new Dimension(350, 200));
        presetPanel.add(presetScrollPane, BorderLayout.CENTER);

        // 添加按钮
        JButton addButton = new JButton("添加 >>");
        addButton.addActionListener(e -> addSelectedPresetRule());
        presetPanel.add(addButton, BorderLayout.SOUTH);

        panel.add(presetPanel, BorderLayout.NORTH);

        // 已选规则列表
        JPanel selectedPanel = new JPanel(new BorderLayout(5, 5));
        selectedPanel.setBorder(BorderFactory.createTitledBorder("已选规则"));

        selectedListModel = new DefaultListModel<>();
        selectedList = new JList<>(selectedListModel);
        selectedList.setCellRenderer(new FilterRuleCellRenderer());
        selectedList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showRuleDetail(selectedList.getSelectedValue());
            }
        });

        JScrollPane selectedScrollPane = new JScrollPane(selectedList);
        selectedScrollPane.setPreferredSize(new Dimension(350, 200));
        selectedPanel.add(selectedScrollPane, BorderLayout.CENTER);

        // 移除按钮
        JButton removeButton = new JButton("◀ 移除");
        removeButton.addActionListener(e -> removeSelectedRule());
        selectedPanel.add(removeButton, BorderLayout.SOUTH);

        panel.add(selectedPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // 规则详情
        JPanel detailPanel = new JPanel(new BorderLayout(5, 5));
        detailPanel.setBorder(BorderFactory.createTitledBorder("规则详情"));

        ruleDetailArea = new JTextArea();
        ruleDetailArea.setEditable(false);
        ruleDetailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ruleDetailArea.setText("选择一个规则查看详情...");

        JScrollPane detailScrollPane = new JScrollPane(ruleDetailArea);
        detailScrollPane.setPreferredSize(new Dimension(350, 150));
        detailPanel.add(detailScrollPane, BorderLayout.CENTER);

        panel.add(detailPanel, BorderLayout.NORTH);

        // 自定义规则
        JPanel customPanel = new JPanel(new GridBagLayout());
        customPanel.setBorder(BorderFactory.createTitledBorder("自定义规则"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 规则名称
        gbc.gridx = 0; gbc.gridy = 0;
        customPanel.add(new JBLabel("规则名称:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JTextField ruleNameField = new JTextField();
        customPanel.add(ruleNameField, gbc);

        // 包含扩展名
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        customPanel.add(new JBLabel("包含扩展名:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        includeExtensionsField = new JTextField();
        includeExtensionsField.setToolTipText("多个扩展名用逗号分隔，如: java,xml,yml");
        customPanel.add(includeExtensionsField, gbc);

        // 排除扩展名
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        customPanel.add(new JBLabel("排除扩展名:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        excludeExtensionsField = new JTextField();
        excludeExtensionsField.setToolTipText("多个扩展名用逗号分隔，如: class,jar");
        customPanel.add(excludeExtensionsField, gbc);

        // 包含路径
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        customPanel.add(new JBLabel("包含路径:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        includePathsField = new JTextField();
        includePathsField.setToolTipText("多个路径用逗号分隔，支持通配符 *");
        customPanel.add(includePathsField, gbc);

        // 排除路径
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        customPanel.add(new JBLabel("排除路径:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        excludePathsField = new JTextField();
        excludePathsField.setToolTipText("多个路径用逗号分隔，支持通配符 *");
        customPanel.add(excludePathsField, gbc);

        // 正则表达式
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        customPanel.add(new JBLabel("正则表达式:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        regexField = new JTextField();
        regexField.setToolTipText("文件路径匹配的正则表达式");
        customPanel.add(regexField, gbc);

        // 添加自定义规则按钮
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JButton addCustomButton = new JButton("添加自定义规则");
        addCustomButton.addActionListener(e -> {
            FilterRule customRule = createCustomRule(ruleNameField.getText());
            if (customRule != null) {
                selectedListModel.addElement(customRule);
                selectedRules.add(customRule);
                ruleNameField.setText("");
                includeExtensionsField.setText("");
                excludeExtensionsField.setText("");
                includePathsField.setText("");
                excludePathsField.setText("");
                regexField.setText("");
            }
        });
        customPanel.add(addCustomButton, gbc);

        panel.add(customPanel, BorderLayout.CENTER);

        return panel;
    }

    private void initializeData() {
        // 加载预设规则
        for (FilterRule rule : allPresetRules) {
            presetListModel.addElement(rule);
        }
    }

    private void showRuleDetail(FilterRule rule) {
        if (rule == null) {
            ruleDetailArea.setText("选择一个规则查看详情...");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("规则名称: ").append(rule.getName()).append("\n");
        sb.append("描述: ").append(rule.getDescription()).append("\n\n");

        if (!rule.getIncludeExtensions().isEmpty()) {
            sb.append("包含扩展名: ").append(String.join(", ", rule.getIncludeExtensions())).append("\n");
        }

        if (!rule.getExcludeExtensions().isEmpty()) {
            sb.append("排除扩展名: ").append(String.join(", ", rule.getExcludeExtensions())).append("\n");
        }

        if (!rule.getIncludePaths().isEmpty()) {
            sb.append("包含路径: ").append(String.join(", ", rule.getIncludePaths())).append("\n");
        }

        if (!rule.getExcludePaths().isEmpty()) {
            sb.append("排除路径: ").append(String.join(", ", rule.getExcludePaths())).append("\n");
        }

        if (!rule.getRegexPatterns().isEmpty()) {
            sb.append("正则表达式: ").append(String.join(", ", rule.getRegexPatterns())).append("\n");
        }

        ruleDetailArea.setText(sb.toString());
    }

    private void addSelectedPresetRule() {
        FilterRule selected = presetList.getSelectedValue();
        if (selected != null) {
            // 检查是否已添加
            for (int i = 0; i < selectedListModel.size(); i++) {
                if (selectedListModel.get(i).getName().equals(selected.getName())) {
                    JOptionPane.showMessageDialog(mainPanel, "该规则已添加", "提示", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            selectedListModel.addElement(selected);
            selectedRules.add(selected);
        }
    }

    private void removeSelectedRule() {
        int selectedIndex = selectedList.getSelectedIndex();
        if (selectedIndex >= 0) {
            FilterRule removed = selectedListModel.remove(selectedIndex);
            selectedRules.remove(removed);
        }
    }

    private FilterRule createCustomRule(String name) {
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel, "请输入规则名称", "错误", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        FilterRule rule = new FilterRule(name, "自定义过滤规则");

        // 解析包含扩展名
        String includeExts = includeExtensionsField.getText().trim();
        if (!includeExts.isEmpty()) {
            String[] exts = includeExts.split(",");
            for (String ext : exts) {
                rule.getIncludeExtensions().add(ext.trim().toLowerCase());
            }
        }

        // 解析排除扩展名
        String excludeExts = excludeExtensionsField.getText().trim();
        if (!excludeExts.isEmpty()) {
            String[] exts = excludeExts.split(",");
            for (String ext : exts) {
                rule.getExcludeExtensions().add(ext.trim().toLowerCase());
            }
        }

        // 解析包含路径
        String includePaths = includePathsField.getText().trim();
        if (!includePaths.isEmpty()) {
            String[] paths = includePaths.split(",");
            for (String path : paths) {
                rule.getIncludePaths().add(path.trim());
            }
        }

        // 解析排除路径
        String excludePaths = excludePathsField.getText().trim();
        if (!excludePaths.isEmpty()) {
            String[] paths = excludePaths.split(",");
            for (String path : paths) {
                rule.getExcludePaths().add(path.trim());
            }
        }

        // 解析正则表达式
        String regex = regexField.getText().trim();
        if (!regex.isEmpty()) {
            rule.getRegexPatterns().add(regex);
        }

        return rule;
    }

    private void updateSelectedList() {
        selectedListModel.clear();
        for (FilterRule rule : selectedRules) {
            selectedListModel.addElement(rule);
        }
    }

    /**
     * 获取选中的过滤规则
     */
    public List<FilterRule> getSelectedRules() {
        return new ArrayList<>(selectedRules);
    }

    @Override
    protected void doOKAction() {
        // 确认选择
        super.doOKAction();
    }

    /**
     * 规则列表单元格渲染器
     */
    private static class FilterRuleCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof FilterRule) {
                FilterRule rule = (FilterRule) value;
                setText(rule.getName());
                setToolTipText(rule.getDescription());
            }

            return this;
        }
    }
}
