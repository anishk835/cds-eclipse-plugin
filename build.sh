#!/bin/bash
# Build script for CDS Eclipse Plugin
# This script ensures the plugin is built with Java 17 for Eclipse compatibility

export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin"

echo "Building with Java 17..."
java -version

mvn "$@"
