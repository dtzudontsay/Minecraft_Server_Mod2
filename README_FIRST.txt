KNOWN WORLD - CLEAN FABRIC 26.3 PROJECT

Create this folder:
D:\Minecraft Development\GameOfThronesMod

Extract the CONTENTS of this ZIP directly into that folder.

Then open PowerShell in the folder and run:

powershell -ExecutionPolicy Bypass -File .\setup-wrapper.ps1
.\gradlew.bat build

Expected:
BUILD SUCCESSFUL

Then:
1. Open the folder itself in IntelliJ.
2. Load/import it as a Gradle project.
3. Use Java 25 for Project SDK and Gradle JVM.
4. Run the generated Minecraft Client configuration.
5. Create a world with commands enabled.
6. Run:

/knownworld status

Expected:
Known World loaded | Minecraft 26.3 | Fabric | 1 block = 1 metre

IMPORTANT:
Do not create a separate Java project first.
Do not initialize/connect Git until this build and in-game test succeed.

Versions used are based on FabricMC's official 26.3 example branch:
Minecraft 26.3
Fabric Loader 0.19.5
Fabric Loom 1.17-SNAPSHOT
Fabric API 0.161.0+26.3
Java 25
Gradle 9.5.1
