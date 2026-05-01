# RutasApp Android

App nativa Android para gestión de rutas y visitas de campo.

**Stack:** Kotlin 2 · Jetpack Compose · Material 3 · Clean Architecture + MVVM · Hilt · Room · WorkManager

**API:** `https://mejoresiagratis.com/rutasproapk/api.php`  
**BD:** `cqvkelal_rutasapp_android` (independiente de la web)

---

## CI/CD — GitHub Actions

### Build automático (cada push a `main`)
Cada push dispara build + tests. APK debug disponible en **Actions → Artifacts** en ~8 min.

### Entrega de sprint
```bash
git tag sprint-s01
git push origin sprint-s01
```
Genera APK release y lo publica como GitHub Release automáticamente.

También desde: `Actions → Build Release APK → Run workflow`

---

## Setup local

```bash
git clone https://github.com/pabl3st/rutapp.git
cd rutapp
# Añadir local.properties:
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
echo "MAPS_API_KEY=AIza..." >> local.properties
# Abrir en Android Studio Ladybug 2024.2+
```

## Estructura

```
app/                    # Shell — MainActivity, NavGraph, DI
core/
  domain/               # Modelos, UseCases, interfaces — Kotlin puro
  data/                 # Room, Retrofit, SyncWorker
  ui/                   # Design system, tokens, componentes
  common/               # Extensions, Utils
feature/
  auth/                 # S01 — Login, registro individual + company
  home/                 # S02 — Dashboard
  rutas/                # S03 — Lista paradas + GPS
  mapa/                 # S04 — Google Maps
  visita/               # S05 — Formulario, foto, OCR
  kpis/                 # S06 — Estadísticas, gráficos
  calendario/           # S07 — Schedule, vacaciones
  importar/             # S08 — XLS import
  admin/                # S09 — Gestión equipo (company)
  perfil/               # S01 — Config personal
```

## Sprints

| Sprint | Estado | Tag |
|--------|--------|-----|
| S01 — Auth + BD | 🚧 En progreso | `sprint-s01` |
| S02 — Room + Sync | ⏳ Planificado | `sprint-s02` |
| S03 — Rutas + GPS | ⏳ Planificado | `sprint-s03` |
| S04 — Maps | ⏳ Planificado | `sprint-s04` |
| S05 — Visitas | ⏳ Planificado | `sprint-s05` |
| S06 — KPIs | ⏳ Planificado | `sprint-s06` |
| S07 — Calendario | ⏳ Planificado | `sprint-s07` |
| S08 — XLS Import | ⏳ Planificado | `sprint-s08` |
| S09 — Admin | ⏳ Planificado | `sprint-s09` |
| S10 — Polish | ⏳ Planificado | `sprint-s10` |
| S11 — Play Store | ⏳ Planificado | `sprint-s11` |
