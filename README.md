しむゆっくり 2026m
===

ゆっくりのシミュレータを作ります。

このプロジェクトはMimisuke氏によって開発された以下のプロジェクトから派生したものです。

* https://ja.osdn.net/projects/simyukkuri/

しむゆっくり 2021を基に、独自機能の移植と不具合修正を行った版です。変更内容は[独自機能保持アップデート](MAKAIZOU_UPDATE.md)をご覧ください。

システム要件
---

* JDK8以降の開発環境 (ビルド後はJRE8以降となります)
* JDK/JREが要求するシステム要件を満たす環境
* WindowsでのビルドにはPowerShellが必要です。

使用方法
---

* ビルドにはJDKインストールが前提です。`javac` と `jar` にPATHを通すか、JAVA_HOMEにJDKのフォルダを設定してください。
1. このリポジトリをクローンします。非公開リポジトリのため、アクセス権のあるGitHubアカウントで認証してください。

   ```text
   git clone https://github.com/yosi2112/Simyukkuri2026m.git
   cd Simyukkuri2026m
   ```

2. リポジトリのフォルダで以下のコマンドを実行して /build にSimYukkuri-makaizou.jarを作成してください。

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File build/build.ps1
   ```

3. `start_makaizou.bat` を実行してください。コマンドから起動する場合は以下のとおりです。

   ```text
   java -jar build/SimYukkuri-makaizou.jar
   ```

* 生成されたJARはGit管理の対象外です。クローン後は最初にビルドしてください。
* JARには画像・設定・台詞と実行に必要なライブラリが含まれます。
* ビルド・テストの詳細とLinux/macOSでの手順は[Build And Test](docs/build-and-test.md)をご覧ください。

操作方法
---

* 起動時の利用確認を読み、続行するとシミュレーション画面が開きます。
* ゲーム画面でF1を押すとショートカット一覧が表示されます。個体を対象とする操作は、先に対象のゆっくりを選択してください。
* Escで一時停止・再開、W/A/S/DまたはShiftを押しながらドラッグで画面移動、Z/Xで拡大・縮小、Cで表示位置と倍率を戻します。
* 「指定したゆっくりと遊ぶ」の「かけっこ」を選んで相手をクリックすると、地面の上のカーソルを追いかけます。「遊ぶのをやめる」で終了します。
* F8で選択個体のパラメータ編集、F9で所持金変更、F12で胎ゆ・茎ゆのDNA編集を開きます。
* 独立したキャラクタープレビューは以下のコマンドで開きます。

  ```text
  java -cp build/SimYukkuri-makaizou.jar org.simyukkuri.viewer.YukkuriViewer
  ```

* 旧版のセーブデータとの読み込み互換は保証しません。元のセーブデータを保持して使用してください。

プロジェクトに参加するには
---

[Build And Test](docs/build-and-test.md)でビルドとテストの手順をご確認ください。不具合報告や変更提案は、このリポジトリの[Issues](https://github.com/yosi2112/Simyukkuri2026m/issues)または[Pull requests](https://github.com/yosi2112/Simyukkuri2026m/pulls)からお願いします。

ライセンス
---

しむゆっくりは「[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)」でライセンスされています。

詳しくは[LICENSE](LICENSE)をご覧ください。

Git管理される前の変更履歴
---

[これまでの変更履歴](CHANGELOG.txt)を御覧ください。
