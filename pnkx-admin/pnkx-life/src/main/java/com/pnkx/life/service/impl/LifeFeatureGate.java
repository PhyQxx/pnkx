package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** 仅切换新功能；禁止降级回旧的直写出账器。配置为空字符串时关闭，* 时全量。 */
@Service
public class LifeFeatureGate {
  public static final Set<String> FEATURES =
      Set.of(
          "actionCenterV2",
          "chargeInstanceV2",
          "reminderEventV2",
          "shoppingPlanV2",
          "operationPlanV2",
          "reportActionV2");
  private final Environment env;

  public LifeFeatureGate(Environment env) {
    this.env = env;
  }

  public boolean enabled(String feature, String owner) {
    if (!FEATURES.contains(feature)) return false;
    String allow = env.getProperty("pnkx.features." + feature + ".owners", "*");
    return Arrays.stream(allow.split(","))
        .map(String::trim)
        .anyMatch(v -> v.equals("*") || v.equals(owner));
  }

  public void require(String feature, String owner) {
    if (!enabled(feature, owner)) throw new ServiceException("FEATURE_DISABLED");
  }

  public Map<String, Boolean> capabilities(String owner) {
    var result = new TreeMap<String, Boolean>();
    for (String key : FEATURES) result.put(key, enabled(key, owner));
    return result;
  }
}
