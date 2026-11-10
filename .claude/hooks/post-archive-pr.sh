#!/usr/bin/env bash
# PostToolUse hook on Bash: after the final `docs(openspec): archive ...` commit
# (CLAUDE.md "Ramas y commits"), pushes the branch and opens the PR to develop.
set -uo pipefail

hook_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
command=$(node "$hook_dir/extract-command.js" 2>/dev/null)

case "$command" in
  *"git commit"*) ;;
  *) exit 0 ;;
esac

repo_root="$(git rev-parse --show-toplevel 2>/dev/null)" || exit 0
cd "$repo_root" || exit 0

node "$hook_dir/create-pr-after-archive.js"
exit 0
