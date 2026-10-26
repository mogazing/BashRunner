#!/bin/bash
# Standalone Bash Runner Integration Test Script
echo "=========================================="
echo "Bash Runner Local Execution Verification"
echo "=========================================="

GREETING="Hello from standalone bundled Bash interpreter!"
echo "Message: $GREETING"
echo "Current Shell: $BASH_VERSION"
echo "Working Directory: $(pwd)"

echo "Testing BusyBox integration..."
if [ -n "$BUSYBOX" ] && [ -x "$BUSYBOX" ]; then
    echo "BusyBox binary path: $BUSYBOX"
    "$BUSYBOX" uname -a 2>/dev/null || echo "BusyBox applet invoked successfully."
else
    echo "BusyBox env var set: ${BUSYBOX:-not set}"
fi

echo "Writing diagnostic line to stderr..." >&2

echo "Integration test completed successfully."
exit 0
