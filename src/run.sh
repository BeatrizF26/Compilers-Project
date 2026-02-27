#!/bin/bash

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

EXECUTABLE="$1"

# compile excutable file
javac "$EXECUTABLE"

# run the file
BASE_NAME=$(basename "$EXECUTABLE" .java)
java "$BASE_NAME"