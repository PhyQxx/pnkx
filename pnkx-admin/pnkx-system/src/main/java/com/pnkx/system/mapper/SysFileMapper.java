package com.pnkx.system.mapper;

import com.pnkx.system.domain.SysFile;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author by PHY
 * @Classname SysFileMapper
 * @date 2021-06-18 16:16
 */

@Mapper
public interface SysFileMapper {
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
     * 删除文件记录
     *
     * @param id 文件记录ID
     * @return 结果
     */
    public int deleteSysFileById(Long id);

    /**
     * 批量删除文件记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteSysFileByIds(Long[] ids);

    /**
     * 查询文件记录
     *
     * @param ids 文件记录ID
     * @return 文件记录
     */
    public List<SysFile> selectSysFileByIds(Long[] ids);

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
}
