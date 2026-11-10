#!/usr/bin/env bash
# PreToolUse hook on Bash: gates `git push` behind ./gradlew connectedAndroidTest.
set -uo pipefail

hook_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
command=$(node "$hook_dir/extract-command.js" 2>/dev/null)

case "$command" in
  *"git push"*) ;;
  *) exit 0 ;;
esac

repo_root="$(git rev-parse --show-toplevel 2>/dev/null)" || exit 0
cd "$repo_root" || exit 0

if ! ./gradlew connectedAndroidTest; then
  echo "Bloqueado: './gradlew connectedAndroidTest' ha fallado. Corrige los tests instrumentados antes de hacer push." >&2
  exit 2
fi

exit 0
