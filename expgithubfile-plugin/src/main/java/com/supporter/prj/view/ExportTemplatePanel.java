package com.supporter.prj.view;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.supporter.prj.entity.ExportTemplate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 导出模板管理面板
 *
 * @author xueguangchen
 * @version 3.0.0
 */
public class ExportTemplatePanel {
    private Project project;
    private JPanel panel;
    private JBList<ExportTemplate> templateList;
    private DefaultListModel<ExportTemplate> listModel;
    private JTextArea detailArea;
    private JLabel statusLabel;
    private List<ExportTemplate> templates;
    private QuickExportPanel quickExportPanel;
    
    private static final String TEMPLATE_DIR = System.getProperty("user.home") + File.separator + ".expgithubfile" + File.separator + "export_templates";

    public ExportTemplatePanel(Project project) {
        this.project = project;
        this.templates = new ArrayList<>();
        initialize();
        loadSavedTemplates();
    }
    
    public void setQuickExportPanel(QuickExportPanel quickExportPanel) {
        this.quickExportPanel = quickExportPanel;
    }

    private void initialize() {
        panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建左侧模板列表
        JPanel leftPanel = createLeftPanel();
        panel.add(leftPanel, BorderLayout.WEST);

        // 创建右侧详情区域
        JPanel rightPanel = createRightPanel();
        panel.add(rightPanel, BorderLayout.CENTER);

        // 创建底部状态栏
        JPanel bottomPanel = createBottomPanel();
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // 加载预设模板
        loadPresetTemplates();
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setBorder(BorderFactory.createTitledBorder("模板列表"));

        // 搜索框
        JPanel searchPanel = new JPanel(new BorderLayout());
        JTextField searchField = new JTextField();
        searchField.setToolTipText("搜索模板...");
        searchPanel.add(searchField, BorderLayout.CENTER);
        panel.add(searchPanel, BorderLayout.NORTH);

        // 模板列表
        listModel = new DefaultListModel<>();
        templateList = new JBList<>(listModel);
        templateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    showTemplateDetail();
                }
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(templateList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 操作按钮
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton newButton = new JButton("新建");
        newButton.addActionListener(e -> createNewTemplate());
        buttonPanel.add(newButton);

        JButton deleteButton = new JButton("删除");
        deleteButton.addActionListener(e -> deleteSelectedTemplate());
        buttonPanel.add(deleteButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("模板详情"));

        // 详情文本区
        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JBScrollPane scrollPane = new JBScrollPane(detailArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 编辑按钮区域
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton editButton = new JButton("编辑模板");
        editButton.addActionListener(e -> editSelectedTemplate());
        buttonPanel.add(editButton);

        JButton duplicateButton = new JButton("复制模板");
        duplicateButton.addActionListener(e -> duplicateSelectedTemplate());
        buttonPanel.add(duplicateButton);

        JButton exportButton = new JButton("导出模板");
        exportButton.addActionListener(e -> exportSelectedTemplate());
        buttonPanel.add(exportButton);

        JButton importButton = new JButton("导入模板");
        importButton.addActionListener(e -> importTemplate());
        buttonPanel.add(importButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        statusLabel = new JBLabel("共 0 个模板");
        panel.add(statusLabel, BorderLayout.WEST);

        // 快速应用
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("应用模板");
        applyButton.addActionListener(e -> applySelectedTemplate());
        quickPanel.add(applyButton);
        panel.add(quickPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 加载预设模板
     */
    private void loadPresetTemplates() {
        // 预设模板1: 按日期分组
        ExportTemplate template1 = new ExportTemplate("按日期分组", "导出文件按日期分组存储");
        template1.setId("preset_1");
        template1.setDirectoryPattern("{date}");
        template1.setFilePattern("{filename}");
        template1.setPreset(true);
        templates.add(template1);

        // 预设模板2: 带时间戳
        ExportTemplate template2 = new ExportTemplate("带时间戳导出", "导出文件名添加时间戳");
        template2.setId("preset_2");
        template2.setDirectoryPattern("");
        template2.setFilePattern("{filename}_{datetime}");
        template2.setPreset(true);
        templates.add(template2);

        // 预设模板3: 多环境分离
        ExportTemplate template3 = new ExportTemplate("多环境分离", "按环境分离导出文件");
        template3.setId("preset_3");
        template3.setDirectoryPattern("{env}/{project}");
        template3.setFilePattern("{filename}");
        template3.setPreset(true);
        template3.getVariables().put("env", "prod");
        template3.getVariables().put("project", "myapp");
        templates.add(template3);

        // 预设模板4: 带README
        ExportTemplate template4 = new ExportTemplate("带说明文档", "导出时自动生成README.txt");
        template4.setId("preset_4");
        template4.setDirectoryPattern("");
        template4.setFilePattern("{filename}");
        template4.setReadmeTemplate("导出时间: {exportTime}\n仓库: {repoPath}\n文件数: {fileCount}\n提交ID: {commitIds}\n\n变更文件:\n{files}");
        template4.setPreset(true);
        templates.add(template4);

        updateTemplateList();
        updateStatusLabel();
    }

    private void updateTemplateList() {
        listModel.clear();
        for (ExportTemplate template : templates) {
            listModel.addElement(template);
        }
    }

    private void updateStatusLabel() {
        statusLabel.setText("共 " + templates.size() + " 个模板");
    }

    private void showTemplateDetail() {
        ExportTemplate selected = templateList.getSelectedValue();
        if (selected != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(selected.getName()).append(" ===\n\n");
            sb.append("描述: ").append(selected.getDescription() != null ? selected.getDescription() : "无").append("\n\n");

            sb.append("── 目录结构模板 ──\n");
            sb.append(selected.getDirectoryPattern() != null && !selected.getDirectoryPattern().isEmpty() 
                    ? selected.getDirectoryPattern() : "(不设置，使用原始结构)").append("\n\n");

            sb.append("── 文件命名模板 ──\n");
            sb.append(selected.getFilePattern() != null && !selected.getFilePattern().isEmpty() 
                    ? selected.getFilePattern() : "(不设置，使用原始文件名)").append("\n\n");

            sb.append("── README模板 ──\n");
            sb.append(selected.getReadmeTemplate() != null && !selected.getReadmeTemplate().isEmpty() 
                    ? selected.getReadmeTemplate() : "(不生成)").append("\n\n");

            if (selected.getVariables() != null && !selected.getVariables().isEmpty()) {
                sb.append("── 自定义变量 ──\n");
                for (java.util.Map.Entry<String, String> entry : selected.getVariables().entrySet()) {
                    sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
                }
                sb.append("\n");
            }

            sb.append("── 信息 ──\n");
            sb.append("类型: ").append(selected.isPreset() ? "预设模板" : "自定义模板").append("\n");
            sb.append("创建时间: ").append(formatDate(selected.getCreateTime())).append("\n");

            detailArea.setText(sb.toString());
        } else {
            detailArea.setText("请选择一个模板查看详情");
        }
    }

    private String formatDate(java.util.Date date) {
        if (date == null) return "未知";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private void createNewTemplate() {
        EditTemplateDialog dialog = new EditTemplateDialog(project, "新建模板");
        if (dialog.showAndGet()) {
            ExportTemplate newTemplate = dialog.getTemplate();
            newTemplate.setId(String.valueOf(System.currentTimeMillis()));
            newTemplate.setPreset(false);
            templates.add(newTemplate);
            updateTemplateList();
            updateStatusLabel();
            saveTemplates();
            JOptionPane.showMessageDialog(panel, "模板创建成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void editSelectedTemplate() {
        ExportTemplate selected = templateList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(panel, "请先选择一个模板", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selected.isPreset()) {
            int result = JOptionPane.showConfirmDialog(panel, 
                    "预设模板不可直接编辑，是否创建副本进行编辑？", 
                    "提示", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                duplicateAndEdit(selected);
            }
            return;
        }

        EditTemplateDialog dialog = new EditTemplateDialog(project, "编辑模板", selected);
        if (dialog.showAndGet()) {
            updateTemplateList();
            showTemplateDetail();
            saveTemplates();
            JOptionPane.showMessageDialog(panel, "模板已更新！", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void duplicateAndEdit(ExportTemplate original) {
        ExportTemplate copy = new ExportTemplate(original.getName() + " (副本)", original.getDescription());
        copy.setDirectoryPattern(original.getDirectoryPattern());
        copy.setFilePattern(original.getFilePattern());
        copy.setReadmeTemplate(original.getReadmeTemplate());
        copy.setVariables(new java.util.HashMap<>(original.getVariables()));
        copy.setPreset(false);
        copy.setId(String.valueOf(System.currentTimeMillis()));

        EditTemplateDialog dialog = new EditTemplateDialog(project, "编辑模板副本", copy);
        if (dialog.showAndGet()) {
            templates.add(copy);
            updateTemplateList();
            updateStatusLabel();
            saveTemplates();
            JOptionPane.showMessageDialog(panel, "模板副本已创建！", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void duplicateSelectedTemplate() {
        ExportTemplate selected = templateList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(panel, "请先选择一个模板", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ExportTemplate copy = new ExportTemplate(selected.getName() + " (副本)", selected.getDescription());
        copy.setDirectoryPattern(selected.getDirectoryPattern());
        copy.setFilePattern(selected.getFilePattern());
        copy.setReadmeTemplate(selected.getReadmeTemplate());
        copy.setVariables(new java.util.HashMap<>(selected.getVariables()));
        copy.setPreset(false);
        copy.setId(String.valueOf(System.currentTimeMillis()));

        templates.add(copy);
        updateTemplateList();
        updateStatusLabel();
        saveTemplates();
        JOptionPane.showMessageDialog(panel, "模板已复制", "成功", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedTemplate() {
        ExportTemplate selected = templateList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(panel, "请先选择一个模板", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selected.isPreset()) {
            JOptionPane.showMessageDialog(panel, "预设模板不可删除", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(panel, 
                "确定要删除模板 '" + selected.getName() + "' 吗？", 
                "确认删除", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // 删除模板文件
            if (selected.getId() != null) {
                File file = new File(TEMPLATE_DIR, selected.getId() + ".template");
                if (file.exists()) {
                    file.delete();
                }
            }
            
            templates.remove(selected);
            updateTemplateList();
            updateStatusLabel();
            detailArea.setText("");
            JOptionPane.showMessageDialog(panel, "模板已删除", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void exportSelectedTemplate() {
        ExportTemplate selected = templateList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(panel, "请先选择一个模板", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出模板");
        fileChooser.setSelectedFile(new File(selected.getName() + ".template"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("模板文件 (*.template)", "template"));
        
        if (fileChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".template")) {
                    file = new File(file.getAbsolutePath() + ".template");
                }
                
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                    oos.writeObject(selected);
                }
                
                JOptionPane.showMessageDialog(panel, 
                    "模板导出成功！\n文件: " + file.getAbsolutePath(), 
                    "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(panel, 
                    "导出失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importTemplate() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导入模板");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("模板文件 (*.template)", "template"));
        
        if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileChooser.getSelectedFile()))) {
                ExportTemplate imported = (ExportTemplate) ois.readObject();
                
                // 检查重名
                String name = imported.getName();
                int counter = 1;
                while (isTemplateNameExists(name)) {
                    name = imported.getName() + "_" + counter++;
                }
                imported.setName(name);
                imported.setPreset(false);
                imported.setId(String.valueOf(System.currentTimeMillis()));
                
                templates.add(imported);
                updateTemplateList();
                updateStatusLabel();
                saveTemplates();
                
                JOptionPane.showMessageDialog(panel, 
                    "模板导入成功！\n名称: " + name, 
                    "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(panel, 
                    "导入失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void applySelectedTemplate() {
        ExportTemplate selected = templateList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(panel, "请先选择一个模板", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (quickExportPanel != null) {
            quickExportPanel.applyExportTemplate(selected);
            JOptionPane.showMessageDialog(panel, 
                "模板 '" + selected.getName() + "' 已应用到当前导出配置", 
                "成功", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(panel, 
                "无法应用模板：导出面板未关联", 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 检查模板名称是否已存在
     */
    private boolean isTemplateNameExists(String name) {
        return templates.stream().anyMatch(t -> t.getName().equals(name));
    }
    
    /**
     * 加载已保存的模板
     */
    private void loadSavedTemplates() {
        File dir = new File(TEMPLATE_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".template"));
            if (files != null) {
                for (File file : files) {
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                        ExportTemplate template = (ExportTemplate) ois.readObject();
                        if (!isTemplateNameExists(template.getName())) {
                            templates.add(template);
                        }
                    } catch (Exception e) {
                        System.err.println("[ExportTemplatePanel] 加载模板失败: " + file.getName());
                    }
                }
            }
        }
        updateTemplateList();
        updateStatusLabel();
    }
    
    /**
     * 保存所有自定义模板
     */
    private void saveTemplates() {
        File dir = new File(TEMPLATE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        for (ExportTemplate template : templates) {
            if (!template.isPreset() && template.getId() != null) {
                try {
                    File file = new File(dir, template.getId() + ".template");
                    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                        oos.writeObject(template);
                    }
                } catch (Exception e) {
                    System.err.println("[ExportTemplatePanel] 保存模板失败: " + template.getName());
                }
            }
        }
    }

    public JPanel getPanel() {
        return panel;
    }

    public List<ExportTemplate> getTemplates() {
        return templates;
    }
}
