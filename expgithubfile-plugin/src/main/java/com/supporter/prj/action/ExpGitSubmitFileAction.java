package com.supporter.prj.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.supporter.prj.view.ExpInfoInputFormDialog;

import javax.swing.*;

/**
 * ExpGitSubmitFile 插件入口 Action
 * 点击 Tools 菜单中的 "ExpGitSubmitFile" 项时触发
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class ExpGitSubmitFileAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 从事件中直接获取 Project 对象
        Project project = e.getProject();

        // 使用 invokeLater 确保在事件调度线程中运行
        SwingUtilities.invokeLater(() -> {
            try {
                // 检查项目是否有效
                if (project == null) {
                    Messages.showErrorDialog(
                            "请先打开一个项目再使用此插件！",
                            "项目未打开"
                    );
                    return;
                }

                // 创建并显示新的主界面对话框
                ExpInfoInputFormDialog formTestDialog = new ExpInfoInputFormDialog(project);
                formTestDialog.setResizable(true); // 允许用户调整窗口大小
                formTestDialog.show();

            } catch (Exception ex) {
                // 显示错误信息
                Messages.showErrorDialog(
                        "插件启动失败: " + ex.getMessage(),
                        "错误"
                );

                // 打印详细的错误堆栈（在控制台）
                ex.printStackTrace();
            }
        });
    }
}
