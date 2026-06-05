# 及时用 - 物品过期管理 App

一款帮助你管理物品保质期的 Android 应用，再也不用担心东西过期了！

## ✨ 功能特性

### 📦 物品管理
- 添加、编辑、删除物品
- 支持物品分类：食品、饮品、日用品、药品、化妆品、电子产品、服饰等
- 记录购买日期、过期日期、数量
- 支持备注和拍照记录

### ⏰ 智能提醒
- **多级提醒**：过期前 7天、3天、1天 分别提醒
- **自定义提醒**：可为每个物品设置不同的提醒天数
- **定时检查**：每天早上 9 点自动检查并推送通知
- **过期预警**：已过期物品也会收到提醒

### 📊 统计分析
- 本月消费/浪费统计
- 分类物品数量统计
- 浪费率计算
- 按月查看历史数据

### 🔍 搜索与筛选
- 按名称搜索物品
- 按分类筛选
- 快速查看即将过期物品

### ⬇️ 版本更新
- 从 GitHub Release 检查最新版本
- 发现新版本后跳转到 GitHub 下载 APK
- 不做静默安装，不在应用内保存签名密钥或 GitHub Token

## 🏗️ 技术栈

| 技术 | 用途 |
|------|------|
| **Kotlin** | 开发语言 |
| **Jetpack Compose** | 声明式 UI |
| **Material 3** | 设计规范 |
| **Room** | 本地数据库 |
| **WorkManager** | 后台定时任务 |
| **Navigation Compose** | 页面导航 |
| **MVVM** | 架构模式 |

## 📁 项目结构

```
app/src/main/java/com/jishiyong/
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt          # Room 数据库
│   │   ├── converter/               # 类型转换器
│   │   │   ├── DateConverter.kt     # 日期转换
│   │   │   └── ListConverter.kt     # 列表/枚举转换
│   │   ├── dao/
│   │   │   └── ItemDao.kt           # 数据访问对象
│   │   └── entity/
│   │       └── Item.kt              # 物品实体
│   └── repository/
│       └── ItemRepository.kt        # 数据仓库
├── notification/
│   ├── ExpirationReminderWorker.kt  # 过期提醒 Worker
│   └── NotificationHelper.kt        # 通知工具类
├── ui/
│   ├── theme/                       # 主题配置
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── screens/                     # 页面
│   │   ├── HomeScreen.kt           # 首页
│   │   ├── AddItemScreen.kt        # 添加物品
│   │   ├── ItemDetailScreen.kt     # 物品详情
│   │   └── StatisticsScreen.kt     # 统计页面
│   └── components/                  # 通用组件
│       ├── ItemCard.kt             # 物品卡片
│       ├── CategoryChip.kt         # 分类筛选
│       └── ExpiryOverviewCard.kt   # 概览卡片
├── viewmodel/
│   ├── MainViewModel.kt            # 主页 ViewModel
│   └── StatisticsViewModel.kt      # 统计 ViewModel
├── util/
│   ├── Constants.kt                # 常量定义
│   └── DateUtils.kt                # 日期工具
├── JiShiYongApp.kt                 # Application 类
└── MainActivity.kt                  # 主 Activity
```

## 🚀 如何使用

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34

### 构建步骤

1. **用 Android Studio 打开项目**
   ```
   File → Open → 选择 jishiyong 文件夹
   ```

2. **同步 Gradle**
   Android Studio 会自动提示同步，点击 "Sync Now"

3. **运行项目**
   - 连接 Android 设备或启动模拟器
   - 点击 ▶️ Run 按钮

### 生成 APK

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本（需要签名配置）
./gradlew assembleRelease
```

### GitHub Release 分发

项目提供 `.github/workflows/release.yml`，用于手动发布签名 APK 到 GitHub Release。

首次使用前，在 GitHub 仓库的 `Settings → Secrets and variables → Actions` 中配置：

- `ANDROID_KEYSTORE_BASE64`：release keystore 文件的 base64 内容
- `ANDROID_KEYSTORE_PASSWORD`：keystore 密码
- `ANDROID_KEY_ALIAS`：签名 key alias
- `ANDROID_KEY_PASSWORD`：签名 key 密码

生成 keystore base64：

```bash
base64 -w 0 release.keystore
```

发布新版本：

1. 打开 GitHub Actions
2. 选择 `Release Android APK`
3. 点击 `Run workflow`
4. 输入 `version_name`，例如 `1.0.1`
5. 输入递增的 `version_code`，例如 `2`
6. 可选填写 release notes

发布完成后，App 会通过 GitHub latest release API 检查最新版本。发现新版本时只打开 APK 下载链接，由浏览器和系统安装界面处理安装确认。

## 📱 使用指南

### 添加物品
1. 点击右下角 ➕ 按钮
2. 填写物品名称、选择分类
3. 设置购买日期和过期日期
4. 可选：添加备注、调整数量、设置提醒天数
5. 点击"保存"

### 查看物品
- **首页**：查看所有未过期物品，按过期日期排序
- **分类筛选**：点击顶部分类标签快速筛选
- **搜索**：点击搜索图标按名称搜索

### 处理物品
- **标记已用完**：点击物品右侧 ✓ 图标
- **删除**：点击 🗑️ 图标
- **查看详情**：点击物品卡片

### 查看统计
- 点击顶部 📊 图标
- 切换月份查看历史数据
- 查看浪费率和分类统计

## ⏰ 提醒机制

- **检查时间**：每天早上 9:00
- **提醒级别**：
  - 🟢 提前 7 天：预提醒
  - 🟡 提前 3 天：提前提醒
  - 🟠 提前 1 天：临近提醒
  - 🔴 已过期：过期提醒

## 📝 注意事项

- 首次打开会请求通知权限，请允许以接收提醒
- 示例数据会在首次安装时自动添加
- 数据存储在本地，卸载 App 会丢失数据

## 📄 License

MIT License
