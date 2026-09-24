package com.pnkx.mapper;

import com.pnkx.domain.po.PxReminderPreference;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PxReminderPreferenceMapper {
    PxReminderPreference selectByUserId(String userId);
    int upsert(PxReminderPreference preference);
}
