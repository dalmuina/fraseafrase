#!/usr/bin/env bash
# PreToolUse hook on Bash: gates `git commit` behind branch guard + ./gradlew check.
set -uo pipefail

hook_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
command=$(node "$hook_dir/extract-command.js" 2>/dev/null)

case "$command" in
  *"git commit"*) ;;
  *) exit 0 ;;
esac

repo_root="$(git rev-parse --show-toplevel 2>/dev/null)" || exit 0
cd "$repo_root" || exit 0

branch="$(git branch --show-current)"
if [ "$branch" = "develop" ] || [ "$branch" = "master" ]; then
  echo "Bloqueado: no se puede commitear en la rama '$branch'. Cambia a una rama de cambio antes de commitear (regla de oro 2 de CLAUDE.md)." >&2
  exit 2
fi

if ! ./gradlew check; then
  echo "Bloqueado: './gradlew check' ha fallado (tests unitarios o detekt). Corrige antes de commitear." >&2
  exit 2
fi

exit 0
