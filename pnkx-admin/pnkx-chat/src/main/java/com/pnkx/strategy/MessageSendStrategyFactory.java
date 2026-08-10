package com.pnkx.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 消息发送策略工厂
 * 负责管理和提供不同的消息发送策略
 * 
 * @author pnkx
 */
@Slf4j
@Component
public class MessageSendStrategyFactory {
    
    private final Map<MessageSendStrategy.StrategyType, MessageSendStrategy> strategies;
    
    @Autowired
    public MessageSendStrategyFactory(List<MessageSendStrategy> strategyList) {
        if (CollectionUtils.isEmpty(strategyList)) {
            throw new IllegalArgumentException("策略列表不能为空");
        }
        
        // 将策略列表转换为Map，便于快速查找
        this.strategies = strategyList.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                MessageSendStrategy::getType,
                strategy -> strategy,
                (existing, replacement) -> {
                    log.warn("发现重复的策略类型: {}，使用现有策略", existing.getType());
                    return existing;
                },
                ConcurrentHashMap::new
            ));
    }
    
    /**
     * 根据策略类型获取策略
     * 
     * @param type 策略类型
     * @return 对应的策略实例
     * @throws IllegalArgumentException 当策略类型不支持时抛出异常
     */
    public MessageSendStrategy getStrategy(MessageSendStrategy.StrategyType type) {
        if (Objects.isNull(type)) {
            throw new IllegalArgumentException("策略类型不能为空");
        }
        
        MessageSendStrategy strategy = strategies.get(type);
        if (Objects.isNull(strategy)) {
            throw new IllegalArgumentException("不支持的策略类型: " + type);
        }
        return strategy;
    }
    
    /**
     * 获取欢迎新用户策略
     * 
     * @return 欢迎新用户策略实例
     */
    public MessageSendStrategy getWelcomeNewUserStrategy() {
        return getStrategy(MessageSendStrategy.StrategyType.WELCOME_NEW_USER);
    }
    
    /**
     * 获取私聊策略
     * 
     * @return 私聊策略实例
     */
    public MessageSendStrategy getSendToUserStrategy() {
        return getStrategy(MessageSendStrategy.StrategyType.SEND_TO_USER);
    }
    
    /**
     * 获取群聊策略
     * 
     * @return 群聊策略实例
     */
    public MessageSendStrategy getSendToGroupStrategy() {
        return getStrategy(MessageSendStrategy.StrategyType.SEND_TO_GROUP);
    }
    
    /**
     * 获取回复消息策略
     * 
     * @return 回复消息策略实例
     */
    public MessageSendStrategy getReplyToMessageStrategy() {
        return getStrategy(MessageSendStrategy.StrategyType.REPLY_TO_MESSAGE);
    }
    
    /**
     * 检查策略类型是否支持
     * 
     * @param type 策略类型
     * @return 如果支持返回true，否则返回false
     */
    public boolean supportsStrategy(MessageSendStrategy.StrategyType type) {
        return Objects.nonNull(type) && strategies.containsKey(type);
    }
    
    /**
     * 获取所有支持的策略类型
     * 
     * @return 策略类型集合
     */
    public Set<MessageSendStrategy.StrategyType> getSupportedStrategyTypes() {
        return Collections.unmodifiableSet(strategies.keySet());
    }
}
