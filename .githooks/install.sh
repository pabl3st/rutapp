#!/usr/bin/env bash
# Instala los hooks de git para RutasApp
# Ejecutar una vez tras clonar: bash .githooks/install.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
HOOKS_DIR="$(git rev-parse --show-toplevel)/.git/hooks"

echo "Instalando hooks de git..."
for hook in "$SCRIPT_DIR"/*.sh; do
  : # skip install.sh
done
cp "$SCRIPT_DIR/pre-push" "$HOOKS_DIR/pre-push"
chmod +x "$HOOKS_DIR/pre-push"
echo "✅ pre-push hook instalado"
echo "   Ejecuta 'git push --no-verify' para saltar en emergencias"
