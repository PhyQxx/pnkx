package com.pnkx.quartz.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Classname WebSpider
 * @Description 网页爬虫工具类
 * @Date 2021-03-22 10:38
 * @Author by PHY
 */
public class WebSpider {
    /**
     * 获得urlStr对应网络内容
     *
     * @param urlStr
     * @return
     */
    public static String getURLContent(String urlStr, String charset) {
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(urlStr);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), Charset.forName(charset)));
            String temp = "";
            while ((temp = reader.readLine()) != null) {
                sb.append(temp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String firstStr = sb.toString();
        String secondStr = firstStr.substring(firstStr.indexOf("<tbody>") + 1, firstStr.lastIndexOf("</tbody>"));
        String thirdStr = secondStr.replaceAll("<td class=\"td-03\">(.*?)</td>", "");
        String forthStr = thirdStr.replaceAll("</a>(.*?)</td>", "</a></td>");
        String fifthStr = forthStr.replace("tbody>", "<table>");
        String sixthStr = fifthStr + "</table>";
        String seventhStr = sixthStr.replace("href=\"", "href=\"https://s.weibo.com");
        return "<style>\n" +
                "  a{\n" +
                "    margin: 0 0 0 1rem;\n" +
                "    color: #0078b6!important;\n" +
                "    text-decoration:none\n" +
                "  }\n" +
                "</style>" + seventhStr;
    }
}
