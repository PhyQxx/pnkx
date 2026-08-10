package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxBook;
import com.pnkx.domain.po.PxBookChapter;
import com.pnkx.domain.po.PxBookTxtPreview;
import com.pnkx.service.IPxBookService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 我的书城。
 */
@RestController
@RequestMapping("/myBook")
@Validated
public class PxBookController extends BaseController {
    @Resource
    private IPxBookService bookService;

    @GetMapping("/list")
    public TableDataInfo list(PxBook book) {
        book.setCreateBy(SecurityUtils.getUserId());
        startPage();
        return getDataTable(bookService.selectBookList(book));
    }

    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(bookService.selectBookById(id, SecurityUtils.getUserId()));
    }

    @Log(title = "我的书城", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody PxBook book) {
        book.setCreateBy(SecurityUtils.getUserId());
        return toAjax(bookService.insertBook(book));
    }

    @Log(title = "我的书城", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody PxBook book) {
        book.setUpdateBy(SecurityUtils.getUserId());
        return toAjax(bookService.updateBook(book));
    }

    @Log(title = "我的书城", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(bookService.deleteBooks(ids, SecurityUtils.getUserId()));
    }

    @GetMapping("/chapter/list")
    public TableDataInfo chapterList(PxBookChapter chapter) {
        chapter.setCreateBy(SecurityUtils.getUserId());
        startPage();
        List<PxBookChapter> list = bookService.selectChapterList(chapter);
        return getDataTable(list);
    }

    @GetMapping("/chapter/{id}")
    public AjaxResult getChapter(@PathVariable Long id) {
        return AjaxResult.success(bookService.selectChapterById(id, SecurityUtils.getUserId()));
    }

    @GetMapping("/chapter/{id}/reader")
    public AjaxResult reader(@PathVariable Long id) {
        return AjaxResult.success(bookService.selectReaderData(id, SecurityUtils.getUserId()));
    }

    @PutMapping("/progress/{chapterId}")
    public AjaxResult updateReadingProgress(@PathVariable Long chapterId) {
        return toAjax(bookService.updateReadingProgress(chapterId, SecurityUtils.getUserId()));
    }

    @Log(title = "书籍章节", businessType = BusinessType.INSERT)
    @PostMapping("/chapter")
    public AjaxResult addChapter(@Valid @RequestBody PxBookChapter chapter) {
        chapter.setCreateBy(SecurityUtils.getUserId());
        int rows = bookService.insertChapter(chapter);
        return rows > 0 ? AjaxResult.success(chapter.getId()) : AjaxResult.error();
    }

    @Log(title = "书籍章节", businessType = BusinessType.INSERT)
    @PostMapping("/chapter/batch")
    public AjaxResult addChapters(
            @Size(min = 1, max = 100, message = "每次可批量新增1至100个章节")
            @RequestBody List<@Valid PxBookChapter> chapters) {
        String userId = SecurityUtils.getUserId();
        for (PxBookChapter chapter : chapters) {
            chapter.setCreateBy(userId);
        }
        return toAjax(bookService.insertChapters(chapters));
    }

    @Log(title = "书籍章节", businessType = BusinessType.UPDATE)
    @PutMapping("/chapter")
    public AjaxResult editChapter(@Valid @RequestBody PxBookChapter chapter) {
        chapter.setUpdateBy(SecurityUtils.getUserId());
        return toAjax(bookService.updateChapter(chapter));
    }

    @Log(title = "书籍章节", businessType = BusinessType.DELETE)
    @DeleteMapping("/chapter/{ids}")
    public AjaxResult removeChapter(@PathVariable Long[] ids) {
        return toAjax(bookService.deleteChapters(ids, SecurityUtils.getUserId()));
    }

    @PostMapping("/txt/preview")
    public AjaxResult previewTxt(@RequestParam("file") MultipartFile file) throws IOException {
        PxBookTxtPreview preview = bookService.previewTxt(file.getBytes(), file.getOriginalFilename());
        return AjaxResult.success(preview);
    }

    @Log(title = "TXT书籍导入", businessType = BusinessType.IMPORT)
    @PostMapping("/txt/import")
    public AjaxResult importTxt(@RequestParam("file") MultipartFile file,
                                @RequestParam(value = "title", required = false) String title,
                                @RequestParam(value = "author", required = false) String author,
                                @RequestParam(value = "status", required = false) String status) throws IOException {
        PxBook book = new PxBook();
        book.setTitle(title);
        book.setAuthor(author);
        book.setStatus(status);
        book.setCreateBy(SecurityUtils.getUserId());
        PxBook imported = bookService.importTxt(file.getBytes(), file.getOriginalFilename(), book);
        return AjaxResult.success("导入成功", imported);
    }

    @Log(title = "TXT书籍批量导入", businessType = BusinessType.IMPORT)
    @PostMapping("/txt/import/batch")
    public AjaxResult importTxtBatch(@RequestParam("files") MultipartFile[] files,
                                     @RequestParam("titles") String[] titles,
                                     @RequestParam("authors") String[] authors,
                                     @RequestParam("statuses") String[] statuses) throws IOException {
        if (files == null || files.length == 0 || files.length > 20) {
            throw new ServiceException("每次可批量导入1至20本书");
        }
        if (titles == null || authors == null || statuses == null
                || titles.length != files.length || authors.length != files.length || statuses.length != files.length) {
            throw new ServiceException("批量导入参数数量不一致");
        }
        String userId = SecurityUtils.getUserId();
        List<byte[]> contents = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        List<PxBook> books = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            contents.add(files[i].getBytes());
            fileNames.add(files[i].getOriginalFilename());
            PxBook book = new PxBook();
            book.setTitle(titles[i]);
            book.setAuthor(authors[i]);
            book.setStatus(statuses[i]);
            book.setCreateBy(userId);
            books.add(book);
        }
        List<PxBook> imported = bookService.importTxtBatch(contents, fileNames, books);
        int chapterCount = imported.stream()
                .map(PxBook::getChapterCount)
                .filter(count -> count != null)
                .mapToInt(Integer::intValue)
                .sum();
        AjaxResult result = AjaxResult.success("批量导入成功", imported);
        result.put("bookCount", imported.size());
        result.put("chapterCount", chapterCount);
        return result;
    }

    @Log(title = "TXT书籍导出", businessType = BusinessType.EXPORT)
    @GetMapping("/txt/export/{bookId}")
    public void exportTxt(@PathVariable Long bookId, HttpServletResponse response) throws IOException {
        PxBook book = bookService.selectBookById(bookId, SecurityUtils.getUserId());
        String content = bookService.exportTxt(bookId, SecurityUtils.getUserId());
        String title = book == null || book.getTitle() == null ? "book" : book.getTitle();
        String encodedName = URLEncoder.encode(title + ".txt", StandardCharsets.UTF_8.name()).replace("+", "%20");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Access-Control-Expose-Headers", HttpHeaders.CONTENT_DISPOSITION);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"book.txt\"; filename*=UTF-8''" + encodedName);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        response.getOutputStream().write(bytes);
    }

    @Log(title = "TXT书籍批量导出", businessType = BusinessType.EXPORT)
    @PostMapping("/txt/export/batch")
    public void exportTxtBatch(@Size(min = 1, max = 100, message = "每次可批量导出1至100本书")
                               @RequestBody Long[] bookIds,
                               HttpServletResponse response) throws IOException {
        if (bookIds == null || bookIds.length == 0) {
            throw new ServiceException("请至少选择一本书");
        }
        Set<Long> distinctIds = new LinkedHashSet<>();
        for (Long bookId : bookIds) {
            if (bookId == null) {
                throw new ServiceException("书籍ID不能为空");
            }
            distinctIds.add(bookId);
        }
        String userId = SecurityUtils.getUserId();
        List<PxBook> books = new ArrayList<>();
        for (Long bookId : distinctIds) {
            PxBook book = bookService.selectBookById(bookId, userId);
            if (book == null) {
                throw new ServiceException("存在无权导出或已删除的书籍");
            }
            books.add(book);
        }

        String zipName = "我的书城_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".zip";
        String encodedName = URLEncoder.encode(zipName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        response.setContentType("application/zip");
        response.setHeader("Access-Control-Expose-Headers", HttpHeaders.CONTENT_DISPOSITION);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"books.zip\"; filename*=UTF-8''" + encodedName);

        Set<String> usedNames = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream())) {
            for (PxBook book : books) {
                String entryName = uniqueTxtName(book.getTitle(), usedNames);
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                zip.write(bookService.exportTxt(book.getId(), userId).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    private String uniqueTxtName(String title, Set<String> usedNames) {
        String base = title == null ? "book" : title.trim();
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (base.isEmpty()) base = "book";
        if (base.length() > 180) base = base.substring(0, 180);
        String name = base + ".txt";
        int index = 2;
        while (!usedNames.add(name.toLowerCase(Locale.ROOT))) {
            name = base + "(" + index++ + ").txt";
        }
        return name;
    }
}
