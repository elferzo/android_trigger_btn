#!/bin/bash
set -e

mkdir -p app/src/main/kotlin/com/example/weeek
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/xml

cp WeeekWidget.kt     app/src/main/kotlin/com/example/weeek/
cp MainActivity.kt    app/src/main/kotlin/com/example/weeek/
cp BootReceiver.kt    app/src/main/kotlin/com/example/weeek/
cp widget_layout.xml  app/src/main/res/layout/
cp widget_info.xml    app/src/main/res/xml/
cp AndroidManifest.xml app/src/main/
cp root_build_gradle.txt build.gradle
cp app_build_gradle.txt  app/build.gradle

cat > settings.gradle << 'SETTINGS'
rootProject.name = "weeek"
include ':app'
SETTINGS

echo "Project structure ready"
