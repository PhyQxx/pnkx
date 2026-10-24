package com.pnkx.web.controller.life;

import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.impl.LifeFeatureGate;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@RestController
public class LifeFeatureConfiguration implements WebMvcConfigurer {
  private final LifeFeatureGate gate;

  public LifeFeatureConfiguration(LifeFeatureGate gate) {
    this.gate = gate;
  }

  @GetMapping("/life/features")
  public AjaxResult features() {
    return AjaxResult.success(gate.capabilities(SecurityUtils.getUserId()));
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(
            new HandlerInterceptor() {
              @Override
              public boolean preHandle(
                  HttpServletRequest request, HttpServletResponse response, Object handler) {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                String feature = null;
                if (path.startsWith("/life/action-center")) feature = "actionCenterV2";
                else if (path.startsWith("/charge/")) feature = "chargeInstanceV2";
                else if (path.startsWith("/reminder/events")) feature = "reminderEventV2";
                else if (path.startsWith("/operation-plans")) feature = "operationPlanV2";
                else if (path.startsWith("/lifeReport/insights")) feature = "reportActionV2";
                else if (path.startsWith("/mealPlan/shopping-")
                    || path.startsWith("/shopping-batches/")
                    || path.matches(
                        "/shopping(Item|List)/[0-9]+/(claim|release|fulfillment|checkout|batches)"))
                  feature = "shoppingPlanV2";
                if (feature != null) gate.require(feature, SecurityUtils.getUserId());
                return true;
              }
            })
        .addPathPatterns(
            "/life/**",
            "/charge/**",
            "/reminder/events/**",
            "/operation-plans/**",
            "/lifeReport/insights/**",
            "/mealPlan/**",
            "/shoppingItem/**",
            "/shoppingList/**",
            "/shopping-batches/**");
  }
}
