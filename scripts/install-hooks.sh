#!/bin/bash
# Instala el pre-commit hook para RutasApp
# Uso: bash scripts/install-hooks.sh
cp scripts/pre-commit-hook.py .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
echo "✅ Pre-commit hook instalado. Verifica con: git commit --dry-run"
