package com.pnkx.web.controller.tool;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.pnkx.common.core.controller.BaseController;

/**
 * swagger 接口
 *
 * @author phy
 */
@Controller
@RequestMapping("/tool/swagger")
public class SwaggerController extends BaseController {
    @GetMapping()
    public String index() {
        return redirect("/swagger-ui.html");
    }
}
