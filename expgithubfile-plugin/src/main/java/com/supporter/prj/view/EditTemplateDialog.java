package com.supporter.prj.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.supporter.prj.entity.ExportTemplate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 编辑模板对话框
 *
 * @author xueguangchen
 * @version 3.0.0
 */
public class EditTemplateDialog extends DialogWrapper {
    private Project project;
    private ExportTemplate template;
    private ExportTemplate originalTemplate;

    private JTextField nameField;
    private JTextArea descriptionArea;
    private JTextField directoryPatternField;
    private JTextField filePatternField;
    private JTextArea readmeTemplateArea;
    private JTextArea variablesArea;

    public EditTemplateDialog(Project project, String title) {
        super(project);
        this.project = project;
        this.template = new ExportTemplate();
        setTitle(title);
        init();
    }

    public EditTemplateDialog(Project project, String title, ExportTemplate original) {
        super(project);
        this.project = project;
        this.originalTemplate = original;
        this.template = original;
        setTitle(title);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setPreferredSize(new Dimension(550, 450));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;

        // 模板名称
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JBLabel("模板名称:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        nameField = new JTextField(30);
        if (originalTemplate != null) {
            nameField.setText(originalTemplate.getName());
        }
        panel.add(nameField, gbc);

        // 描述
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JBLabel("描述:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        descriptionArea = new JTextArea(2, 30);
        descriptionArea.setLineWrap(true);
        if (originalTemplate != null && originalTemplate.getDescription() != null) {
            descriptionArea.setText(originalTemplate.getDescription());
        }
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        panel.add(descScroll, gbc);

        // 目录结构模板
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JBLabel("目录结构模板:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        directoryPatternField = new JTextField(30);
        directoryPatternField.setToolTipText("可用变量: {date}, {datetime}, {env}, {project}");
        if (originalTemplate != null && originalTemplate.getDirectoryPattern() != null) {
            directoryPatternField.setText(originalTemplate.getDirectoryPattern());
        }
        panel.add(directoryPatternField, gbc);

        // 示例说明
        gbc.gridx = 1; gbc.gridy = ++row;
        panel.add(new JBLabel("示例: {date}/{env} -> 2024-04-01/prod"), gbc);

        // 文件命名模板
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        panel.add(new JBLabel("文件命名模板:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        filePatternField = new JTextField(30);
        filePatternField.setToolTipText("可用变量: {filename}, {date}, {datetime}");
        if (originalTemplate != null && originalTemplate.getFilePattern() != null) {
            filePatternField.setText(originalTemplate.getFilePattern());
        }
        panel.add(filePatternField, gbc);

        // 示例说明
        gbc.gridx = 1; gbc.gridy = ++row;
        panel.add(new JBLabel("示例: {filename}_{datetime} -> MyFile_2024-04-01_143000"), gbc);

        // README模板
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JBLabel("README模板:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        readmeTemplateArea = new JTextArea(5, 30);
        readmeTemplateArea.setLineWrap(true);
        readmeTemplateArea.setToolTipText("可用变量: {exportTime}, {repoPath}, {fileCount}, {commitIds}, {files}");
        if (originalTemplate != null && originalTemplate.getReadmeTemplate() != null) {
            readmeTemplateArea.setText(originalTemplate.getReadmeTemplate());
        }
        JScrollPane readmeScroll = new JScrollPane(readmeTemplateArea);
        panel.add(readmeScroll, gbc);

        // 自定义变量
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JBLabel("自定义变量:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        variablesArea = new JTextArea(3, 30);
        variablesArea.setLineWrap(true);
        variablesArea.setToolTipText("每行一个变量，格式: key=value");
        if (originalTemplate != null && originalTemplate.getVariables() != null) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : originalTemplate.getVariables().entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            variablesArea.setText(sb.toString().trim());
        }
        JScrollPane varScroll = new JScrollPane(variablesArea);
        panel.add(varScroll, gbc);

        return panel;
    }

    @Override
    protected void doOKAction() {
        // 验证
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(getWindow(), "请输入模板名称", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 保存
        if (originalTemplate != null) {
            template = originalTemplate;
        } else {
            template = new ExportTemplate();
        }

        template.setName(name);
        template.setDescription(descriptionArea.getText().trim());
        template.setDirectoryPattern(directoryPatternField.getText().trim());
        template.setFilePattern(filePatternField.getText().trim());
        template.setReadmeTemplate(readmeTemplateArea.getText().trim());

        // 解析自定义变量
        Map<String, String> variables = new HashMap<>();
        String varText = variablesArea.getText().trim();
        if (!varText.isEmpty()) {
            for (String line : varText.split("\n")) {
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    variables.put(key, value);
                }
            }
        }
        template.setVariables(variables);
        template.setLastModifyTime(new java.util.Date());

        super.doOKAction();
    }

    public ExportTemplate getTemplate() {
        return template;
    }
}
