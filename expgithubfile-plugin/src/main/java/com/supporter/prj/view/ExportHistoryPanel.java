package com.supporter.prj.view;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.supporter.prj.entity.ExportHistory;
import com.supporter.prj.util.ExpGitHubUtil;
import com.supporter.prj.util.ExportHistoryManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 导出历史标签页
 * 用于查看和管理导出历史记录
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class ExportHistoryPanel {
    private Project project;
    private JPanel panel;
    private JBList<ExportHistory> historyList;
    private DefaultListModel<ExportHistory> listModel;
    private JTextArea detailArea;
    private JTextField searchField;
    private JComboBox<String> filterComboBox;
    private JLabel statusLabel;
    private JButton refreshButton;
    private JButton clearButton;
    private JButton exportCsvButton;
    private JButton reExportButton;
    private JButton openFolderButton;

    private ExportHistoryManager historyManager;
    private String currentRepoPath; // 当前仓库路径，用于过滤历史记录

    public ExportHistoryPanel(Project project) {
        this.project = project;
        this.historyManager = ExportHistoryManager.getInstance();
        initialize();
    }

    private void initialize() {
        // 创建主面板
        panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建顶部控制区域
        JPanel topPanel = createTopPanel();
        panel.add(topPanel, BorderLayout.NORTH);

        // 创建中间区域（列表+详情）
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(500);

        // 左侧：历史列表
        JPanel leftPanel = createLeftPanel();
        splitPane.setLeftComponent(leftPanel);

        // 右侧：详情
        JPanel rightPanel = createRightPanel();
        splitPane.setRightComponent(rightPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        // 创建底部统计区域
        JPanel bottomPanel = createBottomPanel();
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // 加载数据
        loadHistory();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 搜索框
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JBLabel("搜索:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(300, 25));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { searchHistory(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { searchHistory(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { searchHistory(); }
        });
        panel.add(searchField, gbc);

        // 过滤下拉框
        gbc.gridx = 2;
        gbc.weightx = 0;
        panel.add(new JBLabel("状态:"), gbc);

        gbc.gridx = 3;
        filterComboBox = new JComboBox<>();
        filterComboBox.addItem("全部");
        filterComboBox.addItem("成功");
        filterComboBox.addItem("失败");
        filterComboBox.addActionListener(e -> filterHistory());
        panel.add(filterComboBox, gbc);

        // 刷新按钮
        gbc.gridx = 4;
        refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> loadHistory());
        panel.add(refreshButton, gbc);

        // 清空按钮
        gbc.gridx = 5;
        clearButton = new JButton("🗑️ 清空");
        clearButton.addActionListener(e -> clearAllHistory());
        panel.add(clearButton, gbc);

        return panel;
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // 标题
        JLabel titleLabel = new JBLabel("导出历史记录");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 14));
        panel.add(titleLabel, BorderLayout.NORTH);

        // 历史列表
        listModel = new DefaultListModel<>();
        historyList = new JBList<>(listModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new HistoryCellRenderer());
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showHistoryDetail();
            }
        });

        // 右键菜单
        historyList.setComponentPopupMenu(createPopupMenu());

        JScrollPane scrollPane = new JBScrollPane(historyList);
        scrollPane.setPreferredSize(new Dimension(480, 400));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // 标题
        JLabel titleLabel = new JBLabel("详情信息");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 14));
        panel.add(titleLabel, BorderLayout.NORTH);

        // 详情文本区域
        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailArea.setText("选择一条历史记录查看详情...");

        JScrollPane scrollPane = new JBScrollPane(detailArea);
        scrollPane.setPreferredSize(new Dimension(280, 350));
        panel.add(scrollPane, BorderLayout.CENTER);

        // 操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        reExportButton = new JButton("重新导出");
        reExportButton.addActionListener(e -> reExport());
        buttonPanel.add(reExportButton);

        openFolderButton = new JButton("打开文件夹");
        openFolderButton.addActionListener(e -> openTargetFolder());
        buttonPanel.add(openFolderButton);

        exportCsvButton = new JButton("导出CSV");
        exportCsvButton.addActionListener(e -> exportToCsv());
        buttonPanel.add(exportCsvButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // 统计信息
        statusLabel = new JBLabel("共 0 条记录");
        statusLabel.setForeground(new Color(100, 100, 100));
        panel.add(statusLabel, BorderLayout.WEST);

        return panel;
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem reExportItem = new JMenuItem("重新导出");
        reExportItem.addActionListener(e -> reExport());
        menu.add(reExportItem);

        JMenuItem openFolderItem = new JMenuItem("打开目标文件夹");
        openFolderItem.addActionListener(e -> openTargetFolder());
        menu.add(openFolderItem);

        menu.addSeparator();

        JMenuItem deleteItem = new JMenuItem("删除此记录");
        deleteItem.addActionListener(e -> deleteSelectedHistory());
        menu.add(deleteItem);

        JMenuItem copyPathItem = new JMenuItem("复制目标路径");
        copyPathItem.addActionListener(e -> copyTargetPath());
        menu.add(copyPathItem);

        return menu;
    }

    private void loadHistory() {
        listModel.clear();

        // 没有选择仓库时，不显示历史记录
        if (currentRepoPath == null || currentRepoPath.isEmpty()) {
            System.out.println("[ExportHistoryPanel] 未选择仓库，不加载历史记录");
            detailArea.setText("请在\"快速导出\"页面选择仓库路径以查看相关历史记录...");
            updateStatistics();
            return;
        }

        List<ExportHistory> histories = historyManager.getAllHistory();

        System.out.println("[ExportHistoryPanel] 加载历史记录, 共 " + histories.size() + " 条, 当前仓库: " + currentRepoPath);

        // 按仓库路径过滤
        histories.removeIf(h -> !currentRepoPath.equals(h.getRepoPath()));
        System.out.println("[ExportHistoryPanel] 过滤后历史记录, 共 " + histories.size() + " 条");

        for (ExportHistory history : histories) {
            listModel.addElement(history);
        }

        updateStatistics();
    }

    /**
     * 设置当前仓库路径并刷新历史记录
     */
    public void setCurrentRepoPath(String repoPath) {
        this.currentRepoPath = (repoPath != null && !repoPath.trim().isEmpty()) ? repoPath.trim() : null;
        loadHistory();
    }

    /**
     * 刷新历史记录列表（公共方法）
     */
    public void refreshHistory() {
        System.out.println("[ExportHistoryPanel] refreshHistory 被调用");
        loadHistory();
    }

    private void searchHistory() {
        String keyword = searchField.getText().trim();
        listModel.clear();

        // 没有选择仓库时，不显示历史记录
        if (currentRepoPath == null || currentRepoPath.isEmpty()) {
            detailArea.setText("请在\"快速导出\"页面选择仓库路径以查看相关历史记录...");
            updateStatistics();
            return;
        }

        List<ExportHistory> results = historyManager.searchHistory(keyword);

        // 按仓库路径过滤
        results.removeIf(h -> !currentRepoPath.equals(h.getRepoPath()));

        for (ExportHistory history : results) {
            listModel.addElement(history);
        }

        updateStatistics();
    }

    private void filterHistory() {
        String filter = (String) filterComboBox.getSelectedItem();
        listModel.clear();

        // 没有选择仓库时，不显示历史记录
        if (currentRepoPath == null || currentRepoPath.isEmpty()) {
            detailArea.setText("请在\"快速导出\"页面选择仓库路径以查看相关历史记录...");
            updateStatistics();
            return;
        }

        List<ExportHistory> histories;
        if ("成功".equals(filter)) {
            histories = historyManager.getSuccessHistory();
        } else if ("失败".equals(filter)) {
            histories = historyManager.getFailedHistory();
        } else {
            histories = historyManager.getAllHistory();
        }

        // 按仓库路径过滤
        histories.removeIf(h -> !currentRepoPath.equals(h.getRepoPath()));

        for (ExportHistory history : histories) {
            listModel.addElement(history);
        }

        updateStatistics();
    }

    private void showHistoryDetail() {
        ExportHistory selected = historyList.getSelectedValue();
        if (selected == null) {
            detailArea.setText("选择一条历史记录查看详情...");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════\n");
        sb.append("           导出详情\n");
        sb.append("═══════════════════════════════════\n\n");

        sb.append("导出时间: ").append(selected.getFormattedExportTime()).append("\n");
        sb.append("配置名称: ").append(selected.getConfigName() != null ? selected.getConfigName() : "未命名").append("\n");
        sb.append("导出类型: ").append(selected.getExportType() != null ? selected.getExportType() : "未知").append("\n");
        sb.append("状态: ").append(selected.isSuccess() ? "[成功]" : "[失败]").append("\n\n");

        sb.append("─── 路径信息 ───\n");
        sb.append("仓库路径: ").append(selected.getRepoPath() != null ? selected.getRepoPath() : "").append("\n");
        sb.append("目标路径: ").append(selected.getTargetPath() != null ? selected.getTargetPath() : "").append("\n\n");

        sb.append("─── 文件统计 ───\n");
        sb.append("文件数量: ").append(selected.getFileCount()).append("\n");
        sb.append("总大小: ").append(selected.getFormattedTotalSize()).append("\n\n");

        if (!selected.getCommitIds().isEmpty()) {
            sb.append("─── 提交记录 ───\n");
            for (String commitId : selected.getCommitIds()) {
                sb.append("  • ").append(commitId).append("\n");
            }
            sb.append("\n");
        }

        if (selected.getFilterRuleName() != null && !selected.getFilterRuleName().isEmpty()) {
            sb.append("过滤规则: ").append(selected.getFilterRuleName()).append("\n\n");
        }

        if (!selected.isSuccess() && selected.getErrorMessage() != null) {
            sb.append("─── 错误信息 ───\n");
            sb.append(selected.getErrorMessage()).append("\n");
        }

        detailArea.setText(sb.toString());
    }

    private void reExport() {
        ExportHistory selected = historyList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(panel, "请先选择一条历史记录", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 检查仓库路径是否存在
        java.io.File repoDir = new java.io.File(selected.getRepoPath());
        if (!repoDir.exists()) {
            JOptionPane.showMessageDialog(panel, "原仓库路径不存在:\n" + selected.getRepoPath(), "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                panel,
                "确定要重新执行此导出操作吗？\n" +
                "仓库路径: " + selected.getRepoPath() + "\n" +
                "目标路径: " + selected.getTargetPath(),
                "确认重新导出",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            // 执行重新导出
            doReExport(selected);
        }
    }
    
    /**
     * 执行重新导出
     */
    private void doReExport(ExportHistory history) {
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(panel, "正在重新导出...", "提示", JOptionPane.INFORMATION_MESSAGE)
                );
                
                String exportType = history.getExportType();
                String repoPath = history.getRepoPath();
                String targetPath = history.getTargetPath();

                // 确保目标目录存在
                java.io.File targetDir = new java.io.File(targetPath);
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }

                com.supporter.prj.entity.ExportResult result = null;
                if ("已提交".equals(exportType) && history.getCommitIds() != null && !history.getCommitIds().isEmpty()) {
                    // 已提交文件导出
                    String[] commitIds = history.getCommitIds().toArray(new String[0]);
                    result = ExpGitHubUtil.expCommittedFile(repoPath, targetPath, commitIds, null);
                } else {
                    // 未提交文件导出
                    result = ExpGitHubUtil.expUncommittedFiles(repoPath, targetPath, null);
                }

                final com.supporter.prj.entity.ExportResult finalResult = result;
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(panel,
                        "重新导出完成！\n导出文件数: " + finalResult.getFileCount() + "\n总大小: " + finalResult.getFormattedTotalSize() + "\n目标路径: " + targetPath,
                        "导出成功", JOptionPane.INFORMATION_MESSAGE);
                    // 刷新历史列表
                    loadHistory();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(panel, 
                        "重新导出失败: " + e.getMessage(), 
                        "错误", JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }

    private void openTargetFolder() {
        ExportHistory selected = historyList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(panel, "请先选择一条历史记录", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String targetPath = selected.getTargetPath();
        if (targetPath == null || targetPath.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "目标路径为空", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Desktop.getDesktop().open(new File(targetPath));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(panel, "无法打开文件夹: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedHistory() {
        ExportHistory selected = historyList.getSelectedValue();
        if (selected == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                panel,
                "确定要删除此历史记录吗？",
                "确认删除",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            historyManager.deleteHistory(selected.getId());
            loadHistory();
        }
    }

    private void copyTargetPath() {
        ExportHistory selected = historyList.getSelectedValue();
        if (selected == null) {
            return;
        }

        String targetPath = selected.getTargetPath();
        if (targetPath != null && !targetPath.isEmpty()) {
            java.awt.datatransfer.StringSelection selection = 
                    new java.awt.datatransfer.StringSelection(targetPath);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            JOptionPane.showMessageDialog(panel, "路径已复制到剪贴板", "提示", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearAllHistory() {
        int confirm = JOptionPane.showConfirmDialog(
                panel,
                "确定要清空所有历史记录吗？此操作不可恢复！",
                "确认清空",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            historyManager.clearAllHistory();
            loadHistory();
        }
    }

    private void exportToCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出历史记录");
        fileChooser.setSelectedFile(new File("export_history_" + 
                new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".csv"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV文件 (*.csv)", "csv"));

        if (fileChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
            try {
                historyManager.exportToCSV(fileChooser.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(panel, "导出成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateStatistics() {
        Map<String, Object> stats = historyManager.getStatistics();
        int totalCount = (int) stats.get("totalCount");
        int successCount = (int) stats.get("successCount");
        int failedCount = (int) stats.get("failedCount");

        statusLabel.setText(String.format("共 %d 条记录 | 成功: %d | 失败: %d", 
                totalCount, successCount, failedCount));
    }

    public JPanel getPanel() {
        return panel;
    }

    /**
     * 历史记录列表单元格渲染器
     */
    private static class HistoryCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ExportHistory) {
                ExportHistory history = (ExportHistory) value;

                StringBuilder sb = new StringBuilder();
                sb.append(history.getFormattedExportTime());
                sb.append(" | ");

                String configName = history.getConfigName();
                sb.append(configName != null ? configName : "未命名");
                sb.append(" | ");
                sb.append(history.getFileCount()).append("个文件");

                if (!history.isSuccess()) {
                    sb.append(" | [失败]");
                    // 选中时使用默认前景色，避免深色主题下看不清
                    if (!isSelected) {
                        setForeground(Color.RED);
                    }
                } else {
                    // 选中时使用默认前景色，避免深色主题下看不清
                    if (!isSelected) {
                        setForeground(new Color(0, 128, 0));
                    }
                }

                setText(sb.toString());
            }

            return this;
        }
    }
}
