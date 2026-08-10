package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.core.domain.entity.SysDictData;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.bean.BeanUtils;
import com.pnkx.domain.po.PxArticle;
import com.pnkx.domain.po.PxArticleType;
import com.pnkx.domain.vo.PxArticleTypeVo;
import com.pnkx.domain.vo.PxArticleVo;
import com.pnkx.mapper.PxArticleMapper;
import com.pnkx.mapper.PxStatisticsMapper;
import com.pnkx.service.IPxArticleService;
import com.pnkx.system.mapper.SysDictDataMapper;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文章Service业务层处理
 *
 * @author phy
 * @date 2021-01-26
 */
@Service
public class PxArticleServiceImpl implements IPxArticleService {
    @Resource
    private PxArticleMapper pxArticleMapper;
    @Resource
    private PxStatisticsMapper pxStatisticsMapper;
    @Resource
    SysDictDataMapper dictDataMapper;
    /**
     * 文章图片正则匹配规则
     */
    Pattern compile = Pattern.compile("<img.*?>");
    /**
     * img src正则
     */
    Pattern pSrc = Pattern.compile("(src|SRC)=(\"|\')(.*?)(\"|\')");

    /**
     * 获取文章根据文章信息
     *
     * @param id 参数
     * @return 文章列表
     */
    @Override
    public PxArticleVo getArticleById(String id) {
        /*文章访问次数更新*/
        pxArticleMapper.updateVisits(id);
        PxArticleVo article = pxArticleMapper.getArticleById(id);
        articleSetCover(article);
        PxArticle lastArticle = pxArticleMapper.getLastArticleById(id);
        if (StringUtils.isNotNull(lastArticle)) {
            articleSetCover(lastArticle);
            article.setLastArticle(lastArticle);
        }
        PxArticle nextArticle = pxArticleMapper.getNextArticleById(id);
        if (StringUtils.isNotNull(nextArticle)) {
            articleSetCover(nextArticle);
            article.setNextArticle(nextArticle);
        }
        return article;
    }

    /**
     * 查询文章列表
     *
     * @param pxArticle 文章
     * @return 文章
     */
    @DataScopeSelf(alias = "a")
    @Override
    public List<PxArticleVo> selectPxArticleList(PxArticle pxArticle) {
        return pxArticleMapper.selectPxArticleList(pxArticle);
    }

    /**
     * 新增文章
     *
     * @param pxArticle 文章
     * @return 结果
     */
    @Override
    public Integer insertPxArticle(PxArticleVo pxArticle) {
        pxArticle.setCreateTime(DateUtils.getNowDate());
        pxArticle.setCreateBy(SecurityUtils.getUserId());
        pxArticleMapper.insertPxArticle(pxArticle);
        return pxArticle.getId();
    }

    /**
     * 修改文章
     *
     * @param pxArticle 文章
     * @return 结果
     */
    @Override
    public int updatePxArticle(PxArticleVo pxArticle) {
        pxArticle.setUpdateTime(DateUtils.getNowDate());
        pxArticle.setUpdateBy(SecurityUtils.getUserName());
        return pxArticleMapper.updatePxArticle(pxArticle);
    }

    /**
     * 批量删除文章
     *
     * @param ids 需要删除的文章ID
     * @return 结果
     */
    @Override
    public int deletePxArticleByIds(String[] ids) {
        return pxArticleMapper.deletePxArticleByIds(ids);
    }

    /**
     * 删除文章信息
     *
     * @param id 文章ID
     * @return 结果
     */
    @Override
    public int deletePxArticleById(String id) {
        return pxArticleMapper.deletePxArticleById(id);
    }

    /**
     * 校验字典项标签、键值唯一性
     *
     * @param dictData
     * @return
     */
    @Override
    public Integer dictDataCheckUniqueness(SysDictData dictData) {
        return pxArticleMapper.dictDataCheckUniqueness(dictData);
    }

    /**
     * 查询文章列表不包含内容
     *
     * @param pxArticle
     * @return
     */
    @DataScopeSelf(alias = "a")
    @Override
    public List<PxArticleVo> selectPxArticleNotContent(PxArticleVo pxArticle) {
        return pxArticleMapper.selectPxArticleNotContent(pxArticle);
    }

    /**
     * 查询文章列表格式化内容
     *
     * @param pxArticle
     * @return
     */
    @DataScopeSelf(alias = "a")
    @Override
    public List<PxArticleVo> selectPxArticleOrdinaryContent(PxArticleVo pxArticle) {
        List<PxArticleVo> pxArticleVos = pxArticleMapper.selectPxArticleList(pxArticle);
        pxArticleVos.forEach(item -> {
            articleSetCover(item);
            if (item.getRichText() != null) {
                item.setRichText(Jsoup.parse(item.getRichText()).text());
            }
            if (item.getContent() != null) {
                item.setContent(Jsoup.parse(item.getContent()).text());
            }
        });
        return pxArticleVos;
    }

    /**
     * 获取首页最热文章
     *
     * @return
     */
    @Override
    public List<Map<String, Object>> getHotArticle() {
        return pxArticleMapper.getHotArticle();
    }

    /**
     * 文章按类型分组
     * @return 结果
     */
    @Override
    public List<PxArticleType> getArticleListGroupByType() {
        return pxStatisticsMapper.getArticlePieData();
    }

    /**
     * 设置文章封面
     *
     * @param article 文章实体
     */
    private void articleSetCover(PxArticle article) {
        if (StringUtils.isEmpty(article.getCover())) {
            String src = "";
            Matcher matcher = compile.matcher(article.getContent());
            if (matcher.find()) {
                while (matcher.find()) {
                    // 获取到匹配的<img />标签中的内容
                    String strImg = matcher.group(0);
                    // 开始匹配<img />标签中的src
                    Matcher mSrc = pSrc.matcher(strImg);
                    if (mSrc.find()) {
                        src = mSrc.group(3);
                    }
                }
            }
            article.setCover(src);
        }
    }

    /**
     * 获取类型下文章数量
     * @param dictData
     * @return
     */
    @Override
    public List<PxArticleTypeVo> selectPxArticleByType(SysDictData dictData) {
        List<SysDictData> list = dictDataMapper.selectDictDataList(dictData);
        List<PxArticleTypeVo> result = new ArrayList<>();
        list.forEach(item -> {
            PxArticleTypeVo pxArticleTypeVo = new PxArticleTypeVo();
            BeanUtils.copyBeanProp(pxArticleTypeVo, item);
            pxArticleTypeVo.setArticleNumber(pxArticleMapper.selectPxArticleNumberByType(pxArticleTypeVo.getDictValue()));
            result.add(pxArticleTypeVo);
        });
        return result;
    }

    /**
     * 获取文章标签列表
     * @return
     */
    @Override
    public List<String> getLabelList() {
        List<String> result = new ArrayList<>();
        List<String> labelList = pxArticleMapper.getLabelList();
        labelList.forEach(item -> {
            String[] split = item.split(",");
            for (String s : split) {
                if (!result.contains(s)) {
                    result.add(s);
                }
            }
        });
        return result;
    }

    @Override
    public String getArticleUrls() {
        List<PxArticleVo> pxArticleVos = pxArticleMapper.selectPxArticleNotContent(new PxArticleVo());
        StringBuilder sb = new StringBuilder();
        for (PxArticleVo articleVo : pxArticleVos) {
            sb.append("https://pnkx.top/post/").append(articleVo.getId()).append("\n");
        }
        return sb.toString();
    }
}
