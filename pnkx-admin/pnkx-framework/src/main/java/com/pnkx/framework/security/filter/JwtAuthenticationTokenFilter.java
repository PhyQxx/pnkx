package com.pnkx.framework.security.filter;

import java.io.IOException;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.pnkx.common.core.domain.entity.SysUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.framework.web.service.TokenService;

/**
 * token过滤器 验证token有效性
 *
 * @author phy
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationTokenFilter.class);

    @Resource
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LoginUser loginUser = tokenService.getLoginUserFromToken(request);
        Authentication existingAuth = SecurityUtils.getAuthentication();
        boolean noAuth = existingAuth == null || existingAuth instanceof AnonymousAuthenticationToken;

        if (noAuth) {
            if (StringUtils.isNotNull(loginUser)) {
                tokenService.verifyToken(loginUser);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } else {
                SysUser sysUser = tokenService.verifyTokenUserId(request);
                if (StringUtils.isNotNull(sysUser)) {
                    // 自动登录
                    String tokenKey = tokenService.getUserKeyFromRequest(request);
                    tokenService.createToken(tokenKey, sysUser);
                    LoginUser newLoginUser = tokenService.getLoginUserFromToken(request);
                    if (StringUtils.isNotNull(newLoginUser)) {
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(newLoginUser, null, newLoginUser.getAuthorities());
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }
}
