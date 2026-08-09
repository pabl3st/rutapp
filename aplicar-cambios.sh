#!/bin/bash
# ─────────────────────────────────────────────────────────────
# RutasApp — aplicar rutapp-cambios-v2.zip desde Codespaces
# Uso: sube el ZIP a la raíz del repo (arrástralo al Explorer)
#      y ejecuta:  bash aplicar-cambios.sh
# ─────────────────────────────────────────────────────────────
set -e
cd "$(git rev-parse --show-toplevel)"   # raíz del repo, esté donde esté

ZIP=$(ls rutapp-cambios*.zip 2>/dev/null | sort | tail -1)
[ -z "$ZIP" ] && { echo "✗ No encuentro rutapp-cambios*.zip en la raíz. Súbelo primero."; exit 1; }
echo "→ Usando $ZIP"

echo "→ Actualizando repo"
git pull --ff-only origin main

echo "→ Descomprimiendo (sobrescribe respetando rutas)"
unzip -o -q "$ZIP" -d .

echo "→ Limpiando ficheros que no van al repo"
# Se borra tambien a si mismo: es una herramienta de despliegue, no
# codigo del proyecto, y no debe acabar commiteada en el repo.
rm -f rutapp-cambios*.zip LEEME-COMO-APLICAR.md rutapp-fixes.bundle aplicar-cambios.sh
git rm -q --cached rutapp-fixes.bundle 2>/dev/null || true

echo "→ Cambios a commitear:"
git add -A
git diff --cached --stat
if git diff --cached --quiet; then
  echo "✗ Nada que commitear — ¿el ZIP era el mismo que ya está aplicado?"
  exit 1
fi

git commit -m "fix(sync): motivo de fallo de descarga visible, cola de sync, firma estable y formulario en cards"

echo "→ Push a main (esto dispara el build en GitHub Actions)"
git push origin main

echo
echo "✓ Hecho. El APK lo compila CI, no esta máquina:"
echo "  https://github.com/pabl3st/rutapp/actions"
echo "  En ~6 min estará en Artifacts si el paso del keystore muestra la huella E9:8C:82:…"
