package com.pnkx.common.ftp;

import lombok.Data;

import java.util.List;

/**
 * FTPFiles
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/8/22 13:38
 * @description 描述
 */
@Data
public class SysFTPFile {

    /**
     * 名称
     */
    String name;

    /**
     * url
     */
    String url;

    /**
     * 类型：0文件，1文件夹
     */
    Integer type;

    /**
     * 子集
     */
    List<SysFTPFile> children;
}
