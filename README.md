# BaritoneGUI Support (Fabric)

为 [Baritone](https://github.com/cabaletta/baritone) 提供**可配置、可扩展的图形化快捷键面板**的 Fabric 客户端模组。
把 Baritone 的寻路 / 挖掘 / 跟随 / 建造 / 设置等核心命令做成带分类、参数输入框与收藏功能的 GUI，
一键执行，并带实时设置状态回显与冲突/缺失检测。

> Baritone 是**可选**运行时依赖：模组通过反射桥接 `baritone.api` 公共 API，未安装 Baritone 时 GUI 仍可打开，只是热键不会执行。

## 功能

- 按分类组织 Baritone 全部命令（移动前往 / 寻路隧道 / 暂停取消 / 挖掘采集 / 建造选区 / 路点 / 工具信息）。
- 设置面板**运行时动态生成**：直接读取当前 Baritone 实例里的所有 `Setting`，每个开关/数值实时反映真实状态。
- 自定义热键：增 / 删 / 改，带冲突检测；配置文件保存在 `.minecraft/config/baritonegui_settings.txt`。
- 收藏常用动作，HUD 叠加层提示最近一次 Baritone 输出。
- 使用快捷键打开面板（可在控制设置里修改）。

## 支持的 Minecraft 版本

| 版本 | 源码树 | 版本 | 源码树 |
|------|--------|------|--------|
| 1.20.1 ✅ | `src`   | 1.21.5 ✅ | `src`   |
| 1.20.2 ✅ | `src`   | 1.21.8 ✅ | `src`   |
| 1.20.4 ✅ | `src`   | 1.21.10 ✅ | `src-v2` |
| 1.20.6 ✅ | `src`   | 1.21.11 ✅ | `src-v2` |
| 1.21.1 ✅ | `src`   |            |          |
| 1.21.3 ✅ | `src`   |            |          |
| 1.21.4 ✅ | `src`   |            |          |

- `src`：旧输入 API（`mouseClicked` / `keyPressed` / `InputUtil.isKeyPressed(long,…)` / `GameProfile.getName()`），用于 1.20.x – 1.21.8。
- `src-v2`：新输入事件 API（`Click` / `KeyInput` 记录类、`InputUtil.isKeyPressed(Window,…)`、`KeyBinding.Category`，authlib 6/7 兼容的 `GameProfile.name()` 反射），用于 1.21.9+。

> **26.x 暂不支持**：Fabric 官方尚未发布 26.x 的 yarn 映射（`26.1+build.1` 等全部 404），Loom 无法解析映射，编译必然失败。
> 这不是源码问题，需等 Fabric 发布 26.x yarn mappings 或改用 mojmap/Parchment 方案。`build.gradle` 里保留了 26.x 条目仅供记录。

## 环境要求

- **JDK 21**（构建 1.20.x – 1.21.11）。
- **Gradle 8.14**：仓库已内置 `gradlew` / `gradlew.bat`（Gradle Wrapper），无需单独安装 Gradle。
- **Baritone**：运行游戏时需要；构建时由 `baritone-jars/` 目录下的对应 jar 提供（**该目录已被 `.gitignore` 忽略，不会上传 GitHub**。克隆后需自行把对应版本的 Baritone jar 放入 `baritone-jars/` 才能构建）。

## 构建

### 方式一：用 `gen_build.ps1`（推荐，Windows）

脚本会自动选择源码树、把源码镜像到 ASCII 临时目录（规避中文路径问题），再调用 Gradle Wrapper，
最后把产物复制到 `release/`。

```powershell
# 默认构建 1.21.1
powershell -ExecutionPolicy Bypass -File gen_build.ps1

# 构建指定版本
powershell -ExecutionPolicy Bypass -File gen_build.ps1 -Target 1.21.11

# 自定义 JDK（可选）
powershell -ExecutionPolicy Bypass -File gen_build.ps1 -Target 1.21.11 -Jdk "C:\Program Files\Java\jdk-21.0.10"
```

产物：`release/baritonegui-<版本>-1.0.0.jar`

### 方式二：直接用 Gradle Wrapper

通过 `ORG_GRADLE_PROJECT_*` 环境变量传入构建参数（版本号含点号，用 `-P` 在 PowerShell 下会被错误解析，环境变量方式跨平台稳定）：

```bash
# Linux / macOS
ORG_GRADLE_PROJECT_target=1.21.1  ORG_GRADLE_PROJECT_srcTree=src     ./gradlew clean build
ORG_GRADLE_PROJECT_target=1.21.11 ORG_GRADLE_PROJECT_srcTree=src-v2  ./gradlew clean build

# Windows (PowerShell)
$env:ORG_GRADLE_PROJECT_target='1.21.1';  $env:ORG_GRADLE_PROJECT_srcTree='src';     ./gradlew.bat clean build
$env:ORG_GRADLE_PROJECT_target='1.21.11'; $env:ORG_GRADLE_PROJECT_srcTree='src-v2';  ./gradlew.bat clean build
```

> 注意：直接构建时项目路径必须是 ASCII（不含中文），否则请用上面的 `gen_build.ps1`（默认把源码镜像到 `%TEMP%` 规避中文路径）。

输出：`build/libs/baritonegui-<版本>-1.0.0.jar`

### 参数说明

| 参数 | 含义 |
|------|------|
| `-Ptarget`   | 目标 MC 版本，如 `1.21.11`（必填） |
| `-PsrcTree`  | `src`（旧 API）或 `src-v2`（新 API），不传则按版本自动推断 |
| `-PmirrorRoot` | 源码镜像目录（ASCII）。设空字符串可直读项目树，但要求项目路径为 ASCII |

## 使用

1. 安装 [Fabric Loader]与对应版本的 [Fabric API]。
2. 安装对应版本的 [Baritone API](https://github.com/cabaletta/baritone-api/releases)。
3. 把 `release/baritonegui-<版本>-1.0.0.jar` 放进 `.minecraft/mods/`。
4. 进游戏按快捷键打开面板。

## 项目结构

```
baritoneGUI支持-Fabric/
├── build.gradle / settings.gradle / gradle.properties   # Gradle 构建（基于 -Ptarget / -PsrcTree）
├── gen_build.ps1                                        # 一键构建脚本（自动选源码树 + 镜像）
├── gradlew / gradlew.bat / gradle/wrapper/              # Gradle Wrapper
├── baritone-jars/        # 各版本 Baritone 二进制（本地构建用，已被 .gitignore 忽略，不上传）
├── src/                  # 旧输入 API 源码树（main/java + main/resources）
├── src-v2/               # 新输入事件 API 源码树（main/java）
├── release/              # 预构建 mod jar（本地构建产物，已被 .gitignore 忽略，不上传）
└── dev/                  # 开发期辅助脚本（探测 API / 拉取版本等，非构建必需）
```

## 许可

[MIT](https://opensource.org/licenses/MIT)
