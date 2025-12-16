package com.supporter.prj.view;

import com.intellij.openapi.project.Project;
import com.supporter.prj.util.GitRepositoryUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author xueguangchen
 * @version 1.0.0
 * @ClassName com.supporter.prj.ExpInfoInputFrame.java
 * @Description 输入窗口
 * @createTime 2024年11月15日 11:04:00
 */
public class ExpInfoInputFrame {
    private Project project;

    private JTextField repoPath;
    private JTextField targetFolderPath;
    private JPanel expInfoJpanel;
    private JButton exportBtn;
    private JTextArea commitIds;
    private JButton selectRepoPathBtn;
    private JButton selectTargetFolderPathBtn;
    private JProgressBar progressBar;
    private JButton selectCommitIdsBtn;
    private JComboBox<String> typeComboBox;
    private JLabel commitIdsLabel;
    private JLabel commitIdsRemarkLabel;

    public ExpInfoInputFrame(Project project) {
        this.project = project;
        // 使用 project 获取项目路径
        if (project != null) {
            String projectPath = project.getBasePath();
            // 在此基础上查找 Git 仓库
            String gitRepoPath = GitRepositoryUtil.findGitRepositoryManually(projectPath);

            if (gitRepoPath != null) {
                repoPath.setText(gitRepoPath);
            }
        }

        selectRepoPathBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setAcceptAllFileFilterUsed(false);

                // 获取当前输入框的值作为初始目录
                String currentPath = repoPath.getText().trim();
                if (!currentPath.isEmpty()) {
                    File currentDir = new File(currentPath);
                    if (currentDir.exists()) {
                        fileChooser.setCurrentDirectory(currentDir);
                    }
                }

                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    repoPath.setText(selectedFile.getAbsolutePath());
                }
            }
        });
        selectTargetFolderPathBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setAcceptAllFileFilterUsed(false);

                // 获取当前输入框的值作为初始目录
                String currentPath = targetFolderPath.getText().trim();
                if (!currentPath.isEmpty()) {
                    File currentDir = new File(currentPath);
                    if (currentDir.exists()) {
                        fileChooser.setCurrentDirectory(currentDir);
                    }
                }

                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    targetFolderPath.setText(selectedFile.getAbsolutePath());
                }
            }
        });
        selectCommitIdsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String repoPathValue = repoPath.getText().trim();
                String repoPathTemp = repoPathValue;
                if(repoPathTemp.endsWith("/.git")|| repoPathTemp.endsWith("\\.git") ){
                    repoPathTemp = repoPathTemp.replaceAll("[/\\\\]\\.git$", "");
                }
                if (repoPathValue.isEmpty()) {
                    // 给出提示
                    JOptionPane.showMessageDialog(expInfoJpanel, "本地仓库路径不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Set<String> initialSelectedCommitIds = null;
                String commitIdValues = commitIds.getText();
                if(StringUtils.isBlank(commitIdValues)){
                    initialSelectedCommitIds = new HashSet<>();
                    String[] commitIdValueArr = commitIdValues.split(",");
                    for (String commitIdValue : commitIdValueArr) {
                        initialSelectedCommitIds.add(commitIdValue);
                    }
                }
                // 弹出列表对话框
                GitCommitHistoryListDialog dialog = new GitCommitHistoryListDialog(expInfoJpanel.getParent(), "选择提交记录", true, repoPathTemp, initialSelectedCommitIds);
                dialog.setVisible(true);

                // 获取选择的项并显示在文本域中
                String selectedItems = dialog.getSelectedItems();
                commitIds.setText(selectedItems);
            }
        });
        typeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateVisibility();
            }
        });
    }

    private void updateVisibility() {
        String selectedValue = (String) typeComboBox.getSelectedItem();
        if ("已提交".equals(selectedValue)) {
            commitIdsRemarkLabel.setVisible(true);
            commitIdsLabel.setVisible(true);
            commitIds.setVisible(true);
            selectCommitIdsBtn.setVisible(true);
        } else {
            commitIdsRemarkLabel.setVisible(false);
            commitIdsLabel.setVisible(false);
            commitIds.setVisible(false);
            selectCommitIdsBtn.setVisible(false);
            commitIds.setText("");
        }
    }

    public JPanel getMainPanel() {
        return expInfoJpanel;
    }

    public JTextField getRepoPath() {
        return repoPath;
    }

    public JTextField getTargetFolderPath() {
        return targetFolderPath;
    }

    public JButton getExportBtn() {
        return exportBtn;
    }

    public JTextArea getCommitIds() {
        return commitIds;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JComboBox<String> getTypeComboBox() {
        return typeComboBox;
    }

    public JLabel getCommitIdsLabel() {
        return commitIdsLabel;
    }

    public void setCommitIdsLabel(JLabel commitIdsLabel) {
        this.commitIdsLabel = commitIdsLabel;
    }

    public JLabel getCommitIdsRemarkLabel() {
        return commitIdsRemarkLabel;
    }

    public void setCommitIdsRemarkLabel(JLabel commitIdsRemarkLabel) {
        this.commitIdsRemarkLabel = commitIdsRemarkLabel;
    }
}
