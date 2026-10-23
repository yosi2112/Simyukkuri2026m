# 独自機能保持アップデート

## リポジトリからの利用

更新版のリポジトリは [Simyukkuri2026m](https://github.com/yosi2112/Simyukkuri2026m) です。`main` ブランチを使用します。非公開リポジトリのため、取得にはアクセス権のあるGitHubアカウントが必要です。

生成JARとバックアップはGit管理の対象外です。クローン後はリポジトリのフォルダで `powershell -NoProfile -ExecutionPolicy Bypass -File build/build.ps1` を実行し、`build/SimYukkuri-makaizou.jar` を作成してください。作成後は `start_makaizou.bat` で起動します。操作方法は [README](README.md)、開発時の手順は [Build And Test](docs/build-and-test.md) をご覧ください。

以下の移植・検証記録にある `plan/` と旧構成のコミットは、移植元の開発リポジトリで管理しています。本リポジトリには含まれません。記載のJARのSHA256は移植時の配布物の値です。再ビルドしたJARは生成時刻などにより別の値になる場合があります。

`MAKAIZOU_FILES.sha256` はファイル照合用の一覧です。マニュアル更新分は現在の内容に合わせて更新しています。生成JARの照合行は移植時の配布物を対象とするため、クローン直後や再ビルド後の照合とは分けて確認してください。

## 移植時の記録

2026-10-22 JST。現行版のパッケージ構成と検証済みの告白・移動補正を基盤とし、`E:/Simyukkuri2021-main` の実行対象ソースを比較して独自機能を移植した版です。

## 起動とビルド

- `start_makaizou.bat`、または `java -jar build/SimYukkuri-makaizou.jar` で起動します。
- ビルドは `powershell -NoProfile -ExecutionPolicy Bypass -File build/build.ps1`。JDKがPATHまたはJAVA_HOMEに必要です。JDK 8以降でJava 8互換のJARを生成します。
- 既存の `build/SimYukkuri.jar` は上書きしません。
- 独立プレビューは `java -cp build/SimYukkuri-makaizou.jar org.simyukkuri.viewer.YukkuriViewer` で開きます。

## 差分と統合方針

比較には `git diff --no-index` を使用しました。現行版と比較元ではディレクトリとクラス名が異なるため、現行履歴の旧構成 `8b0b31d` も参照し、コメント・配置変更と独自機能を区別しました。

| 比較元の独自機能 | 更新版の対応 |
|---|---|
| 利用確認、スプラッシュ画像 | `ui/StartupScreen`。元画像を保持し、Swingのイベントスレッドで起動・終了処理 |
| 指定個体とのかけっこ | `logic/PlayerPlayLogic`、`event/impl/RunningWithPlayerEvent`。右側・右クリックの「指定したゆっくりと遊ぶ」から操作 |
| 拡張ショートカット | `ui/InputController`。元のH/I/K/L/M/O/P/R/T/Y、数字、F1～F12の操作を現行APIへ移植。F1で操作一覧 |
| 個体パラメーター編集 | `ui/CharacterEditorDialog` |
| 胎内・茎のDNA編集 | `ui/DnaEditorDialog`、`ui/DnaEditor`。種別は現行の型付きenumを使用 |
| 緊急出産操作 | `logic/EmergencyBirth`。元のF11操作を保持 |
| カード型ステータス、家族・胎内プレビュー | `ui/ShowStatusFrame` |
| 破裂前の★表示・なつき度 | 右側ステータス欄に統合 |
| 不死身切替 | 開発用カテゴリに統合 |
| キャラクターの表情・伸縮プレビュー | `viewer/YukkuriViewer`。旧パッケージへの動的参照も現行へ更新 |
| 独自の日本語台詞・追加画像 | 実行用テキストと画像を保持。設定は現行に追加されたキーを維持して比較元の対応値と照合 |
| 告白時の相撲許可設定 | `data/world_ini/play.ini` の `AllowPeniWrestling` を保持 |

旧版のコピー・バックアップソース、古いclass/JAR、作業ログ、セーブデータは実行対象へ混入させません。現行版で改善済みの保存・ロード、描画、イベントの基本処理は現行実装を保持します。旧版でコメントアウトされていた設定を新しい動作として有効化しません。

旧版のセーブには旧クラス名が記録されているため、本更新版との読み込み互換は保証しません。元フォルダーとセーブデータは保持しています。

## 検証

検証コマンド、結果、配布対象のハッシュ照合は `plan/ws003-makaizou-update/` に記録します。比較元は変更せず、検証済みのコードと実行資産を `E:/programing work/Simyukkuri2021-makaizou` にコピーします。

検証結果: Java 8互換ビルド成功。全6135テスト成功、既存skip1、追加lint0。独立した作業ディレクトリから配布JARだけでGUIの描画・DNA保存を確認し、修正版では全21種別の追加ダイアログからの生成・登録も検証しました。実行JARのSHA256は `5f977742dddefbfd079f439e313e7445f0abe6a65906cdfafd964b3fcd1a1586` です。コピー後の各ファイルは `MAKAIZOU_FILES.sha256` で照合できます。

## 2026-10-23 修正

追加画面で希少種の7番目以降（ちるの、えーき、らん、にとり、ゆうか、さくや）を選ぶと、種別変換の順序によりnull参照で追加できない不具合を修正しました。カテゴリのIDオフセットを先に適用することで、選択した種別を正しく生成します。通常種・捕食種と既存のランダム・変異処理も保持しています。全21種別の日英表示名との一致と、実際のダイアログ・読み込み処理・個体登録を検証済みです。詳細は `plan/ws004-add-dialog/verification.md` に記録しています。
