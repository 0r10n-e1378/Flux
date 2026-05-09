#!/bin/bash
# Run script for the Flux game

echo "Compiling Java files..."
cd "$(dirname "$0")"
javac -d bin -cp src src/core/Main.java src/entities/*.java src/math/*.java src/systems/*.java src/ui/*.java src/world/*.java src/ai/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful. Starting game..."
    cd bin
    java core.Main
else
    echo "Compilation failed."
    exit 1
fi