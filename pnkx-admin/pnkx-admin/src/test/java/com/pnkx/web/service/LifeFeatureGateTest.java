package com.pnkx.web.service;

import static org.junit.jupiter.api.Assertions.*;

import com.pnkx.life.service.impl.LifeFeatureGate;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LifeFeatureGateTest {
  @Test
  void rolloutIsPerOwnerAndEmptyListDisables() {
    var env =
        new MockEnvironment()
            .withProperty("pnkx.features.chargeInstanceV2.owners", "101, 102")
            .withProperty("pnkx.features.shoppingPlanV2.owners", "");
    var gate = new LifeFeatureGate(env);
    assertTrue(gate.enabled("chargeInstanceV2", "101"));
    assertFalse(gate.enabled("chargeInstanceV2", "103"));
    assertFalse(gate.enabled("shoppingPlanV2", "101"));
    assertTrue(gate.enabled("actionCenterV2", "103"));
    assertThrows(Exception.class, () -> gate.require("chargeInstanceV2", "103"));
  }
}
