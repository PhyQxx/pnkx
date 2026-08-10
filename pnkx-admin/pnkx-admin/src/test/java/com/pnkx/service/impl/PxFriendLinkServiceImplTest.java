package com.pnkx.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pnkx.domain.po.PxFriendLink;
import com.pnkx.mapper.PxFriendLinkMapper;
import com.pnkx.system.service.ISysEmailService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PxFriendLinkServiceImplTest {

    @BeforeAll
    public static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PxFriendLink.class);
    }

    @Test
    public void mapperAndServiceUseMybatisPlusBaseTypes() {
        assertTrue(BaseMapper.class.isAssignableFrom(PxFriendLinkMapper.class));
        assertTrue(ServiceImpl.class.isAssignableFrom(PxFriendLinkServiceImpl.class));
    }

    @Test
    public void insertKeepsStatusOnDatabaseDefault() throws NoSuchFieldException {
        TableField tableField = PxFriendLink.class.getDeclaredField("status").getAnnotation(TableField.class);
        assertEquals(FieldStrategy.NEVER, tableField.insertStrategy());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void selectFriendLinkListDelegatesToMybatisPlusSelectList() {
        PxFriendLinkMapper mapper = mock(PxFriendLinkMapper.class);
        PxFriendLinkServiceImpl service = new PxFriendLinkServiceImpl();
        ReflectionTestUtils.setField(service, "pxFriendLinkMapper", mapper);
        ReflectionTestUtils.setField(service, "sysEmailService", mock(ISysEmailService.class));

        PxFriendLink query = new PxFriendLink();
        query.setTitle("看雪");
        query.setStatus("1");

        List<PxFriendLink> expected = Collections.singletonList(new PxFriendLink());
        when(mapper.selectList(any())).thenReturn(expected);

        List<PxFriendLink> actual = service.selectPxFriendLinkList(query);

        assertSame(expected, actual);
        ArgumentCaptor<Wrapper<PxFriendLink>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("title"));
        assertTrue(sqlSegment.contains("status"));
        assertTrue(sqlSegment.contains("ORDER BY create_time DESC"));
    }
}
