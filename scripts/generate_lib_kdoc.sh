#!/bin/bash

SCRIPT_DIR=$(realpath "$(dirname "${BASH_SOURCE[0]}")")
PROJECT_DIR=$(realpath "$SCRIPT_DIR"/..)

DOCS_DIR=$PROJECT_DIR/docs
MODULE_NAME="sdk"

# Clean previous docs
rm -rf "$DOCS_DIR"
mkdir -p "$DOCS_DIR"

# Generate new kdoc
cd "$PROJECT_DIR" || exit
./gradlew $MODULE_NAME:dokka

# Copy fresly generated kdoc
cp -r "$PROJECT_DIR"/$MODULE_NAME/build/dokka "$DOCS_DIR"/dokka
