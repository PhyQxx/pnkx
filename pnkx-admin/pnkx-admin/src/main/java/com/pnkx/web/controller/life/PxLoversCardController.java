package com.pnkx.web.controller.life;

import com.pnkx.common.annotation.Log;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.common.enums.BusinessType;
import com.pnkx.common.utils.ExcelUtil;
import com.pnkx.domain.po.PxCardRecord;
import com.pnkx.domain.po.PxLoversCard;
import com.pnkx.domain.vo.PxCardRecordVo;
import com.pnkx.service.IPxLoversCardService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 情侣卡券Controller
 *
 * @author pnkx
 * @date 2022-05-21
 */
@RestController
@RequestMapping("/px/card")
public class PxLoversCardController extends BaseController {
    @Resource
    private IPxLoversCardService pxLoversCardService;

    /**
     * 查询情侣卡券列表
     */
    @GetMapping("/list")
    public TableDataInfo list(PxLoversCard pxLoversCard) {
        startPage();
        List<PxLoversCard> list = pxLoversCardService.selectPxLoversCardList(pxLoversCard);
        return getDataTable(list);
    }

    /**
     * 查询情侣卡券使用记录列表
     */
    @GetMapping("/listRecord")
    public TableDataInfo listRecord(PxCardRecordVo pxCardRecordVo) {
        startPage();
        List<PxCardRecordVo> list = pxLoversCardService.selectPxLoversCardRecordList(pxCardRecordVo);
        return getDataTable(list);
    }

    /**
     * 查询情侣卡券使用记录
     */
    @GetMapping("/record/{id}")
    public AjaxResult getRecordById(@PathVariable("id") Long id) {
        return AjaxResult.success(pxLoversCardService.selectPxCardRecordById(id));
    }

    /**
     * 导出情侣卡券列表
     */
    @Log(title = "情侣卡券", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(PxLoversCard pxLoversCard) {
        List<PxLoversCard> list = pxLoversCardService.selectPxLoversCardList(pxLoversCard);
        ExcelUtil<PxLoversCard> util = new ExcelUtil<PxLoversCard>(PxLoversCard.class);
        return util.exportExcel(list, "card");
    }

    /**
     * 获取情侣卡券详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(pxLoversCardService.selectPxLoversCardById(id));
    }

    /**
     * 新增情侣卡券
     */
    @Log(title = "情侣卡券", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PxLoversCard pxLoversCard) {
        int rows = pxLoversCardService.insertPxLoversCard(pxLoversCard);
        if (rows > 0) {
            return AjaxResult.success(pxLoversCard.getId());
        }
        return AjaxResult.error();
    }

    /**
     * 修改情侣卡券
     */
    @Log(title = "情侣卡券", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody PxLoversCard pxLoversCard) {
        return toAjax(pxLoversCardService.updatePxLoversCard(pxLoversCard));
    }

    /**
     * 删除情侣卡券
     */
    @Log(title = "情侣卡券", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(pxLoversCardService.deletePxLoversCardByIds(ids));
    }

    /**
     * 获取当前人的卡券
     */
    @GetMapping("/getCardByUserId")
    public AjaxResult getCardByUserId() {
        return AjaxResult.success(pxLoversCardService.getCardByUserId());
    }

    /**
     * 使用卡券
     */
    @Log(title = "使用卡券", businessType = BusinessType.UPDATE)
    @PostMapping("/useCard")
    public AjaxResult useCard(@RequestBody PxCardRecord pxCardRecord) {
        return toAjax(pxLoversCardService.useCard(pxCardRecord));
    }

    /**
     * 卡券确认使用
     */
    @Log(title = "卡券确认使用", businessType = BusinessType.UPDATE)
    @PostMapping("/confirmCard")
    public AjaxResult confirmCard(@RequestBody PxCardRecord pxCardRecord) {
        return toAjax(pxLoversCardService.confirmCard(pxCardRecord));
    }

    /**
     * 使用卡券评分
     */
    @Log(title = "使用卡券评分", businessType = BusinessType.UPDATE)
    @PostMapping("/scoreCard")
    public AjaxResult scoreCard(@RequestBody PxCardRecord pxCardRecord) {
        return toAjax(pxLoversCardService.scoreCard(pxCardRecord));
    }
    /**
     * 获取待处理的卡券
     */
    @Log(title = "获取待处理的卡券", businessType = BusinessType.UPDATE)
    @GetMapping("/getToDoCard")
    public AjaxResult getToDoCard() {
        return AjaxResult.success(pxLoversCardService.getToDoCard());
    }
}
