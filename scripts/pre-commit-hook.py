#!/usr/bin/env python3
"""
Pre-commit hook para RutasApp — Kotlin/Android
Detecta los patrones que han causado BUILD FAILED en CI.
Skills: git-commit (github/awesome-copilot) + static-code-analysis (aj-geddes)
"""
import re, sys, subprocess
from pathlib import Path

RED  = "\033[31m"; YEL = "\033[33m"; GRN = "\033[32m"
RESET = "\033[0m"; BOLD = "\033[1m"

result = subprocess.run(
    ["git", "diff", "--cached", "--name-only", "--diff-filter=ACM"],
    capture_output=True, text=True
)
staged = [f for f in result.stdout.strip().splitlines() if f.endswith(".kt")]
if not staged:
    sys.exit(0)

errors = []; warnings = []

for filepath in staged:
    p = Path(filepath)
    if not p.exists(): continue
    src   = p.read_text(encoding="utf-8")
    lines = src.splitlines()
    is_theme = "core/ui/theme" in filepath

    for i, raw in enumerate(lines, 1):
        s = raw.strip()
        # E1: Comillas tipográficas
        if '\u201c' in raw or '\u201d' in raw:
            errors.append((filepath, i, "E1", "Comillas tipográficas \" \" — usar \" ASCII"))
        # E2: Doble comilla antes de interpolación
        if re.search(r'""(\$\{|\$[a-zA-Z_])', raw):
            errors.append((filepath, i, "E2", f'""$var — debe ser "$var": {s[:60]}'))
        # E3: Escape malformado joinToString
        if re.search(r'"\\""\s*\+\s*\w+\s*\+\s*"\\""', raw):
            errors.append((filepath, i, "E3", f'Usar "\\\"$x\\\\"": {s[:60]}'))
        # E4: Import MaterialTheme duplicado
        if 'import androidx.compose.material3.MaterialTheme' in raw \
                and 'import androidx.compose.material3.*' in src:
            errors.append((filepath, i, "E4", "MaterialTheme ya incluido en material3.*"))
        # E5: StatusTokens sin import
        if "RouteStatusTokens.of(" in raw and not is_theme:
            if "import com.pabl3st.rutapp.core.ui.theme.RouteStatusTokens" not in src:
                errors.append((filepath, i, "E5", "RouteStatusTokens sin import"))
        if "StopStatusTokens.of(" in raw and not is_theme:
            if "import com.pabl3st.rutapp.core.ui.theme.StopStatusTokens" not in src:
                errors.append((filepath, i, "E5", "StopStatusTokens sin import"))
        # E6: RutasColors sin import
        if "RutasColors." in raw and not is_theme:
            if "import com.pabl3st.rutapp.core.ui.theme.RutasColors" not in src:
                errors.append((filepath, i, "E6", f"RutasColors sin import: {s[:50]}"))
        # E7: Triple() residual
        if "Triple(" in raw and "RouteStatusTokens" in src:
            errors.append((filepath, i, "E7", f"Triple() residual con StatusTokens: {s[:60]}"))

    # E8: Variable de forEach usada fuera de su scope
    # Mejorado: ignorar si la variable está redefinida como parámetro de función
    #           en el rango entre el forEach y el punto de uso
    for i, raw in enumerate(lines):
        m = re.search(r'\.(forEach|map)\s*\{\s*(\w+)\s*->', raw)
        if not m: continue
        var = m.group(2)

        # Encontrar el cierre del bloque forEach
        depth, close = 0, None
        for j, l in enumerate(lines[i:i+60], i):
            for ch in l:
                if ch == '{': depth += 1
                elif ch == '}':
                    depth -= 1
                    if depth < 0: close = j; break
            if close: break
        if not close: continue

        # Buscar uso de la variable después del cierre
        for k in range(close, min(close + 15, len(lines))):
            l = lines[k]
            if not re.search(rf'\$\{{{var}\.|\${var}\.', l):
                continue

            # ── FILTRO DE FALSO POSITIVO ──────────────────────
            # ¿Hay una declaración `fun xxx(var:` entre el forEach y esta línea?
            context_between = "\n".join(lines[i:k])
            # Si `var` aparece como parámetro de función en ese rango → válido
            if re.search(rf'fun \w+\([^)]*\b{var}\s*:', context_between):
                continue
            # Si `val var =` o `var var =` aparece entre forEach y uso → válido
            if re.search(rf'\b(val|var)\s+{var}\b', context_between):
                continue

            errors.append((filepath, k+1, "E8",
                f"'{var}' del forEach usado fuera de scope: {l.strip()[:70]}"))

    # Warnings
    if src.count("object Spacing {") > 1:
        warnings.append((filepath, 0, "W1", "object Spacing duplicado"))

# Output
print(f"\n{BOLD}🔍 Pre-commit — {len(staged)} fichero(s) Kotlin verificados{RESET}\n")
for f, ln, code, msg in warnings:
    loc = f":{ln}" if ln else ""
    print(f"  {YEL}⚠  {code}{RESET}  {f}{loc}\n     {msg}\n")
if errors:
    for f, ln, code, msg in errors:
        loc = f":{ln}" if ln else ""
        print(f"  {RED}✖  {code}{RESET}  {f}{loc}\n     {msg}\n")
    print(f"{RED}{BOLD}❌ {len(errors)} error(s) — commit bloqueado{RESET}\n")
    sys.exit(1)
print(f"{GRN}{BOLD}✅ Sin errores — commit permitido{RESET}\n")
sys.exit(0)
