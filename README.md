# 儿童故事机（Kids Story Player）

面向幼儿的离线故事机 App：大图标、卡通风格、熄屏可播，适配 3 寸小屏（Android 5.0+）。

## 功能

- 三级结构：分类 → 故事 → 集数
- 全屏播放页：超大播放键 + 左右切集
- 后台 / 熄屏播放（MediaSessionService，通知栏/锁屏可控制）
- 内置示例音频（assets）

## 目录结构（App 自动扫描）

内置示例在 `app/src/main/assets/audio/`：

```
audio/
├── 睡前故事/
│   ├── 小星星/
│   │   ├── 01 一闪一闪.mp3
│   │   └── 02 亮晶晶.mp3
│   └── 小熊晚安.mp3        ← 单集也可以直接放
├── 儿歌/
│   └── 两只老虎.mp3
└── 英语/
    └── ABC Song/
        └── 01 ABC.mp3
```

规则：
- 一级目录 = 分类
- 二级目录 = 故事（多集）
- MP3 = 集数（按文件名排序）
- 分类下直接放 MP3 = 单集故事

## 构建

### 方案 A：GitHub Actions（推荐）

1. 推送到 GitHub 仓库（main 分支）
2. Actions 自动运行 `gradle assembleDebug`
3. 从 Actions 页面下载 `kids-story-player-debug` 制品（app-debug.apk）

### 方案 B：本地（需要 Android SDK）

```bash
gradle assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk
```

## 开发环境

- Kotlin 2.0.21 / AGP 8.7.3 / Gradle 8.9+
- compileSdk 34 / minSdk 21（Android 5.0）
- Jetpack Compose + Media3
