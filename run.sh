#!/bin/bash

# Usage:
# ./run.sh ClassName [arg1 arg2 ...]

# Resolve script's own directory (project root)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR"

CLASS_NAME="$1"
shift  # Shift to access remaining args
JAVA_ARGS="$*"

if [[ -z "$CLASS_NAME" ]]; then
  echo "Usage: $0 <ClassName> [args...]"
  exit 1
fi

# Find the .java file (first match)
CLASS_FILE=$(find "$ROOT_DIR" -type f -name "$CLASS_NAME.java" | head -n 1)

if [[ -z "$CLASS_FILE" ]]; then
  echo "Error: Could not find class file for '$CLASS_NAME'"
  exit 2
fi

# Extract the module name from the path (top-level folder under root)
MODULE_NAME=$(echo "$CLASS_FILE" | sed -E "s|$ROOT_DIR/([^/]+)/.*|\1|")

# Extract the package name from the file
PACKAGE_NAME=$(grep "^package " "$CLASS_FILE" | sed 's/package //' | sed 's/;//' | tr -d '[:space:]')

# Combine to get the full class name
FULL_CLASS="${PACKAGE_NAME}.${CLASS_NAME}"


mvn -q -f "$ROOT_DIR/pom.xml" clean install -pl "$MODULE_NAME" -am -DskipTests
mvn -q -f "$ROOT_DIR/pom.xml" -pl "$MODULE_NAME" exec:java -Dexec.mainClass="$FULL_CLASS" -Dexec.args="$JAVA_ARGS"
