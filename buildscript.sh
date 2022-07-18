#!/bin/bash

#Set java to 1.8 to match ForgeGradle 3.+.
#JAVA_HOME="C:\Program Files\Java\jdk1.8.0_241"

VERPREFIX="1.12.2-"

#Get the string of line with the mod version number.
VERSTRING=$(cat src/main/java/mcinterface1122/InterfaceLoader.java | grep "MODVER")

#Now parse out the part right after the first qote.
VERSTRING=${VERSTRING#*\"}

#Now remove the trailing quote.
VERSTRING=${VERSTRING%%\"*}

#Now that we have the version we need to inject it into the build.gradle file.
echo "Configuring build.gradle for $VERPREFIX$VERSTRING"
sed -i '16s/.*version.*/version = "'$VERPREFIX$VERSTRING'"/' build.gradle

#Finally, build the mod.
if [[ "$PULL_DEPS" != 1 ]]; then
    # Default behavior: build it with `--offline`
    ./gradlew build --offline
else
    # Set PULL_DEPS=1 to build it without `--offline`
    ./gradlew build
fi

exit $?
