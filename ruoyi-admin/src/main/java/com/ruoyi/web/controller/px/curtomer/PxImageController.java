package com.ruoyi.web.controller.px.curtomer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @Author 裴浩宇
 * @Date 2021/2/27 09:37
 * @Class 类说明：以流的形式回显图片
 */
@RestController
@RequestMapping("/profile")
public class PxImageController {

    @Value("${ruoyi.profile}")
    private String savePath;

    @RequestMapping("/avatar/{year}/{month}/{day}/{fileName}")
    private void showImg(HttpServletResponse response,
                         @PathVariable(value = "year") String year,
                         @PathVariable(value = "month") String month,
                         @PathVariable(value = "day") String day,
                         @PathVariable(value = "fileName") String fileName
    ) throws IOException {
        String filePath = "/" + year + "/" + month + "/" + day + "/" + fileName;
        File imageFile = new File(savePath + "/avatar" + filePath);
        if (imageFile.exists()) {
            FileInputStream fis = null;
            OutputStream os = null;
            try {
                fis = new FileInputStream(imageFile);
                os = response.getOutputStream();
                int count = 0;
                byte[] buffer = new byte[1024 * 8];
                while ((count = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, count);
                    os.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    fis.close();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
