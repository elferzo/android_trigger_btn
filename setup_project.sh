#!/bin/bash
set -e

mkdir -p app/src/main/kotlin/com/example/triggerbtn
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/xml

cp WeeekWidget.kt      app/src/main/kotlin/com/example/triggerbtn/
cp PlaudWidget.kt      app/src/main/kotlin/com/example/triggerbtn/
cp MainActivity.kt     app/src/main/kotlin/com/example/triggerbtn/
cp BootReceiver.kt     app/src/main/kotlin/com/example/triggerbtn/
cp widget_weeek.xml    app/src/main/res/layout/
cp widget_plaud.xml    app/src/main/res/layout/
cp widget_info_weeek.xml app/src/main/res/xml/
cp widget_info_plaud.xml app/src/main/res/xml/
cp AndroidManifest.xml app/src/main/
cp root_build_gradle.txt build.gradle
cp app_build_gradle.txt  app/build.gradle

cat > settings.gradle << 'SETTINGS'
rootProject.name = "triggerbtn"
include ':app'
SETTINGS

echo "Project structure ready"
