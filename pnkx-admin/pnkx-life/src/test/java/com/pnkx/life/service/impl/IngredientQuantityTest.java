package com.pnkx.life.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class IngredientQuantityTest {
  @Test
  void compatibleUnitsNormalize() {
    assertEquals("300g", IngredientQuantity.parse("0.3kg", BigDecimal.ONE).display());
    assertEquals("500g", IngredientQuantity.parse("250克", new BigDecimal("2")).display());
    assertEquals("1500ml", IngredientQuantity.parse("1.5升", BigDecimal.ONE).display());
  }

  @Test
  void unknownQuantityRemainsUnchanged() {
    assertNull(IngredientQuantity.parse("适量", new BigDecimal("2")).quantity());
    assertEquals("适量", IngredientQuantity.parse("适量", BigDecimal.ONE).display());
  }

  @Test
  void countDoesNotBecomeWeight() {
    assertEquals("个", IngredientQuantity.parse("2个", BigDecimal.ONE).unit());
    assertEquals("g", IngredientQuantity.parse("2g", BigDecimal.ONE).unit());
  }
}
