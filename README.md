# 朝阳师范学院职业发展平台

## 本地启动

需要 Java 21、Maven 3.9+、Node `^20.19.0 || >=22.12.0` 和 Docker Compose。

```powershell
docker compose -f infra/compose.yaml up -d
Set-Location backend
$env:E2E_AUTH_TRUSTED_ORIGINS='http://127.0.0.1:5173,http://localhost:5173'
./mvnw.cmd "-Dspring-boot.run.profiles=e2e" spring-boot:run
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

- 后端 `e2e` profile，使用专用 MySQL schema `career_platform_e2e`
- 前端 Vite dev server，监听 `127.0.0.1:5173`

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

这些账号不会进入默认生产配置。
