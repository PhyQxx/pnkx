package com.pnkx.domain.po;

import lombok.Data;

import java.util.Date;

@Data
public class PxWxSubscription {
    private Long id;
    private Long userId;
    private String templateType;
    private Boolean accepted;
    private Date updateTime;
}
