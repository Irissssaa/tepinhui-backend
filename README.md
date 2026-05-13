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

## 当前阶段说明

当前仓库处于“前期框架搭建完成、业务逐步补齐”阶段。

- 已具备较完整业务逻辑的模块：认证、商品 CRUD、部分溯源查询/审核链路。
- 已搭建接口框架但业务未实现或仅占位的模块：用户资料、地址、购物车、订单、评价、分类、特产、文化内容、地图统计、平台看板、商家审核与角色流转的多数细节。
- Swagger 中标注 `（未实现）` 的接口，表示路径、鉴权规则、请求参数、响应结构和统一返回格式 `Result<T>` 已确定，但 Service 仍未接入真实业务；这类接口当前返回空结构占位数据或 `501` 业务异常。
- 路径前缀由 `server.servlet.context-path` 统一追加，默认实际访问前缀为 `/tph`，Controller 中不手写 `/tph`。

当前角色流转约束：

- 普通注册用户固定创建为 `CONSUMER`。
- `MERCHANT` 角色应在商家入驻审核通过后由后端授予。
- `ADMIN` 账号只能由受控后台或初始化脚本创建，不走普通注册流程。

当前建议验证命令：

```powershell
.\mvnw.cmd test
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
