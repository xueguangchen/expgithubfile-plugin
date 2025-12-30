package com.supporter.prj.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.supporter.prj.util.ExpGitHubUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;

/**
 * @author xueguangchen
 * @version 1.0.0
 * @ClassName com.supporter.prj.view.ExpInfoInputFormDialog.java
 * @Description 输入弹框
 * @createTime 2024年11月16日 14:32:00
 */
public class ExpInfoInputFormDialog extends DialogWrapper {

    private ExpInfoInputFrame expInfoInputFrame;
    private Project project;

    public ExpInfoInputFormDialog(Project project) {
        super(true);
        this.project = project;
        init(); //触发一下init方法，否则swing样式将无法展示在会话框
        setTitle("导出信息输入"); //设置会话框标题
        setSize(660, 300); // 设置窗口大小

    }

    @Override
    protected JComponent createNorthPanel() {
        return null; //返回位于会话框north位置的swing样式
    }

    // 特别说明：不需要展示SouthPanel要重写返回null，否则IDEA将展示默认的"Cancel"和"OK"按钮
    @Override
    protected JComponent createSouthPanel() {
        return null;
    }

    @Override
    protected JComponent createCenterPanel() {
        expInfoInputFrame = new ExpInfoInputFrame(this.project);
        JPanel mainPanel = expInfoInputFrame.getMainPanel();
        expInfoInputFrame.getProgressBar().setVisible(false);
        JComboBox<String> typeComboBox = expInfoInputFrame.getTypeComboBox();
        typeComboBox.addItem("已提交");
        typeComboBox.addItem("未提交");
        typeComboBox.setSelectedItem("已提交");
        JButton exportBtn = expInfoInputFrame.getExportBtn();

        // 为按钮添加事件监听器
        exportBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JTextField repoPath = expInfoInputFrame.getRepoPath();
                JTextField targetFolderPath = expInfoInputFrame.getTargetFolderPath();
                JTextArea commitIds = expInfoInputFrame.getCommitIds();
                String repoPathValue = repoPath.getText().trim();
                String targetFolderPathValue = targetFolderPath.getText().trim();
                String commitIdsValue = commitIds.getText().trim();
                if (repoPathValue.isEmpty()) {
                    // 给出提示
                    JOptionPane.showMessageDialog(mainPanel, "本地仓库路径不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (targetFolderPathValue.isEmpty()) {
                    // 给出提示
                    JOptionPane.showMessageDialog(mainPanel, "目标文件路径不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String selectedValue = (String) typeComboBox.getSelectedItem();
                String[] commitIdsArr = {};
                if(selectedValue == "已提交"){
                    if (commitIdsValue.isEmpty()) {
                        // 给出提示
                        JOptionPane.showMessageDialog(mainPanel, "提交的SHA-1哈希值不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if(commitIdsValue.contains(",")){
                        commitIdsArr = commitIdsValue.split(",");
                    }else if(commitIdsValue.contains("，")){
                        commitIdsArr = commitIdsValue.split(",");
                    }else{
                        commitIdsArr = new String[]{commitIdsValue};
                    }
                }
                final String[] finalCommitIdsArr = commitIdsArr;

                // 禁用按钮
                exportBtn.setEnabled(false);

                // 创建进度条
                JProgressBar progressBar = expInfoInputFrame.getProgressBar();
                progressBar.setValue(0);
                progressBar.setStringPainted(true);
                progressBar.setVisible(true);  // 确保进度条可见
                // 在后台线程中执行任务，并更新进度条
                SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        try {
                            // 执行实际的导出操作
                            if ("已提交".equals(selectedValue)) {
                                ExpGitHubUtil.expCommittedFile(repoPathValue, targetFolderPathValue, finalCommitIdsArr);
                            } else {
                                ExpGitHubUtil.expUncommittedFiles(repoPathValue, targetFolderPathValue);
                            }

                            // 等待操作完成并更新进度
                            int progress;
                            do {
                                progress = ExpGitHubUtil.getProgress();
                                if (progress < 100) {
                                    publish(progress);
                                    Thread.sleep(200); // 避免过度占用CPU
                                }
                            } while (progress < 100 && !isCancelled()); // 支持取消操作

                            publish(100); // 确保进度条显示100%
                        } catch (Exception ex) {
                            // 重新抛出异常，让done()方法处理
                            throw ex;
                        }
                        return null;
                    }

                    @Override
                    protected void process(java.util.List<Integer> chunks) {
                        for (int value : chunks) {
                            progressBar.setValue(value);
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            get(); // 确保任何异常都被抛出
                            SwingUtilities.invokeLater(() -> {
                                // 重新启用按钮
                                if (exportBtn != null) {
                                    exportBtn.setEnabled(true);
                                }
                                // 显示任务完成的消息对话框
                                JOptionPane.showMessageDialog(mainPanel, "导出完成！");
                            });
                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                            SwingUtilities.invokeLater(() -> {
                                // 重新启用按钮
                                if (exportBtn != null) {
                                    exportBtn.setEnabled(true);
                                }
                                // 显示错误消息
                                JOptionPane.showMessageDialog(mainPanel,
                                        "导出失败：" + ex.getCause().getMessage(),
                                        "错误",
                                        JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    }
                };
                worker.execute();
                /*try {
                    if (selectedValue != "已提交") {
                        // 启动后台线程
                        ExpGitHubUtil.expUncommittedFiles(repoPathValue, targetFolderPathValue);
                    } else {
                        ExpGitHubUtil.expCommittedFile(repoPathValue, targetFolderPathValue, commitIdsArr);
                    }
                } catch (Exception ex) {
                    // 给出提示
                    JOptionPane.showMessageDialog(mainPanel, "程序异常，请检查输入的信息是否正确！", "错误", JOptionPane.ERROR_MESSAGE);
                    // 重新启用按钮
                    exportBtn.setEnabled(true);
                }*/

            }
        });

        // 这里我们只使用MyFrame的mainPanel，而不是整个JFrame
        return mainPanel;
    }

    @Override
    public void doOKAction() {
        // 处理OK按钮的操作
        super.doOKAction();
    }

    @Override
    protected void init() {
        super.init();
    }
}