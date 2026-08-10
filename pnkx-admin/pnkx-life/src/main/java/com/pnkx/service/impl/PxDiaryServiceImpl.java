package com.pnkx.service.impl;

import com.pnkx.ai.AiClient;
import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxDiary;
import com.pnkx.mapper.PxDiaryMapper;
import com.pnkx.service.IPxDiaryService;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author PHY
 * @classname PxDiaryServiceImpl
 * @data 2021/12/30 0030 17:54
 * @description 日记Service业务层处理
 */
@Service
public class PxDiaryServiceImpl implements IPxDiaryService {

    private static final Logger logger = LoggerFactory.getLogger(PxDiaryServiceImpl.class);

    @Resource
    private PxDiaryMapper pxDiaryMapper;

    @Resource
    private AiClient aiClient;

    @Resource
    private DataPermissionService dataPermissionService;

    private static final String HTML_TAG_REGEX = "(<([^>]+)>)";

    /**
     * 查询日记
     *
     * @param id 日记ID
     * @return 日记
     */
    @Override
    public PxDiary selectPxDiaryById(Long id) {
        return pxDiaryMapper.selectPxDiaryById(id);
    }

    /**
     * 查询日记列表
     *
     * @param pxDiary 日记
     * @return 日记
     */
    @DataScopeSelf
    @Override
    public List<PxDiary> selectPxDiaryList(PxDiary pxDiary) {
        return pxDiaryMapper.selectPxDiaryList(pxDiary);
    }


    /**
     * 新增日记
     *
     * @param pxDiary 日记
     * @return 结果
     */
    @Override
    public int insertPxDiary(PxDiary pxDiary) {
        pxDiary.setCreateTime(DateUtils.getNowDate());
        return pxDiaryMapper.insertPxDiary(pxDiary);
    }

    /**
     * 修改日记
     *
     * @param pxDiary 日记
     * @return 结果
     */
    @Override
    public int updatePxDiary(PxDiary pxDiary) {
        pxDiary.setUpdateTime(DateUtils.getNowDate());
        return pxDiaryMapper.updatePxDiary(pxDiary);
    }

    /**
     * 批量删除日记
     *
     * @param ids 需要删除的日记ID
     * @return 结果
     */
    @Override
    public int deletePxDiaryByIds(Long[] ids) {
        return pxDiaryMapper.deletePxDiaryByIds(ids);
    }

    /**
     * 删除日记信息
     *
     * @param id 日记ID
     * @return 结果
     */
    @Override
    public int deletePxDiaryById(Long id) {
        return pxDiaryMapper.deletePxDiaryById(id);
    }

    /**
     * 全局检索日记
     * @param searchCode
     * @return
     */
    @Override
    public List<PxDiary> retrieval(String searchCode) {
        PxDiary query = new PxDiary();
        query.setSearchValue(searchCode);
        // 数据权限：管理员不限；否则仅本人+群组成员
        List<Long> visibleUserIds = dataPermissionService.getVisibleUserIds();
        if (visibleUserIds == null) {
            query.getParams().put(DataScopeSelf.SCOPE_ALL, true);
        } else {
            query.getParams().put(DataScopeSelf.SCOPE_ALL, false);
            query.getParams().put(DataScopeSelf.SCOPE_USER_IDS, visibleUserIds);
        }
        return pxDiaryMapper.retrieval(query);
    }

    @Override
    public void aiAnalysisStream(Boolean isAll, Consumer<String> onChunk, Runnable onError) {
        PxDiary query = new PxDiary();
        if (isAll == null || !isAll) {
            query.setDate(DateUtils.getNowDate());
            try {
                query.setCreateBy(SecurityUtils.getUserId());
            } catch (Exception e) {
                logger.warn("无法获取当前用户ID", e);
            }
        }

        List<PxDiary> diaryList = pxDiaryMapper.selectPxDiaryList(query);

        StringBuilder question = new StringBuilder();
        question.append("请分析以下日记数据，总结情感趋势、生活主题和提供生活建议：\n\n");
        for (PxDiary diary : diaryList) {
            question.append("日期：").append(diary.getDate() != null ? DateUtils.parseDateToStr("yyyy-MM-dd", diary.getDate()) : "未知")
                    .append(" | 心情：").append(diary.getMood() != null ? diary.getMood() : "未记录")
                    .append(" | 天气：").append(diary.getWeather() != null ? diary.getWeather() : "未记录")
                    .append("\n");
            if (diary.getContent() != null) {
                String plainText = diary.getContent().replaceAll(HTML_TAG_REGEX, "").trim();
                if (!plainText.isEmpty()) {
                    question.append(plainText.length() > 500 ? plainText.substring(0, 500) + "..." : plainText);
                }
            }
            question.append("\n\n");
        }
        question.append("请从以下维度分析：\n");
        question.append("1. 情感趋势：整体情绪走向，是否有明显变化\n");
        question.append("2. 生活主题：日记中反复出现的关键话题\n");
        question.append("3. 建议：基于日记内容给出积极的生活建议\n");

        logger.info("AI日记流式分析，日记数量：{}", diaryList.size());

        Flux<ChatResponse> stream = aiClient.chatStream(
                "你是一个善于倾听和分析的AI日记助手，请用温暖、共情的语气分析用户的日记内容，用中文回复。",
                question.toString()
        );
        stream.subscribe(
                resp -> {
                    if (resp.getContent() != null) {
                        for (ContentBlock block : resp.getContent()) {
                            if (block instanceof TextBlock textBlock) {
                                String text = textBlock.getText();
                                if (text != null && !text.isEmpty()) {
                                    onChunk.accept(text);
                                }
                            }
                        }
                    }
                },
                error -> {
                    logger.error("AI日记分析失败: {}", error.getMessage());
                    onError.run();
                },
                () -> {
                    try {
                        onChunk.accept("[DONE]");
                    } catch (Exception ignored) {
                    }
                }
        );
    }
}
