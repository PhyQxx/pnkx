package com.pnkx.common.core.controller;

import java.beans.PropertyEditorSupport;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnkx.common.constant.HttpStatus;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.PageDomain;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.core.page.TableSupport;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.sql.SqlUtil;

/**
 * web层通用数据处理
 *
 * @author phy
 */
public class BaseController {
    protected final Logger logger = LoggerFactory.getLogger(BaseController.class);

    /**
     * 当前线程的分页对象，用于兼容 {@link #startPage()} 的 ThreadLocal 模式。
     * 调用 {@link #getPage()} 取出后应立即清理，避免线程复用串号。
     */
    private static final ThreadLocal<Page<?>> PAGE_HOLDER = new ThreadLocal<>();

    /**
     * 将前台传递过来的日期格式的字符串，自动转化为Date类型
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Date 类型转换
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(DateUtils.parseDate(text));
            }
        });
    }

    /**
     * 设置请求分页数据（ThreadLocal 兼容写法，等价于原 PageHelper.startPage）
     * <p>
     * 调用后紧接着执行查询，并通过 {@link #getDataTable(IPage)} 或 {@link #getPage()} 取出分页结果。
     */
    protected void startPage() {
        buildPage();
    }

    /**
     * 根据请求参数构造 MyBatis-Plus 分页对象，并缓存到当前线程。
     * <p>
     * 推荐用法：{@code Page<T> page = buildPage(); service.selectXxxList(page, entity); return getDataTable(page);}
     *
     * @param <T> 结果记录类型
     * @return 当前线程的分页对象
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <T> Page<T> buildPage() {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        Page<T> page = new Page<>();
        if (StringUtils.isNotNull(pageNum)) {
            page.setCurrent(pageNum);
        }
        if (StringUtils.isNotNull(pageSize)) {
            page.setSize(pageSize);
        }
        // 排序：复用原有 SqlUtil.escapeOrderBySql 做 SQL 注入防护
        String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
        if (StringUtils.isNotEmpty(orderBy)) {
            String[] parts = orderBy.split("\\s+");
            String column = parts[0];
            boolean asc = parts.length < 2 || !StringUtils.equalsIgnoreCase(parts[1], "desc");
            page.addOrder(asc ? OrderItem.asc(column) : OrderItem.desc(column));
        }
        PAGE_HOLDER.set(page);
        return page;
    }

    /**
     * 获取当前线程缓存的分页对象，类型由调用方指定。
     * 主要用于 Service/Mapper 需要显式拿到 IPage 参数的场景。
     */
    @SuppressWarnings("unchecked")
    public static <T> Page<T> getPage() {
        return (Page<T>) PAGE_HOLDER.get();
    }

    /**
     * 手动设置当前线程的分页对象。
     * <p>
     * 用于非标准分页场景（如分页参数来自 @RequestParam 而非前端统一参数），
     * 调用方自行构造 {@link Page} 后通过本方法写入 ThreadLocal，
     * 随后调用单参数查询方法，由分页拦截器自动应用分页。
     *
     * @param page 分页对象
     */
    public static void setPage(Page<?> page) {
        PAGE_HOLDER.set(page);
    }

    /**
     * 清理当前线程的分页对象，避免线程复用串号
     */
    public static void clearPage() {
        PAGE_HOLDER.remove();
    }

    /**
     * 响应请求分页数据（MyBatis-Plus 分页结果）
     *
     * @param page 分页结果
     * @return 表格数据
     */
    protected TableDataInfo getDataTable(IPage<?> page) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(page.getRecords());
        rspData.setTotal(page.getTotal());
        clearPage();
        return rspData;
    }

    /**
     * 响应请求分页数据（兼容 List 入参，常用于 export 等非分页场景）
     *
     * @param list 列表数据
     * @return 表格数据
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected TableDataInfo getDataTable(List<?> list) {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        // 若当前线程有分页上下文（如走 buildPage 后调用 List 形式查询），则取其总数；否则视为全量查询
        Page<?> page = getPage();
        rspData.setTotal(page != null ? page.getTotal() : list.size());
        clearPage();
        return rspData;
    }

    /**
     * 响应返回结果
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 页面跳转
     */
    public String redirect(String url) {
        return StringUtils.format("redirect:{}", url);
    }
}
