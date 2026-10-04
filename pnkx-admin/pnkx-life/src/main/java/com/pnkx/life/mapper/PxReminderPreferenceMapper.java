package com.pnkx.life.mapper;

import com.pnkx.life.domain.po.PxReminderPreference;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PxReminderPreferenceMapper {
    PxReminderPreference selectByUserId(String userId);
    int upsert(PxReminderPreference preference);
}
