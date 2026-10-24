package com.pnkx.framework.web.exception;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.exception.UtilException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import com.pnkx.common.constant.HttpStatus;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.exception.BaseException;
import com.pnkx.common.exception.CustomException;
import com.pnkx.common.utils.StringUtils;

/**
 * 全局异常处理器
 *
 * @author phy
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 基础异常
     */
    @ExceptionHandler(BaseException.class)
    public AjaxResult baseException(BaseException e) {
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(CustomException.class)
    public AjaxResult businessException(CustomException e) {
        if (StringUtils.isNull(e.getCode())) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public AjaxResult handlerNoFoundException(Exception e) {
        log.error(e.getMessage(), e);
        return AjaxResult.error(HttpStatus.NOT_FOUND, "路径不存在，请检查路径是否正确");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAuthorizationException(AccessDeniedException e) {
        log.error(e.getMessage());
        return AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权");
    }

    @ExceptionHandler(AccountExpiredException.class)
    public AjaxResult handleAccountExpiredException(AccountExpiredException e) {
        log.error(e.getMessage(), e);
        return AjaxResult.error(e.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public AjaxResult handleUserNameNotFoundException(UsernameNotFoundException e) {
        log.error(e.getMessage(), e);
        return AjaxResult.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e) {
        // 未分类异常的 message 可能含 SQL 片段、类名等内部信息，不透出给前端
        log.error(e.getMessage(), e);
        return AjaxResult.error("系统繁忙，请稍后重试");
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult validatedBindException(BindException e) {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return AjaxResult.error(message);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object validExceptionHandler(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return AjaxResult.error(message);
    }

    /**
     * 工具类异常
     */
    @ExceptionHandler(UtilException.class)
    public AjaxResult utilException(UtilException e) {
        log.error(e.getMessage(), e);
        String message = e.getMessage();
        return AjaxResult.error(message);
    }

    /**
     * 服务器异常
     */
    @ExceptionHandler(ServiceException.class)
    public AjaxResult serviceException(ServiceException e) {
        log.error(e.getMessage(), e);
        String message = e.getMessage();
        var labels = java.util.Map.ofEntries(
                java.util.Map.entry("FEATURE_DISABLED", "该功能暂未向当前用户启用"),
                java.util.Map.entry("REVISION_CONFLICT", "内容已在其他操作中更新，请刷新后重试"),
                java.util.Map.entry("INVALID_TRANSITION", "当前状态不支持此操作，请刷新查看"),
                java.util.Map.entry("SOURCE_UNAVAILABLE", "来源不存在或无权访问"),
                java.util.Map.entry("PERMISSION_DENIED", "当前空间无访问权限"),
                java.util.Map.entry("PREVIEW_EXPIRED", "预览已过期，请重新生成预览"),
                java.util.Map.entry("IDEMPOTENCY_CONFLICT", "请求标识对应的内容已变化，请刷新后重新提交"),
                java.util.Map.entry("REQUEST_ID_REQUIRED", "请求缺少有效标识，请刷新后重试"),
                java.util.Map.entry("INVALID_ACTION", "不支持此操作"),
                java.util.Map.entry("INVALID_STATE", "无效状态"),
                java.util.Map.entry("EXECUTION_SUPERSEDED", "该计划已由另一执行接管，请刷新结果"));
        var result=AjaxResult.error(labels.getOrDefault(message,message));
        if(labels.containsKey(message))result.put("errorCode",message);
        return result;
    }
}
