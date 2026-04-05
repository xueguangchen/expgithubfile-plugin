package com.supporter.prj.view;

import com.intellij.openapi.project.Project;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;

import javax.swing.*;
import java.awt.*;

/**
 * ExpGitSubmitFile 插件主界面
 * 使用 IntelliJ Platform UI Components 实现
 *
 * @author xueguangchen
 * @version 3.0.0
 */
public class ExpGitSubmitMainFrame {
    private Project project;
    private JBTabbedPane tabbedPane;
    
    // 第一阶段：核心功能
    private QuickExportPanel quickExportPanel;
    private FilePreviewPanel filePreviewPanel;
    private ConfigManagePanel configManagePanel;
    
    // 第二阶段：效率提升
    private ExportHistoryPanel exportHistoryPanel;
    
    // 第三阶段：高级功能
    private DiffViewerPanel diffViewerPanel;

    public ExpGitSubmitMainFrame(Project project) {
        this.project = project;
        initialize();
    }

    private void initialize() {
        // 创建 Tab 面板
        tabbedPane = new JBTabbedPane();

        // 创建各个标签页 - 第一阶段
        quickExportPanel = new QuickExportPanel(project);
        filePreviewPanel = new FilePreviewPanel(project);
        configManagePanel = new ConfigManagePanel(project);
        
        // 创建各个标签页 - 第二阶段
        exportHistoryPanel = new ExportHistoryPanel(project);
        
        // 创建各个标签页 - 第三阶段
        diffViewerPanel = new DiffViewerPanel(project);
        
        // 设置面板之间的关联（必须在所有面板创建后）
        quickExportPanel.setFilePreviewPanel(filePreviewPanel);
        quickExportPanel.setExportHistoryPanel(exportHistoryPanel);
        quickExportPanel.setConfigManagePanel(configManagePanel);
        filePreviewPanel.setDiffViewerPanel(diffViewerPanel);
        filePreviewPanel.setExportHistoryPanel(exportHistoryPanel);
        configManagePanel.setQuickExportPanel(quickExportPanel);
        
        // 所有面板关联完成后，重新同步仓库路径（防止被其他组件覆盖）
        quickExportPanel.syncRepoPath();

        // 添加标签页 - 核心功能
        tabbedPane.addTab("快速导出", quickExportPanel.getPanel());
        tabbedPane.addTab("文件预览", filePreviewPanel.getPanel());
        tabbedPane.addTab("差异对比", diffViewerPanel.getPanel());
        tabbedPane.addTab("配置管理", configManagePanel.getPanel());

        // 添加标签页 - 效率提升
        tabbedPane.addTab("导出历史", exportHistoryPanel.getPanel());
    }

    public JBPanel<?> getMainPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        // 移除 Tab 内容区域的边框，减少空白
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    // 第一阶段：核心功能 Getters
    public QuickExportPanel getQuickExportPanel() {
        return quickExportPanel;
    }

    public FilePreviewPanel getFilePreviewPanel() {
        return filePreviewPanel;
    }

    public ConfigManagePanel getConfigManagePanel() {
        return configManagePanel;
    }

    // 第二阶段：效率提升 Getters
    public ExportHistoryPanel getExportHistoryPanel() {
        return exportHistoryPanel;
    }

    // 第三阶段：高级功能 Getters
    public DiffViewerPanel getDiffViewerPanel() {
        return diffViewerPanel;
    }
}
