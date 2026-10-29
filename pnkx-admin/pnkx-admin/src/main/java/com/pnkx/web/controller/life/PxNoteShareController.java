package com.pnkx.web.controller.life;

import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.impl.NoteShareService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/note/share")
public class PxNoteShareController {
    private final NoteShareService shares;
    public PxNoteShareController(NoteShareService shares) { this.shares = shares; }
    public record ShareRequest(String username, String permission, Integer days) {}

    @GetMapping("/manage/{type}/{id}")
    public AjaxResult managed(@PathVariable String type, @PathVariable Long id) {
        return AjaxResult.success(shares.managed(type,id,SecurityUtils.getUserId()));
    }
    @PostMapping("/manage/{type}/{id}")
    public AjaxResult create(@PathVariable String type, @PathVariable Long id, @RequestBody ShareRequest request) {
        return AjaxResult.success(shares.create(type,id,SecurityUtils.getUserId(),request.username(),request.permission(),request.days()));
    }
    @DeleteMapping("/{id}")
    public AjaxResult revoke(@PathVariable Long id) {
        shares.revoke(id,SecurityUtils.getUserId());
        return AjaxResult.success();
    }
    @GetMapping("/received")
    public AjaxResult received() { return AjaxResult.success(shares.received(SecurityUtils.getUserId())); }
    @GetMapping("/{shareId}/browse")
    public AjaxResult browse(@PathVariable Long shareId, @RequestParam(required=false) String type, @RequestParam(required=false) Long id) {
        return AjaxResult.success(shares.browse(shareId,null,SecurityUtils.getUserId(),type,id));
    }
}
