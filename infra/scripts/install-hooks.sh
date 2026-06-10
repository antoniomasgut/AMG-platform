#!/bin/bash
# Instal·la els git hooks del projecte
# Ús: bash infra/scripts/install-hooks.sh

set -e

ROOT="$(git rev-parse --show-toplevel)"
HOOKS_SRC="$ROOT/infra/git-hooks"
HOOKS_DST="$ROOT/.git/hooks"

for hook in "$HOOKS_SRC"/*; do
  name="$(basename "$hook")"
  ln -sf "$hook" "$HOOKS_DST/$name"
  chmod +x "$hook"
  echo "✅ Hook instal·lat: $name"
done

echo ""
echo "Hooks actius a .git/hooks:"
ls -la "$HOOKS_DST" | grep -v "^total\|^\." | awk '{print "  " $NF}'
