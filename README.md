# MAGLE

Android 端 Eagle 素材库查看与管理工具，当前版本 **0.8.73**。不是 Eagle 官方客户端。

本版使用 R8 裁剪未使用的代码和资源，关闭调试，安装包约 7.1 MB。修正文件夹和标签的层级返回、保留返回后的列表位置，优化界面切换及下拉刷新提示；多选仅显示选中边框。更新包由应用自身后台下载，支持断点续传，并提供公开版本信息后备以应对 GitHub API 限额。

## 功能

- 手机和平板布局，网格、瀑布流、列表视图及双指缩放。
- 文件夹层级、标签分组、名称/标签/文件夹搜索。
- 大图预览、素材详情、原文件下载与分享；支持图片及 SVG 等素材的预览，无法预览的格式显示扩展名与名称。
- 支持本地目录、OneDrive、WebDAV、Dropbox、Google Drive、SMB；S3 为 Beta，暂不支持来源切换和索引导入目标。
- 按来源能力提供上传、标签/文件夹编辑、回收站及还原；不能保证所有来源的所有功能均可用。
- 索引缓存、启动/定时检查、下拉刷新、缓存容量设置。
- 同库副本之间切换来源，复用未变索引；不搬运或上传原文件。
- 可选择一个或多个库，分别导出索引、目录/标签数据和已有缩略图，再导入到同库副本；不包含原文件、连接配置或登录凭据。

## 使用

1. 从 Releases 下载最新版 APK，在 Android 10 或更高版本上安装；已有安装请覆盖更新，不要先卸载。
2. 打开设置，在“素材库”右侧点击加号，选择来源并授权或填写连接信息，选择 `.library` 库目录。
3. 首次建立索引后，在视图页浏览、搜索和切换文件夹/标签；点击素材查看大图，详情按钮展开信息，长按开启多选。
4. 上传按钮添加素材；设置中素材库标题右侧的更新图标或视图页下拉可检查当前库更新。
5. 素材库标题右侧备份图标可选择要导出的库；只有一个库时直接选择保存文件，多选时选择目标目录并分别生成备份。添加菜单底部可导入备份索引。在新设备先连接同库副本，再导入备份。备份不代替原文件备份。
6. 设置底部显示版本号，可访问 GitHub 或手动检查软件更新；不会在启动时自动检查软件版本。
7. 检查到新版后点击立即更新，应用后台下载完成后点击安装更新。首次须允许安装未知应用，并由系统确认安装。中断后点击继续下载，保留部分文件并尝试续传；安装成功后下次启动清理安装包。

## 构建

需要 JDK 17、Android SDK 36。将本机 SDK 位置配置在不提交的 `local.properties` 中，例如：

```properties
sdk.dir=C:/Android/Sdk
```

```powershell
./gradlew.bat :app:testReleaseUnitTest :app:assembleRelease :app:lintRelease
./gradlew.bat :app:assembleDebug
```

Linux/macOS 可使用 `bash gradlew`。构建产物放在系统临时目录 `magle-github-build/app/outputs/apk/debug/`，避免同步服务锁定生成文件。

优化版 APK 位于同目录下的 `outputs/apk/release/`。当前 GitHub 兼容发布继续使用此前的本机开发签名，使旧版本能覆盖更新；应用关闭调试，但不宣称已迁移到独立生产签名或具备商店发行资格。签名私钥不在仓库中，自行构建会使用自己的签名，不能覆盖官方 APK。不要通过卸载解决签名不匹配，否则会丢失应用数据。

原生只读冒烟测试可用 `:app:assembleReleaseAndroidTest` 构建，在测试设备安装优化 APK 和测试 APK 后，运行 `adb shell am instrument -w com.tai.oeviewer.test/com.tai.oeviewer.ReleaseSmokeInstrumentation`；测试 APK 不作为发行文件。

## 网盘授权

- OneDrive 使用本项目的公共客户端注册；可通过 Gradle 属性 `magleOneDriveClientId` 指定自己的客户端 ID。客户端 ID 不是密钥，密码/令牌不放在源码中。
- Google Drive 需要启用 Drive API、配置 Android OAuth 包名 `com.tai.oeviewer` 和实际签名 SHA-1；测试阶段还需要账户在测试用户列表中。更换签名或构建环境后须重新配置，不能保证自行构建的 APK 直接可授权。
- Dropbox 当前为开发版 PKCE 授权流程，需要 App Key，不需要将 App Secret 放入 APK。
- WebDAV/SMB/S3 需由使用者提供服务地址和凭据。优先使用 HTTPS；局域网 HTTP 仅在显式允许后可用，会明文传输凭据。

## 注意

- 来源切换/索引导入要求目标已有**同一个 Eagle 库的副本**：名称可以不同，素材 ID、目录和配套元数据须对应；仅放了相同图片并不算同一库。
- 导入时须能连接对应库以核对，新设备通常需要重新授权。当前没有无需连接即可激活的完全离线备份库模式。
- 修改前请备份重要素材，避免多个设备同时编辑同一库。远程失败可能留下待继续的任务。
- 本仓库不包含个人素材库、测试图片、真实索引备份、账号凭据、签名私钥或个人测试日志。
- 当前提供测试版，不是正式商店发行版。最新版 APK 将放在 GitHub Releases，不将历史 APK 放入 Git 源码历史。

## 依赖与许可证

主要依赖：AndroidX/Compose、Material Icons、AndroidSVG、Google Play Services、Dropbox SDK、jcifs-ng、MinIO、StAX、Woodstox、JUnit。各依赖按其自身许可使用；参见 [第三方依赖](THIRD_PARTY.md)。

项目暂未指定开源许可证；上传到 GitHub 不等于已授予开源使用许可。
