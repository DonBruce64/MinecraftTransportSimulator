#!/bin/bash
VERPREFIX="1.12.2-"

#Get the string of line with the mod version number.
VERSTRING=$(cat src/main/java/minecrafttransportsimulator/MTS.java | grep "MODVER")
#Now parse out the part right after MODVER.
VERSTRING=${VERSTRING##*MODVER}
#Now isolate the version.
VERSTRING=${VERSTRING:2:${#VERSTRING}-4}

#Now that we have the version we need to inject it into the build.gradle file.
echo "Configuring build.gradle for $VERPREFIX$VERSTRING"
sed -i '14s/.*version.*/version = "'$VERPREFIX$VERSTRING'"/' build.gradle

#Finally, build the mod.
./gradlew build --offline