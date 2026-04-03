package com.supporter.prj.view;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.supporter.prj.entity.GitCommitHistory;
import com.supporter.prj.util.ExpGitHubUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 提交选择对话框
 * 用于选择 Git 提交记录
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class SelectCommitDialog extends DialogWrapper {
    private Project project;
    private JBScrollPane scrollPane;
    private JTable commitTable;
    private DefaultTableModel tableModel;
    private JTextField authorField;
    private JTextField keywordField;
    private JDatePicker startDatePicker;
    private JDatePicker endDatePicker;
    private JButton searchButton;
    private JButton quickSelectButton;
    private JButton quickSelectRecentButton;
    private JButton quickSelectTodayButton;
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 15;
    private java.util.List<GitCommitHistory> allItems;
    private Set<String> selectedCommitIds = new HashSet<>();
    private Map<Integer, Set<String>> selectedCommitIdMaps = new LinkedHashMap<>();
    private String repoPath;

    public SelectCommitDialog(Project project) {
        super(project);
        this.project = project;
        init();
        setTitle("选择提交记录");
        setOKButtonText("确定");
        setCancelButtonText("取消");
    }

    public SelectCommitDialog(Project project, String repoPath, Set<String> initialSelectedCommitIds) {
        super(project);
        this.project = project;
        this.repoPath = repoPath;
        this.selectedCommitIds = initialSelectedCommitIds != null ? new HashSet<>(initialSelectedCommitIds) : new HashSet<>();
        this.selectedCommitIdMaps = new LinkedHashMap<>();
        System.out.println("[SelectCommitDialog] 构造函数, repoPath: " + repoPath);
        init();
        setTitle("选择提交记录");
        setOKButtonText("确定");
        setCancelButtonText("取消");
    }

    @Override
    protected JComponent createCenterPanel() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建顶部过滤区域
        JPanel filterPanel = createFilterPanel();
        mainPanel.add(filterPanel, BorderLayout.NORTH);

        // 创建表格区域
        JComponent tablePanel = createTablePanel();
        mainPanel.add(tablePanel, BorderLayout.CENTER);

        // 创建底部操作区域
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 第一行：提交人和关键字
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JBLabel("提交人:"), gbc);
        
        gbc.gridx = 1;
        authorField = new JTextField(15);
        panel.add(authorField, gbc);
        
        gbc.gridx = 2;
        panel.add(new JBLabel("关键字:"), gbc);
        
        gbc.gridx = 3;
        keywordField = new JTextField(15);
        panel.add(keywordField, gbc);
        
        gbc.gridx = 4;
        searchButton = new JButton("搜索");
        searchButton.addActionListener(this::performSearch);
        panel.add(searchButton, gbc);
        
        // 第二行：日期筛选
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JBLabel("开始日期:"), gbc);
        
        gbc.gridx = 1;
        startDatePicker = new JDatePicker();
        startDatePicker.setPreferredSize(new Dimension(140, 30));
        panel.add(startDatePicker, gbc);
        
        gbc.gridx = 2;
        panel.add(new JBLabel("结束日期:"), gbc);
        
        gbc.gridx = 3;
        endDatePicker = new JDatePicker();
        endDatePicker.setPreferredSize(new Dimension(140, 30));
        panel.add(endDatePicker, gbc);

        return panel;
    }

    private JComponent createTablePanel() {
        // 创建表格模型
        tableModel = new CommitTableModel(
            new Object[][]{},
            new Object[]{"选择", "提交ID", "提交人", "提交日期", "提交信息"}
        );

        // 创建表格
        commitTable = new JBTable(tableModel);
        commitTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        commitTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        commitTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        commitTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        commitTable.getColumnModel().getColumn(4).setPreferredWidth(300);

        // 设置表格行高
        commitTable.setRowHeight(24);

        // 设置选择模式
        commitTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 创建滚动面板
        scrollPane = new JBScrollPane(commitTable);

        // 加载数据
        System.out.println("[SelectCommitDialog] createTablePanel, repoPath: " + repoPath);
        if (StringUtils.isNotBlank(repoPath)) {
            loadCommitData();
        } else {
            System.out.println("[SelectCommitDialog] repoPath 为空，无法加载数据");
        }

        return scrollPane;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // 底部操作按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        quickSelectButton = new JButton("全选");
        quickSelectButton.addActionListener(e -> selectAll());
        buttonPanel.add(quickSelectButton);

        quickSelectRecentButton = new JButton("清空选择");
        quickSelectRecentButton.addActionListener(e -> clearSelection());
        buttonPanel.add(quickSelectRecentButton);

        quickSelectTodayButton = new JButton("反选");
        quickSelectTodayButton.addActionListener(e -> invertSelection());
        buttonPanel.add(quickSelectTodayButton);

        // 分页和选择信息
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel pageInfo = new JLabel("第 " + currentPage + " 页，共 " + getTotalPages() + " 页");
        infoPanel.add(pageInfo);

        // 添加分页按钮
        JPanel pagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton prevButton = new JButton("上一页");
        prevButton.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                updateTableData();
                pageInfo.setText("第 " + currentPage + " 页，共 " + getTotalPages() + " 页");
            }
        });
        pagePanel.add(prevButton);

        JButton nextButton = new JButton("下一页");
        nextButton.addActionListener(e -> {
            if (currentPage < getTotalPages()) {
                currentPage++;
                updateTableData();
                pageInfo.setText("第 " + currentPage + " 页，共 " + getTotalPages() + " 页");
            }
        });
        pagePanel.add(nextButton);
        pagePanel.add(pageInfo);

        panel.add(buttonPanel, BorderLayout.WEST);
        panel.add(pagePanel, BorderLayout.EAST);

        // 添加表格模型监听器，监听checkbox列的变化
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0) {
                updateSelectedCommitIds();
            }
        });

        // 添加表格渲染器
        commitTable.getColumnModel().getColumn(0).setCellRenderer(new BooleanTableCellRenderer());
        commitTable.getColumnModel().getColumn(0).setCellEditor(new BooleanTableCellEditor());

        return panel;
    }

    private void loadCommitData() {
        try {
            System.out.println("[SelectCommitDialog] 开始加载提交记录, repoPath: " + repoPath);
            
            // 检查是否是有效的 Git 仓库
            File gitDir = new File(repoPath, ".git");
            if (!gitDir.exists()) {
                JOptionPane.showMessageDialog(getWindow(), 
                    "指定的路径不是 Git 仓库！\n请确保选择的目录包含 .git 文件夹。\n\n当前路径: " + repoPath, 
                    "无效的 Git 仓库", JOptionPane.WARNING_MESSAGE);
                // 延迟关闭对话框，确保对话框已完全初始化
                ApplicationManager.getApplication().invokeLater(() -> close(CANCEL_EXIT_CODE));
                return;
            }
            
            allItems = ExpGitHubUtil.fetchGitCommitHistory(repoPath, 0, null, null, null, null);
            System.out.println("[SelectCommitDialog] 加载完成, 记录数: " + (allItems != null ? allItems.size() : 0));
            updateTableData();
        } catch (Exception e) {
            System.err.println("[SelectCommitDialog] 加载失败: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(getWindow(), "加载提交记录失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTableData() {
        tableModel.setRowCount(0);

        System.out.println("[SelectCommitDialog] updateTableData, allItems: " + (allItems != null ? allItems.size() : "null"));
        if (allItems == null) {
            System.out.println("[SelectCommitDialog] allItems 为空，返回");
            return;
        }

        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allItems.size());
        System.out.println("[SelectCommitDialog] 显示行: " + start + " - " + end);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = start; i < end; i++) {
            GitCommitHistory commit = allItems.get(i);
            String formattedDate = dateFormat.format(commit.getDate());

            Object[] rowData = {
                selectedCommitIds.contains(commit.getCommitId()),
                commit.getCommitId(),
                commit.getAuthorName(),
                formattedDate,
                commit.getMessage()
            };
            tableModel.addRow(rowData);
        }

        restoreSelectedItems();
    }

    private void updateSelectedCommitIds() {
        // 遍历当前页所有行，读取checkbox状态来收集选中的提交ID
        Set<String> currentPageSelectedIds = new HashSet<>();
        for (int i = 0; i < commitTable.getRowCount(); i++) {
            Object checked = commitTable.getValueAt(i, 0);
            if (Boolean.TRUE.equals(checked)) {
                Object rowData = commitTable.getValueAt(i, 1);
                currentPageSelectedIds.add(rowData.toString());
            }
        }

        // 更新 selectedCommitIdMaps
        if (!currentPageSelectedIds.isEmpty()) {
            selectedCommitIdMaps.put(currentPage, currentPageSelectedIds);
        } else {
            selectedCommitIdMaps.remove(currentPage);
        }

        // 汇总所有页面的选中ID
        selectedCommitIds.clear();
        for (Set<String> ids : selectedCommitIdMaps.values()) {
            selectedCommitIds.addAll(ids);
        }
    }

    private void restoreSelectedItems() {
        commitTable.clearSelection();

        if (selectedCommitIdMaps.containsKey(currentPage)) {
            Set<String> selectedRows = selectedCommitIdMaps.get(currentPage);
            for (int i = 0; i < commitTable.getRowCount(); i++) {
                Object rowData = commitTable.getValueAt(i, 1);
                if (selectedRows.contains(rowData.toString())) {
                    commitTable.addRowSelectionInterval(i, i);
                    commitTable.setValueAt(true, i, 0);
                }
            }
        }
    }

    private void performSearch(ActionEvent e) {
        String author = authorField.getText().trim();
        String keyword = keywordField.getText().trim();
        Date startDate = startDatePicker.getModel().getValue();
        Date endDate = endDatePicker.getModel().getValue();

        try {
            allItems = ExpGitHubUtil.fetchGitCommitHistory(repoPath, 0, author, keyword, startDate, endDate);
            currentPage = 1;
            updateTableData();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(getWindow(), "搜索失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectAll() {
        // 选中当前页所有记录
        for (int i = 0; i < commitTable.getRowCount(); i++) {
            commitTable.setValueAt(true, i, 0);
        }
    }

    private void clearSelection() {
        // 清除当前页所有选择
        for (int i = 0; i < commitTable.getRowCount(); i++) {
            commitTable.setValueAt(false, i, 0);
        }
        // 清除所有页面的选中记录
        selectedCommitIdMaps.clear();
        selectedCommitIds.clear();
    }

    private void invertSelection() {
        // 反选当前页记录
        for (int i = 0; i < commitTable.getRowCount(); i++) {
            boolean current = (Boolean) commitTable.getValueAt(i, 0);
            commitTable.setValueAt(!current, i, 0);
        }
    }

    private int getTotalPages() {
        return allItems != null ? (int) Math.ceil((double) allItems.size() / ITEMS_PER_PAGE) : 1;
    }

    public String getSelectedCommitIds() {
        StringBuilder sb = new StringBuilder();
        if (selectedCommitIds != null && !selectedCommitIds.isEmpty()) {
            for (String id : selectedCommitIds) {
                if (sb.length() > 0) {
                    sb.append(",\n");
                }
                sb.append(id);
            }
        }
        return sb.toString();
    }

    // 自定义布尔型表格单元格渲染器
    private static class BooleanTableCellRenderer extends JCheckBox implements TableCellRenderer {
        public BooleanTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setSelected(Boolean.TRUE.equals(value));
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            return this;
        }
    }

    // 自定义布尔型表格单元格编辑器
    private static class BooleanTableCellEditor extends DefaultCellEditor {
        public BooleanTableCellEditor() {
            super(new JCheckBox());
            ((JCheckBox) editorComponent).setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Object getCellEditorValue() {
            return ((JCheckBox) editorComponent).isSelected();
        }
    }

    // 扩展 DefaultTableModel 以支持布尔列
    private static class CommitTableModel extends DefaultTableModel {
        public CommitTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Boolean.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }
    }
}