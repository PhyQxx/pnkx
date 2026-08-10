# PNKX Chat Module

聊天模块，提供自定义回复规则、AI智能回复和消息发送策略等功能。

## 项目结构

```
src/main/java/com/pnkx/
├── domain/po/                    # 实体类
│   ├── PxCustomReplyRule.java    # 自定义回复规则实体
│   ├── PxCustomReplyContent.java # 自定义回复内容实体
│   └── ...
├── mapper/                       # 数据访问层
│   ├── PxCustomReplyRuleMapper.java
│   ├── PxCustomReplyContentMapper.java
│   └── ...
├── service/                      # 服务层接口
│   ├── IPxCustomReplyService.java
│   ├── IPxDeepSeekService.java
│   └── ...
├── service/impl/                 # 服务层实现
│   ├── PxCustomReplyService.java
│   ├── PxDeepSeekService.java
│   └── ...
└── strategy/                     # 策略模式实现
    ├── MessageSendStrategy.java
    ├── MessageSendStrategyFactory.java
    └── ...
```

## 主要功能

### 1. 自定义回复规则管理
- 支持关键词匹配（精确匹配和模糊匹配）
- 支持多回复内容随机选择
- 支持权重随机选择
- 支持时间变量替换

### 2. AI智能回复
- 集成DeepSeek API
- 支持上下文对话
- 可配置的模型参数

### 3. 消息发送策略
- 策略模式实现不同类型的消息发送
- 支持欢迎新用户、私聊、群聊等场景

## 代码优化总结

### 1. 代码规范优化
- **命名规范**: 统一使用驼峰命名法，常量使用大写+下划线
- **注释规范**: 添加详细的JavaDoc注释，说明参数、返回值和异常
- **导入优化**: 使用通配符导入减少代码行数，保持整洁

### 2. 代码逻辑优化
- **空值检查**: 统一使用`StringUtils.hasText()`和`CollectionUtils.isEmpty()`
- **异常处理**: 添加详细的错误日志和异常信息
- **参数验证**: 在方法入口处进行参数有效性检查

### 3. 性能优化
- **随机数生成**: 使用`ThreadLocalRandom`替代`Random`，提高性能
- **集合操作**: 使用Stream API进行集合操作，提高可读性
- **常量定义**: 将魔法值提取为常量，提高可维护性

### 4. 设计模式应用
- **策略模式**: 消息发送策略使用策略模式，便于扩展
- **工厂模式**: 策略工厂统一管理策略实例
- **单一职责**: 每个类和方法职责单一，便于测试和维护

## 最佳实践

### 1. 服务层设计
```java
// 参数验证
if (!StringUtils.hasText(messageContent)) {
    log.warn("参数为空");
    return null;
}

// 异常处理
try {
    // 业务逻辑
} catch (Exception e) {
    log.error("操作失败", e);
    throw new RuntimeException("操作失败: " + e.getMessage());
}
```

### 2. 实体类设计
```java
@Data
@EqualsAndHashCode(callSuper = true)
public class PxCustomReplyRule extends BaseEntity {
    private static final long serialVersionUID = 1L;
    // 字段定义
}
```

### 3. 策略模式使用
```java
// 获取策略
MessageSendStrategy strategy = factory.getStrategy(type);
// 执行策略
strategy.sendMessage(params);
```

## 配置说明

### 1. 数据库配置
在`application.yml`中配置数据源和MyBatis:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pnkx_chat
    username: root
    password: password

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.pnkx.domain.po
```

### 2. AI服务配置
```yaml
app:
  deepseek:
    api-key: your-api-key
    base-url: https://api.deepseek.com

ai:
  model: deepseek-chat
  temperature: 0.7
```

## 使用示例

### 1. 自定义回复规则
```java
@Autowired
private IPxCustomReplyService customReplyService;

// 匹配回复规则
String reply = customReplyService.matchCustomReply("你好");
```

### 2. AI智能回复
```java
@Autowired
private IPxDeepSeekService deepSeekService;

// 生成回复
String aiReply = deepSeekService.generateReply("什么是人工智能？");
```

### 3. 消息发送策略
```java
@Autowired
private MessageSendStrategyFactory strategyFactory;

// 发送欢迎消息
MessageSendStrategy strategy = strategyFactory.getWelcomeNewUserStrategy();
strategy.sendMessage(userId, welcomeMessage);
```

## 开发规范

1. **代码提交**: 每次提交前运行测试，确保功能正常
2. **代码审查**: 重要修改需要代码审查
3. **文档更新**: 功能修改后及时更新文档
4. **异常处理**: 所有可能抛出异常的地方都要有处理逻辑

## 扩展建议

1. **缓存优化**: 可以考虑添加Redis缓存，提高查询性能
2. **异步处理**: 耗时的AI调用可以考虑使用异步处理
3. **监控告警**: 添加业务监控和告警机制
4. **国际化**: 支持多语言回复内容
