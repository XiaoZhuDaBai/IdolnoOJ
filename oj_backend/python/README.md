# 竞赛抓取脚本说明

该目录包含定时获取各大平台编程竞赛信息的 Python 脚本：

- `atcoder_contest.py`
- `codeforces_contest.py`
- `nowcoder_contest.py`

## 依赖安装

建议使用虚拟环境：

```bash
python -m venv .venv
source .venv/bin/activate  # Windows 使用 .venv\Scripts\activate
pip install -r requirements.txt
```

若尚未创建 `requirements.txt`，可根据脚本使用添加依赖，如：

```
requests
beautifulsoup4
```

## 运行方式

```bash
python atcoder_contest.py
```

脚本会输出 JSON 字符串供后端解析。生产环境中由 `OjBackendApplication#fetchContests` 通过 `ProcessBuilder` 调用系统 `python` 命令执行。

## 与后端联动

- 配置项：`schedule.contest.time`（毫秒），控制抓取频率。
- Redis 键：`oj:contest:all`，存放合并后的赛事列表。
- 若脚本异常，后端日志会提示，建议在部署时配置监控并增加告警。

## 常见问题

- **SSL / 网络错误**：检查代理或在脚本中禁用验证。
- **编码问题**：确保使用 UTF-8 输出，Python 默认编码可通过 `sys.stdout.reconfigure(encoding='utf-8')` 设置。
- **依赖缺失**：按需补全 `requirements.txt` 并在部署机安装。

