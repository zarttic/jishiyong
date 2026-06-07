# 百度短语音直连配置

本项目支持在系统语音识别和外部识别 Activity 都不可用时，使用百度短语音识别作为兜底。

## 控制台准备

在百度智能云创建语音技术应用，并启用短语音识别能力。记录以下信息：

- `AppID`
- `API Key`
- `Secret Key`

## 本地配置

不要把百度密钥提交到 Git。把配置写入仓库根目录的 `local.properties`：

```properties
BAIDU_ASR_APP_ID=你的 AppID
BAIDU_ASR_API_KEY=你的 API Key
BAIDU_ASR_SECRET_KEY=你的 Secret Key
BAIDU_ASR_DEV_PID=1537
```

`BAIDU_ASR_DEV_PID` 默认是 `1537`，用于普通话输入。

也可以通过同名环境变量提供这些值，GitHub Actions release 构建时建议使用 Actions secrets。

## 安全边界

当前实现没有后端中转，因此 APK 中必然包含可还原的百度凭据。构建脚本会把 `API Key` 和 `Secret Key` 做 XOR + Base64 混淆，避免明文字符串直接出现在 APK 中，但这不是严格安全措施，无法防止反编译或动态调试。

如果后续有服务器，应该改成服务端保存百度密钥，App 只调用自己的 ASR 接口。

## 识别链路

语音入口按以下顺序兜底：

1. Android 系统 `SpeechRecognizer`
2. 外部 `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`
3. 百度短语音 REST API

百度兜底会使用 `AudioRecord` 录制 16 kHz 单声道 PCM，上传到百度短语音接口，成功后继续调用现有 `MainViewModel.handleVoiceText()`，库存 agent 解析流程不变。
