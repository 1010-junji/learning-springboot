# Quick Start (配布用1ページ)

このページは、新人向け配布用に最小手順だけをまとめたものです。

## 前提

- Java 21 をインストール済み
- `appendix/equipment-lending-app` ディレクトリに移動済み

## 最初に実行する3コマンド（Windows PowerShell）

```powershell
$env:JDK21_HOME = '<your-jdk21-path>'
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\bootrun-jdk21.ps1
```

例:

```powershell
$env:JDK21_HOME = 'C:\Program Files\Microsoft\jdk-21'
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\bootrun-jdk21.ps1
```

## 起動確認

- API: `http://localhost:8080/api/equipments`
- H2 Console: `http://localhost:8080/h2-console`

## 停止方法

- 実行中ターミナルで `Ctrl + C`

## テスト実行

```powershell
.\test-jdk21.ps1
```