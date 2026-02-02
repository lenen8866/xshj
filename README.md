# 信使汇集_安卓
### 后台版本测试
<<<<<<< HEAD
如果我给你db文件，那么你需要 改为这个名字"xshj.db"


而这个是测试更新的，非修改人员无需改动（改动也没用.）
=======
>>>>>>> master
http://xshj.version.sdattg.com/BanBen/version/admin888.html
“执行方案”写得接近工程级 SOP了
#### 介绍
包含2个版本，apk和aab

#### 软件架构

✅git status 会告诉你：

👇

✅ Git 最常用的 4 个命令（大白话版）
1️⃣ git status（查看状态）
哪些文件 被修改了（modified）
哪些文件 新增了（new file / untracked）
哪些文件 被删除了（deleted）
哪些文件已经 准备提交（staged / 暂存区）
2️⃣ git add（把文件放进“暂存区”，准备提交）

✅ 添加全部改动：
git add .
git add -A
. 的意思：当前文件夹下全部改动
✅ 添加某一个文件：
git add app/src/main/java/xxx.kt
✅ 添加某一个文件夹（这个文件夹里所有改动都会加进去）：
git add app/src/main/java/

3️⃣ git commit（保存一次备份记录）
git commit -m "写你的提交说明"
4️⃣ git push（同步到网站 GitHub）
git push

✅ 改完 → add → commit → push（网站才会变）

#### 安装教程

1.  xxxx111123
2.  xxxx
3.  xxxx

#### 使用说明

1.  xxxx
2.  xxxx
3.  xxxx

#### 参与贡献

验证编译是否正常
.\gradlew clean assembleDebug



1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 提示词
当前进度表（对应你文件里的步骤）


#### 最终功能验证清单
## ✅ 最终功能验证清单

请安装 APK 并完成以下测试。

---

### 🔹 核心功能测试（必测）

- [ ] 应用启动成功
- [ ] 欢迎页显示正常
- [ ] 数据库加载完成（首次启动从 assets 复制）
- [ ] 分类页加载成功
- [ ] 切换多个分类（至少 3 个）
- [ ] 书籍列表显示正确
- [ ] 打开书籍（至少 3 本）
- [ ] 章节内容显示正常
- [ ] 章节切换正常（上一章 / 下一章）
- [ ] 搜索功能正常
- [ ] 搜索结果显示与跳转正常
- [ ] 设置页面正常
- [ ] 主题切换正常
- [ ] 字体大小调整正常
- [ ] 行间距调整正常
- [ ] 关于页面正常

---

### 🌐 网络相关功能测试（重点）

> 由于 OkHttp 跨大版本升级，请重点测试以下功能：

- [ ] 如果有更新检查功能：测试更新检查
- [ ] 如果有下载功能：测试 FileDownloader 下载
- [ ] 如果有图片加载：测试图片显示
- [ ] 如果有网络请求：测试 OkGo 接口调用
- [ ] 无网络环境下应用不崩溃

---

### ⚠️ 异常场景测试（可选）

- [ ] 弱网环境测试（限速）
- [ ] 断网重连测试
- [ ] 请求超时处理正确


### 依赖清单

.\gradlew app:dependencies --configuration debugRuntimeClasspath > deps.txt

