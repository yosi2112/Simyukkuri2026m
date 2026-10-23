# Build And Test

## 前提

- ターゲットランタイムは Java 8 のまま維持する。
- コンパイルも Java 8 互換を前提にする。JDK 9 以降では `--release 8`、JDK 8 では `-source 8 -target 8` を使う。
- 現行リポジトリは Maven/Gradle を使っていない。
- 製品ソースは `src/main/java/`、テストソースは `src/test/java/`。
- パッケージ名は `org.simyukkuri`。起動クラスは `org.simyukkuri.SimYukkuri`。
- 依存 jar は `lib/` に置く。
- ローカルの一時出力先は Git 管理対象外の `build/.makaizou-build/`。GitHub Actions のコンパイル出力先は `bin/`。
- コマンドはリポジトリのルートから実行する。

主な依存:

- Jackson 2.9.9
- JUnit Platform Console Standalone 1.10.2（テスト用）

依存 jar の管理方針:

- Jackson 2.9.9 の 3 jar はリポジトリで追跡している。
  - `lib/jackson-annotations-2.9.9.jar`
  - `lib/jackson-core-2.9.9.jar`
  - `lib/jackson-databind-2.9.9.jar`
- JUnit Console standalone は Jupiter API・実行エンジンと関連依存を含む。個別の JUnit jar の追加は不要。
- GitHub Actions は JUnit Console standalone を Maven Central から取得する。以下のローカル手順も同じバージョンを一時領域へ取得する。
- 現行 CI は JaCoCo を使っていない。過去のカバレッジ値は `COVERAGE_STATUS.md` の計測時点の記録である。

## リポジトリの取得

```text
git clone https://github.com/yosi2112/Simyukkuri2026m.git
cd Simyukkuri2026m
```

非公開リポジトリなので、アクセス権のある GitHub アカウントで認証する。生成された配布 jar と `.gna-backup/` は登録されていない。クローン後はビルドが必要。

## Windows: 配布 jar ビルド

通常のアプリ配布物を作る Windows ビルドは `build/build.ps1`。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File build/build.ps1
```

主な処理:

- `JAVA_HOME`、または PATH 上の `javac` から JDK を決定する。
- `build/.makaizou-build/` の専用一時フォルダへ全製品ソースをコンパイルする。
- Jackson jar を展開し、画像・設定・台詞・ライセンスとともに `build/SimYukkuri-makaizou.jar` を作成する。
- 正常終了時・異常終了時ともに専用一時フォルダを削除する。既存の `build/SimYukkuri.jar` は上書きしない。

JDK を明示する場合:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File build/build.ps1 -JavaHome "C:\path\to\jdk"
```

この手順はアプリ配布用であり、JUnit テストクラスはコンパイルしない。`build/build.bat` は従来名の `build/SimYukkuri.jar` を作る別の手順であり、`start_makaizou.bat` が参照する jar は作らない。

## Linux/macOS: 配布 jar ビルド

現行リポジトリには `build/build.sh` がない。JDK 9 以降を使い、以下を同じシェルで順に実行する。

```sh
set -eu
mkdir -p build/.makaizou-build
# 再実行時も新しい出力先を使う。
work=$(mktemp -d build/.makaizou-build/manual-XXXXXX)
mkdir -p "$work/classes" "$work/libraries"
find src/main/java -name '*.java' | sort > "$work/sources.txt"
javac --release 8 -encoding UTF-8 -cp 'lib/*' -d "$work/classes" @"$work/sources.txt"
repo=$(pwd)
for library in "$repo"/lib/jackson-*.jar; do
  (cd "$work/libraries" && jar xf "$library")
done
find "$work/libraries/META-INF" -type f \( -name '*.SF' -o -name '*.DSA' -o -name '*.RSA' \) -delete
jar cfe build/SimYukkuri-makaizou.jar org.simyukkuri.SimYukkuri \
  -C "$work/classes" . -C "$work/libraries" . \
  -C . data -C . images -C . resources -C . LICENSE -C . NOTICE.md
```

JDK 8 では `--release 8` を `-source 8 -target 8` に置き換える。一時出力は `build/.makaizou-build/` に残る。Linux/macOS 上の GUI 動作は別途確認が必要。

## 起動

Windows:

```bat
start_makaizou.bat
```

各 OS でコマンドから起動する場合:

```text
java -jar build/SimYukkuri-makaizou.jar
```

メモリ量と英語表示を明示する例:

```text
java -Duser.language=en -Duser.country=US -Xms1024m -Xmx4096m -jar build/SimYukkuri-makaizou.jar
```

独立プレビュー:

```text
java -cp build/SimYukkuri-makaizou.jar org.simyukkuri.viewer.YukkuriViewer
```

現行リポジトリには `start_2GB.*` などのメモリ別起動スクリプトはない。必要な JVM オプションは `-jar` より前に指定する。操作方法は [README](../README.md) を参照する。

## Windows: テスト実行

現行リポジトリには `build/run_tests.bat` と `build/run_tests.sh` がない。PowerShell で以下を順に実行する。初回は JUnit のダウンロードにネットワーク接続が必要。

```powershell
$ErrorActionPreference = 'Stop'
$repo = (Get-Location).Path
if ($env:JAVA_HOME) {
    $javac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
} else {
    $javac = (Get-Command javac -ErrorAction Stop).Source
}
$java = Join-Path (Split-Path $javac) 'java.exe'
$compatibility = @('--release', '8')
if ((& $javac -version 2>&1 | Out-String) -match 'javac 1\.8\.') {
    $compatibility = @('-source', '8', '-target', '8')
}
$testRoot = Join-Path $repo ('build/.makaizou-build/tests-' + [Guid]::NewGuid().ToString('N'))
$testClasses = Join-Path $testRoot 'classes'
New-Item -ItemType Directory -Path $testClasses -Force | Out-Null
$junit = Join-Path $testRoot 'junit-platform-console-standalone-1.10.2.jar'
Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar' -OutFile $junit
$utf8 = [Text.UTF8Encoding]::new($false)
foreach ($kind in @('main', 'test')) {
    $sources = @(Get-ChildItem -LiteralPath "$repo/src/$kind/java" -Recurse -Filter '*.java' |
        ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    $sourceList = Join-Path $testRoot "$kind-sources.txt"
    [IO.File]::WriteAllLines($sourceList, $sources, $utf8)
    & $javac @compatibility -encoding UTF-8 -cp "$testClasses;$repo/lib/*;$junit" -d $testClasses "@$sourceList"
    if ($LASTEXITCODE -ne 0) { throw "$kind compilation failed." }
}
$testClassPath = "$repo;$testClasses;$repo/lib/*;$junit"
& $java '-Djava.awt.headless=true' -Xmx1024m -cp $testClassPath org.junit.platform.console.ConsoleLauncher execute --scan-class-path $testClasses --fail-if-no-tests --reports-dir "$testRoot/results"
if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
```

注意:

- 毎回新しい一時フォルダを作り、古い class が混入しないようにする。
- テストにはルートの画像・設定も必要なので、リポジトリのルートを classpath に含める。
- `-cp` は Java ランチャーへ渡す。JUnit の `--class-path` に未展開の `lib/*` を直接渡さない。
- コンパイルに失敗した場合は JUnit を実行しない。
- テスト結果と依存 jar は `$testRoot` に残る。生成物なので Git 管理しない。
- この手順はテスト実行用で、配布 jar は作らない。
- テスト実行時にルートへ `SampleLogging*.log` が生成される場合がある。ログは Git 管理しない。

## Linux/macOS: テスト実行

JDK 9 以降を使う例。依存 jar の取得には `curl` が必要。

```sh
set -eu
mkdir -p build/.makaizou-build
test_root=$(mktemp -d build/.makaizou-build/tests-XXXXXX)
mkdir -p "$test_root/classes"
junit="$test_root/junit-platform-console-standalone-1.10.2.jar"
curl -fL 'https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar' -o "$junit"
find src/main/java -name '*.java' | sort > "$test_root/main-sources.txt"
find src/test/java -name '*.java' | sort > "$test_root/test-sources.txt"
javac --release 8 -encoding UTF-8 -cp "lib/*:$junit" -d "$test_root/classes" @"$test_root/main-sources.txt"
javac --release 8 -encoding UTF-8 -cp "$test_root/classes:lib/*:$junit" -d "$test_root/classes" @"$test_root/test-sources.txt"
cp_test=".:$test_root/classes:lib/*:$junit"
java -Djava.awt.headless=true -Xmx1024m -cp "$cp_test" \
  org.junit.platform.console.ConsoleLauncher execute \
  --scan-class-path "$test_root/classes" --fail-if-no-tests --reports-dir "$test_root/results"
```

JDK 8 では両方の `--release 8` を `-source 8 -target 8` に置き換える。Windows と同じく、一時出力と結果は残る。

## パッケージ単位テスト

Windows の全テスト実行手順でコンパイル済みの場合、同じ PowerShell で以下を実行する。

```powershell
& $java '-Djava.awt.headless=true' -Xmx1024m -cp $testClassPath org.junit.platform.console.ConsoleLauncher execute --scan-class-path $testClasses --include-package org.simyukkuri.logic --fail-if-no-tests
```

## 単一テストクラス

```powershell
& $java '-Djava.awt.headless=true' -Xmx1024m -cp $testClassPath org.junit.platform.console.ConsoleLauncher execute --select-class org.simyukkuri.yukkuri.DeibuTest --fail-if-no-tests
```

## 単一テストメソッド

```powershell
& $java '-Djava.awt.headless=true' -Xmx1024m -cp $testClassPath org.junit.platform.console.ConsoleLauncher execute --select-method org.simyukkuri.yukkuri.DeibuTest#testDeibuIdentity --fail-if-no-tests
```

Linux/macOS では同じテスト選択オプションを使い、Java 起動部分を `java -Djava.awt.headless=true -Xmx1024m -cp "$cp_test" org.junit.platform.console.ConsoleLauncher execute` に置き換える。

## 既知の注意点

- GUI/Swing に触るテストは headless 実行で失敗することがある。実際の画面操作は GUI 環境で別途確認する。
- `MainCommandUi` など AWT 初期化に触る経路は画面環境の有無に影響される。
- `SimYukkuri.RND`, `SimYukkuri.world`, 画像ロード済みフラグなど static 状態を使うテストは順序依存になりやすい。
- ゆっくり画像ロード状態はクラスごとの static boolean なので、`isImageLoaded()` の戻り値を未ロード前提で固定しない。
- 2026-10-23 の移植・修正時の検証記録は 6135 成功、既存 skip 1。最新の結果はその都度実行して確認する。
