#!/bin/bash

if [ "$(basename "$PWD")" = "src" ]; then
  :
elif [ "$(basename "$PWD")" = "ish" ]; then
  cd ..
else
  echo "Please run \"./build\" from the src or ish directory"
  exit 1
fi

echo "Building main language..."
antlr4-build CompSh

echo "Building secondary language..."
cd ish
antlr4-build

echo "Finished building both languages!"