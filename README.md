# 朝阳师范学院职业发展平台

## 本地启动

需要 Java 21、Maven 3.9+、Node `^20.19.0 || >=22.12.0` 和 Docker Compose。

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
