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

## ⚠️ 开发流程规范

本项目采用 Git Flow 工作流，请遵循以下规范：
- ✅ **不要直接在 main 分支上开发**
- ✅ 从 main 分支创建自己的功能分支进行开发和测试
- ✅ 在功能分支上完成开发和充分测试后，提交 Pull Request (PR)
- ✅ PR 经过代码审查和测试通过后，方可合并到 main 分支
- ✅ 合并到 main 分支后将自动部署到生产环境

## 自动化部署

推送到 `main` 分支后自动触发部署，在 [GitHub Actions](../../actions) 查看部署状态。

详细部署文档：[docs/deploy.md](docs/deploy.md)
