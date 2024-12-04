package com.supporter.prj.view;

import com.supporter.prj.entity.GitCommitHistory;
import com.supporter.prj.util.ExpGitHubUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * @author xueguangchen
 * @version 1.0.0
 * @ClassName com.supporter.prj.view.GitCommitHistoryListDialog.java
 * @Description 提交历史弹框
 * @createTime 2024年11月16日 14:32:00
 */
public class GitCommitHistoryListDialog extends JDialog {
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton okButton;
    private JButton cancelButton;
    private JButton prevButton;
    private JButton nextButton;
    // 在类成员变量部分添加以下字段
    private JTextField authorField;
    private JTextField keywordField;
    private JDatePicker startDatePicker;
    private JDatePicker endDatePicker;
    private JLabel pageLabel;
    private int currentPage = 1;
    private int itemsPerPage = 15;
    private List<GitCommitHistory> allItems;
    private Set<String> selectedCommitIds; // 用于存储选中的提交记录 ID
    private Map<Integer, Set<String>> selectedCommitIdMaps;

    public GitCommitHistoryListDialog(Component owner, String title, boolean modal, String repoPath, Set<String> initialSelectedCommitIds) {
        //super(owner, title, modal);
        setTitle(title);
        setModal(modal);
        setSize(700, 410);
        setLocationRelativeTo(owner);

        // 初始化选中的提交记录 ID 集合
        this.selectedCommitIds = initialSelectedCommitIds != null ? new HashSet<>(initialSelectedCommitIds) : new HashSet<>();
        selectedCommitIdMaps = new LinkedHashMap<>();
        // 获取提交记录
        allItems = ExpGitHubUtil.fetchGitCommitHistory(repoPath, 0, null, null, null, null);

        // 添加作者输入框
        authorField = new JTextField();
        authorField.setPreferredSize(new Dimension(180, 30));
        // 添加关键字输入框
        keywordField = new JTextField();
        keywordField.setPreferredSize(new Dimension(180, 30));
        startDatePicker = new JDatePicker();
        startDatePicker.setPreferredSize(new Dimension(180, 30));
        endDatePicker = new JDatePicker();
        endDatePicker.setPreferredSize(new Dimension(180, 30));

        // 添加查询按钮
        JButton searchButton = new JButton("查询");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch(repoPath);
            }
        });

        // 创建表格模型
        tableModel = new DefaultTableModel();
        tableModel.addColumn("ID");
        tableModel.addColumn("提交人");
        tableModel.addColumn("提交日期");
        tableModel.addColumn("消息");

        // 创建表格
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // 在构造方法中添加以下代码
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) { // 确保只在选择完成时处理
                    updateSelectedCommitIds();
                }
            }
        });

        updateTableData();

        // 创建按钮
        okButton = new JButton("确定");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //updateSelectedCommitIds();
                dispose();
            }
        });

        cancelButton = new JButton("取消");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedCommitIds.clear();
                dispose();
            }
        });

        prevButton = new JButton("上一页");
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage > 1) {
                    currentPage--;
                    updateTableData();
                    updatePageLabel();
                }
            }
        });

        nextButton = new JButton("下一页");
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPage < getTotalPages()) {
                    currentPage++;
                    updateTableData();
                    updatePageLabel();
                }
            }
        });

        // 更新按钮面板布局
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2, 3));
        // 第一行
        JPanel firstRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        firstRowPanel.add(new JLabel("提交人:"));
        firstRowPanel.add(authorField);
        firstRowPanel.add(new JLabel("关键字:"));
        firstRowPanel.add(keywordField);
        inputPanel.add(firstRowPanel);

        // 第二行
        JPanel secondRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        secondRowPanel.add(new JLabel("开始日期:"));
        secondRowPanel.add(startDatePicker);
        secondRowPanel.add(new JLabel("结束日期:"));
        secondRowPanel.add(endDatePicker);
        secondRowPanel.add(searchButton);
        inputPanel.add(secondRowPanel);

        // 创建分页标签
        pageLabel = new JLabel("第 " + currentPage + " 页，共 " + getTotalPages() + " 页");

        // 布局
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(pageLabel);

        setLayout(new BorderLayout());
        add(inputPanel, BorderLayout.NORTH); // 将输入面板放在顶部
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // 添加 performSearch 方法以执行查询操作
    private void performSearch(String repoPath) {
        String author = authorField.getText().trim();
        String keyword = keywordField.getText().trim();
        Date startDate = startDatePicker.getModel().getValue();
        Date endDate = endDatePicker.getModel().getValue();

        // 获取过滤后的提交历史记录
        allItems = ExpGitHubUtil.fetchGitCommitHistory(repoPath, 0, author, keyword, startDate, endDate);

        // 更新当前页码为第一页并刷新表格数据
        currentPage = 1;
        updateTableData();
        updatePageLabel();
    }

    private void updateTableData() {
        tableModel.setRowCount(0);
        int start = (currentPage - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allItems.size());
        // 格式化日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = start; i < end; i++) {
            GitCommitHistory gitCommitHistory = allItems.get(i);
            String formattedDate = dateFormat.format(gitCommitHistory.getDate());
            Object[] rowData = {
                  gitCommitHistory.getCommitId(),
                    gitCommitHistory.getAuthorName() + " <" + gitCommitHistory.getEmailAddress() + ">",
                    formattedDate,
                    gitCommitHistory.getMessage()
            };
            tableModel.addRow(rowData);
        }
        // 恢复选中的提交记录
        restoreSelectedItems();
    }

    // 添加 updateSelectedCommitIds 方法
    /*private void updateSelectedCommitIds() {
        //selectedCommitIds.clear();
        int[] selectedRows = table.getSelectedRows();
        for (int row : selectedRows) {
            Object rowData = table.getValueAt(row, 0);
            selectedCommitIds.add(rowData.toString());
        }
    }*/

    private void updateSelectedCommitIds() {
        // 获取当前页面的所有行索引
        int start = (currentPage - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allItems.size());

        // 创建一个临时集合来存储当前页面的选中ID
        Set<String> currentPageSelectedIds = new HashSet<>();

        // 获取当前页面所有选中的行索引
        int[] selectedRows = table.getSelectedRows();
        for (int row : selectedRows) {
            Object rowData = table.getValueAt(row, 0);
            currentPageSelectedIds.add(rowData.toString());
        }

        /*for (int row : selectedRows) {
            int modelIndex = table.convertRowIndexToModel(row);
            if (modelIndex >= start && modelIndex < end) {
                Object rowData = table.getValueAt(row, 0);
                currentPageSelectedIds.add(rowData.toString());
            }
        }*/

        // 更新 selectedCommitIdMaps
        if(currentPageSelectedIds != null && currentPageSelectedIds.size() > 0){
            selectedCommitIdMaps.put(currentPage, currentPageSelectedIds);
        }

        // 更新 selectedCommitIds
        selectedCommitIds.clear();
        for (Set<String> ids : selectedCommitIdMaps.values()) {
            selectedCommitIds.addAll(ids);
        }

        // 移除不在当前页面选中的ID
        /*for (int i = start; i < end; i++) {
            GitCommitHistory gitCommitHistory = allItems.get(i);
            String commitId = gitCommitHistory.getCommitId();
            if (!currentPageSelectedIds.contains(commitId)) {
                selectedCommitIds.remove(commitId);
            }
        }*/

        // 添加当前页面选中的ID
        //selectedCommitIds.addAll(currentPageSelectedIds);
    }


    private void restoreSelectedItems() {
        table.clearSelection();
        if(selectedCommitIdMaps.containsKey(currentPage)){
            Set<String> selectedRows = selectedCommitIdMaps.get(currentPage);
            for (int i = 0; i < table.getRowCount(); i++) {
                Object rowData = table.getValueAt(i, 0);
                if (selectedRows.contains(rowData.toString())) {
                    table.addRowSelectionInterval(i, i);
                }
            }
        }
    }

    private void updatePageLabel() {
        pageLabel.setText("第 " + currentPage + " 页，共 " + getTotalPages() + " 页");
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) allItems.size() / itemsPerPage);
    }

    public String getSelectedItems() {
        StringBuilder sb = new StringBuilder();
        if(selectedCommitIds != null && selectedCommitIds.size() > 0){
            for(String item : selectedCommitIds){
                System.out.println("item:" + item);
                if(!sb.isEmpty()){
                    sb.append(",");
                    sb.append("\n");
                }
                sb.append(item);
            }
        }

        /*int[] selectedRows = table.getSelectedRows();
        for (int row : selectedRows) {
            Object rowData = table.getValueAt(row, 0);
            if(!sb.isEmpty()){
                sb.append(",");
                sb.append("\n");
            }
            sb.append(rowData.toString());
        }*/
        return sb.toString();
    }

    private static List<String> getSelectedRows(JTable table) {
        List<String> selectedRows = new ArrayList<>();
        int[] selectedIndices = table.getSelectedRows();
        for (int index : selectedIndices) {
            StringBuilder rowString = new StringBuilder();
            for (int col = 0; col < table.getColumnCount(); col++) {
                rowString.append(table.getValueAt(index, col)).append(" ");
            }
            selectedRows.add(rowString.toString().trim());
        }
        return selectedRows;
    }


}


