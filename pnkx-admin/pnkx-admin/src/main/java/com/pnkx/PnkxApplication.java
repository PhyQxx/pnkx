package com.pnkx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 *
 * @author phy
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class PnkxApplication {
    public static void main(String[] args) {
        SpringApplication.run(PnkxApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  Pei你看雪博客管理系统启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                "              _ \n" +
                "             | |\n" +
                " _ __  _ __  | | ____  __      \n" +
                " | '_ \\| '_ \\| |/ /\\ \\/ /  \n" +
                " | |_) | | | |   <  >  <       \n" +
                " | .__/|_| |_|_|\\_\\/_/\\_\\  \n" +
                " | |                           \n" +
                " |_|                           \n");
    }
}
