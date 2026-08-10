package com.pnkx.common.ftp;

import com.pnkx.common.constant.WebsiteAddressConstants;
import com.pnkx.common.utils.StringUtils;
import lombok.Data;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * FtpConfig
 *
 * @author 裴浩宇
 * @version 1.0
 * @date 2023/8/3 10:39
 * @description FTP配置类
 */
@Data
@Component
public class FtpTool {

    private static final Logger log = LoggerFactory.getLogger(FtpTool.class);

    /**
     * 连接超时时间设置
     */
    private static final int TIMEOUT = 1000 * 30;
    /**
     * ftp 服务地址
     */
    @Value("${ftp.host}")
    private String HOST;
    /**
     * ftp 服务账户
     */
    @Value("${ftp.username}")
    private String USER_NAME;
    /**
     * ftp 密码
     */
    @Value("${ftp.password}")
    private String PWD;
    /**
     * 端口 ftp 默认 21 ，登录端口。20为传输端口，此处使用连接端口
     */
    @Value("${ftp.port}")
    private Integer PORT;
    /**
     * 目录
     */
    @Value("${ftp.path}")
    private String PATH;

    /**
     * 创建连接
     */
    public FTPClient connectFtp() {
        FTPClient ftpClient;
        try {
            ftpClient = new FTPClient();    
            ftpClient.setConnectTimeout(TIMEOUT);
            ftpClient.connect(HOST, PORT);
            ftpClient.setRemoteVerificationEnabled(false);
            ftpClient.login(USER_NAME, PWD);
            //开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就用本地编码（ISO-8859-1）
            if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
                ftpClient.setControlEncoding("UTF-8");
            } else {
                //FTP协议里面，规定文件名编码为iso-8859-1
                ftpClient.setControlEncoding("ISO-8859-1");
            }
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                log.error("连接FTP失败，用户名或密码错误；用户名：{}；密码{}", USER_NAME, PWD);
                ftpClient.disconnect();
            } else {
                log.info("FTP连接成功!");
            }
        } catch (Exception e) {
            log.info("登陆FTP失败，请检查FTP相关配置信息是否正确！；用户名：{}；密码{}；异常信息：{}", USER_NAME, PWD, e.toString());
            return null;
        }
        return ftpClient;
    }

    /**
     * 关闭FTP连接
     *
     * @param ftpClient 链接
     */
    public void closeFtpClient(FTPClient ftpClient) {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                log.error("关闭FTP连接异常；异常信息：{}", e.toString());
            }
        }
    }

    public InputStream previewFile(FTPClient ftpClient, String path) {
        InputStream inputStream = null;
        try {
            inputStream = ftpClient.retrieveFileStream(path);
        } catch (Exception e) {
            log.error("获取文件失败；异常信息：{}", e.toString());
        }
        return inputStream;
    }

    /**
     * 按前后缀查询文件
     *
     * @param facePrefix
     * @param prefix
     * @param suffix
     * @return
     */
    public List<String> showPatternFiles(String facePrefix, String prefix, String suffix) {
        List<String> ret = new ArrayList<>();
        String fileName = null;
        FTPClient ftpClient = null;
        try {
            ftpClient = connectFtp();
            boolean changeFlag = ftpClient.changeWorkingDirectory(facePrefix);
            if (!changeFlag) {
                throw new IOException("进入Ftp目录" + facePrefix + "失败");
            }
            FTPFile[] files = ftpClient.listFiles(PATH);
            for (FTPFile ftpFile : files) {
                fileName = ftpFile.getName();
                if ((!".".equals(fileName)) && (!"..".equals(fileName))) {
                    String regEx;
                    if (StringUtils.isNotBlank(prefix)) {
                        regEx = prefix + "*." + suffix;
                    } else {
                        regEx = suffix + "$";
                    }
                    Pattern pattern = Pattern.compile(regEx);
                    if (pattern.matcher(fileName).find()) {
                        ret.add(fileName);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取文件失败；异常信息：{}", e.toString());
        } finally {
            if (ftpClient != null) {
                //close(ftpClient);
            }
        }
        return ret;
    }

    /**
     * 从FTP下载文件到本地
     */
    public String downloadFile(FTPClient ftpClient, String remotePath, String fileName, String downloadPath) {
        InputStream is = null;
        FileOutputStream fos = null;
        final File targetFile = new File(downloadPath + File.separator + fileName);
        try {
            // 获取ftp上的文件
            is = ftpClient.retrieveFileStream(remotePath + "/" + fileName);
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            fos = new FileOutputStream(downloadPath + File.separator + fileName);
            // 文件读取方式一
            int i;
            byte[] bytes = new byte[1024];
            while ((i = is.read(bytes)) != -1) {
                fos.write(bytes, 0, i);
            }
            // 文件读取方式二
            //ftpClient.retrieveFile(fileName, new FileOutputStream(new File(downloadPath)));
            ftpClient.completePendingCommand();
            log.info("FTP文件下载成功！");
        } catch (Exception e) {
            log.error("FTP文件下载失败！异常信息{}", e.toString());
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                log.error("下载流关闭失败；异常信息{}", e.toString());
            }
        }
        return targetFile.getAbsolutePath();
    }

    /**
     * 上传文件
     *
     * @param serviceDec ftp服务保存地址
     * @param fileName   上传到ftp的文件名
     * @param file       待上传文件的名称（绝对地址） *
     * @return
     */
    public String uploadFile(FTPClient ftpClient, String serviceDec, String fileName, File file) {
        try (InputStream input = Files.newInputStream(file.toPath())) {
            return uploadFile(ftpClient, PATH + serviceDec, fileName, input);
        } catch (IOException e) {
            log.error("文件上传失败；异常信息{}", e.toString());
        }
        return "";
    }

    /**
     * 上传文件
     *
     * @param serviceDec ftp服务保存地址
     * @param fileName   上传到ftp的文件名
     * @param file       待上传文件的名称（绝对地址） *
     * @return
     */
    public String uploadMultipartFile(FTPClient ftpClient, String serviceDec, String fileName, MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return uploadFile(ftpClient, PATH + serviceDec, fileName, input);
        } catch (IOException e) {
            log.error("文件上传失败；异常信息{}", e.toString());
        }
        return "";
    }

    /**
     * 上传文件
     *
     * @param serviceDec  ftp服务保存地址
     * @param fileName    上传到ftp的文件名
     * @param inputStream 输入文件流
     * @return
     */
    private String uploadFile(FTPClient ftpClient, String serviceDec, String fileName, InputStream inputStream) {
        try {
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            createDirectory(ftpClient, serviceDec);
            ftpClient.makeDirectory(serviceDec);
            ftpClient.changeWorkingDirectory(serviceDec);
            while (existFile(ftpClient, new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1))) {
                String frontName = fileName.substring(0, fileName.lastIndexOf("."));
                // 名称里有-，并且后面是数字，识别为重复文件
                if (frontName.lastIndexOf("-") != -1 && frontName.substring(frontName.lastIndexOf("-") + 1).matches("-?\\d+(\\.\\d+)?")) {
                    frontName = frontName.substring(0, frontName.lastIndexOf("-") + 1) + (Integer.parseInt(frontName.substring(frontName.lastIndexOf("-") + 1)) + 1);
                } else {
                    frontName = frontName + "-1";
                }
                fileName = frontName + fileName.substring(fileName.lastIndexOf("."));
            }
            ftpClient.storeFile(new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1), inputStream);
            inputStream.close();
            ftpClient.logout();
            log.info("上传文件成功；文件名：{}", serviceDec + "/" + fileName);
        } catch (Exception e) {
            log.error("上传文件失败；文件名：{}；异常信息：{}", fileName, e.toString());
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
                if (null != inputStream) {
                    inputStream.close();
                }
            } catch (IOException e) {
                log.error("上传文件失败；异常信息：{}", e.toString());
            }
        }
        return fileName;
    }

    /**
     * 改变目录路径
     *
     * @param ftpClient
     * @param directory
     * @return
     */
    private boolean changeWorkingDirectory(FTPClient ftpClient, String directory) {
        boolean flag = true;
        try {
            flag = ftpClient.changeWorkingDirectory(directory);
            if (flag) {
                log.info("进入文件夹{}成功！", directory);

            } else {
                log.info("进入文件夹{}失败！开始创建文件夹", directory);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return flag;
    }

    /**
     * 创建多层目录文件，如果有ftp服务器已存在该文件，则不创建，如果无，则创建
     *
     * @param ftpClient
     * @param remote
     * @return
     * @throws IOException
     */
    private boolean createDirectory(FTPClient ftpClient, String remote) throws IOException {
        boolean success = true;
        String directory = remote + "/";
        // 如果远程目录不存在，则递归创建远程服务器目录
        if (!directory.equalsIgnoreCase("/") && !changeWorkingDirectory(ftpClient, directory)) {
            int start = 0;
            int end = 0;
            if (directory.startsWith("/")) {
                start = 1;
            } else {
                start = 0;
            }
            end = directory.indexOf("/", start);
            String path = "";
            String paths = "";
            while (true) {
                String subDirectory = new String(remote.substring(start, end).getBytes("GBK"), StandardCharsets.ISO_8859_1);
                path = path + "/" + subDirectory;
                if (!existFile(ftpClient, path)) {
                    if (makeDirectory(ftpClient, subDirectory)) {
                        changeWorkingDirectory(ftpClient, subDirectory);
                    } else {
                        log.info("创建目录[{}]失败", subDirectory);
                        changeWorkingDirectory(ftpClient, subDirectory);
                    }
                } else {
                    changeWorkingDirectory(ftpClient, subDirectory);
                }
                paths = paths + "/" + subDirectory;
                start = end + 1;
                end = directory.indexOf("/", start);
                // 检查所有目录是否创建完毕
                if (end <= start) {
                    break;
                }
            }
        }
        return success;
    }

    /**
     * 判断ftp服务器文件是否存在
     *
     * @param ftpClient
     * @param path
     * @return
     * @throws IOException
     */
    private boolean existFile(FTPClient ftpClient, String path) throws IOException {
        boolean flag = false;
        FTPFile[] ftpFileArr = ftpClient.listFiles(path);
        if (ftpFileArr.length > 0) {
            flag = true;
        }
        return flag;
    }

    /**
     * 创建目录
     *
     * @param ftpClient
     * @param dir
     * @return
     */
    private boolean makeDirectory(FTPClient ftpClient, String dir) {
        boolean flag = true;
        try {
            flag = ftpClient.makeDirectory(dir);
            if (flag) {
                log.info("创建文件夹{}成功！", dir);

            } else {
                log.info("创建文件夹{}失败！", dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 获取FTP某一特定目录下的所有文件名称
     *
     * @param ftpClient  已经登陆成功的FTPClient
     * @param ftpDirPath FTP上的目标文件路径
     * @param type       类型（0：文件夹，1文件，2：全部）
     */
    public List<SysFTPFile> getFileNameList(FTPClient ftpClient, String ftpDirPath, String type) {
        List<SysFTPFile> files = new ArrayList<>();
        try {
            // 通过提供的文件路径获取FTPFile对象列表
            FTPFile[] ftpFiles = ftpClient.listFiles(new String(ftpDirPath.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
            for (FTPFile ftpFile : ftpFiles) {
                SysFTPFile sysFTPFile = new SysFTPFile();
                sysFTPFile.setName(ftpFile.getName());
                sysFTPFile.setType(ftpFile.getType());
                sysFTPFile.setUrl(WebsiteAddressConstants.FTP_SITE_ADDRESS + ftpDirPath.replace("/ftp", "") + "/" + sysFTPFile.getName());
                if ("2".equals(type)) {
                    files.add(sysFTPFile);
                } else if (type.equals(sysFTPFile.getType().toString())) {
                    files.add(sysFTPFile);
                }
                if (sysFTPFile.getType() == 1) {
                    sysFTPFile.setChildren(this.getFileNameList(ftpClient, ftpDirPath + "/" + sysFTPFile.getName(), "2"));
                }
            }
            // 遍历文件列表，打印出文件名称
        } catch (IOException e) {
            log.error("获取FTP某一特定目录下的所有文件名称异常；异常信息{}", e.toString());
        }
        return files;
    }

    /**
     * 获取到服务器文件夹里面最新创建的文件名称
     *
     * @param ftpDirPath 文件路径
     * @param ftpClient  ftp的连接
     * @return fileName
     */
    public String getNewFile(FTPClient ftpClient, String ftpDirPath) throws Exception {

        // 通过提供的文件路径获取FTPFile对象列表
        FTPFile[] files = ftpClient.listFiles(ftpDirPath);
        if (files == null) {
            throw new Exception("文件数组为空");
        }
        Arrays.sort(files, new Comparator<FTPFile>() {
            @Override
            public int compare(FTPFile f1, FTPFile f2) {
                return f1.getTimestamp().compareTo(f2.getTimestamp());
            }

            public boolean equals(Object obj) {
                return true;
            }
        });
        return ftpDirPath + "/" + files[files.length - 1].getName();
    }


    /**
     * 删除服务器的文件
     *
     * @param ftpClient   连接成功且有效的 FTP客户端
     * @param deleteFiles 待删除的文件或者目录，为目录时，会逐个删除，
     *                    路径必须是绝对路径，如 "/1.png"、"/video/3.mp4"、"/images/2018"
     *                    "/" 表示用户根目录,则删除所有内容
     */
    public boolean deleteServerFiles(FTPClient ftpClient, String deleteFiles) {
        boolean deleteFlag = false;
        // 如果 FTP 连接已经关闭，或者连接无效，则直接返回
        if (!ftpClient.isConnected() || !ftpClient.isAvailable()) {
            log.info(">>>>>FTP服务器连接已经关闭或者连接无效*****放弃文件上传****");
            return false;
        }
        try {
            /** 尝试改变当前工作目录到 deleteFiles
             * 1）changeWorkingDirectory：变更FTPClient当前工作目录，变更成功返回true，否则失败返回false
             * 2）如果变更工作目录成功，则表示 deleteFiles 为服务器已经存在的目录
             * 3）否则变更失败，则认为 deleteFiles 是文件，是文件时则直接删除
             */
            boolean changeFlag = ftpClient.changeWorkingDirectory(deleteFiles);
            if (changeFlag) {
                // 当被删除的是目录时
                FTPFile[] ftpFiles = ftpClient.listFiles();
                for (FTPFile ftpFile : ftpFiles) {
                    log.info("----------------::::" + ftpClient.printWorkingDirectory());
                    if (ftpFile.isFile()) {
                        deleteFlag = ftpClient.deleteFile(ftpFile.getName());
                        if (deleteFlag) {
                            log.info(">>>>>删除服务器文件成功****" + ftpFile.getName());
                        } else {
                            log.info(">>>>>删除服务器文件失败****" + ftpFile.getName());
                        }
                    } else {
                        /**printWorkingDirectory：获取 FTPClient 客户端当前工作目录
                         * 然后开始迭代删除子目录
                         */
                        String workingDirectory = ftpClient.printWorkingDirectory();
                        deleteServerFiles(ftpClient, workingDirectory + "/" + ftpFile.getName());
                    }
                }
                /**printWorkingDirectory：获取 FTPClient 客户端当前工作目录
                 * removeDirectory：删除FTP服务端的空目录，注意如果目录下存在子文件或者子目录，则删除失败
                 * 运行到这里表示目录下的内容已经删除完毕，此时再删除当前的为空的目录，同时将工作目录移动到上移层级
                 * */
                String workingDirectory = ftpClient.printWorkingDirectory();
                ftpClient.removeDirectory(workingDirectory);
                ftpClient.changeToParentDirectory();
            } else {
                /**deleteFile：删除FTP服务器上的文件
                 * 1）只用于删除文件而不是目录，删除成功时，返回 true
                 * 2）删除目录时无效,方法返回 false
                 * 3）待删除文件不存在时，删除失败，返回 false
                 * */
                deleteFlag = ftpClient.deleteFile(deleteFiles);
                if (deleteFlag) {
                    log.info(">>>>>删除服务器文件成功****" + deleteFiles);
                } else {
                    log.info(">>>>>删除服务器文件失败****" + deleteFiles);
                }
            }
        } catch (IOException e) {
            log.error("删除服务器文件失败异常，异常信息：{}", e.toString());
        }
        return deleteFlag;
    }
}
