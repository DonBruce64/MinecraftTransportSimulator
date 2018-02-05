#!/bin/bash
DSTPATH="../../forge2/src/main/java"
VERPREFIX="1.10.0-"

#Now that we are done with changes, update build number in build.gradle and run buildscript.
#First get the string of line with the mod version number.
VERSTRING=$(cat $DSTPATH/minecrafttransportsimulator/MTS.java | grep "MODVER")
#Now parse out the part right after MODVER.
VERSTRING=${VERSTRING##*MODVER}
#Now isolate the version.
VERSTRING=${VERSTRING:2:${#VERSTRING}-4}

#Now that we have the version we need to inject it into the build.gradle file.
echo "Configuring build.gradle for $VERPREFIX$VERSTRING"
sed -i '13s/.*version.*/version = "'$VERPREFIX$VERSTRING'"/' $DSTPATH/../../../build.gradle

#Finally, build the mod.'
cd $DSTPATH/../../../
./gradlew build --offline
cd ../forge2/src