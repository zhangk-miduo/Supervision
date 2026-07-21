#!/bin/bash
# goal-loop wrapper — routes to the real skill scripts.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SKILL="$HOME/.workbuddy/skills/goal-loop__skillhub/scripts/loop.sh"
if [ -x "$SKILL" ]; then
    exec "$SKILL" "$@"
else
    echo "goal-loop scripts not found at $SKILL" >&2
    exit 1
fi
