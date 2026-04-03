package com.supporter.prj.util;

import com.supporter.prj.entity.ExportHistory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 导出历史管理工具类
 * 负责保存、加载和管理导出历史记录
 *
 * @author xueguangchen
 * @version 2.0.0
 */
public class ExportHistoryManager {
    private static final String HISTORY_DIR = System.getProperty("user.home") + "/.ExpGitFilePrj";
    private static final String HISTORY_FILE = HISTORY_DIR + "/export_history.dat";
    private static final int MAX_HISTORY_COUNT = 100; // 最大保存历史记录数

    private static ExportHistoryManager instance;
    private List<ExportHistory> historyList;

    private ExportHistoryManager() {
        historyList = new ArrayList<>();
        loadHistory();
    }

    /**
     * 获取单例实例
     */
    public static synchronized ExportHistoryManager getInstance() {
        if (instance == null) {
            instance = new ExportHistoryManager();
        }
        return instance;
    }

    /**
     * 添加导出历史记录
     */
    public void addHistory(ExportHistory history) {
        if (history == null) {
            System.err.println("[ExportHistoryManager] addHistory: history is null");
            return;
        }

        System.out.println("[ExportHistoryManager] 添加历史记录: " + history.getBriefDescription());

        // 添加到列表开头
        historyList.add(0, history);

        // 限制历史记录数量
        if (historyList.size() > MAX_HISTORY_COUNT) {
            historyList = historyList.subList(0, MAX_HISTORY_COUNT);
        }

        // 保存到文件
        saveHistory();
    }

    /**
     * 获取所有历史记录
     */
    public List<ExportHistory> getAllHistory() {
        return new ArrayList<>(historyList);
    }

    /**
     * 获取最近N条历史记录
     */
    public List<ExportHistory> getRecentHistory(int count) {
        return historyList.stream()
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取历史记录
     */
    public ExportHistory getHistoryById(String id) {
        return historyList.stream()
                .filter(h -> h.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 删除指定历史记录
     */
    public void deleteHistory(String id) {
        historyList.removeIf(h -> h.getId().equals(id));
        saveHistory();
    }

    /**
     * 清空所有历史记录
     */
    public void clearAllHistory() {
        historyList.clear();
        saveHistory();
    }

    /**
     * 搜索历史记录
     */
    public List<ExportHistory> searchHistory(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllHistory();
        }

        String lowerKeyword = keyword.toLowerCase();
        return historyList.stream()
                .filter(h -> 
                    (h.getConfigName() != null && h.getConfigName().toLowerCase().contains(lowerKeyword)) ||
                    (h.getRepoPath() != null && h.getRepoPath().toLowerCase().contains(lowerKeyword)) ||
                    (h.getTargetPath() != null && h.getTargetPath().toLowerCase().contains(lowerKeyword))
                )
                .collect(Collectors.toList());
    }

    /**
     * 按日期范围筛选历史记录
     */
    public List<ExportHistory> getHistoryByDateRange(Date startDate, Date endDate) {
        return historyList.stream()
                .filter(h -> {
                    Date exportTime = h.getExportTime();
                    if (exportTime == null) {
                        return false;
                    }
                    boolean afterStart = startDate == null || !exportTime.before(startDate);
                    boolean beforeEnd = endDate == null || !exportTime.after(endDate);
                    return afterStart && beforeEnd;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取成功的历史记录
     */
    public List<ExportHistory> getSuccessHistory() {
        return historyList.stream()
                .filter(ExportHistory::isSuccess)
                .collect(Collectors.toList());
    }

    /**
     * 获取失败的历史记录
     */
    public List<ExportHistory> getFailedHistory() {
        return historyList.stream()
                .filter(h -> !h.isSuccess())
                .collect(Collectors.toList());
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", historyList.size());
        stats.put("successCount", getSuccessHistory().size());
        stats.put("failedCount", getFailedHistory().size());
        
        long totalFiles = historyList.stream()
                .mapToLong(ExportHistory::getFileCount)
                .sum();
        stats.put("totalFiles", totalFiles);
        
        long totalSize = historyList.stream()
                .mapToLong(ExportHistory::getTotalSize)
                .sum();
        stats.put("totalSize", totalSize);
        
        return stats;
    }

    /**
     * 加载历史记录
     */
    @SuppressWarnings("unchecked")
    private void loadHistory() {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                historyList = (List<ExportHistory>) obj;
            }
        } catch (Exception e) {
            // 加载失败时使用空列表
            historyList = new ArrayList<>();
        }
    }

    /**
     * 保存历史记录
     */
    private void saveHistory() {
        try {
            // 确保目录存在
            File dir = new File(HISTORY_DIR);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("[ExportHistoryManager] 创建目录: " + HISTORY_DIR + ", 结果: " + created);
            }

            // 保存到文件
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HISTORY_FILE))) {
                oos.writeObject(historyList);
                System.out.println("[ExportHistoryManager] 保存历史记录成功, 共 " + historyList.size() + " 条");
            }
        } catch (Exception e) {
            System.err.println("[ExportHistoryManager] 保存历史记录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 导出历史记录到CSV
     */
    public void exportToCSV(String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // 写入表头
            writer.println("ID,导出时间,配置名称,仓库路径,目标路径,导出类型,文件数量,总大小,是否成功,错误信息");

            // 写入数据
            for (ExportHistory history : historyList) {
                StringBuilder sb = new StringBuilder();
                sb.append(history.getId()).append(",");
                sb.append(history.getFormattedExportTime()).append(",");
                sb.append(escapeCSV(history.getConfigName())).append(",");
                sb.append(escapeCSV(history.getRepoPath())).append(",");
                sb.append(escapeCSV(history.getTargetPath())).append(",");
                sb.append(history.getExportType()).append(",");
                sb.append(history.getFileCount()).append(",");
                sb.append(history.getFormattedTotalSize()).append(",");
                sb.append(history.isSuccess() ? "成功" : "失败").append(",");
                sb.append(escapeCSV(history.getErrorMessage()));
                writer.println(sb.toString());
            }
        }
    }

    /**
     * CSV字段转义
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
