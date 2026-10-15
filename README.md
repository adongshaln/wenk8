# 轻小说文库 · WENK8 Reading Archive

纸墨风格的原生安卓轻小说阅读器。米白纸面、墨色书脊、编目式排版——书架即藏书柜。

<p align="center"><em>01 / CATALOG · 最近更新 · 循环浏览 · 逐册轻触定位</em></p>

## 特性

- **书柜式书架**：每本书的书脊颜色、厚度、高度由书 ID 确定性生成——同一本书在所有设备上是同一副样子
- **TXT 导入**：自动识别「第X章 / 卷 / 话 / 序章 / 楔子」切分章节
- **EPUB 导入**：解析 OPF spine 顺序，按章读取
- **阅读器**：纸面长文、左右轻触翻章、字号/行距/深色/缩进设置，进度自动记忆
- **零权限收集**：除网络外无任何权限，数据全部留在本机

## 构建方式

**本地无需任何环境**——全部构建由 GitHub Actions 完成：

| 工作流 | 触发 | 产物 |
|---|---|---|
| CI 编译验证 | push / PR | Debug APK（Actions 工件，保留 14 天） |
| 构建并发布 | 推送 `v*` tag 或手动触发 | 签名 Release APK → GitHub Releases |

发版流程：

```bash
git tag v0.1.0 && git push origin v0.1.0
```

版本号自动注入（versionName = tag 去 `v`，versionCode = 提交总数）；签名三级来源：仓库 Secrets → 复用上个 Release 密钥 → 现场新生成并随 Release 附上。

## 技术栈

Kotlin 2.1 + Jetpack Compose（BOM 2024.12）、Material 3、kotlinx-serialization、jsoup（EPUB 解析）。minSdk 26 / targetSdk 35。

## 目录

```
app/src/main/java/io/wenk8/app/
├── BookLook.kt      # 书脊外观：FNV-1a → PRNG 确定性生成（移植自 Gloss lookOf）
├── TxtParser.kt     # TXT 章节切分
├── EpubParser.kt    # EPUB 解析（container.xml → OPF → spine）
├── LibraryRepo.kt   # 本地书库（JSON 持久化 + StateFlow）
├── DiscoverScreen.kt# 发现页：书脊阵列 + 抽出的大书
├── ShelfScreen.kt   # 书架管理
└── ReaderScreen.kt  # 阅读器
```
