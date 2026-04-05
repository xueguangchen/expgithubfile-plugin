package com.supporter.prj.util;

import java.io.File;

public class GitRepositoryUtil {

    /**
     * 全面查找Git仓库
     * 查找顺序：当前层级 -> 向上查找 -> 向下查找（一层子目录）
     * @param workspacePath 工作空间路径
     * @return Git仓库根目录路径
     */
    public static String findGitRepositoryComprehensively(String workspacePath) {
        System.out.println("[GitRepositoryUtil] ========== 开始查找Git仓库 ==========");
        System.out.println("[GitRepositoryUtil] 工作空间路径: " + workspacePath);

        if (workspacePath == null || workspacePath.isEmpty()) {
            System.out.println("[GitRepositoryUtil] 路径为空，返回null");
            return null;
        }

        File workspaceDir = new File(workspacePath);
        System.out.println("[GitRepositoryUtil] 工作空间目录存在: " + workspaceDir.exists());
        System.out.println("[GitRepositoryUtil] 工作空间绝对路径: " + workspaceDir.getAbsolutePath());

        if (!workspaceDir.exists()) {
            System.out.println("[GitRepositoryUtil] 工作空间目录不存在，返回原路径");
            return workspacePath;
        }

        // 1. 检查当前层级
        System.out.println("[GitRepositoryUtil] --- 检查当前层级 ---");
        if (isGitRepo(workspaceDir)) {
            System.out.println("[GitRepositoryUtil] ✓ 当前层级是Git仓库: " + workspacePath);
            return workspacePath;
        }

        // 2. 向上查找（从父目录开始）
        System.out.println("[GitRepositoryUtil] --- 向上查找 ---");
        File parentDir = workspaceDir.getParentFile();
        int level = 0;
        while (parentDir != null && parentDir.exists()) {
            level++;
            System.out.println("[GitRepositoryUtil] 第" + level + "层父目录: " + parentDir.getAbsolutePath());
            if (isGitRepo(parentDir)) {
                String result = parentDir.getAbsolutePath();
                System.out.println("[GitRepositoryUtil] ✓ 向上找到Git仓库: " + result);
                return result;
            }
            parentDir = parentDir.getParentFile();
        }

        // 3. 向下查找（一层直接子目录）
        System.out.println("[GitRepositoryUtil] --- 向下查找 ---");
        File[] subDirs = workspaceDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            System.out.println("[GitRepositoryUtil] 共有 " + subDirs.length + " 个子目录");
            for (File subDir : subDirs) {
                System.out.println("[GitRepositoryUtil] 检查子目录: " + subDir.getName());
                if (isGitRepo(subDir)) {
                    String result = subDir.getAbsolutePath();
                    System.out.println("[GitRepositoryUtil] ✓ 向下找到Git仓库: " + result);
                    return result;
                }
            }
        }

        System.out.println("[GitRepositoryUtil] ✗ 未找到Git仓库，返回工作空间路径");
        return workspacePath;
    }

    /**
     * 检查目录是否是Git仓库
     */
    private static boolean isGitRepo(File dir) {
        File gitDir = new File(dir, ".git");
        boolean exists = gitDir.exists();
        boolean isDir = gitDir.isDirectory();
        boolean result = exists && isDir;
        System.out.println("[GitRepositoryUtil]   检查 .git: " + gitDir.getAbsolutePath() + " | 存在=" + exists + " | 是目录=" + isDir);
        return result;
    }

}
