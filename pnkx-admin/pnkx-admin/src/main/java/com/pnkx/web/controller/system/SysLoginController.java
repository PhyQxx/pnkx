package com.pnkx.web.controller.system;

import com.pnkx.common.constant.Constants;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.domain.entity.SysMenu;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginBody;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.utils.ServletUtils;
import com.pnkx.common.utils.ip.IpLocation;
import com.pnkx.common.utils.ip.IpUtils;
import com.pnkx.common.utils.uuid.UUID;
import com.pnkx.domain.po.PxLikeRecord;
import com.pnkx.framework.web.service.SysLoginService;
import com.pnkx.framework.web.service.SysPermissionService;
import com.pnkx.framework.web.service.TokenService;
import com.pnkx.service.IPxLikeRecordService;
import com.pnkx.system.service.ISysMenuService;
import com.pnkx.system.service.ISysUserService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录验证
 *
 * @author phy
 */
@RestController
public class SysLoginController {

    @Resource
    private SysLoginService loginService;
    @Resource
    private ISysMenuService menuService;
    @Resource
    private SysPermissionService permissionService;
    @Resource
    private TokenService tokenService;
    @Resource
    private IPxLikeRecordService pxLikeRecordService;
    @Resource
    private ISysUserService userService;

    /**
     * 登录方法
     *
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody) {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.login(loginBody.getUserName(),
                loginBody.getPassword(),
                loginBody.getCode(),
                loginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 博客客户端登录
     *
     * @param loginBody
     * @return
     */
    @PostMapping("clientLogin")
    public AjaxResult clientLogin(@RequestBody LoginBody loginBody) {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.userNameAndPassWordLogin(loginBody.getUserName(), loginBody.getPassword());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo(HttpServletRequest request) {
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        SysUser user = userService.selectUserByUserName(loginUser.getUserName());
        PxLikeRecord like = new PxLikeRecord();
        like.setCreateBy(user.getUserId().toString());
        String ip = IpUtils.getIpAddr(request);
        IpLocation location = IpUtils.getLocation(ip);
        user.setLocation(location);
        like.setType("0");
        user.setArticleLikeSet(pxLikeRecordService.selectPxLikeRecordList(like).stream().map(PxLikeRecord::getItemId).collect(Collectors.toList()));
        like.setType("1");
        user.setCommentLikeSet(pxLikeRecordService.selectPxLikeRecordList(like).stream().map(PxLikeRecord::getItemId).collect(Collectors.toList()));
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        return ajax;
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters() {
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        // 用户信息
        SysUser user = loginUser.getUser();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(user.getUserId());
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    @PostMapping("register")
    public AjaxResult register(@RequestBody LoginBody registerBody) throws Exception {
        // 用户注册
        loginService.register(registerBody.getUserName(), registerBody.getPassword());
        return AjaxResult.success(true);
    }

    /**
     * 账号激活
     *
     * @param userName 用户名
     * @return 激活结果
     */
    @GetMapping("/activation/{userName}")
    public AjaxResult activation(@PathVariable("userName") String userName) {
        SysUser sysUser = new SysUser();
        sysUser.setUserName(userName);
        sysUser.setStatus("0");
        return AjaxResult.success(userService.updateUserByUserName(sysUser));
    }

    /**
     * 发送重置邮件
     *
     * @param userName 用户名
     * @return 激活结果
     */
    @GetMapping("/sendResetEmail/{userName}")
    public AjaxResult sendResetEmail(@PathVariable("userName") String userName) {
        try {
            return AjaxResult.success(loginService.sendResetEmail(userName));
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.success(false);
        }
    }

    /**
     * 重置账号密码
     *
     * @param userName 用户名
     * @return 重置结果
     */
    @GetMapping("/restPassword/{userName}")
    public AjaxResult restPassword(@PathVariable("userName") String userName) {
        SysUser sysUser = new SysUser();
        sysUser.setUserName(userName);
        String newPassword = UUID.randomString(8);
        sysUser.setPassword(newPassword);
        if (userService.updateUserByUserName(sysUser) > 0) {
            return AjaxResult.success("重置密码成功", newPassword);
        }
        return AjaxResult.error("重置密码失败");
    }
}
