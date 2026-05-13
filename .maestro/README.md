# RutasApp — E2E Tests (Maestro)

Tests end-to-end con [Maestro](https://docs.maestro.dev/) para verificar los flujos principales de la app.

## Setup

```bash
# Instalar Maestro CLI
curl -Ls "https://get.maestro.mobile.dev" | bash

# Verificar instalación
maestro --version
```

## Ejecutar tests

```bash
# Smoke test completo (login + ruta + visita)
maestro test .maestro/smoke-test.yaml

# Test de jornada laboral
maestro test .maestro/jornada-test.yaml

# Test de sync
maestro test .maestro/sync-test.yaml

# Todos los tests con tag CI
maestro test --include-tags ci .maestro/
```

## Prerequisitos

- APK instalado en el emulador o dispositivo: `adb install app/build/outputs/apk/debug/app-debug.apk`
- Usuario de prueba: `god` / `God2026!` con al menos 1 ruta y 1 parada en la BD
- Servidor API accesible desde el dispositivo: `https://mejoresiagratis.com/rutasproapk/api.php`

## Estructura

```
.maestro/
├── smoke-test.yaml       # Flujo principal: login → ruta → visita → guardar
├── jornada-test.yaml     # Jornada laboral: start → pause → resume → finish
├── sync-test.yaml        # Verificación de sync: visita → KPIs actualizados
└── flows/
    └── login.yaml        # Sub-flow reutilizable de autenticación
```

## testTags disponibles

| Tag | Composable |
|-----|-----------|
| `bottom-nav-home` | BottomNavBar → Home |
| `bottom-nav-rutas` | BottomNavBar → Rutas |
| `bottom-nav-kpis` | BottomNavBar → KPIs |
| `bottom-nav-calendario` | BottomNavBar → Calendario |
| `bottom-nav-perfil` | BottomNavBar → Perfil |
| `home-screen` | HomeScreen raíz |
| `rutas-screen` | RutasScreen raíz |
| `route-detail-screen` | RouteDetailScreen raíz |
| `visita-screen` | VisitaScreen raíz |
| `kpis-screen` | KpisScreen raíz |
| `route-card-{n}` | Card de ruta en lista (0-indexed) |
| `stop-card-{n}` | Card de parada en RouteDetail (0-indexed) |
| `visita-save-button` | Botón "Guardar visita" |

## CI con Maestro Cloud (futuro)

Para ejecutar en dispositivos reales en CI, añadir a `.github/workflows/`:

```yaml
- name: Run Maestro Cloud Tests
  uses: mobile-dev-inc/action-maestro-cloud@v2
  with:
    api-key: ${{ secrets.MAESTRO_API_KEY }}
    app-file: app/build/outputs/apk/debug/app-debug.apk
    workspace: .maestro
    include-tags: ci
```
