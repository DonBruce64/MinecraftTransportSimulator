[![image](https://user-images.githubusercontent.com/46881115/181852836-2db66bee-7d6c-4d57-9ba8-17c313f25098.png)](https://www.curseforge.com/minecraft/mc-mods/minecraft-transport-simulator)

[![Stars](https://img.shields.io/github/stars/DonBruce64/MinecraftTransportSimulator?style=for-the-badge)](https://github.com/DonBruce64/MinecraftTransportSimulator/stargazers)
[![Forks](https://img.shields.io/github/forks/DonBruce64/MinecraftTransportSimulator?style=for-the-badge)](https://github.com/DonBruce64/MinecraftTransportSimulator/network/members)
[![Open Issues](https://img.shields.io/github/issues/DonBruce64/MinecraftTransportSimulator?style=for-the-badge)](https://github.com/DonBruce64/MinecraftTransportSimulator/issues)\
\
[![Discord](https://discordapp.com/api/guilds/232316230852280320/widget.png?style=banner2)](https://discord.com/invite/KaaSUjm)

# Immersive Vehicles (Formerly Transport Simulator)
From the folks that brought you Minecraft Flight Simulator comes the all-new Immersive Vehicles mod!\
This mod is the result of over a year's worth of work and is the continuation and future of Minecraft Flight Simulator.

## Building From Source
These instructions assume you are using Windows.   If you are using a flavor of Linux, you should be able to tell where your instructions differ and how to handle such differences.
### Requirements
- [Git Bash](https://gitforwindows.org/)
- A JDK (Java Development Kit) of 8 or 17, such as Oracle's [JDK 8](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)/[JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or [Eclipse Temurin's OpenJDK](https://adoptium.net/temurin/releases/).   This is **NOT** the same as the JRE (Java Runtime Environment) you likely already have installed.\
Note: A 17 JDK will eventually be **required** when the Gradle version the mod uses is updated
- The `JAVA_HOME` environment variable should be set to your JDK make the entire process easier.   If you don't know what that is or how to change it, see steps one through three [here](https://docs.oracle.com/en/database/oracle/machine-learning/oml4r/1.5.1/oread/creating-and-modifying-environment-variables-on-windows.html).\
You should replace the `JAVA_HOME` variable with the path to your previously/newly installed JDK if it isn't already the same.   If it doesn't exit, use the "New" button and make it.   `JAVA_HOME` should be set to `C:\Program Files\Java\someJDKVersion\` if you have an Oracle JDK or `C:\Program Files\Eclipse Adoptium\someJDKVersion\` if you have a Temurin JDK.
### Getting it on Your Machine
Now that you have a JDK and Git Bash setup, you can clone the repository for the mod.
1. Open the folder that you want this repository's folder to be in
2. Right click anywhere in the folder in File Explorer (not on a file or any of the toolbars)
3. Click "Git Bash Here", copy `git clone https://github.com/DonBruce64/MinecraftTransportSimulator.git` then paste it into the git bash terminal using Left Shift + Insert to paste, then hit enter.   When it is finished, set up the development environment according to the instructions for your IDE.
   - **CONTRIBUTORS, READ ME!**\
   If you plan on contributing, make a fork using the fork button in the top right on <https://github.com/DonBruce64/MinecraftTransportSimulator>, then clone you the forked repository you are redirected to in the form of `https://github.com/yourGitHubUsername/MinecraftTransportSimulator` instead of the main repository.   You will not be able to push changes to the main repository and will need to make a pull request with changes from your fork.
### IDEs
#### Eclipse
1. Using the same Git Bash terminal from before, run `cd ./MinecraftTransportSimulator` to get into the folder that was created, `./gradlew eclipse` and `./gradlew genEclipseRuns`\
Note: May need updating, not sure if this is still accurate
2. Make a folder called `eclipse` in the cloned folder and open Eclipse.   When it asks you for a "Workspace Location", select the `eclipse` folder that you just created.
3. Go to the menu and click "Import Gradle Project".   Choose the folder that was created after you ran `git clone` before, it should be called "MinecraftTransportSimulator".
4. To test changes you make to the code, you can click the little bug icon and then go to Debug Configurations -> Java Application-runClient.   Select this, go to the Environment tab, and set the `MC_VERSION` entry to the game version.
5. When you're ready to build the mod, run `./gradlew buildForge1.12.2` or `./gradlew buildForge1.16.5` for 1.12.2 and 1.16.5 respectively.
#### IntelliJ IDEA
1. Right-click the folder that was created when you ran `git clone` and click "Open Folder as IntelliJ yourEdition Edition IDEA Project".   If you don't have the option, start IDEA and click File -> Open then navigate to the directory and choose the folder.
2. Wait for IDEA to finish setting up the Gradle script then use the "Gradle" tab on the right side of your screen to run Gradle tasks.   Run `genIntelliJRuns` in the project for the Forge version and game version you want, e.g. `mcinterfaceforge1122` for Forge for 1.12.2.
3. When you want to test your changes on the client or server, use the `runClient` or `runServer` configurations.   Click the dropdown then "Edit Configurations..." and change the `MC_VERSION` environment variable to the game version.   If the run configuration has a red x, change the project module to `Immersive_Vehicles.forgeInterfaceVersion.main` in the same menu.
4. When you're ready to build the mod, run `./gradlew buildForge1.12.2` or `./gradlew  buildForge1.16.5` (`buildForge1.12.2` and `buildForge1.16.5` in the Gradle tab) for 1.12.2 and 1.16.5 respectively.

Need more help with IDEA? Ask Elephant_1214#3698 in the Discord server.
#### VSCode
Bug TurboDefender on Discord about how to set this up in VSCode
#### Other
You're on your own unless someone else in the Discord server uses it.   Keep in mind that Notepad, Notepad++, vi, vim, nano, etc, are **not** meant for Java development, and you will not receive help with issues unrelated to the mod that arise while using them.
### Contributing
Once you have made the changes you want to commit, open Git Bash in the project folder (the one named `MinecraftTransportSimulator`) and run `git add -u` then `git commit -m "Your commit message here"`.   Finally, run `git push` to push the changes to your fork on GitHub which you should see your commit added to within a few seconds.   After that, all you need to do is make a pull request to get your changes reviewed and added to the mod!
