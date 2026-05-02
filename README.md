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

| Sprint | Estado | Contenido |
|--------|--------|-----------|
| S01 — Auth + BD | ✅ Completado | Auth completo, SessionManager cifrado, CI/CD, API PHP, BD MySQL |
| S02 — Room + Sync | ✅ Completado | Room (Route/Stop/SyncQueue), SyncWorker offline-first, HomeScreen, Firebase FCM |
| S03 — Rutas + GPS | ✅ Completado | RutasScreen, RouteDetailScreen, RouteMapScreen (MapLibre), GPS |
| S04 — Mapa global | ✅ Completado | GlobalMapScreen con MapLibre, filtros por estado |
| S05 — Visitas | ✅ Completado | VisitaScreen, formulario visita, fotos CameraX + Coil |
| S06 — KPIs | ✅ Completado | KpisScreen con estadísticas y tendencias |
| S07 — Creador paradas + OSRM | ✅ Completado | CrearParadaScreen, routing OSRM en MapLibreProvider |
| S08 — Calendario | ⏳ Planificado | Placeholder activo en nav |
| S09 — Admin | ⏳ Planificado | Placeholder activo en nav |
| S10 — XLS Import | ⏳ Planificado | — |
| S11 — Polish | ⏳ Planificado | — |
| S12 — Play Store | ⏳ Planificado | — |
