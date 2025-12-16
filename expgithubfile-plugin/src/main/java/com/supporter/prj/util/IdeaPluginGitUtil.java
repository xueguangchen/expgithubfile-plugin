package com.supporter.prj.util;// 在IDEA插件的Action或Component中使用

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class IdeaPluginGitUtil {

    /**
     * 获取IDEA当前项目的根路径
     * @param project IDEA项目对象
     * @return 项目根路径
     */
    public static String getIdeaProjectRootPath(Project project) {
        if (project == null) {
            return null;
        }

        // 获取项目根目录
        VirtualFile baseDir = project.getProjectFile();
        if (baseDir != null) {
            return baseDir.getPath();
        }
        return null;
    }

    /**
     * 在IDEA插件中获取Git仓库路径
     * @param project IDEA项目对象
     * @return Git仓库路径
     */
    public static String getIdeaGitRepositoryPath(Project project) {
        String projectRoot = getIdeaProjectRootPath(project);
        if (projectRoot != null) {
            // 使用原有的手动查找方法
            return GitRepositoryUtil.findGitRepositoryManually(projectRoot);
        }
        return null;
    }
}
