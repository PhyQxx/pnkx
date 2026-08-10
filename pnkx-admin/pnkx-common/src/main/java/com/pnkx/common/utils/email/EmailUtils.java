package com.pnkx.common.utils.email;

import com.pnkx.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.Email;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * EmailUtils
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2024/4/30 17:36
 * @description 邮件工具类
 */
public class EmailUtils {
    public static String[] validEmail(String emails) {
        if (StringUtils.isNotBlank(emails)) {
            return Arrays.stream(emails.split(","))
                    .filter(email -> isEmail(email.trim()))
                    .distinct()
                    .toArray(String[]::new);
        }
        return null;
    }

    public static Boolean isEmail(String email) {
        String regEx1 = "^([a-z0-9A-Z]+[-|.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        return Pattern.compile(regEx1).matcher(email.trim()).matches();
    }
}
