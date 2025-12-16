package com.supporter.prj.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.supporter.prj.view.ExpInfoInputFormDialog;

/**
 * @author xueguangchen
 * @version 1.0.0
 * @ClassName com.supporter.prj.ExpGitFileAction.java
 * @Description 入口类
 * @createTime 2024年11月14日 18:13:00
 */
public class ExpGitSubmitFileAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 从事件中直接获取 Project 对象
        Project project = e.getProject();
        ExpInfoInputFormDialog formTestDialog = new ExpInfoInputFormDialog(project);
        formTestDialog.setResizable(true); //是否允许用户通过拖拽的方式扩大或缩小你的表单框，我这里定义为true，表示允许
        formTestDialog.show();

        /*SwingUtilities.invokeLater(() -> {
            ExpInfoInputFormDialog dialog = new ExpInfoInputFormDialog();
            dialog.setResizable(true); //是否允许用户通过拖拽的方式扩大或缩小你的表单框，我这里定义为true，表示允许
            dialog.show();
        });*/
    }
}
