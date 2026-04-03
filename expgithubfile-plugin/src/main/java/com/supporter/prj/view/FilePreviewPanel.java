package com.supporter.prj.view;

import com.intellij.openapi.project.Project;
import com.intellij.ui.*;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import com.supporter.prj.entity.DiffEntryItem;
import com.supporter.prj.entity.ExportHistory;
import com.supporter.prj.entity.ExportOptions;
import com.supporter.prj.entity.FileInfo;
import com.supporter.prj.entity.FilterRule;
import com.supporter.prj.util.ExpGitHubUtil;
import com.supporter.prj.util.ExportHistoryManager;
import org.eclipse.jgit.diff.DiffEntry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.*;
import java.awt.datatransfer.StringSelection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件预览标签页
 * 用于预览将要导出的文件列表
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class FilePreviewPanel {
    private Project project;
    private JPanel panel;
    private JTree fileTree;
    private DefaultMutableTreeNode root;
    private Map<String, FilePreviewItem> fileItemMap;
    private JTextField searchField;
    private JComboBox<String> filterComboBox;
    private JButton selectAllButton;
    private JButton deselectAllButton;
    private JButton copyListButton;
    private JButton refreshButton;
    private JLabel statisticsLabel;
    private JLabel statusLabel;
    
    // 预览数据来源
    private String repoPath;
    private String[] commitIds;
    private boolean isCommitted;
    
    // 过滤规则
    private ExportOptions exportOptions;
    
    // 当前过滤类型
    private String currentFilterType = "全部";
    private String currentSearchText = "";
    
    // 关联差异对比面板
    private DiffViewerPanel diffViewerPanel;
    
    // 关联导出历史面板
    private ExportHistoryPanel exportHistoryPanel;
    
    // 目标路径（用于导出差异报告）
    private String targetPath;

    public FilePreviewPanel(Project project) {
        this.project = project;
        this.fileItemMap = new LinkedHashMap<>();
        initialize();
    }

    private void initialize() {
        // 创建主面板
        panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建顶部控制区域
        JPanel topPanel = createTopPanel();
        panel.add(topPanel, BorderLayout.NORTH);

        // 创建文件树区域
        JComponent treePanel = createTreePanel();
        panel.add(treePanel, BorderLayout.CENTER);

        // 创建底部统计区域
        JPanel bottomPanel = createBottomPanel();
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // 初始化空状态
        initializeEmptyState();
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
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterTree(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterTree(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterTree(); }
        });
        panel.add(searchField, gbc);

        // 过滤下拉框
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 10, 5, 5);
        panel.add(new JBLabel("过滤:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        filterComboBox = new JComboBox<>();
        filterComboBox.addItem("全部");
        filterComboBox.addItem("仅修改");
        filterComboBox.addItem("仅新增");
        filterComboBox.addItem("仅删除");
        filterComboBox.addActionListener(e -> filterTree());
        panel.add(filterComboBox, gbc);

        // 刷新按钮
        gbc.gridx = 4;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 10, 5, 5);
        refreshButton = new JButton("刷新预览");
        refreshButton.addActionListener(e -> refreshPreview());
        panel.add(refreshButton, gbc);

        // 按钮区域
        gbc.gridx = 5;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        selectAllButton = new JButton("全选");
        selectAllButton.addActionListener(e -> selectAll());
        panel.add(selectAllButton, gbc);

        gbc.gridx = 6;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        deselectAllButton = new JButton("取消全选");
        deselectAllButton.addActionListener(e -> deselectAll());
        panel.add(deselectAllButton, gbc);

        gbc.gridx = 7;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        copyListButton = new JButton("复制列表");
        copyListButton.addActionListener(e -> copyFileList());
        panel.add(copyListButton, gbc);
        
        // 反选按钮
        gbc.gridx = 8;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        JButton invertSelectionButton = new JButton("反选");
        invertSelectionButton.addActionListener(e -> invertSelection());
        panel.add(invertSelectionButton, gbc);

        return panel;
    }

    private JComponent createTreePanel() {
        // 创建文件树
        root = new DefaultMutableTreeNode("将要导出的文件");
        fileTree = new JTree(root);
        fileTree.setCellRenderer(new FileTreeCellRenderer());
        fileTree.setShowsRootHandles(true);

        // 设置选择模式
        fileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // 添加展开/折叠监听器
        fileTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                updateTreeSelection();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                updateTreeSelection();
            }
        });
        
        // 添加鼠标点击事件，点击文件节点切换选中状态
        fileTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = fileTree.getRowForLocation(e.getX(), e.getY());
                if (row < 0) return;
                
                TreePath path = fileTree.getPathForRow(row);
                if (path == null) return;
                
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof FilePreviewItem) {
                    FilePreviewItem item = (FilePreviewItem) node.getUserObject();
                    // 切换选中状态
                    item.setSelected(!item.isSelected());
                    // 更新树显示
                    ((DefaultTreeModel) fileTree.getModel()).nodeChanged(node);
                    // 更新统计信息
                    updateStatistics();
                }
            }
        });

        // 创建滚动面板
        JScrollPane scrollPane = new JBScrollPane(fileTree);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        return scrollPane;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // 统计信息
        statisticsLabel = new JBLabel();
        updateStatistics();

        // 状态标签
        statusLabel = new JBLabel("请先在\"快速导出\"标签页配置导出参数后点击\"预览文件\"");
        statusLabel.setForeground(new Color(128, 128, 128));

        // 批量操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportSelectedButton = new JButton("导出选中文件");
        exportSelectedButton.addActionListener(e -> exportSelectedFiles());
        buttonPanel.add(exportSelectedButton);

        panel.add(statisticsLabel, BorderLayout.WEST);
        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 初始化空状态
     */
    private void initializeEmptyState() {
        fileItemMap.clear();
        // 显示提示信息
        root.removeAllChildren();
        DefaultMutableTreeNode hintNode = new DefaultMutableTreeNode("请在\"快速导出\"标签页配置参数后点击\"预览文件\"按钮");
        root.add(hintNode);
        ((DefaultTreeModel) fileTree.getModel()).reload();
        updateStatistics();
    }

    /**
     * 设置预览数据源
     * @param repoPath 仓库路径
     * @param commitIds 提交ID数组
     * @param isCommitted 是否为已提交
     */
    public void setPreviewSource(String repoPath, String[] commitIds, boolean isCommitted) {
        setPreviewSource(repoPath, commitIds, isCommitted, null);
    }
    
    /**
     * 设置预览数据源（支持过滤规则）
     * @param repoPath 仓库路径
     * @param commitIds 提交ID数组
     * @param isCommitted 是否为已提交
     * @param exportOptions 导出选项（包含过滤规则）
     */
    public void setPreviewSource(String repoPath, String[] commitIds, boolean isCommitted, ExportOptions exportOptions) {
        this.repoPath = repoPath;
        this.commitIds = commitIds;
        this.isCommitted = isCommitted;
        this.exportOptions = exportOptions;
    }

    /**
     * 刷新预览
     */
    public void refreshPreview() {
        if (repoPath == null || repoPath.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "请先在\"快速导出\"标签页设置仓库路径！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        statusLabel.setText("正在加载文件列表...");
        statusLabel.setForeground(new Color(64, 158, 255));
        
        new Thread(() -> {
            try {
                fileItemMap.clear();
                
                System.out.println("[FilePreviewPanel] refreshPreview: isCommitted=" + isCommitted + 
                    ", commitIds=" + (commitIds != null ? commitIds.length : "null"));
                
                if (isCommitted) {
                    // 加载已提交文件的预览
                    if (commitIds == null || commitIds.length == 0) {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("没有选择提交记录");
                            statusLabel.setForeground(new Color(244, 67, 54));
                            updateTree();
                        });
                        return;
                    }
                    loadCommittedFiles();
                } else {
                    // 加载未提交文件的预览
                    loadUncommittedFiles();
                }
                
                SwingUtilities.invokeLater(() -> {
                    updateTree();
                    if (fileItemMap.isEmpty()) {
                        statusLabel.setText("没有找到文件");
                        statusLabel.setForeground(new Color(255, 152, 0));
                    } else {
                        statusLabel.setText("已加载 " + fileItemMap.size() + " 个文件");
                        statusLabel.setForeground(new Color(46, 125, 50));
                    }
                    // 同步差异数据到差异对比面板
                    syncToDiffViewer();
                });
            } catch (Exception e) {
                System.err.println("[FilePreviewPanel] 加载文件失败: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("加载失败: " + e.getMessage());
                    statusLabel.setForeground(new Color(244, 67, 54));
                    JOptionPane.showMessageDialog(panel, "加载文件列表失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    updateTree();
                });
            }
        }).start();
    }

    /**
     * 加载已提交文件
     */
    private void loadCommittedFiles() throws Exception {
        System.out.println("[FilePreviewPanel] 开始加载已提交文件, commitIds数量: " + (commitIds != null ? commitIds.length : 0));
        int totalFiles = 0;
        int filteredFiles = 0;
        int processedCommits = 0;
        
        for (String commitId : commitIds) {
            commitId = commitId.replaceAll("[\\s\\n\\r]", "");
            if (commitId.isEmpty()) {
                System.out.println("[FilePreviewPanel] 跳过空提交ID");
                continue;
            }
            
            processedCommits++;
            System.out.println("[FilePreviewPanel] 正在加载提交: " + commitId);
            try {
                java.util.List<FileInfo> files = ExpGitHubUtil.getCommitFiles(repoPath, commitId);
                System.out.println("[FilePreviewPanel] 提交 " + commitId + " 返回文件数: " + (files != null ? files.size() : 0));
                
                if (files != null && !files.isEmpty()) {
                    for (FileInfo fileInfo : files) {
                        String filePath = fileInfo.getFilePath();
                        
                        // 应用过滤规则
                        if (exportOptions != null && exportOptions.shouldFilterFile(filePath)) {
                            filteredFiles++;
                            System.out.println("[FilePreviewPanel] 文件被过滤: " + filePath);
                            continue;
                        }
                        
                        String changeType = convertChangeType(fileInfo.getFileType());
                        File file = new File(repoPath, filePath);
                        long fileSize = file.exists() ? file.length() : 0;
                        
                        FilePreviewItem item = new FilePreviewItem(
                            filePath, 
                            changeType, 
                            fileSize, 
                            0, 0
                        );
                        fileItemMap.put(filePath, item);
                        totalFiles++;
                    }
                }
            } catch (Exception e) {
                System.err.println("[FilePreviewPanel] 加载提交 " + commitId + " 的文件失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("[FilePreviewPanel] 已提交文件加载完成, 处理提交数: " + processedCommits + 
            ", 总文件数: " + totalFiles + ", 过滤文件数: " + filteredFiles);
    }

    /**
     * 加载未提交文件
     */
    private void loadUncommittedFiles() throws Exception {
        System.out.println("[FilePreviewPanel] 开始加载未提交文件");
        java.util.List<FileInfo> files = ExpGitHubUtil.getUncommittedFiles(repoPath);
        System.out.println("[FilePreviewPanel] 未提交文件数量: " + (files != null ? files.size() : 0));
        
        int totalFiles = 0;
        int filteredFiles = 0;
        
        if (files != null) {
            for (FileInfo fileInfo : files) {
                String filePath = fileInfo.getFilePath();
                
                // 应用过滤规则
                if (exportOptions != null && exportOptions.shouldFilterFile(filePath)) {
                    filteredFiles++;
                    System.out.println("[FilePreviewPanel] 文件被过滤: " + filePath);
                    continue;
                }
                
                String changeType = convertChangeType(fileInfo.getFileType());
                File file = new File(repoPath, filePath);
                long fileSize = file.exists() ? file.length() : 0;
                
                FilePreviewItem item = new FilePreviewItem(
                    filePath, 
                    changeType, 
                    fileSize, 
                    0, 0
                );
                fileItemMap.put(filePath, item);
                totalFiles++;
            }
        }
        System.out.println("[FilePreviewPanel] 未提交文件加载完成, 总文件数: " + totalFiles + ", 过滤文件数: " + filteredFiles);
    }

    /**
     * 转换变更类型
     */
    private String convertChangeType(DiffEntry.ChangeType changeType) {
        switch (changeType) {
            case ADD:
                return "新增";
            case DELETE:
                return "删除";
            case MODIFY:
                return "修改";
            case RENAME:
                return "重命名";
            case COPY:
                return "复制";
            default:
                return "未知";
        }
    }

    private void addFileItem(String filePath, String changeType, long size, int addedLines, int deletedLines) {
        FilePreviewItem item = new FilePreviewItem(filePath, changeType, size, addedLines, deletedLines);
        fileItemMap.put(filePath, item);
    }

    private void updateTree() {
        if (fileItemMap == null || root == null) {
            return;
        }
        root.removeAllChildren();

        // 检查是否有文件
        if (fileItemMap.isEmpty()) {
            DefaultMutableTreeNode hintNode = new DefaultMutableTreeNode("没有可显示的文件");
            root.add(hintNode);
            ((DefaultTreeModel) fileTree.getModel()).reload();
            updateStatistics();
            return;
        }

        // 按目录分组
        Map<String, DefaultMutableTreeNode> directoryNodes = new HashMap<>();
        int visibleCount = 0;

        for (FilePreviewItem item : fileItemMap.values()) {
            // 应用过滤条件
            if (!shouldShowItem(item)) {
                continue;
            }
            
            if (item.isSelected()) {
                visibleCount++;
                String directoryPath = getDirectoryPath(item.getFilePath());

                // 确保目录节点存在
                if (!directoryNodes.containsKey(directoryPath)) {
                    DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode(directoryPath);
                    directoryNodes.put(directoryPath, dirNode);
                    root.add(dirNode);
                }

                // 添加文件节点
                DefaultMutableTreeNode fileNode = createFileNode(item);
                directoryNodes.get(directoryPath).add(fileNode);
            }
        }

        // 排序目录节点
        java.util.List<String> sortedDirectories = new ArrayList<>(directoryNodes.keySet());
        Collections.sort(sortedDirectories);

        // 重新排序子节点
        root.removeAllChildren();
        for (String dir : sortedDirectories) {
            root.add(directoryNodes.get(dir));
        }

        // 如果没有符合条件的文件，显示提示
        if (directoryNodes.isEmpty()) {
            DefaultMutableTreeNode hintNode = new DefaultMutableTreeNode("没有符合过滤条件的文件");
            root.add(hintNode);
        }

        // 展开所有节点
        expandAll(root, true);

        // 更新树模型
        ((DefaultTreeModel) fileTree.getModel()).reload();
        updateStatistics();
    }
    
    /**
     * 判断文件是否应该显示（根据过滤条件）
     */
    private boolean shouldShowItem(FilePreviewItem item) {
        // 按变更类型过滤
        if (!"全部".equals(currentFilterType)) {
            String filterType = currentFilterType;
            String changeType = item.getChangeType();
            
            // 过滤类型和变更类型的映射
            if ("仅新增".equals(filterType) && !"新增".equals(changeType)) {
                return false;
            }
            if ("仅修改".equals(filterType) && !"修改".equals(changeType)) {
                return false;
            }
            if ("仅删除".equals(filterType) && !"删除".equals(changeType)) {
                return false;
            }
        }
        
        // 按搜索文本过滤
        if (currentSearchText != null && !currentSearchText.isEmpty()) {
            String filePath = item.getFilePath().toLowerCase();
            String fileName = new java.io.File(item.getFilePath()).getName().toLowerCase();
            if (!filePath.contains(currentSearchText) && !fileName.contains(currentSearchText)) {
                return false;
            }
        }
        
        return true;
    }

    private String getDirectoryPath(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        int lastBackslash = filePath.lastIndexOf('\\');
        int lastSeparator = Math.max(lastSlash, lastBackslash);

        return lastSeparator > 0 ? filePath.substring(0, lastSeparator) : "根目录";
    }

    private DefaultMutableTreeNode createFileNode(FilePreviewItem item) {
        String displayName = getDisplayName(item);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(displayName);
        node.setUserObject(item);
        return node;
    }

    private String getDisplayName(FilePreviewItem item) {
        String fileName = new java.io.File(item.getFilePath()).getName();
        String icon = getFileIcon(item.getChangeType());
        String size = formatFileSize(item.getFileSize());
        String lines = String.format("↑%d ↓%d", item.getAddedLines(), item.getDeletedLines());

        return String.format("%s %s [%s] %s", icon, fileName, size, lines);
    }

    private String getFileIcon(String changeType) {
        switch (changeType) {
            case "新增": return "[+]";
            case "删除": return "[-]";
            case "修改": return "[~]";
            default: return "[ ]";
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + "B";
        if (size < 1024 * 1024) return String.format("%.1fKB", size / 1024.0);
        return String.format("%.1fMB", size / (1024.0 * 1024));
    }

    private void expandAll(TreeNode node, boolean expand) {
        // 递归展开/折叠所有子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            TreeNode child = node.getChildAt(i);
            expandAll(child, expand);
        }
        
        // 使用 JTree 的方法展开或折叠节点
        if (node instanceof DefaultMutableTreeNode) {
            TreePath path = new TreePath(((DefaultMutableTreeNode) node).getPath());
            if (expand) {
                fileTree.expandPath(path);
            } else {
                fileTree.collapsePath(path);
            }
        }
    }

    private void filterTree() {
        // 保存当前过滤条件
        currentSearchText = searchField.getText().toLowerCase();
        currentFilterType = (String) filterComboBox.getSelectedItem();
        
        System.out.println("[FilePreviewPanel] 过滤条件: 类型=" + currentFilterType + ", 搜索=" + currentSearchText);
        
        // 重新构建树（应用过滤条件）
        updateTree();
    }

    private void selectAll() {
        if (fileItemMap == null) {
            return;
        }
        for (FilePreviewItem item : fileItemMap.values()) {
            item.setSelected(true);
        }
        updateTree();
    }

    private void deselectAll() {
        if (fileItemMap == null) {
            return;
        }
        for (FilePreviewItem item : fileItemMap.values()) {
            item.setSelected(false);
        }
        updateTree();
    }
    
    /**
     * 反选 - 将所有文件的选中状态取反
     */
    private void invertSelection() {
        if (fileItemMap == null) {
            return;
        }
        for (FilePreviewItem item : fileItemMap.values()) {
            item.setSelected(!item.isSelected());
        }
        updateTree();
    }

    private void copyFileList() {
        if (fileItemMap == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (FilePreviewItem item : fileItemMap.values()) {
            if (item.isSelected()) {
                sb.append(item.getFilePath()).append("\n");
            }
        }

        if (sb.length() > 0) {
            StringSelection selection = new StringSelection(sb.toString());
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            JOptionPane.showMessageDialog(panel, "文件列表已复制到剪贴板", "提示", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(panel, "没有选中的文件", "提示", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exportSelectedFiles() {
        if (fileItemMap == null) {
            return;
        }
        // 统计选中文件数量
        long selectedCount = fileItemMap.values().stream()
                .filter(FilePreviewItem::isSelected)
                .count();

        if (selectedCount > 0) {
            // 选择目标目录
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogTitle("选择导出目标目录");
            
            int result = fileChooser.showSaveDialog(panel);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            
            String targetPath = fileChooser.getSelectedFile().getAbsolutePath();
            
            int confirm = JOptionPane.showConfirmDialog(
                    panel,
                    String.format("确定要导出 %d 个选中的文件到\n%s", selectedCount, targetPath),
                    "确认导出",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                // 执行选中文件的导出
                exportFiles(targetPath);
            }
        } else {
            JOptionPane.showMessageDialog(panel, "请先选择要导出的文件", "提示", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * 导出选中的文件到指定目录
     */
    private void exportFiles(String targetPath) {
        statusLabel.setText("正在导出...");
        statusLabel.setForeground(new Color(64, 158, 255));
        
        new Thread(() -> {
            try {
                java.io.File targetDir = new java.io.File(targetPath);
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                
                int successCount = 0;
                int failCount = 0;
                long totalSize = 0;
                List<String> exportedFiles = new ArrayList<>();
                
                for (FilePreviewItem item : fileItemMap.values()) {
                    if (item.isSelected()) {
                        try {
                            java.io.File sourceFile = new java.io.File(repoPath, item.getFilePath());
                            if (sourceFile.exists()) {
                                // 构建目标文件路径（保留目录结构）
                                java.io.File targetFile = new java.io.File(targetPath, item.getFilePath());
                                java.io.File parentDir = targetFile.getParentFile();
                                if (!parentDir.exists()) {
                                    parentDir.mkdirs();
                                }
                                
                                // 复制文件
                                java.nio.file.Files.copy(
                                    sourceFile.toPath(), 
                                    targetFile.toPath(), 
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                                );
                                successCount++;
                                totalSize += sourceFile.length();
                                exportedFiles.add(item.getFilePath());
                            } else {
                                System.err.println("[FilePreviewPanel] 源文件不存在: " + item.getFilePath());
                                failCount++;
                            }
                        } catch (Exception e) {
                            System.err.println("[FilePreviewPanel] 导出文件失败: " + item.getFilePath() + ", " + e.getMessage());
                            failCount++;
                        }
                    }
                }
                
                final int finalSuccess = successCount;
                final int finalFail = failCount;
                final long finalTotalSize = totalSize;
                final List<String> finalExportedFiles = exportedFiles;
                final String finalTargetPathResult = targetPath;
                
                // 保存导出历史记录
                ExportHistory history = new ExportHistory();
                history.setConfigName("快速导出");
                history.setRepoPath(repoPath);
                history.setTargetPath(finalTargetPathResult);
                history.setExportType(isCommitted ? "已提交" : "未提交");
                history.setFileCount(finalSuccess);
                history.setTotalSize(finalTotalSize);
                history.setExportedFiles(finalExportedFiles);
                if (commitIds != null && commitIds.length > 0) {
                    history.setCommitIds(Arrays.asList(commitIds));
                }
                history.setSuccess(finalFail == 0);
                if (finalFail > 0) {
                    history.setErrorMessage("部分文件导出失败: " + finalFail + " 个");
                }
                
                System.out.println("[FilePreviewPanel] 准备保存历史记录, exportHistoryPanel=" + (exportHistoryPanel != null ? "已设置" : "null"));
                ExportHistoryManager.getInstance().addHistory(history);
                
                SwingUtilities.invokeLater(() -> {
                    // 刷新导出历史面板
                    if (exportHistoryPanel != null) {
                        exportHistoryPanel.refreshHistory();
                    }
                    
                    if (finalFail == 0) {
                        statusLabel.setText("导出完成，成功导出 " + finalSuccess + " 个文件");
                        statusLabel.setForeground(new Color(46, 125, 50));
                        JOptionPane.showMessageDialog(panel, 
                            "导出完成！\n成功导出 " + finalSuccess + " 个文件到:\n" + finalTargetPathResult, 
                            "导出成功", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        statusLabel.setText("导出完成，成功 " + finalSuccess + " 个，失败 " + finalFail + " 个");
                        statusLabel.setForeground(new Color(255, 152, 0));
                        JOptionPane.showMessageDialog(panel, 
                            "导出完成！\n成功: " + finalSuccess + " 个\n失败: " + finalFail + " 个\n目标路径: " + finalTargetPathResult, 
                            "导出完成", JOptionPane.WARNING_MESSAGE);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("导出失败: " + e.getMessage());
                    statusLabel.setForeground(new Color(244, 67, 54));
                    JOptionPane.showMessageDialog(panel, "导出失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    
                    // 保存失败的历史记录
                    ExportHistory history = new ExportHistory();
                    history.setConfigName("快速导出");
                    history.setRepoPath(repoPath);
                    history.setTargetPath(targetPath);
                    history.setExportType(isCommitted ? "已提交" : "未提交");
                    history.setSuccess(false);
                    history.setErrorMessage(e.getMessage());
                    ExportHistoryManager.getInstance().addHistory(history);
                    
                    // 刷新导出历史面板
                    if (exportHistoryPanel != null) {
                        exportHistoryPanel.refreshHistory();
                    }
                });
            }
        }).start();
    }

    private void updateTreeSelection() {
        // 更新树形选择状态
        updateStatistics();
    }

    private void updateStatistics() {
        if (fileItemMap == null || statisticsLabel == null) {
            return;
        }
        
        // 应用过滤条件统计
        long totalCount = fileItemMap.values().stream()
                .filter(this::shouldShowItem)
                .filter(FilePreviewItem::isSelected)
                .count();

        long modifiedCount = fileItemMap.values().stream()
                .filter(this::shouldShowItem)
                .filter(item -> item.isSelected() && item.getChangeType().equals("修改"))
                .count();

        long addedCount = fileItemMap.values().stream()
                .filter(this::shouldShowItem)
                .filter(item -> item.isSelected() && item.getChangeType().equals("新增"))
                .count();

        long deletedCount = fileItemMap.values().stream()
                .filter(this::shouldShowItem)
                .filter(item -> item.isSelected() && item.getChangeType().equals("删除"))
                .count();

        long totalSize = fileItemMap.values().stream()
                .filter(this::shouldShowItem)
                .filter(FilePreviewItem::isSelected)
                .mapToLong(FilePreviewItem::getFileSize)
                .sum();

        String sizeText = formatFileSize(totalSize);
        statisticsLabel.setText(String.format("统计: 共 %d 个文件 | 新增:%d | 修改:%d | 删除:%d | 总大小:%s",
                totalCount, addedCount, modifiedCount, deletedCount, sizeText));
    }
    
    /**
     * 设置差异对比面板关联
     */
    public void setDiffViewerPanel(DiffViewerPanel diffViewerPanel) {
        this.diffViewerPanel = diffViewerPanel;
    }
    
    /**
     * 设置导出历史面板关联
     */
    public void setExportHistoryPanel(ExportHistoryPanel exportHistoryPanel) {
        this.exportHistoryPanel = exportHistoryPanel;
        System.out.println("[FilePreviewPanel] 设置 ExportHistoryPanel: " + (exportHistoryPanel != null ? "成功" : "null"));
    }
    
    /**
     * 设置目标路径
     */
    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
        // 同步到差异对比面板
        if (diffViewerPanel != null) {
            diffViewerPanel.setTargetPath(targetPath);
        }
    }
    
    /**
     * 同步差异数据到差异对比面板
     */
    private void syncToDiffViewer() {
        if (diffViewerPanel == null) {
            return;
        }
        
        diffViewerPanel.setRepoPath(repoPath);
        diffViewerPanel.setTargetPath(targetPath);
        
        List<DiffEntryItem> diffItems = new ArrayList<>();
        for (FilePreviewItem item : fileItemMap.values()) {
            DiffEntryItem diffItem = new DiffEntryItem(item.getFilePath(), convertToDiffChangeType(item.getChangeType()));
            diffItem.setAddedLines(item.getAddedLines());
            diffItem.setDeletedLines(item.getDeletedLines());
            diffItem.setNewContent("文件: " + item.getFilePath() + "\n变更类型: " + item.getChangeType() + "\n大小: " + formatFileSize(item.getFileSize()));
            diffItems.add(diffItem);
        }
        
        diffViewerPanel.setDiffItems(diffItems);
    }
    
    /**
     * 转换变更类型为差异对比类型
     */
    private String convertToDiffChangeType(String changeType) {
        switch (changeType) {
            case "新增": return "ADD";
            case "删除": return "DELETE";
            case "修改": return "MODIFY";
            default: return "MODIFY";
        }
    }

    public JPanel getPanel() {
        return panel;
    }

    // 文件树单元格渲染器
    private static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof FilePreviewItem) {
                    FilePreviewItem item = (FilePreviewItem) userObject;
                    
                    // 显示选中状态
                    String checkIcon = item.isSelected() ? "☑ " : "☐ ";
                    String changeIcon = getFileIcon(item.getChangeType());
                    String fileName = new java.io.File(item.getFilePath()).getName();
                    String size = formatFileSize(item.getFileSize());
                    String lines = String.format("↑%d ↓%d", item.getAddedLines(), item.getDeletedLines());
                    
                    setText(checkIcon + changeIcon + " " + fileName + " [" + size + "] " + lines);
                    
                    // 未选中的文件显示为灰色
                    if (!item.isSelected()) {
                        setForeground(new Color(150, 150, 150));
                    }
                }
            }

            return this;
        }

        private String getFileIcon(String changeType) {
            switch (changeType) {
                case "新增": return "[+]";
                case "删除": return "[-]";
                case "修改": return "[~]";
                default: return "[ ]";
            }
        }

        private String formatFileSize(long size) {
            if (size < 1024) return size + "B";
            if (size < 1024 * 1024) return String.format("%.1fKB", size / 1024.0);
            return String.format("%.1fMB", size / (1024.0 * 1024));
        }
    }
}

// 文件预览项数据模型
class FilePreviewItem {
    private String filePath;
    private String changeType;
    private long fileSize;
    private int addedLines;
    private int deletedLines;
    private boolean selected;

    public FilePreviewItem(String filePath, String changeType, long fileSize, int addedLines, int deletedLines) {
        this.filePath = filePath;
        this.changeType = changeType;
        this.fileSize = fileSize;
        this.addedLines = addedLines;
        this.deletedLines = deletedLines;
        this.selected = true; // 默认选中
    }

    public String getFilePath() { return filePath; }
    public String getChangeType() { return changeType; }
    public long getFileSize() { return fileSize; }
    public int getAddedLines() { return addedLines; }
    public int getDeletedLines() { return deletedLines; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}