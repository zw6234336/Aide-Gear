# Aide-Gear (AixLink) 项目开发规范文档

> 本文档用于约束项目开发、记录日常开发异常错误、规范项目约定、核心功能调用规则等。
> 
> **文档版本**: v1.0  
> **最后更新**: 2026-03-18  
> **适用范围**: Aide-Gear 项目全体开发人员

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈与依赖](#2-技术栈与依赖)
3. [项目架构](#3-项目架构)
4. [核心开发规范](#4-核心开发规范)
5. [注解体系规范](#5-注解体系规范)
6. [安全隔离机制](#6-安全隔离机制)
7. [扫描与注册规范](#7-扫描与注册规范)
8. [动作执行规范](#8-动作执行规范)
9. [Spring Boot Starter 集成规范](#9-spring-boot-starter-集成规范)
10. [异常处理规范](#10-异常处理规范)
11. [日志规范](#11-日志规范)
12. [测试规范](#12-测试规范)
13. [常见错误与陷阱](#13-常见错误与陷阱)
14. [变更记录](#14-变更记录)

---

## 1. 项目概述

### 1.1 项目简介

Aide-Gear（又名 Spring-Aix-Linker / AixLink）是一款专为 Spring Cloud 微服务架构设计的 **AI 能力激活插件（Starter）**。它通过极简的注解映射，将传统的业务方法（Local Service）和遗留系统接口（Feign Client）转化为 AI 可感知、可调度的"语义化技能（Skills）"，并内置了基于 JWT 的全自动安全注入机制。

### 1.2 项目信息

| 配置项 | 值 |
|-------|-----|
| **groupId** | com.aidegear |
| **artifactId** | aide-gear |
| **版本** | 1.0.0-SNAPSHOT |
| **Java版本** | 17 |
| **Spring Boot** | 3.2.5 |
| **Spring Cloud** | 2023.0.1 |
| **打包方式** | 多模块 Maven (pom) |

### 1.3 模块结构

| 模块 | artifactId | 描述 |
|-----|-----------|------|
| **公共模块** | aide-gear-common | 注解定义、枚举、模型、异常类 |
| **核心模块** | aide-gear-core | 扫描器、注册器、执行器、安全机制、REST端点 |
| **Starter** | aide-gear-spring-boot-starter | 自动配置、配置属性定义 |
| **演示工程** | aide-gear-demo | 集成演示、使用示例 |

### 1.4 构建与运行

```bash
# 编译全部模块
mvn clean install -DskipTests

# 运行演示工程
mvn spring-boot:run -pl aide-gear-demo -Pdev

# 运行测试
mvn test

# 指定环境打包
mvn clean package -Pdev -DskipTests    # 开发环境
mvn clean package -Puat -DskipTests    # UAT环境
mvn clean package -Ppro -DskipTests    # 生产环境
```

### 1.5 环境配置

| Profile | 说明 | 特点 |
|---------|------|------|
| `dev` | 开发环境 | 默认激活，Nacos 服务发现禁用 |
| `uat` | UAT 环境 | 预发布环境 |
| `pro` | 生产环境 | 完整配置 |

---

## 2. 技术栈与依赖

### 2.1 核心依赖版本

| 依赖 | 版本 | 说明 |
|-----|------|------|
| Spring Boot | 3.2.5 | Web 框架 |
| Spring Cloud | 2023.0.1 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | Nacos 服务发现/配置 |
| LangChain4j | 0.35.0 | AI 工具规范构建 |
| Hutool | 5.8.29 | 工具库 |
| FastJSON2 | 2.0.47 | JSON 处理 |
| Lombok | 1.18.32 | 代码简化 |
| SpringDoc | 2.3.0 | API 文档 |

---

## 3. 项目架构

### 3.1 包结构

```
aide-gear/
├── aide-gear-common/                      # 公共模块
│   └── src/main/java/com/aidegear/common/
│       ├── annotation/                    # 注解定义
│       │   ├── AiAbility.java            # 能力分组注解（类级别）
│       │   ├── AiAction.java             # 动作注解（方法级别）
│       │   └── AiParam.java              # 参数注解（参数级别）
│       ├── enums/                         # 枚举
│       │   └── ParamSource.java          # 参数来源枚举
│       ├── model/                         # 模型
│       │   ├── AiActionMeta.java         # 动作元数据
│       │   ├── AiParamMeta.java          # 参数元数据
│       │   └── ActionResult.java         # 执行结果
│       └── exception/                     # 异常
│           ├── AideGearException.java    # 基础异常
│           └── ActionExecutionException.java  # 执行异常
│
├── aide-gear-core/                        # 核心模块
│   └── src/main/java/com/aidegear/core/
│       ├── scanner/                       # 扫描器
│       │   └── AiAbilityScanner.java     # BeanPostProcessor 扫描
│       ├── registry/                      # 注册中心
│       │   └── AiActionRegistry.java     # 动作注册表
│       ├── executor/                      # 执行器
│       │   └── ActionExecutor.java       # 动作执行器（安全核心）
│       ├── security/                      # 安全机制
│       │   ├── JwtParamResolver.java     # JWT 解析器接口
│       │   ├── SystemParamResolver.java  # 系统参数解析器接口
│       │   ├── DefaultJwtParamResolver.java     # 默认 JWT 解析器
│       │   └── DefaultSystemParamResolver.java  # 默认系统解析器
│       └── endpoint/                      # REST 端点
│           ├── CapabilitiesEndpoint.java  # 能力列表端点
│           └── ActionInvokeEndpoint.java  # 动作调用端点
│
├── aide-gear-spring-boot-starter/         # Starter 模块
│   └── src/main/
│       ├── java/com/aidegear/starter/config/
│       │   ├── AideGearAutoConfiguration.java  # 自动配置
│       │   └── AideGearProperties.java         # 配置属性
│       └── resources/META-INF/spring/
│           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
└── aide-gear-demo/                        # 演示工程
    └── src/main/java/com/aidegear/demo/
        ├── AideGearDemoApplication.java   # 主入口
        ├── config/                        # 配置
        │   └── DemoJwtParamResolver.java  # 自定义 JWT 解析器示例
        └── service/                       # 服务示例
            ├── UserService.java           # 用户服务（含 JWT 隔离示例）
            └── ProductQueryService.java   # 产品查询服务
```

### 3.2 分层架构

| 层级 | 命名规范 | 职责 |
|-----|---------|------|
| **注解层** | `@Ai*` | 注解标记，元数据声明 |
| **扫描层** | `*Scanner` | Spring 启动时自动扫描 Bean |
| **注册层** | `*Registry` | 存储和查询元数据 |
| **执行层** | `*Executor` | 参数合并、安全注入、方法调用 |
| **安全层** | `*Resolver` | JWT/System 参数解析 |
| **端点层** | `*Endpoint` | REST API 暴露 |

---

## 4. 核心开发规范

### 4.1 命名规范

| 类型 | 命名规范 | 示例 |
|-----|---------|------|
| 注解 | `@Ai*` | @AiAbility, @AiAction, @AiParam |
| 扫描器 | `*Scanner` | AiAbilityScanner |
| 注册中心 | `*Registry` | AiActionRegistry |
| 执行器 | `*Executor` | ActionExecutor |
| 解析器接口 | `*Resolver` | JwtParamResolver |
| 默认实现 | `Default*` | DefaultJwtParamResolver |
| 端点 | `*Endpoint` | CapabilitiesEndpoint |
| 元数据模型 | `*Meta` | AiActionMeta, AiParamMeta |
| 配置属性 | `*Properties` | AideGearProperties |
| 自动配置 | `*AutoConfiguration` | AideGearAutoConfiguration |

### 4.2 依赖注入规范

```java
// ✅ 推荐使用 @Resource
@Resource
private AiActionRegistry registry;

@Resource(name = "jwtParamResolver")
private JwtParamResolver jwtParamResolver;

// ❌ 避免使用 @Autowired
@Autowired
private AiActionRegistry registry;  // 不推荐
```

### 4.3 参数校验规范

```java
// ✅ 正确 —— 约束写在 DTO 字段上
import jakarta.validation.constraints.NotBlank;

@Data
public class InvokeRequest {
    @NotBlank(message = "actionId 不能为空")
    private String actionId;
}

// ✅ 正确 —— Controller 仅加 @Valid
@PostMapping("/action/invoke")
public ActionResult invoke(@RequestBody @Valid InvokeRequest request) { ... }
```

---

## 5. 注解体系规范

### 5.1 @AiAbility（类级别）

**用途**: 标记一个 Spring Bean 为 AI 可感知的"能力分组"。

```java
@Service
@AiAbility(name = "个人中心服务", description = "提供用户保单查询、个人信息管理等能力")
public class UserService { ... }
```

**要求**:
- ✅ 必须标注在 Spring 管理的 Bean 上（@Service、@Component 等）
- ✅ `name` 属性必填，使用中文语义化描述
- ❌ 禁止在非 Spring Bean 上使用

### 5.2 @AiAction（方法级别）

**用途**: 标记一个方法为 AI 可调度的"动作/技能"。

```java
@AiAction(name = "查询我的保单", desc = "获取当前登录用户的所有保单列表",
          returnDesc = "返回保单列表，包含保单号、产品名称、保费等信息")
public List<Policy> getMyPolicies(...) { ... }
```

**要求**:
- ✅ 方法必须在标记了 @AiAbility 的类中
- ✅ `name` 属性必填，简洁描述动作用途
- ✅ `desc` 建议填写，帮助 AI 理解使用场景
- ❌ 禁止在无 @AiAbility 的类中使用（不会被扫描）

### 5.3 @AiParam（参数级别）

**用途**: 描述方法参数的语义含义和来源。

```java
public List<Policy> getMyPolicies(
    @AiParam(value = "用户ID", source = ParamSource.JWT, jwtKey = "userId") Long userId,
    @AiParam(value = "保单类型", required = false, example = "寿险") String type
) { ... }
```

**属性说明**:

| 属性 | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| `value` | String | 必填 | 参数语义描述 |
| `source` | ParamSource | CONVERSATION | 参数来源 |
| `required` | boolean | true | 是否必填 |
| `jwtKey` | String | "" | JWT Claim Key |
| `systemKey` | String | "" | 系统属性 Key |
| `example` | String | "" | 示例值 |

---

## 6. 安全隔离机制

### 6.1 参数来源定义 (ParamSource)

| 来源类型 | 描述 | AI 是否可见 | 注入方式 |
|---------|------|-----------|---------|
| **CONVERSATION** | 对话内容提取 | **是** | AI 填充 |
| **JWT** | 鉴权令牌解析 | **否** | 框架从 Authorization Header 解析 |
| **SYSTEM** | 系统环境变量 | **否** | 从 ThreadLocal 或上下文中提取 |

### 6.2 安全运行机制

1. **元数据屏蔽**: 在 `/aide-gear/capabilities/list` 接口中，所有标注为 JWT 或 SYSTEM 的参数自动剔除
2. **强制注入**: 执行器截获请求，根据 ParamSource 定义从安全上下文中提取真实信息
3. **零伪造风险**: 即使 AI 尝试伪造，JWT/SYSTEM 参数值会被强制覆盖

### 6.3 JwtParamResolver 实现规范

```java
@Component("jwtParamResolver")
public class MyJwtParamResolver implements JwtParamResolver {
    
    @Override
    public Object resolve(AiParamMeta paramMeta) {
        HttpServletRequest request = getCurrentRequest();
        String token = request.getHeader("Authorization");
        
        // 解析 JWT 并根据 paramMeta.getJwtKey() 提取对应 claim
        Claims claims = Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token.replace("Bearer ", ""))
            .getBody();
            
        return claims.get(paramMeta.getJwtKey());
    }
}
```

**强制要求**:
- ✅ Bean 名称必须为 `jwtParamResolver`
- ✅ 必须处理 Token 不存在或解析失败的情况
- ✅ 返回值类型需与目标参数类型兼容
- ❌ 禁止在解析器中抛出未捕获的异常

---

## 7. 扫描与注册规范

### 7.1 扫描器工作流

```
Spring 容器初始化
  ↓
BeanPostProcessor.postProcessAfterInitialization()
  ↓
检查 Bean 是否有 @AiAbility 注解
  ↓
遍历所有方法，查找 @AiAction 注解
  ↓
构建 AiActionMeta（含参数元数据）
  ↓
注册到 AiActionRegistry
```

### 7.2 actionId 生成规则

```
actionId = @AiAbility.name + "." + @AiAction.name
```

示例: `"个人中心服务.查询我的保单"`

**⚠️ 重要**: actionId 必须全局唯一，否则后注册的会覆盖先注册的。

---

## 8. 动作执行规范

### 8.1 执行流程

```
收到 AI 调用请求 (actionId + arguments)
  ↓
从 Registry 查找 AiActionMeta
  ↓
构建方法参数数组:
  - CONVERSATION 参数: 从 AI 提供的 arguments 中取
  - JWT 参数: 调用 JwtParamResolver.resolve()
  - SYSTEM 参数: 调用 SystemParamResolver.resolve()
  ↓
Method.invoke(targetBean, args)
  ↓
JSON 序列化结果 → ActionResult
```

### 8.2 调用方式

```java
// 通过 REST 端点
POST /aide-gear/action/invoke
{
    "actionId": "个人中心服务.查询我的保单",
    "arguments": {
        "type": "寿险"
    }
}

// 通过 Java 代码
@Resource
private ActionExecutor actionExecutor;

ActionResult result = actionExecutor.execute(
    "个人中心服务.查询我的保单",
    Map.of("type", "寿险")
);
```

---

## 9. Spring Boot Starter 集成规范

### 9.1 引入依赖

```xml
<dependency>
    <groupId>com.aidegear</groupId>
    <artifactId>aide-gear-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 9.2 配置属性

```yaml
aide-gear:
  enabled: true
  base-path: /aide-gear
  jwt:
    header-name: Authorization
    token-prefix: "Bearer "
    secret: your-jwt-secret
  scanner:
    base-packages: com.yourcompany.service
```

### 9.3 自定义解析器

使用方必须实现 `JwtParamResolver` 接口并注册为 Bean:

```java
@Component("jwtParamResolver")
public class MyJwtParamResolver implements JwtParamResolver {
    @Override
    public Object resolve(AiParamMeta paramMeta) {
        // 你的 JWT 解析逻辑
    }
}
```

---

## 10. 异常处理规范

### 10.1 框架异常体系

| 异常类 | 说明 |
|-------|------|
| `AideGearException` | 框架异常基类 |
| `ActionExecutionException` | 动作执行失败异常 |

### 10.2 执行器异常处理

```java
// 执行器内部统一捕获异常，不向外传播
try {
    Object result = method.invoke(targetBean, args);
    return ActionResult.success(actionId, data, costMs);
} catch (Exception e) {
    log.error("[AideGear] 动作执行失败: {}", actionId, e);
    return ActionResult.fail(actionId, e.getMessage(), costMs);
}
```

**强制要求**:
- ✅ 所有异常在执行器内捕获
- ✅ 返回 `ActionResult.fail()` 而非抛出异常
- ✅ 记录错误日志包含 actionId 和异常堆栈
- ❌ 禁止让异常传播到 AI 调用层

---

## 11. 日志规范

### 11.1 日志级别使用

| 级别 | 使用场景 |
|-----|---------|
| ERROR | 动作执行失败、系统错误 |
| WARN | 默认解析器被使用、actionId 重复 |
| INFO | 能力注册、动作执行成功 |
| DEBUG | 扫描过程、参数构建详情 |

### 11.2 日志格式

所有框架日志使用 `[AideGear]` 前缀:

```java
log.info("[AideGear] 注册动作: {} -> {}.{}", actionId, beanName, methodName);
log.warn("[AideGear] 动作ID重复, 将覆盖: {}", actionId);
log.error("[AideGear] 动作执行失败: {}", actionId, e);
```

---

## 12. 测试规范

### 12.1 测试类配置

```java
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
public class AiAbilityScannerTest {

    @Resource
    private AiActionRegistry registry;

    @Test
    void testScanAbilities() {
        // 验证扫描结果
        assertTrue(registry.size() > 0);
        assertNotNull(registry.getAction("个人中心服务.查询我的保单"));
    }
}
```

### 12.2 运行测试

```bash
mvn test
mvn test -pl aide-gear-core
mvn test -Dtest=AiAbilityScannerTest
```

---

## 13. 常见错误与陷阱

### 13.1 严重错误（必须避免）

| 错误 | 说明 | 正确做法 |
|-----|------|---------|
| **遗漏 @AiAbility** | 方法上有 @AiAction 但类上无 @AiAbility | 类必须标注 @AiAbility |
| **Bean Name 冲突** | 自定义 JwtParamResolver 未命名为 jwtParamResolver | Bean 名称必须为 jwtParamResolver |
| **actionId 冲突** | 不同类中 abilityName+actionName 相同 | 确保 actionId 全局唯一 |
| **JWT 异常外泄** | JwtParamResolver 中未捕获异常 | 必须在 resolve() 内捕获所有异常 |
| **类型不匹配** | resolve() 返回值与目标参数类型不兼容 | 确保返回兼容类型 |
| **CGLIB 代理问题** | 扫描器未处理代理类 | 已内置 CGLIB 兼容处理 |

### 13.2 常见问题排查

**问题1**: 启动后 capabilities/list 为空
- **原因**: 服务 Bean 未标注 @AiAbility 或方法未标注 @AiAction
- **解决**: 检查注解是否正确标注

**问题2**: JWT 参数始终为 null
- **原因**: 未实现自定义 JwtParamResolver 或 Bean 名称错误
- **解决**: 实现 JwtParamResolver 接口，Bean 名称为 `jwtParamResolver`

**问题3**: 动作调用返回 "未找到动作"
- **原因**: actionId 不匹配（格式: abilityName.actionName）
- **解决**: 通过 capabilities/list 确认正确的 actionId

---

## 14. 变更记录

### v1.0 (2026-03-18)
- 初始版本
- 完成多模块 Maven 项目结构
- 完成注解体系: @AiAbility, @AiAction, @AiParam
- 完成参数来源隔离机制 (ParamSource)
- 完成 BeanPostProcessor 自动扫描
- 完成 AiActionRegistry 注册中心
- 完成 ActionExecutor 安全执行器
- 完成 JwtParamResolver / SystemParamResolver SPI
- 完成 Spring Boot Starter 自动配置
- 完成 REST 端点 (capabilities/list, action/invoke)
- 完成 Demo 演示工程

---

> **维护说明**: 本文档由项目团队维护，如有更新请同步修改变更记录。遇到新的开发问题和规范请及时补充。
