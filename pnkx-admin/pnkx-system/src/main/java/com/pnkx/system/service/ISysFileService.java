package com.pnkx.system.service;

import com.pnkx.system.domain.SysFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * @author by PHY
 * @Classname ISysFileService
 * @date 2021-06-18 16:13
 */
public interface ISysFileService {

    /**
     * 上传到ftp
     * @param path 路径
     * @param file 文件
     * @param fileName 文件名
     * @return
     */
    String uploadFile(File file, String path, String fileName);

    /**
     * 上传到ftp
     * @param file 文件
     * @param path 指定路径
     * @return
     */
    String uploadMultipartFile(MultipartFile file, String path);

    /**
     * 查询文件记录
     *
     * @param id 文件记录ID
     * @return 文件记录
     */
    public SysFile selectSysFileById(Long id);

    /**
     * 查询文件记录列表
     *
     * @param sysFile 文件记录
     * @return 文件记录集合
     */
    public List<SysFile> selectSysFileList(SysFile sysFile);

    /**
     * 新增文件记录
     *
     * @param sysFile 文件记录
     * @return 结果
     */
    public int insertSysFile(SysFile sysFile);

    /**
     * 修改文件记录
     *
     * @param sysFile 文件记录
     * @return 结果
     */
    public int updateSysFile(SysFile sysFile);

    /**
     * 批量删除文件记录
     *
     * @param ids 需要删除的文件记录ID
     * @return 结果
     */
    public int deleteSysFileByIds(Long[] ids);

    /**
     * 删除文件记录信息
     *
     * @param id 文件记录ID
     * @return 结果
     */
    public int deleteSysFileById(Long id);

    /**
     * 获取随机图片
     * @param number 数量
     * @return 随机图片
     */
    List<SysFile> getRandomPicture(Integer number);

    /**
     * 浏览+1
     * @param id 文件记录ID
     * @return 结果
     */
    Boolean browse(Long id);

    /**
     * 点赞+1
     * @param id 文件记录ID
     * @return 结果
     */
    Boolean like(Long id);

    /**
     * 获取ftp文件
     * @param path 路径
     * @return 文件
     */
    InputStream getFtpFile(String path);
}
