# ========================================
# 一键修复脚本 - 直接在 Android Studio Terminal 运行
# ========================================

Write-Host "开始修复..." -ForegroundColor Cyan

# 1. 复制异常处理工具
if (Test-Path "TEMP_ExceptionHandler.kt") {
    Copy-Item "TEMP_ExceptionHandler.kt" "app\src\main\java\com\sda\books\reader\util\ExceptionHandler.kt" -Force
    Write-Host "✓ ExceptionHandler.kt 已复制" -ForegroundColor Green
}

if (Test-Path "TEMP_NetworkMonitor.kt") {
    Copy-Item "TEMP_NetworkMonitor.kt" "app\src\main\java\com\sda\books\reader\util\NetworkMonitor.kt" -Force
    Write-Host "✓ NetworkMonitor.kt 已复制" -ForegroundColor Green
}

# 2. 删除临时文件
Remove-Item "TEMP_*.kt" -ErrorAction SilentlyContinue
Write-Host "✓ 临时文件已清理" -ForegroundColor Green

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "✅ 修复完成！" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "下一步：" -ForegroundColor Yellow
Write-Host "1. 点击 Android Studio 顶部的 'Sync Project with Gradle Files' 按钮" -ForegroundColor White
Write-Host "2. 点击绿色 ▶️ 运行按钮" -ForegroundColor White
Write-Host "" 
