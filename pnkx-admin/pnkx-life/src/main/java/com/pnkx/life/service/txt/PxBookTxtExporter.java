package com.pnkx.life.service.txt;

import com.pnkx.life.domain.po.PxBook;
import com.pnkx.life.domain.po.PxBookChapter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 书籍 TXT 导出器
 * <p>
 * 从 PxBookServiceImpl 拆出的导出拼装职责：把书与章节列表拼成
 * 【书名】/【作者】/【章节】格式的纯文本。无数据库访问。
 *
 * @author phy
 */
@Component
public class PxBookTxtExporter {

    /**
     * 拼接导出文本（章节按传入顺序）
     */
    public String export(PxBook book, List<PxBookChapter> chapters) {
        StringBuilder output = new StringBuilder();
        output.append("【书名】").append(book.getTitle()).append('\n');
        if (trimToNull(book.getAuthor()) != null) {
            output.append("【作者】").append(book.getAuthor().trim()).append('\n');
        }
        output.append('\n');
        for (PxBookChapter chapter : chapters) {
            output.append("【章节】").append(chapter.getChapterName().trim()).append("\n\n");
            if (chapter.getContent() != null) {
                output.append(chapter.getContent().trim());
            }
            output.append("\n\n");
        }
        return output.toString();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
