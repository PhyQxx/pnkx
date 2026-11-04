package com.pnkx.web.service;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import com.pnkx.life.service.impl.ShoppingActionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ShoppingExpenseVersionTest {
    @Test void expenseKeysFitLegacyColumnsAndRemainStableForRetries() {
        var records=mock(PxBookkeepingRecordMapper.class);
        var accounts=mock(PxBookkeepingAccountMapper.class);
        var categories=mock(PxBookkeepingClassificationMapper.class);
        var service=new ShoppingActionService(null,null,null,null,records,accounts,categories);
        var account=new PxBookkeepingAccount();account.setCreateBy("1");
        var category=new PxBookkeepingClassification();category.setCreateBy("1");category.setTypeDifference("1");
        when(accounts.selectPxBookkeepingAccountById(1L)).thenReturn(account);
        when(categories.selectPxBookkeepingClassificationById(2L)).thenReturn(category);
        var captured=new ArrayList<PxBookkeepingRecord>();
        when(records.insertPxBookkeepingRecord(any())).thenAnswer(call->{
            PxBookkeepingRecord r=call.getArgument(0);captured.add(r);r.setId(3L);return 1;
        });
        var input=new JSONObject();input.put("accountId",1L);input.put("classificationId",2L);
        input.put("totalAmount",new BigDecimal("12.50"));input.put("paidAt","2020-01-01");
        String key="purchase_1_"+"a".repeat(64);
        for(String stableKey:new String[]{key,key,"purchase_batch_9223372036854775807"}) {
            assertEquals(3L,ReflectionTestUtils.<Long>invokeMethod(service,"createExpense",input,"1",stableKey,java.util.List.of("橘子 × 1", "葡萄 × 2")));
        }
        for(var r:captured){
            assertEquals("购物采购结算：橘子 × 1、葡萄 × 2",r.getRemark());
            assertEquals("0", r.getVersion());
            assertTrue(r.getVersion().length() <= 10, "px_bookkeeping_record.version is varchar(10)");
            assertEquals(36,r.getClientUuid().length());
            assertEquals(new BigDecimal("12.50"),r.getMoney());
        }
        assertEquals(captured.get(0).getClientUuid(),captured.get(1).getClientUuid());
        assertNotEquals(captured.get(0).getClientUuid(),captured.get(2).getClientUuid());
    }
    @Test void remarkFitsColumnAndReportsOmittedItems() {
        String remark=ReflectionTestUtils.invokeMethod(ShoppingActionService.class,"expenseRemark",java.util.Collections.nCopies(200,"橘".repeat(128)+" × 1"));
        assertTrue(remark.length()<=1000);
        assertTrue(remark.contains("另有"));
        assertEquals("购物采购结算",ReflectionTestUtils.invokeMethod(ShoppingActionService.class,"expenseRemark",java.util.List.of()));
        assertEquals("葡萄",ReflectionTestUtils.invokeMethod(ShoppingActionService.class,"describeItem","葡萄",null));
    }
}
