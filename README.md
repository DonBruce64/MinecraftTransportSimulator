[![image](https://user-images.githubusercontent.com/46881115/181852836-2db66bee-7d6c-4d57-9ba8-17c313f25098.png)](https://www.curseforge.com/minecraft/mc-mods/minecraft-transport-simulator)

[![Stars](https://img.shields.io/github/stars/DonBruce64/MinecraftTransportSimulator?style=for-the-badge)](https://github.com/DonBruce64/MinecraftTransportSimulator/stargazers)
[![Forks](https://img.shields.io/github/forks/DonBruce64/MinecraftTransportSimulator?style=for-the-badge)](https://github.com/DonBruce64/MinecraftTransportSimulator/network/members)
[![Open Issues](https://img.shields.io/github/issues/DonBruce64/MinecraftTransportSimulator?style=for-the-badge)](https://github.com/DonBruce64/MinecraftTransportSimulator/issues)\
\
[![Discord](https://discordapp.com/api/guilds/232316230852280320/widget.png?style=banner2)](https://discord.com/invite/KaaSUjm)

# Immersive Vehicles (Formerly Transport Simulator)
From the folks that brought you Minecraft Flight Simulator comes the all-new Immersive Vehicles mod!\
This mod is the result of over a year's worth of work and is the continuation and future of Minecraft Flight Simulator.

## Building From Source or Contributing
To build from source, you will need to do a few things.  These instructions assume you are on Windows.  If you are on Linux, you should be able to tell where your instructions differ and how to handle such differences.

<ol>
<li>Create a fork of this repository.  Git has tons of tutorials for this.</li>
<li>Ensure you have Git Bash installed.  While you can make updates and commits through the web interface, this is a Bad Idea and will lead to pain down the road.  Command line programs are your freind when coding.</li>
<li>Open Git Bash in the folder where you want to keep your code via the right-click option `Git Bash Here`.  Then do a `git clone URL`, where the URL is the one for your forked repository.  Git will download all your code and files locally to allow you to start programming.</li>
<li>Ensure you have the Java 8 JDK installed.  This is NOT the same as the Java 8 JRE program that you use to run most Java applications.  You will specifically need to install the JDK.  Check your progam files if you are un-sure.</li>
<li>If you just installed the JDK, you will need to update your `path`.  This is a System Environment Variable on Windows that tells it where to look for things.  You want to add the directory where the Java 8 JDK was installed (likely `C:\Program Files\Java\SomeJDKVersion\bin`.  If you already have Java 8 JRE installed, you will see it in this list.  Just remove that entry and replace it with the Java 8 JDK entry (the location should match except for one folder).  You know you have done this correctly if you can open your command prompt and type `javac` and get a result besides "command not found".  (Note, you will have to close and open Command Prompt to reflect any PATH changes.</li>
<li>Now that you have your Java set up, you can use the same Git Bash prompt inside the folder you just cloned to setup your workspace:</li>
<ul>
<li>If you are running Eclipse, run `./gradlew eclipse` and then `./gradlew genEclipseRuns`.  Then open your Eclipse program.  When it asks you for a "workspace location" do NOT pick any folder to do with code.  Just pick a folder somewhere where Eclipse can throw things and you can forget about.  Once this is done, you need to go into the menu and "Import Gradle Project".  Pick the folder where all your code was copied to and the files should show up in the editor.  To test them, you can click the little bug icon and then go to Debug Configurations->Java Application-runClient. Select this, and go to the Environment tab and delete the MC_VERSION entry.  Apply, close, and then select it for debugging and you're off to testing your code!</li>
<li>If you are using IDEA, import the build script (`build.gradle`) with IDEA then run the `genIntellijRuns` Gradle task when it finishes importing.</li>
<li>If you are running VSCode, run `./gradlew genVSCodeRuns`.  Then bug TurboDefender on the Discord to fill this section out.</li>
<li>If you aren't using any of these three, START DOING SO.  Notepad is not meant for coding.  Do you want a Bad Time?  I didn't think so.</li>
</ul>
<li>After making your code changes, use Git Bash to do a `git add -u` to add all modified files.  Then you can do a `git commit -m "Message for what you commited"`. This marks the changes, and the reason for them.  Finally, do a `git push` to push the changes back up to Git.  From there, you can create a Pull Request and have your code be part of the mod!</li>
</ol>

