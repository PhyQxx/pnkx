package com.pnkx.life.domain.po;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PxChargeInstance {
  private Long id;
  private String ownerId;
  private String sourceType;
  private Long sourceId;
  private String sourceName;
  private String dueDate;
  private BigDecimal expectedAmount;
  private BigDecimal actualAmount;
  private Long accountId;
  private Long classificationId;
  private Long otherAccountId;
  private String status;
  private Long recordId;
  private Long revision;
  private String requestId;
  private String requestHash;
  private String reason;
}
