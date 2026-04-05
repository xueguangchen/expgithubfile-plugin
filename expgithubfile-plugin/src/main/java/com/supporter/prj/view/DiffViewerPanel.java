package com.supporter.prj.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.supporter.prj.entity.DiffEntryItem;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 差异对比视图面板
 * 用于查看文件的变更差异
 *
 * @author xueguangchen
 * @version 3.0.0
 */
public class DiffViewerPanel {
    private Project project;
    private String repoPath;  // 仓库路径，用于打开文件
    private String targetPath; // 目标路径，用于导出差异报告
    private JPanel panel;
    private JTable diffTable;
    private DefaultTableModel tableModel;
    private JTextArea diffContentArea;
    private JLabel statisticsLabel;
    private List<DiffEntryItem> diffItems;
    private JComboBox<String> filterComboBox;
    private JTextField searchField;

    public DiffViewerPanel(Project project) {
        this.project = project;
        this.diffItems = new ArrayList<>();
        initialize();
    }

    private void initialize() {
        panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建顶部过滤区域
        JPanel topPanel = createTopPanel();
        panel.add(topPanel, BorderLayout.NORTH);

        // 创建中间分割区域
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(200);

        // 上部：文件列表
        JPanel fileListPanel = createFileListPanel();
        splitPane.setTopComponent(fileListPanel);

        // 下部：差异内容
        JPanel diffContentPanel = createDiffContentPanel();
        splitPane.setBottomComponent(diffContentPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        // 创建底部统计区域
        JPanel bottomPanel = createBottomPanel();
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // 初始状态为空
        clearDiffItems();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 搜索框
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JBLabel("搜索:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        searchField = new JTextField();
        searchField.setColumns(30);
        panel.add(searchField, gbc);

        // 过滤类型
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JBLabel("变更类型:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.3;
        filterComboBox = new JComboBox<>(new String[]{"全部", "新增", "修改", "删除", "重命名"});
        panel.add(filterComboBox, gbc);

        // 刷新按钮
        gbc.gridx = 4;
        gbc.weightx = 0;
        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> refreshDiffList());
        panel.add(refreshButton, gbc);

        return panel;
    }

    private JPanel createFileListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("变更文件列表"));

        // 创建表格模型
        tableModel = new DefaultTableModel(
                new Object[][]{},
                new Object[]{"文件路径", "变更类型", "状态"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        diffTable = new JBTable(tableModel);
        diffTable.getColumnModel().getColumn(0).setPreferredWidth(400);
        diffTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        diffTable.getColumnModel().getColumn(2).setPreferredWidth(80);

        diffTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedDiff();
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(diffTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createDiffContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("差异内容"));

        diffContentArea = new JTextArea();
        diffContentArea.setEditable(false);
        diffContentArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        diffContentArea.setText("请选择一个文件查看差异内容...");

        JBScrollPane scrollPane = new JBScrollPane(diffContentArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        statisticsLabel = new JBLabel("统计: 共 0 个文件 | 新增:0 | 修改:0 | 删除:0 | 总变更行数:0");
        panel.add(statisticsLabel, BorderLayout.WEST);

        // 操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportDiffButton = new JButton("导出差异报告");
        exportDiffButton.addActionListener(e -> exportDiffReport());
        buttonPanel.add(exportDiffButton);

        JButton openInEditorButton = new JButton("在编辑器中打开");
        openInEditorButton.addActionListener(e -> openInEditor());
        buttonPanel.add(openInEditorButton);

        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 加载示例数据
     */
    private void loadSampleData() {
        // 添加示例差异项
        addDiffItem(createSampleDiff("src/main/java/com/example/service/UserService.java", "MODIFY", 25, 10));
        addDiffItem(createSampleDiff("src/main/java/com/example/controller/UserController.java", "ADD", 120, 0));
        addDiffItem(createSampleDiff("src/main/java/com/example/util/OldUtil.java", "DELETE", 0, 50));
        addDiffItem(createSampleDiff("src/main/java/com/example/util/CommonUtil.java", "RENAME", 5, 3));
        addDiffItem(createSampleDiff("src/main/resources/application.yml", "MODIFY", 8, 2));

        updateTable();
        updateStatistics();
    }

    private DiffEntryItem createSampleDiff(String filePath, String changeType, int added, int deleted) {
        DiffEntryItem item = new DiffEntryItem(filePath, changeType);
        item.setAddedLines(added);
        item.setDeletedLines(deleted);
        
        // 生成模拟差异内容
        StringBuilder diffContent = new StringBuilder();
        diffContent.append("=== ").append(filePath).append(" ===\n\n");
        
        if (changeType.equals("ADD")) {
            diffContent.append("--- /dev/null\n");
            diffContent.append("+++ b/").append(filePath).append("\n");
            for (int i = 1; i <= added; i++) {
                diffContent.append("+新增行 ").append(i).append(": 示例代码内容\n");
            }
        } else if (changeType.equals("DELETE")) {
            diffContent.append("--- a/").append(filePath).append("\n");
            diffContent.append("+++ /dev/null\n");
            for (int i = 1; i <= deleted; i++) {
                diffContent.append("-删除行 ").append(i).append(": 原有代码内容\n");
            }
        } else if (changeType.equals("MODIFY")) {
            diffContent.append("--- a/").append(filePath).append("\n");
            diffContent.append("+++ b/").append(filePath).append("\n");
            diffContent.append("@@ -1,5 +1,8 @@\n");
            for (int i = 1; i <= deleted; i++) {
                diffContent.append("-删除行 ").append(i).append("\n");
            }
            for (int i = 1; i <= added; i++) {
                diffContent.append("+新增行 ").append(i).append("\n");
            }
        } else if (changeType.equals("RENAME")) {
            diffContent.append("--- a/").append(filePath.replace("CommonUtil", "OldUtil")).append("\n");
            diffContent.append("+++ b/").append(filePath).append("\n");
            diffContent.append("similarity index 90%\n");
            diffContent.append("rename from ").append(filePath.replace("CommonUtil", "OldUtil")).append("\n");
            diffContent.append("rename to ").append(filePath).append("\n");
        }
        
        item.setNewContent(diffContent.toString());
        return item;
    }

    private void addDiffItem(DiffEntryItem item) {
        diffItems.add(item);
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        String filterType = (String) filterComboBox.getSelectedItem();
        String searchText = searchField.getText().toLowerCase();

        for (DiffEntryItem item : diffItems) {
            // 应用过滤
            if (!filterType.equals("全部") && !item.getChangeTypeDesc().equals(filterType)) {
                continue;
            }
            if (StringUtils.isNotBlank(searchText) && !item.getFilePath().toLowerCase().contains(searchText)) {
                continue;
            }

            tableModel.addRow(new Object[]{
                    item.getFilePath(),
                    item.getChangeTypeIcon() + " " + item.getChangeTypeDesc(),
                    "查看"
            });
        }
    }

    private void updateStatistics() {
        int total = diffItems.size();
        int addCount = 0, modifyCount = 0, deleteCount = 0;

        for (DiffEntryItem item : diffItems) {
            switch (item.getChangeType()) {
                case "ADD": addCount++; break;
                case "MODIFY": modifyCount++; break;
                case "DELETE": deleteCount++; break;
            }
        }

        statisticsLabel.setText(String.format(
                "统计: 共 %d 个文件 | 新增:%d | 修改:%d | 删除:%d",
                total, addCount, modifyCount, deleteCount
        ));
    }

    private void showSelectedDiff() {
        int selectedRow = diffTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < diffItems.size()) {
            DiffEntryItem item = diffItems.get(selectedRow);
            String content = item.getNewContent();
            diffContentArea.setText(content != null ? content : "无差异内容");
            diffContentArea.setCaretPosition(0);
        }
    }

    private void refreshDiffList() {
        updateTable();
        updateStatistics();
    }

    private void exportDiffReport() {
        if (diffItems == null || diffItems.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "没有差异数据可导出", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 检查目标路径是否设置
        if (targetPath == null || targetPath.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "请先在\"快速导出\"页面设置目标路径", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 确保目标目录存在
        java.io.File targetDir = new java.io.File(targetPath);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        // 生成报告文件名
        String reportFileName = "diff_report_" + 
            new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".txt";
        java.io.File targetFile = new java.io.File(targetPath, reportFileName);
        
        StringBuilder report = new StringBuilder();
        report.append("═══════════════════════════════════════════════════════════\n");
        report.append("                      差异报告\n");
        report.append("═══════════════════════════════════════════════════════════\n\n");
        report.append("生成时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("\n");
        report.append("仓库路径: ").append(repoPath != null ? repoPath : "未知").append("\n");
        report.append("目标路径: ").append(targetPath).append("\n\n");
        
        report.append("─── 统计信息 ───\n");
        report.append(statisticsLabel.getText()).append("\n\n");
        
        report.append("─── 文件列表 ───\n");
        for (DiffEntryItem item : diffItems) {
            report.append(item.getDiffSummary()).append("\n");
        }
        
        report.append("\n─── 详细差异 ───\n\n");
        for (DiffEntryItem item : diffItems) {
            report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            report.append("文件: ").append(item.getFilePath()).append("\n");
            report.append("类型: ").append(item.getChangeTypeDesc()).append("\n");
            if (item.getNewContent() != null && !item.getNewContent().isEmpty()) {
                report.append("\n").append(item.getNewContent()).append("\n");
            }
            report.append("\n");
        }
        
        report.append("═══════════════════════════════════════════════════════════\n");
        report.append("                    报告生成完成\n");
        report.append("═══════════════════════════════════════════════════════════\n");
        
        try (java.io.FileWriter writer = new java.io.FileWriter(targetFile)) {
            writer.write(report.toString());
            JOptionPane.showMessageDialog(panel, 
                "差异报告已保存到:\n" + targetFile.getAbsolutePath(), 
                "导出成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(panel, 
                "保存失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openInEditor() {
        int selectedRow = diffTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(panel, "请先选择一个文件", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        DiffEntryItem item = diffItems.get(selectedRow);
        String filePath = item.getFilePath();
        
        // 构建完整文件路径
        String fullPath;
        if (repoPath != null && !repoPath.isEmpty()) {
            fullPath = repoPath + java.io.File.separator + filePath;
        } else {
            // 尝试从项目路径获取
            if (project != null && project.getBasePath() != null) {
                fullPath = project.getBasePath() + java.io.File.separator + filePath;
            } else {
                JOptionPane.showMessageDialog(panel, 
                    "无法确定文件路径，请先设置仓库路径", 
                    "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        
        // 使用 IDEA API 打开文件
        java.io.File file = new java.io.File(fullPath);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(panel, 
                "文件不存在: " + fullPath, 
                "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        if (virtualFile != null) {
            // 打开文件编辑器
            com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(virtualFile, true);
        } else {
            JOptionPane.showMessageDialog(panel, 
                "无法在编辑器中打开文件: " + filePath, 
                "提示", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * 设置仓库路径
     */
    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }
    
    /**
     * 设置目标路径（用于导出差异报告）
     */
    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    /**
     * 设置差异数据
     */
    public void setDiffItems(List<DiffEntryItem> items) {
        this.diffItems = items != null ? items : new ArrayList<>();
        updateTable();
        updateStatistics();
    }
    
    /**
     * 清空差异数据
     */
    public void clearDiffItems() {
        this.diffItems = new ArrayList<>();
        updateTable();
        statisticsLabel.setText("统计: 共 0 个文件 | 新增:0 | 修改:0 | 删除:0");
        diffContentArea.setText("请先预览文件，差异数据将在预览后显示...");
    }

    public JPanel getPanel() {
        return panel;
    }
}
