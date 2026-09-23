package com.pnkx.system.mapper;

import com.pnkx.system.domain.PxPushDevice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * App 推送设备 Mapper
 *
 * @author PHY
 * @date 2026-09-23
 */
public interface PxPushDeviceMapper {

    /**
     * 登记设备（同用户同 clientId 则刷新上报时间，否则插入）
     */
    int upsertDevice(PxPushDevice device);

    /**
     * 查询用户全部在册设备
     */
    List<PxPushDevice> selectByUserId(@Param("userId") String userId);

    /**
     * 清理指定天数未上报的设备（客户端卸载/换机后 clientId 失效）
     */
    int cleanStaleDevices(@Param("days") int days);
}
