package com.supporter.prj.util;

import com.supporter.prj.entity.FileInfo;
import com.supporter.prj.entity.GitCommitHistory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xueguangchen
 * @version 1.0.0
 * @ClassName com.supporter.prj.util.ExpGitHubUtil.java
 * @Description 导出相关工具类
 * @createTime 2024年11月17日 10:24:00
 */
public class ExpGitHubUtil {

    //进度
    private static int progress = 0;
    // 删除文件路径记录文件
    private static String deletedFilesName = "deleted_files.txt";
    // 新增或修改文件路径记录文件
    private static String modifiedFilesName = "modified_files.txt";

    /**
     * 返回进度
     * @return
     */
    public static int getProgress() {
        return progress;
    }

    /**
     * 获取git提交历史记录
     * @param repoPath 本地仓库路径
     * @param maxCount
     * @return
     */
    public static List<GitCommitHistory> fetchGitCommitHistory(String repoPath, long maxCount) {
        List<GitCommitHistory> gitCommitHistoryList = new ArrayList<>();
        try {
            File repoDir = new File(repoPath); // 替换为你的 Git 仓库路径
            Git git = Git.open(repoDir);
            Iterable<RevCommit> commits = git.log().call();
            int count = 0;
            for (RevCommit commit : commits) {
                if(maxCount > 0){
                    if (count >= maxCount) break; // 限制获取的提交记录数量
                }
                gitCommitHistoryList.add(new GitCommitHistory(
                        commit.getName(),
                        commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getEmailAddress(),
                        commit.getAuthorIdent().getWhen(),
                        commit.getFullMessage()
                ));
                count++;
            }
            git.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gitCommitHistoryList;
    }

    /**
     *
     * @param repoPath Git本地仓库的路径
     * @param targetFolderPath 目标文件夹路径
     * @param commitIds 多个提交的SHA-1哈希值
     */
    public static void expGitSubmitFile1(String repoPath, String targetFolderPath, String[] commitIds ) {
        progress = 0;

        try {
            // 打开仓库
            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            String allRepoPath = repoPath;
            if(!repoPath.endsWith("/.git") && !repoPath.endsWith("\\.git") ){
                allRepoPath += "/.git";
            }
            Repository repository = repositoryBuilder.setGitDir(new File(allRepoPath))
                    .readEnvironment()
                    .findGitDir()
                    .build();
            progress = 5;
            // 定义记录文件路径
            String deletedFilesPath = Paths.get(targetFolderPath, deletedFilesName).toString();
            String modifiedFilesPath = Paths.get(targetFolderPath, modifiedFilesName).toString();

            // 创建文件写入器
            File deletedFiles = new File(deletedFilesPath);
            File modifiedFiles = new File(modifiedFilesPath);

            // 检查并创建文件及其父目录
            if (!deletedFiles.exists()) {
                Files.createDirectories(Paths.get(deletedFiles.getParent()));
                deletedFiles.createNewFile();
            }
            if (!modifiedFiles.exists()) {
                Files.createDirectories(Paths.get(modifiedFiles.getParent()));
                modifiedFiles.createNewFile();
            }

            FileWriter deletedFilesWriter = new FileWriter(deletedFiles, true);
            FileWriter modifiedFilesWriter = new FileWriter(modifiedFiles, true);
            progress = 10;
            int eachProgress = (int) (80 / commitIds.length);
            int num = 1;
            // 遍历每个提交ID
            for (String commitId : commitIds) {
                //commitId = commitId.trim();
                commitId = commitId.replaceAll("[\\s\\n\\r]", "");

                // 写入删除文件路径
                deletedFilesWriter.write("-------------" + commitId + "---------------------\n");
                // 写入删除文件路径
                modifiedFilesWriter.write("-------------" + commitId + "---------------------\n");
                // 获取指定提交
                ObjectId commitObjectId = repository.resolve(commitId);
                RevCommit commit = new Git(repository).log().add(commitObjectId).call().iterator().next();
                // 解析树对象
                try (RevWalk revWalk = new RevWalk(repository)) {
                    RevCommit parentCommit = commit.getParent(0);
                    ObjectId oldTreeId = parentCommit.getTree();
                    ObjectId newTreeId = commit.getTree();

                    CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
                    try (ObjectReader oldReader = repository.newObjectReader()) {
                        oldTreeParser.reset(oldReader, oldTreeId);
                    }

                    CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
                    try (ObjectReader newReader = repository.newObjectReader()) {
                        newTreeParser.reset(newReader, newTreeId);
                    }

                    // 获取差异
                    List<DiffEntry> diffs = new Git(repository).diff()
                            .setOldTree(oldTreeParser)
                            .setNewTree(newTreeParser)
                            .call();
                    progress += (int)num * eachProgress * 0.5;
                    // 输出文件路径
                    for (DiffEntry diff : diffs) {
                        DiffEntry.ChangeType changeType = diff.getChangeType();
                        String filePath;
                        if (changeType == DiffEntry.ChangeType.DELETE) {
                            filePath = diff.getOldPath();
                            // 构建绝对路径
                            Path sourcePath = Paths.get(repoPath, filePath);
                            // 写入删除文件路径
                            deletedFilesWriter.write(sourcePath.toAbsolutePath() + "\n");
                        } else {
                            filePath = diff.getNewPath();
                            // 构建绝对路径
                            Path sourcePath = Paths.get(repoPath, filePath);
                            Path targetPath = Paths.get(targetFolderPath, filePath);
                            // 写入新增或修改文件路径
                            modifiedFilesWriter.write(sourcePath.toAbsolutePath() + "\n");

                            // 创建目标目录
                            Files.createDirectories(targetPath.getParent());

                            // 复制文件，如果存在则覆盖
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            /*// 检查目标文件是否已存在
                            if (Files.exists(targetPath)) {
                                System.out.println("文件已存在，跳过复制: " + targetPath.toAbsolutePath());
                                continue;
                            }
                            // 复制文件
                            Files.copy(sourcePath, targetPath);*/
                        }
                    }
                    progress += (int)num * eachProgress;
                }
            }
            progress = 90;
            // 关闭文件写入器
            deletedFilesWriter.close();
            modifiedFilesWriter.close();

            // 关闭仓库
            repository.close();
            progress = 100;
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 导出已提交文件
     * @param repoPath Git本地仓库的路径
     * @param targetFolderPath 目标文件夹路径
     * @param commitIds 多个提交的SHA-1哈希值
     */
    public static void expCommittedFile(String repoPath, String targetFolderPath, String[] commitIds) {
        if(commitIds == null || commitIds.length == 0){
            return;
        }
        progress = 0;
        Map<String, List<FileInfo>> filePathsMap = new LinkedHashMap<>();
        Repository repository = null;
        Git git = null;
        try {
            // 打开仓库
            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            String allRepoPath = repoPath;
            if(!repoPath.endsWith("/.git") && !repoPath.endsWith("\\.git") ){
                allRepoPath += "/.git";
            }
            repository = repositoryBuilder.setGitDir(new File(allRepoPath))
                    .readEnvironment()
                    .findGitDir()
                    .build();
            // 创建 Git 对象
            git = new Git(repository);
            progress = 5;

            int eachProgress = (int) (45 / commitIds.length);
            int num = 1;
            // 遍历每个提交ID
            for (String commitId : commitIds) {
                List<FileInfo> filePathList = new ArrayList<>();
                //commitId = commitId.trim();
                commitId = commitId.replaceAll("[\\s\\n\\r]", "");
                // 获取指定提交
                ObjectId commitObjectId = repository.resolve(commitId);
                RevCommit commit = git.log().add(commitObjectId).call().iterator().next();
                // 解析树对象
                try (RevWalk revWalk = new RevWalk(repository)) {
                    RevCommit parentCommit = commit.getParent(0);
                    ObjectId oldTreeId = parentCommit.getTree();
                    ObjectId newTreeId = commit.getTree();

                    CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
                    try (ObjectReader oldReader = repository.newObjectReader()) {
                        oldTreeParser.reset(oldReader, oldTreeId);
                    }

                    CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
                    try (ObjectReader newReader = repository.newObjectReader()) {
                        newTreeParser.reset(newReader, newTreeId);
                    }

                    // 获取差异
                    List<DiffEntry> diffs = new Git(repository).diff()
                            .setOldTree(oldTreeParser)
                            .setNewTree(newTreeParser)
                            .call();
                    progress += (int)num * eachProgress * 0.5;
                    // 输出文件路径
                    for (DiffEntry diff : diffs) {
                        DiffEntry.ChangeType changeType = diff.getChangeType();
                        String filePath;
                        if (changeType == DiffEntry.ChangeType.DELETE) {
                            filePath = diff.getOldPath();
                        } else {
                            filePath = diff.getNewPath();
                        }
                        filePathList.add(new FileInfo(filePath, repoPath, "", changeType));
                    }
                    progress += (int)num * eachProgress;
                }
                filePathsMap.put(commitId, filePathList);
            }
            progress = 50;
            expFiles(targetFolderPath, filePathsMap);
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException(e);
        } finally {
             // 确保文件写入器被关闭
            if (git != null) {
                git.close();
            }
            // 确保文件写入器被关闭
            if (repository != null) {
                repository.close();
            }
        }
    }

    public static void expUncommittedFiles(String repoPath, String targetFolderPath) {
        progress = 0;
        Map<String, List<FileInfo>> filePathsMap = new LinkedHashMap<>();
        Repository repository = null;
        Git git = null;
        try {
            // 打开仓库
            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            String allRepoPath = repoPath;
            if(!repoPath.endsWith("/.git") && !repoPath.endsWith("\\.git") ){
                allRepoPath += "/.git";
            }
            repository = repositoryBuilder.setGitDir(new File(allRepoPath))
                    .readEnvironment()
                    .findGitDir()
                    .build();
            progress = 5;

            // 创建 Git 对象
            git = new Git(repository);
            // 获取未提交的文件列表
            List<DiffEntry> diffEntries = git.diff().call();
            List<FileInfo> filePathList = new ArrayList<>();
            // 打印未提交的文件路径
            for (DiffEntry diff : diffEntries) {
                DiffEntry.ChangeType changeType = diff.getChangeType();
                String filePath;
                if (changeType == DiffEntry.ChangeType.DELETE || changeType == DiffEntry.ChangeType.RENAME) {
                    filePath = diff.getOldPath();
                } else {
                    filePath = diff.getNewPath();
                }
                filePathList.add(new FileInfo(filePath, repoPath, "", changeType));
            }
            filePathsMap.put("---------", filePathList);
            // 关闭仓库
            repository.close();
            progress = 50;
            expFiles(targetFolderPath, filePathsMap);
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException(e);
        } finally {
            // 确保文件写入器被关闭
            if (git != null) {
                git.close();
            }
            // 确保文件写入器被关闭
            if (repository != null) {
                repository.close();
            }
        }
    }

    public static void expFiles(String targetFolderPath, Map<String, List<FileInfo>> filePathsMap) {
        FileWriter deletedFilesWriter = null;
        FileWriter modifiedFilesWriter = null;
        try {
            // 定义记录文件路径
            String deletedFilesPath = Paths.get(targetFolderPath, deletedFilesName).toString();
            String modifiedFilesPath = Paths.get(targetFolderPath, modifiedFilesName).toString();

            // 创建文件写入器
            File deletedFiles = new File(deletedFilesPath);
            File modifiedFiles = new File(modifiedFilesPath);

            // 检查并创建文件及其父目录
            if (!deletedFiles.exists()) {
                Files.createDirectories(Paths.get(deletedFiles.getParent()));
                deletedFiles.createNewFile();
            }
            if (!modifiedFiles.exists()) {
                Files.createDirectories(Paths.get(modifiedFiles.getParent()));
                modifiedFiles.createNewFile();
            }

            deletedFilesWriter = new FileWriter(deletedFiles, true);
            modifiedFilesWriter = new FileWriter(modifiedFiles, true);

            int num = 1;
            int eachProgress = (int) (50 / filePathsMap.size());
            // 遍历 filePathsMap
            for (Map.Entry<String, List<FileInfo>> entry : filePathsMap.entrySet()) {
                String commitId = entry.getKey();
                List<FileInfo> fileInfoList = entry.getValue();
                if (fileInfoList == null || fileInfoList.size() == 0) {
                    continue;
                }
                int num2 = 1;
                int eachProgress2 = (int) (eachProgress / filePathsMap.size());
                // 写入删除文件路径
                deletedFilesWriter.write("-------------" + commitId + "---------------------\n");
                // 写入删除文件路径
                modifiedFilesWriter.write("-------------" + commitId + "---------------------\n");
                progress += (int)num * eachProgress * 0.2;
                // 遍历每个提交ID
                for (FileInfo fileInfo : fileInfoList) {
                    DiffEntry.ChangeType fileType = fileInfo.getFileType();
                    String rootPath = fileInfo.getRootPath();
                    String filePath = fileInfo.getFilePath();
                    // 构建绝对路径
                    Path sourcePath = Paths.get(rootPath, filePath);
                    progress += (int)num2 * eachProgress2 * 0.4;
                    if (fileType == DiffEntry.ChangeType.DELETE) {
                        // 写入删除文件路径
                        deletedFilesWriter.write(sourcePath.toAbsolutePath() + "\n");
                    } else {
                        Path targetPath = Paths.get(targetFolderPath, filePath);
                        // 写入新增或修改文件路径
                        modifiedFilesWriter.write(sourcePath.toAbsolutePath() + "\n");
                        // 创建目标目录
                        Files.createDirectories(targetPath.getParent());
                        // 复制文件，如果存在则覆盖
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        /*// 检查目标文件是否已存在
                        if (Files.exists(targetPath)) {
                            System.out.println("文件已存在，跳过复制: " + targetPath.toAbsolutePath());
                            continue;
                        }
                        // 复制文件
                        Files.copy(sourcePath, targetPath);*/
                    }
                    num2++;
                }
                num++;
            }
            progress = 100;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // 确保文件写入器被关闭
            try {
                if (deletedFilesWriter != null) {
                    deletedFilesWriter.close();
                }
                if (modifiedFilesWriter != null) {
                    modifiedFilesWriter.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
