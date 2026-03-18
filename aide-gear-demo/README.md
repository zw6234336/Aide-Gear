# Aide-Gear Demo 项目使用指南

> 本文档介绍如何使用 **Aide-Gear** 将传统业务方法转化为 **AI 可调度的智能能力**，无需修改原有业务逻辑。

## 📚 目录

1. [快速开始](#快速开始)
2. [项目结构](#项目结构)
3. [核心概念](#核心概念)
4. [集成步骤](#集成步骤)
5. [实际示例](#实际示例)
6. [API 端点](#api-端点)
7. [测试验证](#测试验证)
8. [常见问题](#常见问题)

---

## 🚀 快速开始

### 前置条件

- Java 17+
- Maven 3.9+
- GLM-5 API Key（或其他 LLM）

### 启动应用

```bash
# 编译
mvn clean install -DskipTests

# 运行 Demo（开发环境）
mvn spring-boot:run -pl aide-gear-demo -Pdev
```

启动后访问：`http://localhost:18080/demo/chat`

### 快速测试

```bash
# 通过 AI 查询产品信息
curl -X POST "http://localhost:18080/demo/chat" \
  -H "Content-Type: application/json" \
  -d '{"question":"帮我查一下产品编码为RISK001的产品详情"}'

# 预期回答：AI 会自动识别意图，调用 ProductQueryService.getProductDetail()，
# 返回产品信息，并以自然语言方式展现
```

---

## 📁 项目结构

```
aide-gear-demo/
├── src/main/java/com/aidegear/demo/
│   ├── config/
│   │   └── DemoJwtParamResolver.java          # 自定义 JWT 解析器
│   ├── controller/
│   │   ├── ChatController.java                # AI 对话端点
│   │   └── DemoController.java                # 演示端点
│   ├── service/
│   │   ├── ChatService.java                   # 大模型集成服务
│   │   ├── UserService.java                   # 用户服务（含 JWT 隔离示例）
│   │   └── ProductQueryService.java           # 产品查询服务
│   └── AideGearDemoApplication.java           # 主入口
│
├── src/main/resources/
│   ├── application.yml                        # 公共配置
│   └── application-dev.yml                    # 开发环境配置
│
├── src/test/java/
│   └── com/aidegear/demo/
│       └── ChatServiceTest.java               # 集成测试
│
└── pom.xml                                     # Maven 依赖
```

---

## 🎯 核心概念

### 三层注解体系

Aide-Gear 通过三个核心注解，将任何 Spring Bean 的方法转化为 AI 能力：

| 注解 | 位置 | 作用 |
|-----|------|------|
| **@AiAbility** | 类 | 标记能力分组，使 Spring Bean 成为 AI 可感知的"能力集合" |
| **@AiAction** | 方法 | 标记单个动作/技能，描述此方法是 AI 可调用的什么功能 |
| **@AiParam** | 参数 | 描述参数语义与来源，支持参数隔离（JWT/System 参数 AI 不可见） |

### 参数来源隔离 (ParamSource)

参数可来自三个来源，不同来源的安全性级别不同：

| 来源 | 示例 | AI 可见 | 用途 |
|-----|------|--------|------|
| **CONVERSATION** | `"产品编码"` | ✅ 是 | AI 从用户对话中提取 |
| **JWT** | `"userId"` | ❌ 否 | 从鉴权令牌自动注入，AI 无法伪造 |
| **SYSTEM** | `"configValue"` | ❌ 否 | 从系统上下文自动注入，AI 无法伪造 |

**安全机制**：API 在返回能力列表时会自动剔除 JWT/SYSTEM 参数，即使 AI 尝试伪造也会被框架强制覆盖真实值。

---

## 📝 集成步骤

### Step 1: 添加依赖

在你的项目 `pom.xml` 中添加 Starter：

```xml
<dependency>
    <groupId>com.aidegear</groupId>
    <artifactId>aide-gear-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Step 2: 配置 application.yml

```yaml
aide-gear:
  enabled: true                          # 启用 Aide-Gear
  base-path: /aide-gear                 # API 基础路径
  jwt:
    header-name: Authorization          # JWT 所在 Header 名称
    token-prefix: "Bearer "             # JWT Token 前缀
    secret: your-jwt-secret             # JWT 签名密钥
  openai:
    api-key: your-api-key               # 大模型 API Key
    model-name: glm-5                   # 大模型名称
    base-url: https://open.bigmodel.cn/api/paas/v4  # 大模型服务地址
```

### Step 3: 实现自定义 JWT 解析器

框架默认无法自动解析 JWT，你需要实现 `JwtParamResolver` 接口：

```java
@Component("jwtParamResolver")
public class MyJwtParamResolver implements JwtParamResolver {
    
    @Override
    public Object resolve(AiParamMeta paramMeta) {
        // 1. 从 ThreadLocal 或 HttpServletRequest 获取当前请求的 JWT Token
        HttpServletRequest request = getCurrentRequest();
        String token = request.getHeader("Authorization");
        
        // 2. 解析 JWT（使用你的库，如 jjwt、auth0、nimbus-jose-jwt 等）
        Claims claims = Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token.replace("Bearer ", ""))
            .getBody();
        
        // 3. 根据 paramMeta.getJwtKey() 获取对应 Claim
        // 例如 paramMeta.getJwtKey() = "userId"，则返回 claims.get("userId")
        return claims.get(paramMeta.getJwtKey());
    }
}
```

**注意**：Bean 名称必须为 `jwtParamResolver`，框架会自动注入。

### Step 4: 在业务方法上标注注解

#### 示例 1：简单能力（仅包含 AI 可见参数）

```java
@Service
@AiAbility(name = "产品查询服务", description = "提供保险产品信息查询能力")
public class ProductQueryService {
    
    @AiAction(name = "查询产品详情", 
              desc = "根据产品编码查询产品详细信息",
              returnDesc = "返回产品名称、保费、保障范围等详细信息")
    public Map<String, Object> getProductDetail(
            @AiParam(value = "产品编码", example = "RISK001") String riskCode
    ) {
        // 原有的业务逻辑不需要改变
        // ...
        return productInfo;
    }
}
```

#### 示例 2：安全能力（包含 JWT 隔离参数）

```java
@Service
@AiAbility(name = "个人中心服务", description = "提供用户保单查询、个人信息管理等能力")
public class UserService {
    
    @AiAction(name = "查询我的保单", 
              desc = "获取当前登录用户的所有保单列表",
              returnDesc = "返回保单列表")
    public List<Policy> getMyPolicies(
            // userId 由框架从 JWT 中自动注入，AI 无法感知
            @AiParam(value = "用户ID", 
                     source = ParamSource.JWT,    // 参数来源
                     jwtKey = "userId")           // JWT 中的 Claim Key
            Long userId,
            
            // type 由 AI 从对话中提取
            @AiParam(value = "保单类型", 
                     required = false,             // 非必填
                     example = "寿险")
            String type
    ) {
        // 此处 userId 已被框架强制设置为当前用户，无法伪造
        // ...
        return policies;
    }
}
```

### Step 5: 实现 ChatService 集成大模型

```java
@Service
@Slf4j
public class ChatService {
    
    @Resource
    private ActionExecutor actionExecutor;
    
    // 大模型客户端初始化（示例）
    private AiServices aiServices;
    
    @PostConstruct
    public void init() {
        // 使用 LangChain4j 构建 AI Services
        // 框架会自动将所有 @AiAction 转化为 Tool Definition
        this.aiServices = AiServices.builder()
            .chatLanguageModel(...)
            .tools(/* 从 Registry 中动态获取 */)
            .build(YourAiService.class);
    }
    
    public String chat(String question) {
        // 大模型根据 Tool Definition 选择合适的 Action
        // 框架自动调用 ActionExecutor.execute() 获取结果
        return aiServices.answer(question);
    }
}
```

---

## 💡 实际示例

### 场景 1：无需修改代码，直接暴露能力

**原有的业务方法**：
```java
public class LoanService {
    public LoanInfo queryLoan(String customerId, String loanId) {
        // 查询贷款信息...
        return loanInfo;
    }
}
```

**添加注解后**：
```java
@Service
@AiAbility(name = "贷款服务", description = "提供贷款查询、还款等能力")
public class LoanService {
    
    @AiAction(name = "查询贷款", desc = "查询客户的贷款信息")
    public LoanInfo queryLoan(
            @AiParam(value = "客户ID", source = ParamSource.JWT, jwtKey = "customerId") String customerId,
            @AiParam(value = "贷款编号", example = "L202503001") String loanId
    ) {
        // 业务逻辑无需改变
        // ...
        return loanInfo;
    }
}
```

**AI 调用流程**：
```
用户提问：
  "我的贷款L202503001还需要还多少钱？"
    ↓
大模型识别意图，调用工具：
  POST /aide-gear/action/invoke
  {
    "actionId": "贷款服务.查询贷款",
    "arguments": {"loanId": "L202503001"}
  }
    ↓
框架自动注入 customerId（从 JWT）
    ↓
LoanService.queryLoan(customerId, "L202503001")
    ↓
框架将结果返回给大模型：
  {
    "principalBalance": 50000,
    "interestBalance": 2500,
    "nextPaymentDate": "2026-04-15"
  }
    ↓
大模型自然语言化：
  "根据查询，您的贷款L202503001还需要还款50000元本金，
   利息2500元，下次还款日期是2026年4月15日。"
```

### 场景 2：多参数调用与参数组合

```java
@AiAction(name = "批量转账", desc = "批量转账给多个收款人")
public Map<String, Object> batchTransfer(
        @AiParam(value = "发款人ID", source = ParamSource.JWT, jwtKey = "userId") Long senderId,
        @AiParam(value = "收款人ID") String recipientId,
        @AiParam(value = "转账金额", example = "10000") BigDecimal amount,
        @AiParam(value = "转账备注", required = false) String remark
) {
    // 发款人自动注入，无法伪造
    // 其他参数由 AI 从对话中提取
    return transferResult;
}
```

**AI 调用**：
```
用户：向张三转账5000块钱，备注是项目报销

框架识别：
{
  "actionId": "转账服务.批量转账",
  "arguments": {
    "recipientId": "张三",
    "amount": 5000,
    "remark": "项目报销"
  }
}

框架注入：
{
  "senderId": 10001,          # 从 JWT 获取，无法伪造
  "recipientId": "张三",      # AI 提取
  "amount": 5000,              # AI 提取
  "remark": "项目报销"        # AI 提取
}

调用：TransferService.batchTransfer(10001, "张三", 5000, "项目报销")
```

---

## 🔌 API 端点

### 1. 获取能力列表 

```bash
GET /aide-gear/capabilities/list
```

**响应**：
```json
{
  "totalActions": 4,
  "version": "1.0.0",
  "actions": [
    {
      "actionId": "产品查询服务.查询产品详情",
      "abilityName": "产品查询服务",
      "actionName": "查询产品详情",
      "description": "根据产品编码查询产品详细信息",
      "abilityDescription": "提供保险产品信息查询能力",
      "params": [
        {
          "paramName": "riskCode",
          "description": "产品编码",
          "type": "String",
          "required": true,
          "example": "RISK001"
        }
      ]
    }
  ]
}
```

**特点**：
- ✅ JWT/SYSTEM 参数自动被过滤
- ✅ 只返回 AI 可见的参数
- ✅ 包含参数示例，便于大模型理解

### 2. 执行动作

```bash
POST /aide-gear/action/invoke
Content-Type: application/json

{
  "actionId": "产品查询服务.查询产品详情",
  "arguments": {
    "riskCode": "RISK001"
  }
}
```

**响应**：
```json
{
  "success": true,
  "actionId": "产品查询服务.查询产品详情",
  "data": {
    "riskCode": "RISK001",
    "productName": "某某终身寿险",
    "minPremium": 1000,
    "maxAge": 65,
    "coverageDesc": "提供身故/全残保障"
  },
  "errorMessage": null,
  "costMs": 45
}
```

### 3. AI 对话接口（Demo 特有）

```bash
# GET 方式
GET /demo/chat?question=查询产品RISK001的详情

# POST 方式
POST /demo/chat
Content-Type: application/json

{
  "question": "查询产品RISK001的详情"
}
```

**响应**：
```json
{
  "question": "查询产品RISK001的详情",
  "answer": "根据查询结果，产品编码为 RISK001 的产品详情如下...",
  "success": true
}
```

---

## 🧪 测试验证

### 单元测试

```bash
mvn test -pl aide-gear-demo -Pdev
```

查看 [ChatServiceTest.java](src/test/java/com/aidegear/demo/ChatServiceTest.java) 了解完整的测试用例。

### 手动测试

#### 测试 1：产品查询（无 JWT 隔离）

```bash
curl -X POST "http://localhost:18080/demo/chat" \
  -H "Content-Type: application/json" \
  -d '{"question":"帮我查一下产品编码为RISK001的产品详情"}'
```

**预期**：AI 调用 `ProductQueryService.getProductDetail("RISK001")`，返回产品信息

#### 测试 2：保单查询（包含 JWT 隔离）

```bash
curl -X POST "http://localhost:18080/demo/chat" \
  -H "Content-Type: application/json" \
  -d '{"question":"查一下我的寿险保单"}'
```

**预期**：
- AI 调用 `UserService.getMyPolicies(userId, "寿险")`
- `userId` 由框架从 Demo JWT 解析器注入（Demo 中固定为 10001）
- 返回用户的保单列表

#### 测试 3：产品对比

```bash
curl -X POST "http://localhost:18080/demo/chat" \
  -H "Content-Type: application/json" \
  -d '{"question":"帮我对比一下RISK001和RISK002两个产品"}'
```

**预期**：AI 调用 `ProductQueryService.compareProducts("RISK001", "RISK002")`，返回对比结果

#### 测试 4：通用问题（无工具调用）

```bash
curl -X POST "http://localhost:18080/demo/chat" \
  -H "Content-Type: application/json" \
  -d '{"question":"你好，你是谁？"}'
```

**预期**：AI 直接回答，无需调用任何工具

---

## ❓ 常见问题

### Q1: 我应该在现有项目的哪些服务上添加 @AiAbility 和 @AiAction？

**A**: 任何 Spring 管理的 Bean（@Service、@Component 等）的公开方法都可以标注。建议优先选择：
- ✅ 查询类接口（不涉及数据修改）
- ✅ 对外暴露的核心业务能力
- ❌ 避免：内部工具方法、大量 IO 操作的方法

### Q2: JWT 参数解析失败了怎么办？

**A**: 检查以下几点：
1. 是否实现了 `JwtParamResolver` 接口，且 Bean 名称为 `jwtParamResolver`
2. 是否正确解析了 JWT Token（Authorization Header 格式）
3. jwtKey 是否与你的 JWT Claims Key 匹配
4. 是否捕获了所有异常（避免异常传播到 AI 层）

```java
@Component("jwtParamResolver")
public class MyJwtParamResolver implements JwtParamResolver {
    @Override
    public Object resolve(AiParamMeta paramMeta) {
        try {
            // 你的解析逻辑
            return claims.get(paramMeta.getJwtKey());
        } catch (Exception e) {
            log.warn("[AideGear] JWT 参数解析失败: {}", paramMeta.getJwtKey(), e);
            // 返回默认值或 null，不要抛出异常
            return null;
        }
    }
}
```

### Q3: AI 无法找到我的能力怎么办？

**A**: 确认以下几点：
1. ✅ 类标注了 @AiAbility
2. ✅ 方法标注了 @AiAction
3. ✅ 类是 Spring 管理的 Bean（@Service、@Component 等）
4. ✅ actionId 全局唯一（格式: `abilityName.actionName`）

验证方法：
```bash
curl http://localhost:18080/aide-gear/capabilities/list | jq '.actions[].actionId'
```

### Q4: 能力返回值太大或包含敏感信息怎么办？

**A**: 在业务方法中进行数据脱敏和裁剪：

```java
@AiAction(name = "查询我的保单", desc = "获取当前登录用户的所有保单列表")
public List<Map<String, Object>> getMyPolicies(Long userId, String type) {
    // 1. 查询完整数据
    List<Policy> allPolicies = policyService.queryByUserId(userId);
    
    // 2. 进行脱敏和裁剪（只返回 AI 需要的字段）
    return allPolicies.stream()
        .filter(p -> type == null || type.equals(p.getType()))
        .map(p -> Map.of(
            "policyNo", p.getNo(),
            "productName", p.getProductName(),
            "premium", p.getPremium(),
            "status", p.getStatus()
            // 隐藏：customerId、bankAccount 等敏感信息
        ))
        .collect(Collectors.toList());
}
```

### Q5: 如何自定义 LLM 模型？

**A**: 修改 `application.yml`：

```yaml
aide-gear:
  openai:
    api-key: your-api-key
    model-name: gpt-4                    # 改成你的模型
    base-url: https://api.openai.com/v1 # 改成模型的服务地址
```

也可以在 `ChatService` 中注入不同的 `ChatLanguageModel` 实现。

### Q6: Demo 中的 userId 是怎么来的？

**A**: Demo 中使用了固定的 `DemoJwtParamResolver`，直接返回 hardcoded 的 userId（10001）：

```java
@Component("jwtParamResolver")
public class DemoJwtParamResolver implements JwtParamResolver {
    @Override
    public Object resolve(AiParamMeta paramMeta) {
        // Demo 环境简化处理，直接返回固定用户
        if ("userId".equals(paramMeta.getJwtKey())) {
            return 10001L;
        }
        return null;
    }
}
```

**生产环境**应该替换为真实的 JWT 解析逻辑。

### Q7: 能否对多个方法使用相同的 actionId？

**A**: **不能**。actionId 必须全局唯一，格式为 `abilityName.actionName`。

如果两个不同的服务有相同的 actionName，建议：
- 方案 A：修改 `abilityName` 使其唯一
- 方案 B：修改 `actionName` 更加具体

```java
// ❌ 错误：两个服务都有 "查询详情"
@AiAbility(name = "产品服务")
public class ProductService {
    @AiAction(name = "查询详情")
    public Product getDetail(String id) { }
}

@AiAbility(name = "订单服务")
public class OrderService {
    @AiAction(name = "查询详情")  // ❌ actionId 重复！
    public Order getDetail(String id) { }
}

// ✅ 正确：使用更具体的 actionName
@AiAbility(name = "产品服务")
public class ProductService {
    @AiAction(name = "查询产品详情")
    public Product getDetail(String id) { }
}

@AiAbility(name = "订单服务")
public class OrderService {
    @AiAction(name = "查询订单详情")
    public Order getDetail(String id) { }
}
```

---

## 🔍 更多资源

- [Aide-Gear 项目主文档](../../AGENT.md)
- [ChatServiceTest.java](src/test/java/com/aidegear/demo/ChatServiceTest.java) - 集成测试用例
- [DemoJwtParamResolver.java](src/main/java/com/aidegear/demo/config/DemoJwtParamResolver.java) - JWT 解析示例

---

## 📄 License

MIT License
