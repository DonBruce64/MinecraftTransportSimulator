#!/bin/bash

#Set java to 1.8 to match ForgeGradle 3.+.
#JAVA_HOME="C:\Program Files\Java\jdk1.8.0_241"

#Get the string of line with the mod version number.
# shellcheck disable=SC2002
VERSTRING=$(cat src/main/java/mcinterface1122/InterfaceLoader.java | grep "MODVER")

#Now parse out the part right after the first qote.
VERSTRING=${VERSTRING#*\"}

#Now remove the trailing quote.
VERSTRING=${VERSTRING%%\"*}

#Now that we have the version we need to inject it into the gradle.properties file.
echo "Configuring gradle for $VERSTRING"
sed -i '9s/.*mod_version.*/mod_version='$VERSTRING'/' gradle.properties

#Finally, build the mod.
if [[ "$PULL_DEPS" != 1 ]]; then
    # Default behavior: build it with `--offline`
    ./gradlew build --offline
else
    # Set PULL_DEPS=1 to build it without `--offline`
    ./gradlew build
fi

exit $?
