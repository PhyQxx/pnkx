package com.pnkx.system.service.impl;

import com.pnkx.common.constant.WebsiteAddressConstants;
import com.pnkx.common.ftp.FtpTool;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.file.FileTypeUtils;
import com.pnkx.common.utils.file.MimeTypeUtils;
import com.pnkx.system.domain.SysFile;
import com.pnkx.system.mapper.SysFileMapper;
import com.pnkx.system.service.ISysFileService;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * @author by PHY
 * @Classname SysFileServiceImpl
 * @date 2021-06-18 16:14
 */
@Service
public class SysFileServiceImpl implements ISysFileService {

    protected final Logger logger = LoggerFactory.getLogger(SysFileServiceImpl.class);

    @Resource
    private SysFileMapper sysFileMapper;
    @Resource
    private FtpTool ftpTool;

    /**
     * 上传到ftp
     * @param file 文件
     * @param path 路径
     * @param fileName 文件名
     * @return
     */
    @Override
    public String uploadFile(File file, String path, String fileName) {
        FTPClient ftpClient = ftpTool.connectFtp();
        LocalDate date = LocalDate.now();
        if (StringUtils.isEmpty(path)) {
            path = date.toString().replace("-", "/");
        }
        String name = ftpTool.uploadFile(ftpClient, path, fileName, file);
        return "/ftp/" + path + "/" + name;
    }

    /**
     * 上传到ftp
     * @param file 文件
     * @param path 指定路径
     * @return
     */
    @Override
    public String uploadMultipartFile(MultipartFile file, String path) {
        FTPClient ftpClient = ftpTool.connectFtp();
        LocalDate date = LocalDate.now();
        if (StringUtils.isEmpty(path)) {
            path = date.toString().replace("-", "/");
        }
        String name = ftpTool.uploadMultipartFile(ftpClient, path, file.getOriginalFilename(), file);
        return "/ftp/" + path + "/" + name;
    }

    /**
     * 查询文件记录
     *
     * @param id 文件记录ID
     * @return 文件记录
     */
    @Override
    public SysFile selectSysFileById(Long id) {
        return sysFileMapper.selectSysFileById(id);
    }

    /**
     * 查询文件记录列表
     *
     * @param sysFile 文件记录
     * @return 文件记录
     */
    @Override
    public List<SysFile> selectSysFileList(SysFile sysFile) {
        return sysFileMapper.selectSysFileList(sysFile);
    }

    /**
     * 新增文件记录
     *
     * @param sysFile 文件记录
     * @return 结果
     */
    @Override
    public int insertSysFile(SysFile sysFile) {
        sysFile.setCreateBy(SecurityUtils.getUserId());
        sysFile.setCreateTime(DateUtils.getNowDate());
        return sysFileMapper.insertSysFile(sysFile);
    }

    /**
     * 修改文件记录
     *
     * @param sysFile 文件记录
     * @return 结果
     */
    @Override
    public int updateSysFile(SysFile sysFile) {
        sysFile.setUpdateBy(SecurityUtils.getUserName());
        sysFile.setUpdateTime(DateUtils.getNowDate());
        return sysFileMapper.updateSysFile(sysFile);
    }

    /**
     * 批量删除文件记录
     *
     * @param ids 需要删除的文件记录ID
     * @return 结果
     */
    @Override
    public int deleteSysFileByIds(Long[] ids) {
        List<SysFile> sysFileDaoList = sysFileMapper.selectSysFileByIds(ids);
        sysFileDaoList.forEach(this::deleteFtpFilesByIds);
        return sysFileMapper.deleteSysFileByIds(ids);
    }

    /**
     * 删除文件记录信息
     *
     * @param id 文件记录ID
     * @return 结果
     */
    @Override
    public int deleteSysFileById(Long id) {
        return sysFileMapper.deleteSysFileById(id);
    }

    /**
     * 获取随机图片
     * @param number 数量
     * @return 随机图片
     */
    @Override
    public List<SysFile> getRandomPicture(Integer number) {
        return sysFileMapper.getRandomPicture(number);
    }

    /**
     * 浏览+1
     * @param id 文件记录ID
     * @return 结果
     */
    @Override
    public Boolean browse(Long id) {
        return sysFileMapper.browse(id);
    }

    /**
     * 点赞+1
     * @param id 文件记录ID
     * @return 结果
     */
    @Override
    public Boolean like(Long id) {
        return sysFileMapper.like(id);
    }

    @Override
    public InputStream getFtpFile(String path) {
        FTPClient ftpClient = ftpTool.connectFtp();
        return ftpTool.previewFile(ftpClient, path);
    }

    public void deleteFtpFilesByIds (SysFile file) {
        FTPClient ftpClient = ftpTool.connectFtp();
        ftpTool.deleteServerFiles(ftpClient, file.getPath());
        if (Arrays.asList(MimeTypeUtils.IMAGE_EXTENSION).contains(FileTypeUtils.getFileType(file.getName()))) {
            try {
                ftpTool.deleteServerFiles(ftpClient, file.getThumbnail().replace(WebsiteAddressConstants.FTP_SITE_ADDRESS, ""));
            } catch (Exception e) {
                logger.error("删除文件失败，文件名：{}，异常信息：{}", file.getThumbnail(),e.getMessage());
            }
        }
    }
}
