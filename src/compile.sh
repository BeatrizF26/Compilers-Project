#!/bin/bash

set -e

if [ "$(basename "$PWD")" = "src" ]; then
  :
elif [ "$(basename "$PWD")" = "ish" ]; then
  cd ..
else
  echo "Please run \"./build\" from the src or ish directory"
  exit 1
fi

if [ $# -ne 1 ]; then
    echo "Usage: $0 <source-file>"
    exit 1
fi

SOURCE_FILE="$1"


if [ ! -f "$SOURCE_FILE" ]; then
    echo "File '$SOURCE_FILE' not found!"
    exit 1
fi

# compile the source code using the main function
java CompShMain "$SOURCE_FILE"

echo "Compiled code: Output.java"