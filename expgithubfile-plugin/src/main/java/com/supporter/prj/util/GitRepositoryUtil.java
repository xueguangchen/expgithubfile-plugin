package com.supporter.prj.util;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;

public class GitRepositoryUtil {

    /**
     * 更可靠地获取当前项目的Git仓库路径
     * @return 仓库根路径
     */
    public static String getCurrentRepositoryPath() {
        try {
            // 方法1: 通过类路径获取应用位置，然后查找Git仓库
            String classPath = GitRepositoryUtil.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File classFile = new File(classPath);

            // 从类文件位置开始向上查找
            return getLocalRepositoryPath(classFile.getAbsolutePath());
        } catch (Exception e) {
            // 回退到原来的方法
            return getLocalRepositoryPath(System.getProperty("user.dir"));
        }
    }

    /**
     * 增强版的本地Git仓库路径查找
     * @param workingDir 工作目录
     * @return 仓库根路径
     */
    public static String getLocalRepositoryPath(String workingDir) {
        try {
            File startDir = new File(workingDir);

            // 确保起始目录存在
            if (!startDir.exists()) {
                return null;
            }

            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder
                    .findGitDir(startDir)  // 从指定目录开始查找
                    .setMustExist(true)    // 必须找到存在的仓库
                    .build();

            File workTree = repository.getWorkTree();
            return workTree != null ? workTree.getAbsolutePath() : null;
        } catch (Exception e) {
            System.err.println("查找Git仓库失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 手动查找Git仓库根目录（备选方案）
     * @param startPath 起始路径
     * @return Git仓库根目录路径
     */
    public static String findGitRepositoryManually(String startPath) {
        File currentDir = new File(startPath);

        while (currentDir != null && currentDir.exists()) {
            File gitDir = new File(currentDir, ".git");
            if (gitDir.exists() && gitDir.isDirectory()) {
                return currentDir.getAbsolutePath();
            }
            currentDir = currentDir.getParentFile();
        }

        return null;
    }

    /**
     * 手动查找Git仓库根目录-全面查找Git仓库
     * @param startPath 起始路径
     * @return Git仓库根目录路径
     */
    public static String findGitRepositoryComprehensively(String startPath) {
        File startDir = new File(startPath);

        if (!startDir.exists()) {
            return startPath;
        }

        // 向上查找
        File currentDir = startDir;
        while (currentDir != null && currentDir.exists()) {
            File gitDir = new File(currentDir, ".git");
            if (gitDir.exists() && gitDir.isDirectory()) {
                return currentDir.getAbsolutePath();
            }
            currentDir = currentDir.getParentFile();
        }

        // 向下查找子目录
        String foundPath = findGitRepositoryInSubdirectories(startDir);
        if (foundPath != null) {
            return foundPath;
        }

        // 如果都找不到，返回最初的路径
        return startPath;
    }

    private static String findGitRepositoryInSubdirectories(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }

        // 先检查当前目录下的直接子目录
        for (File file : files) {
            if (file.isDirectory()) {
                File gitDir = new File(file, ".git");
                if (gitDir.exists() && gitDir.isDirectory()) {
                    return file.getAbsolutePath();
                }
            }
        }

        // 递归检查子目录的子目录
        for (File file : files) {
            if (file.isDirectory()) {
                String result = findGitRepositoryInSubdirectories(file);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

}
