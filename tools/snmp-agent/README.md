# 本机模拟 SNMP v2c Agent

阶段 6 没有真实 SNMP 设备时，用这个脚本在本机 UDP `1161` 端口模拟一个只读 Agent。

特点：

- 只使用 Python 标准库，不需要 `pip install`
- 只实现 SNMP v2c GET
- 支持 OID：
  - `1.3.6.1.2.1.1.1.0` sysDescr
  - `1.3.6.1.2.1.1.3.0` sysUpTime
  - `1.3.6.1.2.1.1.5.0` sysName
- 社区字符串不匹配时丢弃请求，应用端会表现为超时/不可达
- 只监听 `127.0.0.1` 或你明确指定的测试地址

## 启动

使用普通 Python：

```powershell
python tools\snmp-agent\simulated_agent.py --host 127.0.0.1 --port 1161 --community sim-public
```

如果系统没有 Python，可以使用 DSH 捆绑 Python：

```powershell
C:\Users\laijdsh\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe `
  tools\snmp-agent\simulated_agent.py --host 127.0.0.1 --port 1161 --community sim-public
```

启动后脚本会一直监听，按 `Ctrl + C` 停止。

## 在系统中配置设备

设备编辑页启用 SNMP：

- 端口：`1161`
- 社区字符串：`sim-public`

然后点击设备详情页的“SNMP 采集”，或在终端调用：

```powershell
curl.exe -X POST http://127.0.0.1:8080/api/devices/3/snmp-check
```

## 安全说明

`sim-public` 只是本机测试用的模拟社区字符串，不是真实设备密码。  
不要把真实设备的 SNMP 社区字符串写入仓库、文档或截图。
