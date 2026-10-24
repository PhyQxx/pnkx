package com.pnkx.life.service.impl;

import java.math.*;
import java.text.Normalizer;
import java.util.regex.*;

/** 非数字用量原样保留；仅在同量纲内换算。 */
public record IngredientQuantity(BigDecimal quantity, String unit, String raw) {
  private static final Pattern NUMBER =
      Pattern.compile(
          "^([0-9]+(?:\\.[0-9]+)?)\\s*(kg|g|ml|l|公斤|千克|克|毫升|升|个|只|袋|盒)?$",
          Pattern.CASE_INSENSITIVE);

  public static IngredientQuantity parse(String raw, BigDecimal ratio) {
    String text = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFKC).trim();
    var m = NUMBER.matcher(text);
    if (!m.matches()) return new IngredientQuantity(null, "", text);
    String u = m.group(2) == null ? "份" : m.group(2).toLowerCase();
    BigDecimal q = new BigDecimal(m.group(1)).multiply(ratio);
    if (java.util.Set.of("kg", "公斤", "千克").contains(u)) {
      q = q.multiply(new BigDecimal("1000"));
      u = "g";
    } else if (java.util.Set.of("l", "升").contains(u)) {
      q = q.multiply(new BigDecimal("1000"));
      u = "ml";
    } else if ("克".equals(u)) u = "g";
    else if ("毫升".equals(u)) u = "ml";
    return new IngredientQuantity(q.setScale(3, RoundingMode.HALF_UP), u, text);
  }

  public String display() {
    return quantity == null ? raw : quantity.stripTrailingZeros().toPlainString() + unit;
  }
}
