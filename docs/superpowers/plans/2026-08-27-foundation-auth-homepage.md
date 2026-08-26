# 工程底座、登录权限与学生首页 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立可持续扩展的 Vue 3 + Spring Boot + MySQL 工程，完成三角色身份认证、权限隔离和符合最终视觉基准的学生首页。

**Architecture:** `frontend/` 是 Vue SPA，`backend/` 是按业务能力分包的 Spring Boot REST 服务，`infra/` 保存本地依赖编排。认证使用短期 JWT access token 与 HttpOnly refresh cookie；未来学校统一身份认证通过 `IdentityProvider` 端口替换，本期页面和权限模型无需重写。

**Tech Stack:** Vue 3.5.41、Vue Router 5.2.0、Pinia 4.0.3、Axios 1.20.0、TypeScript 7.0.2、Vite 8.2.2、Vitest 4.1.11、Playwright 1.62.1、Spring Boot 4.1.1、Java 21、Maven 3.9+、Spring Security、Spring Data JPA、Flyway、MySQL 8.4 LTS、JUnit 5、Testcontainers。

**Spec:** `docs/superpowers/specs/2026-08-26-career-platform-design.md`

## Global Constraints

- 平台仅服务朝阳师范学院，角色固定为 `STUDENT`、`TEACHER`、`ADMIN`。
- 学生首页不展示校园活动，不在学生提供信息前展示个性化岗位。
- 朝小职在本计划只实现非阻塞悬浮外壳；真实 Coze 会话由后续独立计划接入。
- 首页导航使用“首页、简历优化、岗位探索、职业测评、课程指导、我的”。
- “职业测评”和“课程指导”显示“即将上线”且不可进入无效页面。
- API 时间一律输出 UTC ISO-8601，前端按 `Asia/Shanghai` 展示。
- 日志、错误响应和管理界面不得包含密码、refresh token 或简历正文。
- 首页视觉以 `docs/design/career-platform-homepage-final-v3.png` 为准，同时满足键盘操作、焦点可见、颜色对比和窄屏布局。

---

## 文件结构

```text
frontend/
  src/app/              # 应用启动、路由、全局样式
  src/features/auth/    # 登录、会话、路由守卫
  src/features/home/    # 首页分区与首页专用组件
  src/shared/api/       # Axios、API envelope、错误类型
  src/shared/ui/        # Button、ComingSoonLink、SkipLink
  tests/e2e/            # Playwright 主流程
backend/
  src/main/java/cn/edu/cysz/careerplatform/
    common/             # API envelope、异常与 trace id
    auth/               # 登录、refresh、logout、JWT
    user/               # 用户、角色和当前用户查询
    health/             # 对外健康接口
  src/main/resources/db/migration/ # Flyway
infra/
  compose.yaml          # MySQL 本地环境
.github/workflows/ci.yml
```

### Task 1: 建立可重复启动的仓库骨架

**Files:**
- Create: `.editorconfig`
- Create: `.gitignore`
- Create: `README.md`
- Create: `infra/compose.yaml`
- Create: `frontend/`（由官方 `create-vue` 生成）
- Create: `backend/`（由 Spring Initializr 生成）

**Interfaces:**
- Consumes: Java 21、Maven 3.9+、Node `^20.19.0 || >=22.12.0`、Docker Compose。
- Produces: `npm run dev`、`npm run test:unit`、`./mvnw spring-boot:run`、`./mvnw test` 和 MySQL `localhost:3307`。

- [ ] **Step 1: 用官方脚手架生成前端并记录确定选项**

```powershell
npm create vue@latest frontend -- --typescript --router --pinia --vitest --playwright --eslint-with-prettier
```

删除脚手架示例页面与示例测试，只保留启动所需文件。执行后确认 `package.json` 锁定本计划头部版本或兼容的补丁版本，并提交 `package-lock.json`。

- [ ] **Step 2: 用 Spring Initializr 生成后端**

```powershell
$uri = 'https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=4.1.1&baseDir=backend&groupId=cn.edu.cysz&artifactId=career-platform&name=career-platform&packageName=cn.edu.cysz.careerplatform&packaging=jar&javaVersion=21&dependencies=web,validation,security,data-jpa,flyway,mysql,actuator'
Invoke-WebRequest -Uri $uri -OutFile backend.zip
Expand-Archive -LiteralPath backend.zip -DestinationPath .
Remove-Item -LiteralPath backend.zip
```

- [ ] **Step 3: 写本地 MySQL 编排**

```yaml
services:
  mysql:
    image: mysql:8.4
    environment:
      MYSQL_DATABASE: career_platform
      MYSQL_USER: career_app
      MYSQL_PASSWORD: career_local
      MYSQL_ROOT_PASSWORD: root_local
      TZ: Asia/Shanghai
    ports:
      - "3307:3306"
    volumes:
      - career_mysql:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-proot_local"]
      interval: 5s
      timeout: 3s
      retries: 20
volumes:
  career_mysql:
```

- [ ] **Step 4: 写统一入口文档**

`README.md` 必须包含以下命令和端口：

```powershell
docker compose -f infra/compose.yaml up -d
Set-Location backend
./mvnw spring-boot:run
Set-Location ../frontend
npm ci
npm run dev
```

```text
Frontend: http://localhost:5173
Backend:  http://localhost:8080
MySQL:    localhost:3307
```

- [ ] **Step 5: 验证双端基线**

```powershell
Set-Location backend
./mvnw test
Set-Location ../frontend
npm ci
npm run test:unit -- --run
npm run build
```

Expected: Maven 测试、Vitest 与 Vite build 均以退出码 0 完成。

- [ ] **Step 6: 提交**

```powershell
git add .editorconfig .gitignore README.md infra frontend backend
git commit -m "build: scaffold career platform applications"
```

### Task 2: 建立数据库迁移与用户领域

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_users.sql`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/user/UserRole.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/user/UserAccount.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/user/UserAccountRepository.java`
- Create: `backend/src/test/java/cn/edu/cysz/careerplatform/user/UserAccountRepositoryTest.java`
- Modify: `backend/src/main/resources/application.yaml`
- Create: `backend/src/test/resources/application-test.yaml`

**Interfaces:**
- Consumes: MySQL 8.4、Spring Data JPA、Flyway。
- Produces: `UserAccountRepository.findByUsernameIgnoreCase(String)` 和角色枚举 `STUDENT|TEACHER|ADMIN`。

- [ ] **Step 1: 添加数据库集成测试依赖**

```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>mysql</artifactId>
  <scope>test</scope>
</dependency>
```

- [ ] **Step 2: 写失败的仓储集成测试**

```java
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class UserAccountRepositoryTest {
  @Container
  static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

  @Autowired UserAccountRepository repository;

  @Test
  void findsActiveUserCaseInsensitively() {
    repository.save(UserAccount.create("20260001", "hash", "张同学", UserRole.STUDENT));
    assertThat(repository.findByUsernameIgnoreCase("20260001")).get()
        .extracting(UserAccount::getRole).isEqualTo(UserRole.STUDENT);
  }
}
```

- [ ] **Step 3: 运行测试确认失败**

```powershell
Set-Location backend
./mvnw -Dtest=UserAccountRepositoryTest test
```

Expected: FAIL，原因是用户领域类型和表尚不存在。

- [ ] **Step 4: 创建迁移**

```sql
CREATE TABLE user_account (
  id BINARY(16) NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(80) NOT NULL,
  role VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  token_version INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_account_username (username),
  CONSTRAINT chk_user_role CHECK (role IN ('STUDENT','TEACHER','ADMIN')),
  CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE','DISABLED'))
);
```

- [ ] **Step 5: 实现最小领域模型与仓储**

```java
public enum UserRole { STUDENT, TEACHER, ADMIN }

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
  Optional<UserAccount> findByUsernameIgnoreCase(String username);
}
```

`UserAccount` 使用 `@UuidGenerator`、`@Enumerated(EnumType.STRING)`，公开 `create` 工厂，不公开密码散列的 JSON 序列化入口。

- [ ] **Step 6: 配置数据源并验证迁移与测试**

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3307/career_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC}
    username: ${DB_USERNAME:career_app}
    password: ${DB_PASSWORD:career_local}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

```powershell
./mvnw -Dtest=UserAccountRepositoryTest test
```

Expected: PASS，Flyway 创建表且 Hibernate 校验通过。

- [ ] **Step 7: 提交**

```powershell
git add backend/src/main backend/src/test
git commit -m "feat: add user account persistence"
```

### Task 3: 定义统一 API 响应、错误和 trace id

**Files:**
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/common/api/ApiResponse.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/common/api/ApiError.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/common/api/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/common/web/TraceIdFilter.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/health/HealthController.java`
- Create: `backend/src/test/java/cn/edu/cysz/careerplatform/health/HealthControllerTest.java`

**Interfaces:**
- Consumes: Servlet request and Bean Validation exceptions。
- Produces: `{data,traceId}`、`{code,message,fieldErrors,traceId}` 和 `GET /api/v1/health`。

- [ ] **Step 1: 写失败的 MVC 契约测试**

```java
@WebMvcTest(HealthController.class)
@Import({TraceIdFilter.class, GlobalExceptionHandler.class})
class HealthControllerTest {
  @Autowired MockMvc mvc;

  @Test
  void returnsEnvelopeAndTraceId() throws Exception {
    mvc.perform(get("/api/v1/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.status").value("UP"))
      .andExpect(jsonPath("$.traceId").isNotEmpty())
      .andExpect(header().exists("X-Trace-Id"));
  }
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
./mvnw -Dtest=HealthControllerTest test
```

Expected: FAIL，`HealthController` 和响应类型尚不存在。

- [ ] **Step 3: 实现响应类型与健康接口**

```java
public record ApiResponse<T>(T data, String traceId) {
  public static <T> ApiResponse<T> of(T data, String traceId) {
    return new ApiResponse<>(data, traceId);
  }
}

public record ApiError(
    String code,
    String message,
    Map<String, String> fieldErrors,
    String traceId) {}

@RestController
@RequestMapping("/api/v1/health")
class HealthController {
  @GetMapping
  ApiResponse<Map<String, String>> health(HttpServletRequest request) {
    return ApiResponse.of(Map.of("status", "UP"), (String) request.getAttribute("traceId"));
  }
}
```

`TraceIdFilter` 接受合法 `X-Trace-Id` 或生成 UUID，并同时写入 request attribute、MDC 和 response header；`GlobalExceptionHandler` 将验证错误映射为 `VALIDATION_FAILED`，不得返回堆栈。

- [ ] **Step 4: 验证**

```powershell
./mvnw -Dtest=HealthControllerTest test
```

Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add backend/src/main/java/cn/edu/cysz/careerplatform/common backend/src/main/java/cn/edu/cysz/careerplatform/health backend/src/test/java/cn/edu/cysz/careerplatform/health
git commit -m "feat: define api response contract"
```

### Task 4: 实现登录、refresh、logout 与身份提供端口

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__create_refresh_sessions.sql`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/auth/IdentityProvider.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/auth/LocalIdentityProvider.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/auth/JwtTokenService.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/auth/RefreshSession.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/auth/RefreshSessionRepository.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/auth/AuthService.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/auth/AuthController.java`
- Create: `backend/src/test/java/cn/edu/cysz/careerplatform/auth/AuthControllerTest.java`
- Modify: `backend/pom.xml`

**Interfaces:**
- Consumes: `IdentityProvider.authenticate(String,char[])`。
- Produces: `POST /api/v1/auth/login`、`POST /api/v1/auth/refresh`、`POST /api/v1/auth/logout`；access token 15 分钟，refresh cookie 7 天并轮换。

- [ ] **Step 1: 添加 JWT 依赖并写失败的登录测试**

```xml
<dependency>
  <groupId>com.nimbusds</groupId>
  <artifactId>nimbus-jose-jwt</artifactId>
</dependency>
```

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
  @Autowired MockMvc mvc;

  @Test
  void loginReturnsAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
    mvc.perform(post("/api/v1/auth/login")
        .contentType(APPLICATION_JSON)
        .content("""{"username":"student","password":"Student123!"}"""))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
      .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
      .andExpect(cookie().httpOnly("career_refresh", true))
      .andExpect(cookie().sameSite("career_refresh", "Strict"));
  }
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
./mvnw -Dtest=AuthControllerTest test
```

Expected: FAIL，认证接口尚不存在。

- [ ] **Step 3: 创建 refresh session 表**

```sql
CREATE TABLE refresh_session (
  id BINARY(16) NOT NULL,
  user_id BINARY(16) NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  revoked_at TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_token_hash (token_hash),
  KEY idx_refresh_user (user_id),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);
```

- [ ] **Step 4: 定义认证端口并实现本地认证**

```java
public interface IdentityProvider {
  AuthenticatedIdentity authenticate(String username, char[] password);
}

public record AuthenticatedIdentity(UUID userId, String username, String displayName, UserRole role) {}
```

`LocalIdentityProvider` 使用 `PasswordEncoder.matches`；无论用户不存在还是密码错误，返回相同 `INVALID_CREDENTIALS`。测试环境通过 `@TestConfiguration` 写入三个 BCrypt 测试账号，不在生产迁移中保存默认密码。

- [ ] **Step 5: 实现 token 与会话轮换**

JWT claim 固定为：

```json
{"sub":"<uuid>","username":"student","role":"STUDENT","tokenVersion":0,"iat":0,"exp":0}
```

refresh token 使用 256 位随机值，仅将 SHA-256 保存到数据库；refresh 成功后撤销旧 session 并创建新 session；logout 撤销当前 session 并清除 cookie。

- [ ] **Step 6: 验证登录、错误密码、refresh 轮换与 logout**

```powershell
./mvnw -Dtest=AuthControllerTest test
```

Expected: 所有认证测试 PASS，响应中不存在 refresh token 明文。

- [ ] **Step 7: 提交**

```powershell
git add backend/pom.xml backend/src/main backend/src/test
git commit -m "feat: add secure session authentication"
```

### Task 5: 建立 Spring Security 权限隔离与当前用户接口

**Files:**
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/auth/SecurityConfig.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/auth/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/cn/edu/cysz/careerplatform/user/CurrentUserController.java`
- Create: `backend/src/test/java/cn/edu/cysz/careerplatform/auth/AuthorizationTest.java`

**Interfaces:**
- Consumes: Bearer access token。
- Produces: `GET /api/v1/users/me`；`/api/v1/admin/**` 仅 ADMIN，`/api/v1/teacher/**` 仅 TEACHER，学生业务仅 STUDENT。

- [ ] **Step 1: 写失败的授权矩阵测试**

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationTest {
  @Autowired MockMvc mvc;
  @Autowired JwtTokenService tokens;

  @Test
  void studentCannotAccessAdminApi() throws Exception {
    mvc.perform(get("/api/v1/admin/ping")
        .header("Authorization", "Bearer " + tokens.testToken(UserRole.STUDENT)))
      .andExpect(status().isForbidden());
  }

  @Test
  void unauthenticatedRequestReturns401Envelope() throws Exception {
    mvc.perform(get("/api/v1/users/me"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
./mvnw -Dtest=AuthorizationTest test
```

Expected: FAIL，安全链尚未配置目标行为。

- [ ] **Step 3: 实现无状态安全链**

```java
http
  .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout"))
  .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/api/v1/health", "/api/v1/auth/**").permitAll()
      .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
      .requestMatchers("/api/v1/teacher/**").hasRole("TEACHER")
      .anyRequest().authenticated())
  .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

401 与 403 必须复用 `ApiError`，不得返回 Spring 默认 HTML 或异常类名。

- [ ] **Step 4: 实现当前用户接口并验证**

```java
public record CurrentUserResponse(UUID id, String username, String displayName, UserRole role) {}
```

```powershell
./mvnw -Dtest=AuthorizationTest test
```

Expected: 授权矩阵 PASS。

- [ ] **Step 5: 提交**

```powershell
git add backend/src/main/java/cn/edu/cysz/careerplatform/auth backend/src/main/java/cn/edu/cysz/careerplatform/user backend/src/test/java/cn/edu/cysz/careerplatform/auth
git commit -m "feat: enforce role based api access"
```

### Task 6: 建立前端 API 客户端、会话 store 与路由守卫

**Files:**
- Create: `frontend/src/shared/api/contracts.ts`
- Create: `frontend/src/shared/api/http.ts`
- Create: `frontend/src/features/auth/auth.api.ts`
- Create: `frontend/src/features/auth/auth.store.ts`
- Create: `frontend/src/features/auth/routeGuard.ts`
- Create: `frontend/src/features/auth/__tests__/auth.store.spec.ts`
- Modify: `frontend/src/app/router.ts`

**Interfaces:**
- Consumes: Task 4–5 的认证 API。
- Produces: `useAuthStore().login/restore/logout`、仅内存 access token、按 `meta.roles` 守卫路由。

- [ ] **Step 1: 定义前端契约并写失败的 store 测试**

```ts
export type UserRole = 'STUDENT' | 'TEACHER' | 'ADMIN'
export interface CurrentUser { id: string; username: string; displayName: string; role: UserRole }
export interface ApiResponse<T> { data: T; traceId: string }
export interface ApiError { code: string; message: string; fieldErrors: Record<string, string>; traceId: string }
export interface LoginResult { accessToken: string; expiresInSeconds: number; user: CurrentUser }
```

```ts
it('keeps access token out of localStorage', async () => {
  vi.spyOn(authApi, 'login').mockResolvedValue({ accessToken: 'access', expiresInSeconds: 900, user: student })
  const store = useAuthStore()
  await store.login('student', 'Student123!')
  expect(store.user?.role).toBe('STUDENT')
  expect(localStorage.getItem('accessToken')).toBeNull()
})
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
Set-Location frontend
npm run test:unit -- auth.store.spec.ts --run
```

Expected: FAIL，store 和 API 尚不存在。

- [ ] **Step 3: 实现 Axios 客户端与单次 refresh 队列**

```ts
export const http = axios.create({ baseURL: '/api/v1', withCredentials: true, timeout: 10_000 })
```

请求拦截器从 Pinia 内存状态添加 Bearer token；响应遇到首次 401 时，只允许一个 refresh Promise，其余请求等待该 Promise，refresh 失败则清空会话并跳转登录。登录、refresh、logout 请求不得进入再次 refresh 的循环。

- [ ] **Step 4: 实现 store 与路由守卫**

```ts
export interface RouteMeta { requiresAuth?: boolean; roles?: UserRole[] }
```

守卫规则：未登录访问受保护路由跳转 `/login?redirect=<encoded>`；角色不匹配跳转 `/forbidden`；第二期路由不注册。

- [ ] **Step 5: 验证**

```powershell
npm run test:unit -- --run
npm run build
```

Expected: store 测试与 TypeScript build PASS。

- [ ] **Step 6: 提交**

```powershell
git add frontend/src
git commit -m "feat: add frontend session management"
```

### Task 7: 实现三角色登录与明确入口

**Files:**
- Create: `frontend/src/features/auth/LoginView.vue`
- Create: `frontend/src/features/auth/components/LoginForm.vue`
- Create: `frontend/src/features/auth/ForbiddenView.vue`
- Create: `frontend/src/features/auth/__tests__/LoginForm.spec.ts`
- Modify: `frontend/src/app/router.ts`

**Interfaces:**
- Consumes: `useAuthStore().login`。
- Produces: `/login`、`/forbidden`；登录后学生去 `/`、老师去 `/teacher`、管理员去 `/admin`。

- [ ] **Step 1: 写失败的组件测试**

```ts
it('submits credentials and exposes accessible error feedback', async () => {
  const wrapper = mount(LoginForm, { global: { plugins: [testingPinia] } })
  await wrapper.get('input[name="username"]').setValue('student')
  await wrapper.get('input[name="password"]').setValue('wrong')
  await wrapper.get('form').trigger('submit')
  expect(wrapper.get('[role="alert"]').text()).toContain('用户名或密码错误')
})
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
npm run test:unit -- LoginForm.spec.ts --run
```

Expected: FAIL，登录组件不存在。

- [ ] **Step 3: 实现登录视图**

页面必须包含：产品全称、学生/指导老师/管理员身份说明、用户名、密码、显示/隐藏密码、登录按钮、隐私说明链接和账号帮助文本。错误区域使用 `role="alert"`；提交期间按钮禁用但保留可读文案“正在登录”。

```ts
const destinationByRole: Record<UserRole, string> = {
  STUDENT: '/',
  TEACHER: '/teacher',
  ADMIN: '/admin',
}
```

- [ ] **Step 4: 验证组件、键盘与构建**

```powershell
npm run test:unit -- --run
npm run build
```

Expected: PASS；Tab 顺序为用户名、密码、显示密码、登录、隐私、帮助。

- [ ] **Step 5: 提交**

```powershell
git add frontend/src/features/auth frontend/src/app/router.ts
git commit -m "feat: add role aware login experience"
```

### Task 8: 建立首页设计 token 与可访问基础组件

**Files:**
- Create: `frontend/src/app/styles/tokens.css`
- Create: `frontend/src/app/styles/base.css`
- Create: `frontend/src/shared/ui/AppButton.vue`
- Create: `frontend/src/shared/ui/ComingSoonLink.vue`
- Create: `frontend/src/shared/ui/SkipLink.vue`
- Create: `frontend/src/shared/ui/__tests__/ComingSoonLink.spec.ts`
- Modify: `frontend/src/main.ts`

**Interfaces:**
- Consumes: 最终首页视觉基准。
- Produces: 颜色、排版、间距、圆角、焦点样式与“即将上线”统一行为。

- [ ] **Step 1: 写失败的无效链接防护测试**

```ts
it('renders coming soon as a non-navigation control', () => {
  const wrapper = mount(ComingSoonLink, { props: { label: '职业测评' } })
  expect(wrapper.find('a').exists()).toBe(false)
  expect(wrapper.get('button').attributes('disabled')).toBeDefined()
  expect(wrapper.text()).toContain('即将上线')
})
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
npm run test:unit -- ComingSoonLink.spec.ts --run
```

Expected: FAIL，组件不存在。

- [ ] **Step 3: 定义 token**

```css
:root {
  --color-bg: #070b1d;
  --color-surface: #101834;
  --color-surface-soft: #162044;
  --color-text: #f7f9ff;
  --color-text-muted: #b8c2df;
  --color-cyan: #62e9ff;
  --color-violet: #9f86ff;
  --color-amber: #ffc96b;
  --color-danger: #ff6b7a;
  --focus-ring: 0 0 0 3px rgb(98 233 255 / 45%);
  --content-max: 1200px;
  --radius-control: 999px;
  --radius-panel: 28px;
}
```

`base.css` 必须设置 `color-scheme: dark`、正文最小 16px、`prefers-reduced-motion` 下关闭非必要动画，并为 `:focus-visible` 提供明显轮廓。

- [ ] **Step 4: 实现组件并验证**

```powershell
npm run test:unit -- --run
npm run build
```

Expected: PASS。

- [ ] **Step 5: 提交**

```powershell
git add frontend/src/app/styles frontend/src/shared/ui frontend/src/main.ts
git commit -m "feat: establish accessible visual foundation"
```

### Task 9: 按视觉基准实现学生首页

**Files:**
- Create: `frontend/src/features/home/HomeView.vue`
- Create: `frontend/src/features/home/components/HomeHeader.vue`
- Create: `frontend/src/features/home/components/HeroSection.vue`
- Create: `frontend/src/features/home/components/JobEntrySection.vue`
- Create: `frontend/src/features/home/components/ResumeSection.vue`
- Create: `frontend/src/features/home/components/IndependentServicesSection.vue`
- Create: `frontend/src/features/home/components/XiaoZhiShell.vue`
- Create: `frontend/src/features/home/__tests__/HomeView.spec.ts`
- Modify: `frontend/src/app/router.ts`

**Interfaces:**
- Consumes: Task 8 token 与基础组件、当前学生会话。
- Produces: `/` 首页；简历和岗位按钮先指向已命名的“功能建设中”路由，后续计划在同一路由替换页面，不产生 404。

- [ ] **Step 1: 写失败的首页产品约束测试**

```ts
it('shows allowed entry points without premature recommendations', () => {
  const wrapper = mount(HomeView, { global: { plugins: [router, testingPinia] } })
  expect(wrapper.text()).toContain('你的未来，不止一种答案')
  expect(wrapper.text()).toContain('选择简历匹配')
  expect(wrapper.text()).toContain('填写求职偏好')
  expect(wrapper.text()).toContain('直接浏览岗位库')
  expect(wrapper.text()).toContain('完成简历或求职偏好中的任意一项后，为你筛选岗位')
  expect(wrapper.text()).not.toContain('推荐岗位')
  expect(wrapper.text()).not.toContain('校园活动')
})
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
npm run test:unit -- HomeView.spec.ts --run
```

Expected: FAIL，首页组件不存在。

- [ ] **Step 3: 实现语义结构**

```html
<SkipLink target="#main-content" />
<HomeHeader />
<main id="main-content">
  <HeroSection />
  <JobEntrySection />
  <ResumeSection />
  <IndependentServicesSection />
</main>
<XiaoZhiShell />
```

必须保留的首页文案：

```text
你的未来，不止一种答案
今天想从哪里开始？
选择简历匹配
填写求职偏好
直接浏览岗位库
简历仅用于本次分析与岗位匹配，你可以随时删除
完成简历或求职偏好中的任意一项后，为你筛选岗位
一份简历，多种用途
你可以随时从任何模块开始
```

- [ ] **Step 4: 实现视觉与响应式布局**

桌面保持星图、轨道、深夜蓝—靛青—青紫体系和非卡片化分区；窄屏改为单列，装饰轨道 `aria-hidden="true"`，文字与操作顺序不依赖绝对定位。朝小职外壳固定在右侧，支持收起/展开，不遮挡主要按钮；`prefers-reduced-motion` 下不漂浮。

- [ ] **Step 5: 验证首页组件与生产构建**

```powershell
npm run test:unit -- --run
npm run build
```

Expected: PASS，构建无类型错误。

- [ ] **Step 6: 提交**

```powershell
git add frontend/src/features/home frontend/src/app/router.ts
git commit -m "feat: implement student career homepage"
```

### Task 10: 添加端到端验收、可访问性检查和 CI

**Files:**
- Create: `frontend/tests/e2e/auth-home.spec.ts`
- Modify: `frontend/playwright.config.ts`
- Create: `.github/workflows/ci.yml`
- Modify: `README.md`

**Interfaces:**
- Consumes: 可运行的 MySQL、后端和前端。
- Produces: 登录到首页、角色拒绝、refresh 恢复、首页关键文案和窄屏不遮挡的自动验收。

- [ ] **Step 1: 写端到端测试**

```ts
async function loginAs(page: Page, username: string, password: string) {
  await page.goto('/login')
  await page.getByLabel('用户名').fill(username)
  await page.getByLabel('密码').fill(password)
  await page.getByRole('button', { name: '登录' }).click()
}

test('student logs in and reaches the independent-service homepage', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('用户名').fill('student')
  await page.getByLabel('密码').fill('Student123!')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL('/')
  await expect(page.getByRole('heading', { name: '你的未来，不止一种答案' })).toBeVisible()
  await expect(page.getByText('完成简历或求职偏好中的任意一项后，为你筛选岗位')).toBeVisible()
  await expect(page.getByText('校园活动')).toHaveCount(0)
})

test('student is denied admin routes', async ({ page }) => {
  await loginAs(page, 'student', 'Student123!')
  await page.goto('/admin')
  await expect(page).toHaveURL('/forbidden')
})
```

- [ ] **Step 2: 运行测试确认环境或实现缺口**

```powershell
Set-Location frontend
npm run test:e2e
```

Expected: 首次运行若测试账号启动配置或 webServer 尚未接入则 FAIL，不能跳过。

- [ ] **Step 3: 配置 Playwright 双 webServer**

```ts
webServer: [
  { command: '../backend/mvnw spring-boot:run -Dspring-boot.run.profiles=e2e', port: 8080, reuseExistingServer: !process.env.CI },
  { command: 'npm run dev -- --host 127.0.0.1', port: 5173, reuseExistingServer: !process.env.CI },
]
```

`application-e2e.yaml` 使用专用数据库，并由启动 runner 写入三个 BCrypt 测试账号；该 profile 不得在生产激活。

- [ ] **Step 4: 写 CI**

CI 顺序固定为：

```text
backend: ./mvnw test
frontend: npm ci
frontend: npm run lint
frontend: npm run test:unit -- --run
frontend: npm run build
e2e: docker compose -f infra/compose.yaml up -d
e2e: npm run test:e2e
```

失败时上传 `frontend/playwright-report/`，不得提交截图基线的临时产物。

- [ ] **Step 5: 运行完整验证**

```powershell
Set-Location backend
./mvnw test
Set-Location ../frontend
npm ci
npm run lint
npm run test:unit -- --run
npm run build
npm run test:e2e
```

Expected: 所有命令退出码 0；学生首页在桌面与 390px 宽度均无横向滚动，朝小职不遮挡主要操作。

- [ ] **Step 6: 提交**

```powershell
git add .github frontend/tests frontend/playwright.config.ts README.md backend/src/main/resources/application-e2e.yaml
git commit -m "test: verify authentication and homepage flow"
```

## Self-Review 结果

- 规格覆盖：本计划覆盖工程底座、三角色登录、权限隔离、首页、第二期入口处理、基础 Coze 外壳、响应式与可访问性；简历、岗位、真实 Coze、管理后台按路线图独立规划。
- 未决项扫描：计划未使用未决实现项；后续业务路由使用明确“功能建设中”页面，不产生 404。
- 类型一致性：角色统一为 `STUDENT | TEACHER | ADMIN`；用户 ID 统一为 UUID；API envelope、登录结果与路由守卫字段一致。
- 安全检查：access token 只驻留内存；refresh token 仅 HttpOnly cookie；数据库只保存 refresh token 哈希；默认账号仅存在于 test/e2e profile。
