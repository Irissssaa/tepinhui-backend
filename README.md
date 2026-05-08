# tepinhui-backend

**API 文档**: https://82.156.12.252/tph/swagger-ui/index.html  
**健康检查**: https://82.156.12.252/tph/health

---

## 快速开始

```bash
git clone <repository-url>
cd tepinhui-backend
./mvnw spring-boot:run
```

## ⚠️ 提交前必读

向 `main` 分支提交代码前，请务必：
- ✅ 本地运行项目并测试所有更改
- ✅ 确认功能正常且无问题
- ✅ 提交后将自动部署到生产环境

## 自动化部署

推送到 `main` 分支后自动触发部署，在 [GitHub Actions](../../actions) 查看部署状态。

详细部署文档：[docs/deploy.md](docs/deploy.md)
