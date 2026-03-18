# Spring-Aix-Linker (AixLink)

**Spring-Aix-Linker** 是一款专为 Spring Cloud 微服务架构设计的 **AI 能力激活插件（Starter）**。它通过极简的注解映射，将传统的业务方法（Local Service）和遗留系统接口（Feign Client）转化为 AI 可感知、可调度的“语义化技能（Skills）”，并内置了基于 JWT 的全自动安全注入机制。

---

## 🚀 核心愿景
在不改造遗留系统的前提下，构建一套“AI 不可篡改”的安全能力矩阵。通过参数来源隔离技术，确保 AI 仅能填充业务参数，而身份信息（如 UserID）由系统强制从 JWT 中提取。

---

## ✨ 核心特性
* **非侵入式自动装载**：通过 `@AiAction` 等注解，启动时自动扫描并注册微服务能力。
* **参数来源隔离 (Security Isolation)**：支持定义参数来源（AI 提取 vs JWT 注入），彻底杜绝提示词攻击。
* **JWT 自动注入**：框架自动解析 `Authorization` Header，将用户信息静默注入方法参数，无需 AI 参与。
* **元数据安全过滤**：自动隐藏所有非 AI 填充的参数，确保 AI 看到的 API 定义是纯净且安全的。
* **遗留系统嫁接**：原生支持 OpenFeign 接口标注，零成本将旧系统能力接入 AI 编排。

---

## 🛠️ 快速开始

### 1. 引入依赖
```xml
<dependency>
    <groupId>com.yourdomain</groupId>
    <artifactId>spring-aix-linker-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 安全能力标注
使用 `source` 属性定义参数的安全性：

```java
@Service
@AiAbility(name = "个人中心服务")
public class UserService {

    @AiAction(name = "查询我的保单", desc = "获取当前登录用户的所有保单列表")
    public List<Policy> getMyPolicies(
        // source = JWT 表示该参数不暴露给 AI，由框架从 Token 中自动解析并注入
        @AiParam(value = "用户ID", source = ParamSource.JWT) Long userId, 
        // 默认 source = CONVERSATION，表示由 AI 从对话中提取
        @AiParam(value = "保单类型") String type 
    ) {
        return policyService.findByUser(userId, type);
    }
}
```

---

## 🔒 安全架构设计 (Security Guard)

### 参数来源定义 (`ParamSource`)
| 来源类型 | 描述 | AI 是否可见 | 注入方式 |
| :--- | :--- | :--- | :--- |
| **`CONVERSATION`** | 对话内容提取 | **是** | AI 填充 |
| **`JWT`** | 鉴权令牌解析 | **否** | 框架从 `Authorization` Header 解析 |
| **`SYSTEM`** | 系统环境变量 | **否** | 从 `ThreadLocal` 或上下文中提取 |

### 安全运行机制
1.  **元数据屏蔽**：在暴露给 AI 的 `capabilities/list` 接口中，所有标注为 `JWT` 或 `SYSTEM` 的参数将被自动剔除。AI 无法感知这些字段的存在。
2.  **强制注入**：当 AI 触发调用请求时，`AixLinker` 执行器会截获请求，根据 `ParamSource` 定义，从当前的请求上下文中提取真实的身份信息，与 AI 的业务参数合并。
3.  **零伪造风险**：即使用户尝试在对话中诱导 AI 传递 `userId=123`，由于该字段在执行器中被设置为强制从 Token 覆盖，AI 的伪造值将被直接忽略。



---

## 📖 注解说明手册

| 注解 | 作用对象 | 核心属性 | 描述 |
| :--- | :--- | :--- | :--- |
| **`@AiAbility`** | 类 / 接口 | `name` | 定义能力分组（业务域）。 |
| **`@AiAction`** | 方法 | `name`, `desc` | 定义动作及其语义用途。 |
| **`@AiParam`** | 参数 | `value`, `source` | 描述含义及来源（AI填充/JWT注入）。 |

---

## 🏗️ 架构演进方案 (Roadmap)

### 第一阶段：内生能力激活
* [x] 实现 Spring Bean 自动扫描与元数据提取。
* [x] **实现基于 JWT 的参数自动注入 (Security Focus)**。
* [x] 提供统一的本地方法动态调用执行器。

### 第二阶段：遗留系统嫁接
* [ ] 支持 OpenFeign 接口的语义化封装与安全透传。
* [ ] 提供可视化界面查看自动扫描到的能力列表。

### 第三阶段：标准化协议适配
* [ ] 适配 MCP (Model Context Protocol) 协议。
* [ ] 集成多模型适配层（Spring AI）。

---

## 🤝 贡献与反馈
目前该项目由开发者个人维护。如果您在安全注入、JWT 解析等方面有任何优化建议，欢迎提交 Issue。

---

### 💡 开发建议（针对单人维护）：
> **Tips：** 你只需要在 Starter 中实现一个 `JwtParamResolver` 接口，并利用 `HttpServletRequest` 抓取 Header。这样你以后在任何微服务中新写方法，只要打上 `@AiParam(source = ParamSource.JWT)`，安全问题就一劳永逸地解决了。

**下一步行动建议：**
README 已经准备就绪。**建议你现在先在 Starter 工程中创建 `ParamSource` 枚举类，并定义好 `AiParam` 注解。** 这是整个安全隔离机制的基石。需要我帮你写出这个 `ParamSource` 的具体 Java 代码吗？
