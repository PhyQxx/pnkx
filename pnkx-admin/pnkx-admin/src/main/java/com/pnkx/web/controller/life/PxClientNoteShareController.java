package com.pnkx.web.controller.life;

import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.life.service.impl.NoteShareService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/note-share")
public class PxClientNoteShareController {
    private final NoteShareService shares;
    public PxClientNoteShareController(NoteShareService shares) { this.shares = shares; }
    @GetMapping("/{token}")
    public AjaxResult browse(@PathVariable String token, @RequestParam(required=false) String type, @RequestParam(required=false) Long id) {
        return AjaxResult.success(shares.browse(null,token,null,type,id));
    }
}
