package com.supporter.prj.view;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * @author xueguangchen
 * @version 1.0.0
 * @ClassName com.supporter.prj.ExpInfoInputFrame.java
 * @Description 输入窗口
 * @createTime 2024年11月15日 11:04:00
 */
public class ExpInfoInputFrame {
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

    public ExpInfoInputFrame() {
        selectRepoPathBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setAcceptAllFileFilterUsed(false);

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

                // 弹出列表对话框
                GitCommitHistoryListDialog dialog = new GitCommitHistoryListDialog(expInfoJpanel.getParent(), "选择提交记录", true, repoPathTemp);
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
