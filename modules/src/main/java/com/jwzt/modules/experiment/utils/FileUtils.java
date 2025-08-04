package com.jwzt.modules.experiment.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileUtils {

    /**
     * 判断文件路径是否存在，不存在则创建其父目录
     * @param shpFilePath shp文件的完整路径，例如 "/data/shapefiles/region.shp"
     */
    public static void ensureFilePathExists(String shpFilePath) {
        File shpFile = new File(shpFilePath);
        File parentDir = shpFile.getParentFile(); // 获取父级目录

        if (!parentDir.exists()) {
            boolean created = parentDir.mkdirs(); // 创建所有不存在的父目录
            if (created) {
                System.out.println("已创建目录: " + parentDir.getAbsolutePath());
            } else {
                System.err.println("目录创建失败: " + parentDir.getAbsolutePath());
            }
        } else {
            System.out.println("目录已存在: " + parentDir.getAbsolutePath());
        }
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == -1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    public static String readFileAsString(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }
}
