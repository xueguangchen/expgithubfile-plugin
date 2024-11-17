package com.supporter.prj.view;

import com.supporter.prj.entity.GitCommitHistory;
import com.supporter.prj.util.ExpGitHubUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private int currentPage = 1;
    private int itemsPerPage = 15;
    private List<GitCommitHistory> allItems;

    public GitCommitHistoryListDialog(Component owner, String title, boolean modal, String repoPath) {
        //super(owner, title, modal);
        setTitle(title);
        setModal(modal);
        setSize(600, 410);
        setLocationRelativeTo(owner);

        // 获取提交记录
        allItems = ExpGitHubUtil.fetchGitCommitHistory(repoPath, 0);

        // 创建表格模型
        tableModel = new DefaultTableModel();
        tableModel.addColumn("ID");
        tableModel.addColumn("提交人");
        tableModel.addColumn("提交日期");
        tableModel.addColumn("消息");
        updateTableData();

        // 创建表格
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 创建按钮
        okButton = new JButton("确定");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        cancelButton = new JButton("取消");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
                }
            }
        });

        // 创建分页标签
        JLabel pageLabel = new JLabel("第 " + currentPage + " 页，共 " + getTotalPages() + " 页");

        // 布局
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(pageLabel);

        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private List<GitCommitHistory> generateData() {
        List<GitCommitHistory> logEntries = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            logEntries.add(new GitCommitHistory(i+"", "User" + i, "User" + i, new Date(), "Log entry " + i));
        }
        return logEntries;
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
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) allItems.size() / itemsPerPage);
    }

    public String getSelectedItems() {
        StringBuilder sb = new StringBuilder();
        int[] selectedRows = table.getSelectedRows();
        for (int row : selectedRows) {
            Object rowData = table.getValueAt(row, 0);
            if(!sb.isEmpty()){
                sb.append(",");
                sb.append("\n");
            }
            sb.append(rowData.toString());
        }
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


