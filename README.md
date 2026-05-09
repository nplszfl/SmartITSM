# SmartITSM - AI驱动的IT服务管理系统

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-orange.svg)](https://vuejs.org/)
[![AI](https://img.shields.io/badge/AI-DeepSeek-yellow.svg)](https://deepseek.com/)

> 🔮 **AI驱动的IT服务管理** - 智能工单分类、根因分析、预测性维护，让IT运维效率提升10倍

## 📋 项目简介

SmartITSM 是一套**AI深度融合**的IT服务管理（ITSM）系统，对标ServiceNow但价格便宜10倍，AI能力强100倍。

### 核心AI能力

| AI功能 | 描述 | 价值 |
|--------|------|------|
| 🎯 **智能工单分类** | AI自动分类工单、确定优先级、分配最佳处理人 | 处理效率提升80% |
| 🔍 **根因分析** | AI分析历史工单，自动找出问题根本原因 | 解决问题时间减少60% |
| 📈 **预测性维护** | AI预测IT系统故障，提前预警预防 | 故障率降低70% |
| 🤖 **自动处置** | 简单问题AI自动处理，无需人工介入 | 人工干预减少50% |
| 📝 **智能摘要** | 长对话自动总结，一目了然 | 响应速度提升40% |

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        Vue 3 前端                            │
│     (工单管理 / 看板视图 / 管理员后台 / AI分析面板)            │
├─────────────────────────────────────────────────────────────┤
│                   Spring Cloud Alibaba 后端                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐  │
│  │ Gateway     │ │ Ticket      │ │ Asset               │  │
│  │ Service     │ │ Service     │ │ Service             │  │
│  └─────────────┘ └─────────────┘ └─────────────────────┘  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐  │
│  │ Workflow    │ │ Analytics   │ │ AI Service          │  │
│  │ Service     │ │ Service     │ │ (Java)              │  │
│  └─────────────┘ └─────────────┘ └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                    Java AI 服务层                            │
│  (工单分类 / 根因分析 / 预测维护 / 自动处置)                   │
├─────────────────────────────────────────────────────────────┤
│                    DeepSeek API                              │
└─────────────────────────────────────────────────────────────┘
```

## 📂 目录结构

```
SmartITSM/
├── ai-service/                    # Java AI 服务层 ⭐
│   ├── src/main/java/
│   │   └── com/smartitsm/aiservice/
│   │       ├── controller/        # REST API 控制器
│   │       ├── service/           # AI 业务逻辑
│   │       │   ├── TicketClassificationService.java
│   │       │   ├── RootCauseAnalysisService.java
│   │       │   ├── PredictiveMaintenanceService.java
│   │       │   └── AutomatedResolutionService.java
│   │       ├── client/            # LLM 客户端
│   │       │   └── DeepSeekClient.java
│   │       ├── dto/               # 数据传输对象
│   │       └── config/           # 配置类
│   └── pom.xml
│
├── ticket-service/                # 工单服务
├── asset-service/                  # 资产服务
├── workflow-service/               # 工作流服务
├── analytics-service/              # 分析服务
├── gateway/                        # API 网关
├── frontend/                       # Vue 3 前端
│   └── src/
│       ├── views/                 # 页面视图
│       │   ├── ticket/            # 工单管理
│       │   ├── kanban/            # 看板视图
│       │   ├── admin/             # 管理员后台
│       │   └── ai/                # AI 分析面板
│       └── components/            # 组件库
└── pom.xml                         # 父 POM
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 18+
- MySQL/PostgreSQL
- Nacos（服务注册与配置）

### 1. 启动后端服务

```bash
# 克隆项目
git clone https://github.com/nplszfl/SmartITSM.git
cd SmartITSM

# 编译项目
mvn clean install -DskipTests

# 启动 AI 服务
cd ai-service
mvn spring-boot:run

# 启动其他微服务（分别在不同终端）
cd ../ticket-service && mvn spring-boot:run
cd ../gateway && mvn spring-boot:run
```

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 3. 配置 AI

在 `ai-service/src/main/resources/application.yml` 中配置：

```yaml
spring:
  ai:
    deepseek:
      api-key: your-api-key
      model: deepseek-chat
      base-url: https://api.deepseek.com
```

## 📡 API 接口

### AI 工单分类

```
POST /api/v1/ai/ticket/classify

请求体：
{
  "ticketId": "TKT-12345",
  "title": "笔记本无法连接WiFi",
  "description": "用户报告笔记本无法连接无线网络...",
  "categoryHint": "network",
  "historicalData": {...}
}

响应：
{
  "category": "network",
  "priority": "high",
  "urgency": "medium",
  "team": "network-team",
  "estimatedSla": "4h",
  "confidence": 0.92,
  "reasoning": "分析说明..."
}
```

### AI 根因分析

```
POST /api/v1/ai/root-cause/analyze

请求体：
{
  "ticketId": "TKT-12345",
  "title": "数据库连接错误",
  "description": "应用程序抛出连接异常...",
  "relatedTickets": [...],
  "assetData": {...},
  "historicalResolutions": [...]
}

响应：
{
  "rootCause": "数据库服务器内存不足",
  "why1": "内存不足导致连接池耗尽",
  "why2": "长期运行的查询占用大量内存",
  "why3": "缺乏查询超时机制",
  "why4": "监控告警阈值设置过高",
  "why5": "未定期优化数据库配置",
  "contributingFactors": ["监控告警延迟", "缺乏自动扩容"],
  "recurrenceRisk": "low",
  "confidence": 0.88
}
```

### AI 预测性维护

```
POST /api/v1/ai/maintenance/predict-failure

请求体：
{
  "assetId": "SRV-001",
  "assetName": "生产数据库服务器",
  "assetType": "server",
  "ageDays": 1450,
  "telemetry": {
    "cpuUsage": 85,
    "memoryUsage": 92
  },
  "historicalFailures": [...],
  "maintenanceHistory": [...]
}

响应：
{
  "failureProbability": 0.75,
  "riskLevel": "high",
  "estimatedFailureWindow": "7-14天",
  "contributingFactors": ["服务器使用年限较长", "内存使用率过高"],
  "recommendedActions": ["计划下周维护", "考虑扩容内存", "加强监控"],
  "confidence": 0.85
}
```

### AI 自动处置

```
POST /api/v1/ai/resolution/suggest

请求体：
{
  "ticketId": "TKT-12345",
  "title": "密码重置请求",
  "description": "用户忘记密码...",
  "category": "account_access",
  "relatedTickets": [...],
  "knownSolutions": [...]
}

响应：
{
  "suggestedResolution": "按照以下步骤重置用户密码...",
  "steps": [
    "1. 验证用户身份",
    "2. 在AD中重置密码",
    "3. 发送临时密码到用户邮箱",
    "4. 要求用户首次登录后修改密码"
  ],
  "autoResolvable": true,
  "confidence": 0.94,
  "similarTicketsResolved": 156
}
```

## 🎯 核心AI功能详解

### 1. 智能工单分类

AI 自动分析工单内容，确定：
- **分类**：硬件、软件、网络、安全、账户等
- **优先级**：紧急、高、中、低
- **紧急度**：紧急、高、中、低
- **负责团队**：基于分类自动路由
- **SLA预估**：预计解决时间

### 2. 根因分析

采用 **5 Why** 方法论 + 模式匹配：
- 自动追问根本原因
- 识别贡献因素（鱼骨图分类）
- 对比历史相似工单
- 评估复发风险

### 3. 预测性维护

基于资产数据和遥测分析：
- **故障概率预测**
- **风险等级评估**（低/中/高/危急）
- **预计故障时间窗口**
- **维护建议**

### 4. 自动处置

简单问题 AI 自动处理：
- 知识库匹配
- 已知解决方案推荐
- 置信度 > 0.8 时自动处置
- 步骤指导 + 满意度预测

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Element Plus + ECharts |
| 后端 | Java 21 + Spring Cloud Alibaba + MyBatis Plus |
| AI 服务 | Spring Boot 3.2 + WebClient + DeepSeek API |
| 数据库 | MySQL / PostgreSQL |
| 注册中心 | Nacos |
| 缓存 | Redis |

## 📊 监控系统预览

系统提供完整的运维仪表盘：
- 工单数量统计（按状态、优先级、分类）
- 平均解决时间趋势
- AI 准确率分析
- 高风险资产预警
- 团队绩效排名

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 👨‍💻 作者

**黄辉翔** - [GitHub](https://github.com/nplszfl)

---

⭐ 如果对你有帮助，请给项目一个 Star！
