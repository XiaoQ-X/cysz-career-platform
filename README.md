# 朝阳师范学院职业发展平台

## 本地启动

需要 Java 21、Maven 3.9+、Node `^20.19.0 || >=22.12.0` 和 Docker Compose。

```powershell
docker compose -f infra/compose.yaml up -d
Set-Location backend
$env:E2E_AUTH_TRUSTED_ORIGINS='http://127.0.0.1:5173,http://localhost:5173'
./mvnw.cmd -Pe2e "-Dspring-boot.run.profiles=e2e" spring-boot:run
Set-Location ../frontend
npm ci
npx playwright install chromium
npm run dev -- --host 127.0.0.1
```

```text
Frontend: http://127.0.0.1:5173
Backend:  http://127.0.0.1:8080
MySQL:    localhost:3307
```

前端开发服务器会把 `/api` 代理到 `http://127.0.0.1:8080`，因此浏览器侧始终通过同源路径访问认证接口。

## 端到端验收

Playwright 会自动启动：

- Maven `e2e` 构建 profile 中的后端测试夹具与 Spring `e2e` profile，使用专用 MySQL schema `career_platform_e2e`
- 前端 Vite dev server，监听 `127.0.0.1:5173`

Playwright 会等待 `http://127.0.0.1:8080/actuator/health/readiness`。该条件只有在应用启动 runner（包括测试账号夹具）完成且数据库可连接后才会就绪。

```powershell
docker compose -f infra/compose.yaml up -d
Set-Location frontend
npm ci
npx playwright install chromium
npm run test:e2e
```

`e2e` profile 只在验收链路中创建测试账号：

- `student / Student123!`
- `teacher / Teacher123!`
- `admin / Admin123!`

`backend/src/e2e/` 只由显式 `-Pe2e` 构建加入运行时。默认生产构建不会编译或打包该目录中的配置、固定测试凭据或不安全 cookie 设置；`ProductionArtifactIT` 在 `verify` 阶段检查这一边界。

## 生产制品检查

```powershell
Set-Location backend
./mvnw.cmd clean verify
```

默认制品是 `backend/target/career-platform-0.0.1-SNAPSHOT.jar`。不要为部署启用 Maven `e2e` profile 或 Spring `e2e` profile；生产环境必须单独提供数据库连接、足够强度的 `JWT_SIGNING_KEY`、HTTPS 下的安全 refresh cookie 和可信前端来源。
