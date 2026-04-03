package com.supporter.prj.util;

import com.supporter.prj.entity.ExportOptions;
import com.supporter.prj.entity.ExportResult;
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
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author xueguangchen
 * @version 1.0.0
 * @ClassName com.supporter.prj.util.ExpGitHubUtil.java
 * @Description 导出相关工具类
 * @createTime 2024年11月17日 10:24:00
 */
public class ExpGitHubUtil {

    //进度 (volatile确保多线程可见性)
    private static volatile int progress = 0;
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
     * 获取指定提交的文件列表（用于预览）
     * @param repoPath 仓库路径
     * @param commitId 提交ID
     * @return 文件列表
     */
    public static List<FileInfo> getCommitFiles(String repoPath, String commitId) {
        List<FileInfo> fileInfoList = new ArrayList<>();
        Repository repository = null;
        Git git = null;
        try {
            System.out.println("[ExpGitHubUtil] getCommitFiles 开始, repoPath: " + repoPath + ", commitId: " + commitId);
            
            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            String allRepoPath = repoPath;
            if (!repoPath.endsWith("/.git") && !repoPath.endsWith("\\.git")) {
                allRepoPath += "/.git";
            }
            repository = repositoryBuilder.setGitDir(new File(allRepoPath))
                    .readEnvironment()
                    .findGitDir()
                    .build();
            git = new Git(repository);

            ObjectId commitObjectId = repository.resolve(commitId);
            if (commitObjectId == null) {
                System.err.println("[ExpGitHubUtil] 无法解析提交ID: " + commitId);
                return fileInfoList;
            }
            
            RevCommit commit = git.log().add(commitObjectId).call().iterator().next();
            System.out.println("[ExpGitHubUtil] 找到提交: " + commit.getName() + ", 父提交数: " + commit.getParentCount());

            // 检查是否有父提交
            if (commit.getParentCount() == 0) {
                // 初始提交：获取所有文件
                System.out.println("[ExpGitHubUtil] 处理初始提交");
                org.eclipse.jgit.treewalk.TreeWalk treeWalk = new org.eclipse.jgit.treewalk.TreeWalk(repository);
                treeWalk.addTree(commit.getTree());
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    String filePath = treeWalk.getPathString();
                    fileInfoList.add(new FileInfo(filePath, repoPath, "", DiffEntry.ChangeType.ADD));
                }
                treeWalk.close();
            } else {
                // 有父提交：计算差异
                System.out.println("[ExpGitHubUtil] 计算差异");
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

                List<DiffEntry> diffs = git.diff()
                        .setOldTree(oldTreeParser)
                        .setNewTree(newTreeParser)
                        .call();
                
                System.out.println("[ExpGitHubUtil] 差异数量: " + diffs.size());

                for (DiffEntry diff : diffs) {
                    DiffEntry.ChangeType changeType = diff.getChangeType();
                    String filePath;
                    if (changeType == DiffEntry.ChangeType.DELETE) {
                        filePath = diff.getOldPath();
                    } else {
                        filePath = diff.getNewPath();
                    }
                    fileInfoList.add(new FileInfo(filePath, repoPath, "", changeType));
                }
            }
            
            System.out.println("[ExpGitHubUtil] getCommitFiles 完成, 文件数: " + fileInfoList.size());
        } catch (Exception e) {
            System.err.println("[ExpGitHubUtil] 获取提交文件失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (git != null) {
                git.close();
            }
            if (repository != null) {
                repository.close();
            }
        }
        return fileInfoList;
    }

    /**
     * 获取未提交的文件列表（用于预览）
     * @param repoPath 仓库路径
     * @return 文件列表
     */
    public static List<FileInfo> getUncommittedFiles(String repoPath) {
        List<FileInfo> fileInfoList = new ArrayList<>();
        Repository repository = null;
        Git git = null;
        try {
            FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
            String allRepoPath = repoPath;
            if (!repoPath.endsWith("/.git") && !repoPath.endsWith("\\.git")) {
                allRepoPath += "/.git";
            }
            repository = repositoryBuilder.setGitDir(new File(allRepoPath))
                    .readEnvironment()
                    .findGitDir()
                    .build();
            git = new Git(repository);

            List<DiffEntry> diffEntries = git.diff().call();
            for (DiffEntry diff : diffEntries) {
                DiffEntry.ChangeType changeType = diff.getChangeType();
                String filePath;
                if (changeType == DiffEntry.ChangeType.DELETE || changeType == DiffEntry.ChangeType.RENAME) {
                    filePath = diff.getOldPath();
                } else {
                    filePath = diff.getNewPath();
                }
                fileInfoList.add(new FileInfo(filePath, repoPath, "", changeType));
            }
        } catch (Exception e) {
            System.err.println("[ExpGitHubUtil] 获取未提交文件失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (git != null) {
                git.close();
            }
            if (repository != null) {
                repository.close();
            }
        }
        return fileInfoList;
    }

    /**
     * 获取git提交历史记录
     * @param repoPath 本地仓库路径
     * @param maxCount
     * @return
     */
    public static List<GitCommitHistory> fetchGitCommitHistory(String repoPath, long maxCount, String author, String keyword, Date startDate, Date endDate) {
        List<GitCommitHistory> gitCommitHistoryList = new ArrayList<>();
        try {
            System.out.println("[ExpGitHubUtil] 开始获取提交历史, repoPath: " + repoPath);
            File repoDir = new File(repoPath);
            System.out.println("[ExpGitHubUtil] 仓库目录存在: " + repoDir.exists() + ", 是目录: " + repoDir.isDirectory());
            File gitDir = new File(repoDir, ".git");
            System.out.println("[ExpGitHubUtil] .git 目录存在: " + gitDir.exists());
            
            Git git = Git.open(repoDir);
            Iterable<RevCommit> commits = git.log().call();
            int count = 0;
            for (RevCommit commit : commits) {
                if(maxCount > 0){
                    if (count >= maxCount) break; // 限制获取的提交记录数量
                }
                String commitAuthor = commit.getAuthorIdent().getName();
                String commitMessage = commit.getFullMessage();
                Date commitDate = commit.getAuthorIdent().getWhen();

                // 过滤条件
                boolean authorMatch = author == null || author.isEmpty() || commitAuthor.toLowerCase().contains(author.toLowerCase()) || commit.getAuthorIdent().getEmailAddress().contains(author.toLowerCase());
                boolean keywordMatch = keyword == null || keyword.isEmpty() || commitMessage.toLowerCase().contains(keyword.toLowerCase());
                boolean dateMatch = (startDate == null || !commitDate.before(startDate)) && (endDate == null || !commitDate.after(endDate));

                if (authorMatch && keywordMatch && dateMatch) {
                    gitCommitHistoryList.add(new GitCommitHistory(
                            commit.getName(),
                            commit.getAuthorIdent().getName(),
                            commit.getAuthorIdent().getEmailAddress(),
                            commit.getAuthorIdent().getWhen(),
                            commit.getFullMessage()
                    ));
                }
                count++;
            }
            System.out.println("[ExpGitHubUtil] 获取到 " + gitCommitHistoryList.size() + " 条提交记录");
            git.close();
        } catch (Exception e) {
            System.err.println("[ExpGitHubUtil] 获取提交历史失败: " + e.getMessage());
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
     * 导出已提交文件（兼容旧版本，使用默认选项）
     * @param repoPath Git本地仓库的路径
     * @param targetFolderPath 目标文件夹路径
     * @param commitIds 多个提交的SHA-1哈希值
     * @return 导出结果（包含文件数量和总大小）
     */
    public static ExportResult expCommittedFile(String repoPath, String targetFolderPath, String[] commitIds) {
        return expCommittedFile(repoPath, targetFolderPath, commitIds, null);
    }

    /**
     * 导出已提交文件（支持高级选项）
     * @param repoPath Git本地仓库的路径
     * @param targetFolderPath 目标文件夹路径
     * @param commitIds 多个提交的SHA-1哈希值
     * @param options 导出选项配置
     * @return 导出结果（包含文件数量和总大小）
     */
    public static ExportResult expCommittedFile(String repoPath, String targetFolderPath, String[] commitIds, ExportOptions options) {
        if(commitIds == null || commitIds.length == 0){
            return new ExportResult(0, 0);
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
            System.out.println("[ExpGitHubUtil] 开始处理 " + commitIds.length + " 个提交");
            for (String commitId : commitIds) {
                List<FileInfo> filePathList = new ArrayList<>();
                //commitId = commitId.trim();
                commitId = commitId.replaceAll("[\\s\\n\\r]", "");
                System.out.println("[ExpGitHubUtil] 正在处理提交: " + commitId);
                try {
                    // 获取指定提交
                    ObjectId commitObjectId = repository.resolve(commitId);
                    if (commitObjectId == null) {
                        System.err.println("[ExpGitHubUtil] 无法解析提交ID: " + commitId);
                        continue;
                    }
                    RevCommit commit = git.log().add(commitObjectId).call().iterator().next();
                    
                    // 检查是否有父提交（初始提交没有父提交）
                    if (commit.getParentCount() == 0) {
                        // 初始提交：导出所有文件
                        System.out.println("[ExpGitHubUtil] 处理初始提交: " + commitId);
                        try (RevWalk revWalk = new RevWalk(repository)) {
                            ObjectId newTreeId = commit.getTree();
                            org.eclipse.jgit.treewalk.TreeWalk treeWalk = new org.eclipse.jgit.treewalk.TreeWalk(repository);
                            treeWalk.addTree(newTreeId);
                            treeWalk.setRecursive(true);
                            while (treeWalk.next()) {
                                String filePath = treeWalk.getPathString();
                                // 应用过滤规则
                                if (options == null || !options.shouldFilterFile(filePath)) {
                                    filePathList.add(new FileInfo(filePath, repoPath, "", DiffEntry.ChangeType.ADD));
                                }
                            }
                            treeWalk.close();
                        }
                    } else {
                        // 有父提交：计算差异
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
                            // 输出文件路径
                            for (DiffEntry diff : diffs) {
                                DiffEntry.ChangeType changeType = diff.getChangeType();
                                String filePath;
                                if (changeType == DiffEntry.ChangeType.DELETE) {
                                    filePath = diff.getOldPath();
                                } else {
                                    filePath = diff.getNewPath();
                                }
                                // 应用过滤规则
                                if (options == null || !options.shouldFilterFile(filePath)) {
                                    filePathList.add(new FileInfo(filePath, repoPath, "", changeType));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[ExpGitHubUtil] 处理提交 " + commitId + " 时出错: " + e.getMessage());
                    e.printStackTrace();
                }
                progress += eachProgress;
                
                // 检查是否包含空提交
                if (options != null && !options.isIncludeEmptyCommits() && filePathList.isEmpty()) {
                    System.out.println("[ExpGitHubUtil] 跳过空提交: " + commitId);
                    continue;
                }

                filePathsMap.put(commitId, filePathList);
            }
            progress = 50;
            System.out.println("[ExpGitHubUtil] expCommittedFile: 开始复制文件...");
            ExportResult result = expFiles(targetFolderPath, filePathsMap, options);
            System.out.println("[ExpGitHubUtil] expCommittedFile: 全部完成, 文件数: " + result.getFileCount());
            return result;
        } catch (IOException e) {
            System.err.println("[ExpGitHubUtil] expCommittedFile 异常: " + e.getMessage());
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

    /**
     * 导出未提交文件（兼容旧版本，使用默认选项）
     * @param repoPath Git本地仓库的路径
     * @param targetFolderPath 目标文件夹路径
     * @return 导出结果（包含文件数量和总大小）
     */
    public static ExportResult expUncommittedFiles(String repoPath, String targetFolderPath) {
        return expUncommittedFiles(repoPath, targetFolderPath, null);
    }

    /**
     * 导出未提交文件（支持高级选项）
     * @param repoPath Git本地仓库的路径
     * @param targetFolderPath 目标文件夹路径
     * @param options 导出选项配置
     * @return 导出结果（包含文件数量和总大小）
     */
    public static ExportResult expUncommittedFiles(String repoPath, String targetFolderPath, ExportOptions options) {
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
                // 应用过滤规则
                if (options == null || !options.shouldFilterFile(filePath)) {
                    filePathList.add(new FileInfo(filePath, repoPath, "", changeType));
                }
            }
            filePathsMap.put("---------", filePathList);
            progress = 50;
            ExportResult result = expFiles(targetFolderPath, filePathsMap, options);
            return result;
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

    /**
     * 导出文件（兼容旧版本，使用默认选项）
     * @param targetFolderPath 目标文件夹路径
     * @param filePathsMap 文件路径映射
     * @return 导出结果（包含文件数量和总大小）
     */
    public static ExportResult expFiles(String targetFolderPath, Map<String, List<FileInfo>> filePathsMap) {
        return expFiles(targetFolderPath, filePathsMap, null);
    }

    /**
     * 导出文件（支持高级选项）
     * @param targetFolderPath 目标文件夹路径
     * @param filePathsMap 文件路径映射
     * @param options 导出选项配置
     * @return 导出结果（包含文件数量和总大小）
     */
    public static ExportResult expFiles(String targetFolderPath, Map<String, List<FileInfo>> filePathsMap, ExportOptions options) {
        System.out.println("[ExpGitHubUtil] expFiles 开始, 目标路径: " + targetFolderPath);
        FileWriter deletedFilesWriter = null;
        FileWriter modifiedFilesWriter = null;
        FileWriter exportLogWriter = null;

        // 判断是否需要创建日志
        boolean createLog = options == null || options.isCreateExportLog();
        // 判断是否保留文件结构
        boolean preserveStructure = options == null || options.isPreserveStructure();
        
        try {
            // 定义记录文件路径
            String deletedFilesPath = Paths.get(targetFolderPath, deletedFilesName).toString();
            String modifiedFilesPath = Paths.get(targetFolderPath, modifiedFilesName).toString();
            String exportLogPath = Paths.get(targetFolderPath, "export_log.txt").toString();

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
            
            // 创建导出日志
            if (createLog) {
                File exportLogFile = new File(exportLogPath);
                exportLogWriter = new FileWriter(exportLogFile, true);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                exportLogWriter.write("========================================\n");
                exportLogWriter.write("导出时间: " + sdf.format(new Date()) + "\n");
                exportLogWriter.write("导出选项: " + (options != null ? options.toString() : "默认") + "\n");
                exportLogWriter.write("========================================\n\n");
            }

            int num = 1;
            int mapSize = filePathsMap.size();
            int eachProgress = mapSize > 0 ? (int) (50 / mapSize) : 50;
            int totalFilesCopied = 0;
            int totalFilesDeleted = 0;
            int totalFilesSkipped = 0;
            long totalSize = 0;
            
            // 遍历 filePathsMap
            for (Map.Entry<String, List<FileInfo>> entry : filePathsMap.entrySet()) {
                String commitId = entry.getKey();
                List<FileInfo> fileInfoList = entry.getValue();
                if (fileInfoList == null || fileInfoList.size() == 0) {
                    System.out.println("[ExpGitHubUtil] 提交 " + commitId + " 没有文件变更");
                    continue;
                }
                System.out.println("[ExpGitHubUtil] 处理提交 " + commitId + ", 文件数: " + fileInfoList.size());
                int num2 = 1;
                int eachProgress2 = fileInfoList.size() > 0 ? (int) (eachProgress / fileInfoList.size()) : eachProgress;
                // 写入删除文件路径
                deletedFilesWriter.write("-------------" + commitId + "---------------------\n");
                // 写入删除文件路径
                modifiedFilesWriter.write("-------------" + commitId + "---------------------\n");
                
                if (createLog && exportLogWriter != null) {
                    exportLogWriter.write("提交: " + commitId + "\n");
                }
                
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
                        totalFilesDeleted++;
                    } else {
                        // 根据是否保留文件结构决定目标路径
                        Path targetPath;
                        if (preserveStructure) {
                            // 保留原有目录结构
                            targetPath = Paths.get(targetFolderPath, filePath);
                        } else {
                            // 不保留目录结构，所有文件放在目标目录下
                            String fileName = Paths.get(filePath).getFileName().toString();
                            targetPath = Paths.get(targetFolderPath, fileName);
                            
                            // 处理文件名冲突
                            int counter = 1;
                            while (Files.exists(targetPath)) {
                                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                                String extension = fileName.substring(fileName.lastIndexOf('.'));
                                targetPath = Paths.get(targetFolderPath, baseName + "_" + counter + extension);
                                counter++;
                            }
                        }
                        
                        // 写入新增或修改文件路径
                        modifiedFilesWriter.write(sourcePath.toAbsolutePath() + "\n");
                        // 创建目标目录
                        Files.createDirectories(targetPath.getParent());
                        // 复制文件，如果存在则覆盖
                        try {
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            totalFilesCopied++;
                            // 累加文件大小
                            try {
                                totalSize += Files.size(sourcePath);
                            } catch (IOException sizeEx) {
                                // 忽略获取大小失败
                            }

                            if (createLog && exportLogWriter != null) {
                                exportLogWriter.write("  [复制] " + filePath + " -> " + targetPath + "\n");
                            }
                        } catch (IOException copyEx) {
                            System.err.println("[ExpGitHubUtil] 复制文件失败: " + sourcePath + " -> " + targetPath + ", 错误: " + copyEx.getMessage());
                            totalFilesSkipped++;
                            
                            if (createLog && exportLogWriter != null) {
                                exportLogWriter.write("  [失败] " + filePath + " - " + copyEx.getMessage() + "\n");
                            }
                        }
                    }
                    num2++;
                }
                
                if (createLog && exportLogWriter != null) {
                    exportLogWriter.write("\n");
                }
                
                num++;
            }
            
            // 写入导出统计信息
            if (createLog && exportLogWriter != null) {
                exportLogWriter.write("========================================\n");
                exportLogWriter.write("导出统计:\n");
                exportLogWriter.write("  - 已复制文件: " + totalFilesCopied + "\n");
                exportLogWriter.write("  - 已删除文件: " + totalFilesDeleted + "\n");
                exportLogWriter.write("  - 跳过文件: " + totalFilesSkipped + "\n");
                exportLogWriter.write("========================================\n");
            }

            progress = 100;
            System.out.println("[ExpGitHubUtil] expFiles 完成, 复制: " + totalFilesCopied + ", 删除: " + totalFilesDeleted + ", 跳过: " + totalFilesSkipped + ", 总大小: " + totalSize);
            return new ExportResult(totalFilesCopied, totalSize);
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
                if (exportLogWriter != null) {
                    exportLogWriter.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
