#!/bin/bash

# Compile all Java files
echo "Compiling Java files..."
find src -name "*.java" -type f -print0 | xargs -0 javac -encoding UTF-8 -d target/classes 2>&1 | head -50

# Check compilation status
if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo "✓ Compilation successful"

    # Run test
    echo ""
    echo "Running R2010 sample file test..."
    java -cp target/classes run.R2010SingleTest
else
    echo "✗ Compilation failed"
    exit 1
fi
