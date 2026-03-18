# Spring-Aix-Linker (AixLink)

**Spring-Aix-Linker** 是一款专为 Spring Cloud 微服务架构设计的 **AI 能力激活插件（Starter）**。它通过极简的注解映射，将传统的业务方法（Local Service）和遗留系统接口（Feign Client）转化为 AI 可感知、可调度的“语义化技能（Skills）”，并原生支持 MCP (Model Context Protocol) 协议对接。

-----

## 🚀 核心愿景

在不改造遗留系统的前提下，通过“语义化嫁接”技术，构建企业级的 AI 原子能力矩阵，实现从“代码驱动”到“意图驱动”的数字化转型。

-----

## ✨ 核心特性

  * **非侵入式自动装载**：通过 `@AiAction` 等注解，启动时自动扫描并注册业务能力。
  * **语义化增强**：支持通过注解为代码字段补充“人话”描述，降低 AI 调用幻觉。
  * **遗留系统嫁接**：原生支持 OpenFeign 接口标注，零成本将旧系统能力接入 AI 编排。
  * **三步走路线图**：内部方法标注 -\> 外部 Feign 代理 -\> MCP 标准化协议适配。
  * **轻量执行引擎**：内置基于反射与类型转换的动态调度逻辑，支持串/并行编排。

-----

## 🛠️ 快速开始

### 1\. 引入依赖 (即将发布)

```xml
<dependency>
    <groupId>com.yourdomain</groupId>
    <artifactId>spring-aix-linker-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2\. 在业务代码中标注能力

只需在原有的 Service 或 Feign 接口上添加少量注解：

```java
@Service
@AiAbility(name = "保险理赔服务")
public class ClaimService {

    @AiAction(name = "查询理赔状态", desc = "根据报案号实时获取理赔进度和审核意见")
    public ClaimResult getStatus(
        @AiParam(value = "理赔报案号", example = "CL20260317001") String claimNo
    ) {
        // 原有业务逻辑
        return claimRepository.findByNo(claimNo);
    }
}
```

### 3\. 获取 AI 能力清单

访问接口：`GET /ai/capabilities/list`
系统将返回符合 OpenAI Tool 调用规范或 MCP 协议的 JSON 元数据。

-----

## 📖 注解说明手册

| 注解 | 作用对象 | 核心属性 | 描述 |
| :--- | :--- | :--- | :--- |
| **`@AiAbility`** | 类 / 接口 | `name` | 定义能力分组（业务域）。 |
| **`@AiAction`** | 方法 | `name`, `desc` | 定义具体的原子动作及其语义用途。 |
| **`@AiParam`** | 方法参数 | `value`, `required` | 描述参数的业务含义，辅助 AI 进行槽位填充。 |
| **`@AiResult`** | DTO 字段 | `value` | 解释出参含义，帮助 AI 提取关键信息。 |

-----

## 🏗️ 架构演进方案 (Roadmap)

### 第一阶段：内生能力激活 (Phase 1)

  * [x] 实现 Spring Bean 自动扫描与注册。
  * [x] 支持反射调用与基础类型转换。
  * [x] 导出 OpenAPI 格式的工具描述。

### 第二阶段：遗留系统嫁接 (Phase 2)

  * [ ] 支持 OpenFeign 接口的语义化封装。
  * [ ] 实现多租户鉴权信息在 AI 链路中的透明传递。
  * [ ] 增加请求/响应的清洗转换器（Data Transformer）。

### 第三阶段：MCP 协议标准化 (Phase 3)

  * [ ] 适配 Anthropic MCP 协议标准。
  * [ ] 提供统一的 MCP Server 端点。
  * [ ] 集成可视化编排引擎（LiteFlow）。

-----

## 🤝 贡献与反馈

目前该项目由开发者个人维护。如果您在遗留系统接入、AI 参数映射等方面有任何建议，欢迎提交 Issue。

-----

## 📄 开源协议

[Apache License 2.0](https://www.google.com/search?q=LICENSE)

-----

### 💡 写给开发者（你自己）

> **开发准则：** 保持 Starter 的极度轻量。业务系统只需关注“注解”，所有的复杂映射和协议转换都应在 Starter 内部闭环。

**下一步建议：**
README 准备好后，**是否需要我为你提供第一步（本地扫描）的 `pom.xml` 依赖配置，以及 `spring.factories` 自动装配文件的具体写法？** 这能让你今天就开始写第一行核心代码。
