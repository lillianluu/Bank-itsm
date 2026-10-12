# 员工和部门模拟数据

`employees.sql` 新增 10 个部门（总部下级）和 500 名员工，按固定随机种子分配部门。所有员工启用并关联 `EMPLOYEE` 角色，包含中文姓名、虚构手机号、`example.com` 邮箱。保留现有数据，重复导入跳过已有账号和部门。

先启动一次后端完成 Flyway 迁移，再在项目根目录执行：

```bash
psql -h localhost -p 5432 -U itsm_app -d itsdb -W -v ON_ERROR_STOP=1 -f itsm-server/scripts/mock-data/employees.sql
```

使用应用配置对应的数据库地址和用户名。目标 schema 为 `itsm`，脚本在一个事务中执行，导入结束输出各部门员工人数。模拟数据不放入 Flyway，避免正常启动自动载入演示账号。

登录账号：`mock_emp_0001` 至 `mock_emp_0500`，统一密码：`mock123456`。电话号码仅为虚构测试值。
