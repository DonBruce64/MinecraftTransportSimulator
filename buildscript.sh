#!/bin/bash
VERPREFIX="1.12.2-"

#Get the string of line with the mod version number.
VERSTRING=$(cat src/main/java/minecrafttransportsimulator/MasterLoader.java | grep "MODVER")

#Now parse out the part right after the first qote.
VERSTRING=${VERSTRING#*\"}

#Now remove the trailing quote.
VERSTRING=${VERSTRING%%\"*}

#Now that we have the version we need to inject it into the build.gradle file.
echo "Configuring build.gradle for $VERPREFIX$VERSTRING"
sed -i '19s/.*version.*/version = "'$VERPREFIX$VERSTRING'"/' build.gradle

#Finally, build the mod.
./gradlew build --offline
