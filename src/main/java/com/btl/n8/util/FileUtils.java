package com.btl.n8.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileUtils {
    /**
     * Chuyển đổi File thành byte array
     * @param file - File object từ FileChooser
     * @return byte array của file
     * @throws IOException nếu không đọc được file
     */
    public static byte[] toByteArray(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Lưu byte array thành file
     * @param bytes - byte array của file
     * @param filePath - đường dẫn file đích (ví dụ: "C:/temp/image.jpg")
     * @throws IOException nếu không ghi được file
     */
    public static void saveByteArrayToFile(byte[] bytes, String filePath) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Byte array không được null hoặc rỗng");
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Đường dẫn file không được null hoặc rỗng");
        }

        Files.write(java.nio.file.Paths.get(filePath), bytes);
    }

    /**
     * Lưu byte array thành file với File object
     * @param bytes - byte array của file
     * @param file - File object đích
     * @throws IOException nếu không ghi được file
     */
    public static void saveByteArrayToFile(byte[] bytes, File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File object không được null");
        }
        saveByteArrayToFile(bytes, file.getAbsolutePath());
    }
}
