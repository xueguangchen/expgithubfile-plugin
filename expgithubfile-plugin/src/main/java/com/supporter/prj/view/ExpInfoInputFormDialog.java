package com.supporter.prj.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * ExpGitSubmitFile 插件主对话框
 * 使用 IntelliJ Platform UI Components 实现的新界面
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class ExpInfoInputFormDialog extends DialogWrapper {

    private Project project;
    private ExpGitSubmitMainFrame mainFrame;
    private QuickExportPanel quickExportPanel;

    public ExpInfoInputFormDialog(Project project) {
        super(true);
        this.project = project;
        init(); // 触发一下init方法，否则swing样式将无法展示在会话框
        setTitle("ExpGitSubmitFile - Git文件导出工具"); // 设置会话框标题
        setSize(900, 650); // 设置窗口大小（增大以适应新界面）
        setResizable(true); // 允许用户调整窗口大小
    }

    @Override
    protected JComponent createNorthPanel() {
        // 可以在这里添加顶部标题栏
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("ExpGitSubmitFile - Git文件导出工具");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        titleLabel.setForeground(new Color(0, 0, 139)); // 深蓝色标题
        titleLabel.setBorder(new EmptyBorder(5, 0, 5, 0));

        headerPanel.add(titleLabel, BorderLayout.WEST);

        // 添加版本信息
        JLabel versionLabel = new JLabel("Version 2.0.0");
        versionLabel.setForeground(Color.GRAY);
        headerPanel.add(versionLabel, BorderLayout.EAST);

        return headerPanel;
    }

    @Override
    protected JComponent createSouthPanel() {
        return null; // 返回null以隐藏默认的OK/Cancel按钮
    }

    @Override
    protected JComponent createCenterPanel() {
        // 创建主框架
        mainFrame = new ExpGitSubmitMainFrame(project);
        JPanel mainPanel = mainFrame.getMainPanel();

        // 获取快速导出面板
        quickExportPanel = mainFrame.getQuickExportPanel();

        // 导出逻辑已由 QuickExportPanel 内部处理

        // 为快速导出面板设置一些默认值
        setupDefaultValues();

        return mainPanel;
    }

    private void setupDefaultValues() {
        // 注意：仓库路径已在 QuickExportPanel.initializeDefaults() 中正确设置
        // 这里只设置目标路径（基于工作空间路径）
        
        if (project != null) {
            String projectPath = project.getBasePath();
            if (projectPath != null) {
                // 设置默认的导出路径（基于工作空间路径）
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                String formattedDateTime = now.format(formatter);
                String defaultTargetPath = projectPath + java.io.File.separator + ("exp_" + formattedDateTime);
                
                quickExportPanel.getTargetFolderPath().setText(defaultTargetPath);
            }
        }

        // 设置默认的导出类型
        quickExportPanel.getTypeComboBox().setSelectedIndex(0); // 选择"已提交"
    }

    @Override
    public void doOKAction() {
        // 这里可以处理OK按钮的操作，如果需要的话
        super.doOKAction();
    }

    @Override
    protected void init() {
        super.init();
        // DialogWrapper 会自动处理窗口关闭事件
    }
}