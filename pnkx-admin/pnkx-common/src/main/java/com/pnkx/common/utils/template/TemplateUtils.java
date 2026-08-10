package com.pnkx.common.utils.template;

import cn.hutool.core.io.file.FileReader;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.file.Files;

/**
 * TemplateUtils
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/8/17 10:39
 * @description 模版工具类
 */
public class TemplateUtils {

    /**
     * 获取模板
     *
     * @param templateName
     * @return
     */
    public static String getTemplate(String templateName) {
        try {
            // 获取项目路径中的文件
            TemplateUtils templateUtils = new TemplateUtils();
            File file = templateUtils.getFilePath(templateName);
            // 读取字符串
            return FileReader.create(file).readString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File getFilePath(String fileName) throws IOException {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("template/" + fileName + ".html");
        File tmp = File.createTempFile("temp", null);
        OutputStream os = Files.newOutputStream(tmp.toPath());
        int bytesRead = 0;
        byte[] buffer = new byte[8192];
        while ((bytesRead = resourceAsStream.read(buffer, 0, 8192)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        resourceAsStream.close();
        return tmp;
    }
}
