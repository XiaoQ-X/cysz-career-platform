# 第一期实施计划索引

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement each linked plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将第一期拆成可独立验收、按依赖顺序交付的子项目，最终形成“简历、岗位与 AI”闭环。

**Architecture:** 仓库采用 `frontend/`、`backend/`、`infra/` 三部分；Vue 通过 `/api/v1` REST 接口访问 Spring Boot，MySQL 保存业务数据，文件存储、AI、Coze 和岗位采集均通过适配器隔离。每个子项目在上一个子项目的稳定接口上继续开发。

**Tech Stack:** Vue 3.5.41、TypeScript 7.0.2、Vite 8.2.2、Spring Boot 4.1.1、Java 21、Maven 3.9+、MySQL 8.4 LTS、Flyway、Vitest、Playwright、JUnit 5、Testcontainers。

**Spec:** `docs/superpowers/specs/2026-08-26-career-platform-design.md`

## Global Constraints

- 平台仅服务朝阳师范学院学生，面向所有专业。
- 学生可从任意模块开始，不设置平台级强制流程。
- AI 只提供分析和修改建议，不擅自改写或提交信息。
- 基础简历由简历诊断和岗位匹配共同使用，禁止重复上传流程。
- 未经学生授权，不向老师、管理员或 Coze 提供简历正文。
- 第一期不实现自动投递、校园活动、就业资讯、招聘会、复杂数据大屏。
- 第一期未开放的第二期入口隐藏或明确显示“即将上线”，不得形成无效链接。
- 首页视觉以 `docs/design/career-platform-homepage-final-v3.png` 为基准。

---

## 子项目与依赖顺序

1. **工程底座、登录权限与学生首页**  
   计划：`docs/superpowers/plans/2026-08-27-foundation-auth-homepage.md`  
   交付：可启动的前后端、数据库迁移、三角色登录、权限隔离、响应式首页、CI。

2. **统一简历中心**  
   依赖：1  
   交付：PDF/DOCX 上传、授权、解析适配器、手工修正、基础简历、版本、恢复与级联删除。

3. **简历诊断与建议确认**  
   依赖：2  
   交付：诊断适配器、可执行建议、逐条接受/编辑/忽略、失败重试、新版本保存。

4. **模板中心与 PDF 导出**  
   依赖：2  
   交付：模板授权记录、分类推荐、预览切换、样式设置、稳定基础模板与受控 PDF 导出。

5. **公共岗位库与管理导入**  
   依赖：1  
   交付：岗位标准模型、搜索筛选、详情收藏、Excel/CSV 映射预览、审核发布、过期与来源状态。

6. **求职偏好与智能匹配**  
   依赖：2、5  
   交付：偏好优先规则、硬条件过滤、可解释排序、无结果降级、岗位定制简历入口。

7. **朝小职 Coze 助手**  
   依赖：1；如解释简历和岗位结果则依赖 3、6  
   交付：可拖动/收起/展开、页面快捷提问、独立适配层、个人数据单独授权、超时降级。

8. **基础管理后台与隐私审计**  
   依赖：2、4、5、7  
   交付：用户角色、岗位与模板运营、采集器/Coze 状态、文件状态、删除请求、授权与管理操作审计。

9. **第一期整体验收与发布**  
   依赖：1–8  
   交付：端到端主流程、可访问性、响应式、安全扫描、备份恢复演练和部署文档。

## 跨计划稳定接口

```text
REST prefix: /api/v1
Auth header: Authorization: Bearer <access-token>
Success body: { "data": ..., "traceId": "..." }
Error body: { "code": "...", "message": "...", "fieldErrors": {}, "traceId": "..." }
Roles: STUDENT | TEACHER | ADMIN
User id: UUID
Time: ISO-8601 UTC on API, Asia/Shanghai for UI display
```

外部系统统一从端口接口进入：

```java
public interface ResumeParserPort {}
public interface ResumeDiagnosisPort {}
public interface JobSourcePort {}
public interface JobMatcherPort {}
public interface CozeConversationPort {}
public interface ObjectStoragePort {}
```

这些端口由对应子项目补充精确方法签名；Controller 和页面不得直接依赖第三方 SDK。

