SimYukkuri 2026m
===

A Yukkuri Simulator

This project is a continuation of the original game developed by Mimisuke.

* https://ja.osdn.net/projects/simyukkuri/

This version is based on SimYukkuri 2021 and includes custom features and bug fixes. Please read [Custom Feature Update](MAKAIZOU_UPDATE.md) for the changes (in Japanese).

System Requirements
---

* JDK 8 or later for development (after building only JRE 8 or later is required)
* A system that meets the JDK/JRE requirements
* PowerShell for the Windows build

How To
---

* To build the game, a JDK installation is assumed. Add `javac` and `jar` to PATH, or set JAVA_HOME to the JDK directory.
1. Clone this repository. As the repository is private, authenticate with a GitHub account that has access.

   ```text
   git clone https://github.com/yosi2112/Simyukkuri2026m.git
   cd Simyukkuri2026m
   ```

2. Run the following command from the repository directory to create SimYukkuri-makaizou.jar inside /build.

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File build/build.ps1
   ```

3. Execute `start_makaizou.bat` to play. To start from the command line, use:

   ```text
   java -jar build/SimYukkuri-makaizou.jar
   ```

* The generated JAR is not tracked by Git. Build it after cloning the repository.
* The JAR includes the images, settings, messages, and libraries needed to run the game.
* Please read [Build And Test](docs/build-and-test.md) for detailed build and test instructions, including Linux/macOS (in Japanese).

Controls
---

* Read the confirmation shown at startup and continue to open the simulation.
* Press F1 in the game window to show the shortcut list. Select a Yukkuri before using a shortcut that acts on an individual.
* Press Esc to pause or resume. Use W/A/S/D or Shift-drag to scroll, Z/X to zoom in or out, and C to reset the view.
* Select the running game under the play-with-a-Yukkuri menu and click a Yukkuri. It follows the cursor over the ground. Use the stop-playing command to finish.
* Press F8 to edit the selected Yukkuri's parameters, F9 to change money, or F12 to edit unborn or stalk offspring DNA.
* To open the standalone character preview, use:

  ```text
  java -cp build/SimYukkuri-makaizou.jar org.simyukkuri.viewer.YukkuriViewer
  ```

* Save files from older versions are not guaranteed to load. Keep a copy of the original save files.

Contributing To The Project
---

Please read [Build And Test](docs/build-and-test.md) for the build and test instructions. Report bugs or propose changes through this repository's [Issues](https://github.com/yosi2112/Simyukkuri2026m/issues) or [Pull requests](https://github.com/yosi2112/Simyukkuri2026m/pulls).

License
---

Simyukkuri is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

Please read the [LICENSE](LICENSE) file.

Changelog before Git was adopted
---

Please read the [CHANGELOG](CHANGELOG.txt) file.
