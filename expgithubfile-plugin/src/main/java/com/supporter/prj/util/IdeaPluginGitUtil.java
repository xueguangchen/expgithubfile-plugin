package com.supporter.prj.util;

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

        // 优先使用 getBasePath() 获取项目根目录
        String basePath = project.getBasePath();
        if (basePath != null) {
            return basePath;
        }

        // 备选方案：使用 getProjectFile() 的父目录
        VirtualFile projectFile = project.getProjectFile();
        if (projectFile != null) {
            VirtualFile parent = projectFile.getParent();
            if (parent != null) {
                return parent.getPath();
            }
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
        System.out.println("[IdeaPluginGitUtil] 项目根路径: " + projectRoot);
        
        if (projectRoot != null) {
            // 使用全面查找方法
            String repoPath = GitRepositoryUtil.findGitRepositoryComprehensively(projectRoot);
            System.out.println("[IdeaPluginGitUtil] 找到仓库路径: " + repoPath);
            return repoPath;
        }
        return null;
    }
}
