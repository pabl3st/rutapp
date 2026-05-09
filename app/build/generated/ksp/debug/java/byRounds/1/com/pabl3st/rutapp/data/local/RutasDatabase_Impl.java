package com.pabl3st.rutapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.pabl3st.rutapp.data.local.dao.BusinessProfileDao;
import com.pabl3st.rutapp.data.local.dao.BusinessProfileDao_Impl;
import com.pabl3st.rutapp.data.local.dao.DaySessionDao;
import com.pabl3st.rutapp.data.local.dao.DaySessionDao_Impl;
import com.pabl3st.rutapp.data.local.dao.KpiDefinitionDao;
import com.pabl3st.rutapp.data.local.dao.KpiDefinitionDao_Impl;
import com.pabl3st.rutapp.data.local.dao.KpiValueDao;
import com.pabl3st.rutapp.data.local.dao.KpiValueDao_Impl;
import com.pabl3st.rutapp.data.local.dao.RouteDao;
import com.pabl3st.rutapp.data.local.dao.RouteDao_Impl;
import com.pabl3st.rutapp.data.local.dao.StopDao;
import com.pabl3st.rutapp.data.local.dao.StopDao_Impl;
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao;
import com.pabl3st.rutapp.data.local.dao.SyncQueueDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RutasDatabase_Impl extends RutasDatabase {
  private volatile RouteDao _routeDao;

  private volatile StopDao _stopDao;

  private volatile SyncQueueDao _syncQueueDao;

  private volatile DaySessionDao _daySessionDao;

  private volatile KpiDefinitionDao _kpiDefinitionDao;

  private volatile BusinessProfileDao _businessProfileDao;

  private volatile KpiValueDao _kpiValueDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(6) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `routes` (`uid` TEXT NOT NULL, `serverId` INTEGER, `accountId` INTEGER NOT NULL, `userId` INTEGER NOT NULL, `name` TEXT NOT NULL, `dateAssigned` TEXT NOT NULL, `status` TEXT NOT NULL, `notes` TEXT, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `deletedAt` TEXT, `syncStatus` TEXT NOT NULL, `syncedAt` TEXT, PRIMARY KEY(`uid`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `stops` (`uid` TEXT NOT NULL, `serverId` INTEGER, `routeUid` TEXT NOT NULL, `accountId` INTEGER NOT NULL, `name` TEXT NOT NULL, `externalId` TEXT, `address` TEXT, `lat` REAL, `lng` REAL, `orderIndex` INTEGER NOT NULL, `contactName` TEXT, `contactPhone` TEXT, `visitFrequency` INTEGER, `priority` INTEGER NOT NULL, `segment` TEXT, `accountStatus` TEXT NOT NULL, `openingHours` TEXT, `status` TEXT NOT NULL, `notes` TEXT, `visitedAt` TEXT, `visitResult` TEXT, `nextAction` TEXT, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, `deletedAt` TEXT, `syncStatus` TEXT NOT NULL, `syncedAt` TEXT, PRIMARY KEY(`uid`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_queue` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entity` TEXT NOT NULL, `entityUid` TEXT NOT NULL, `operation` TEXT NOT NULL, `payload` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, `lastError` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `day_sessions` (`routeUid` TEXT NOT NULL, `dateStr` TEXT NOT NULL, `state` TEXT NOT NULL, `startedAt` INTEGER, `pausedAt` INTEGER, `elapsedMs` INTEGER NOT NULL, `distanceKm` REAL NOT NULL, `lastLat` REAL, `lastLng` REAL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`routeUid`, `dateStr`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_sessions_dateStr` ON `day_sessions` (`dateStr`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `kpi_definitions` (`id` TEXT NOT NULL, `accountId` INTEGER NOT NULL, `sector` TEXT NOT NULL, `label` TEXT NOT NULL, `type` TEXT NOT NULL, `unit` TEXT, `options` TEXT, `required` INTEGER NOT NULL, `visible` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `section` TEXT NOT NULL, `isSystem` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `business_profiles` (`accountId` INTEGER NOT NULL, `sector` TEXT NOT NULL, `name` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`accountId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `kpi_values` (`stopUid` TEXT NOT NULL, `kpiId` TEXT NOT NULL, `valueText` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, PRIMARY KEY(`stopUid`, `kpiId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f8e68ca0072dd8f687315846dbbe6205')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `routes`");
        db.execSQL("DROP TABLE IF EXISTS `stops`");
        db.execSQL("DROP TABLE IF EXISTS `sync_queue`");
        db.execSQL("DROP TABLE IF EXISTS `day_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `kpi_definitions`");
        db.execSQL("DROP TABLE IF EXISTS `business_profiles`");
        db.execSQL("DROP TABLE IF EXISTS `kpi_values`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsRoutes = new HashMap<String, TableInfo.Column>(13);
        _columnsRoutes.put("uid", new TableInfo.Column("uid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("serverId", new TableInfo.Column("serverId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("accountId", new TableInfo.Column("accountId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("userId", new TableInfo.Column("userId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("dateAssigned", new TableInfo.Column("dateAssigned", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("updatedAt", new TableInfo.Column("updatedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("deletedAt", new TableInfo.Column("deletedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRoutes.put("syncedAt", new TableInfo.Column("syncedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRoutes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRoutes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRoutes = new TableInfo("routes", _columnsRoutes, _foreignKeysRoutes, _indicesRoutes);
        final TableInfo _existingRoutes = TableInfo.read(db, "routes");
        if (!_infoRoutes.equals(_existingRoutes)) {
          return new RoomOpenHelper.ValidationResult(false, "routes(com.pabl3st.rutapp.data.local.entity.RouteEntity).\n"
                  + " Expected:\n" + _infoRoutes + "\n"
                  + " Found:\n" + _existingRoutes);
        }
        final HashMap<String, TableInfo.Column> _columnsStops = new HashMap<String, TableInfo.Column>(27);
        _columnsStops.put("uid", new TableInfo.Column("uid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("serverId", new TableInfo.Column("serverId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("routeUid", new TableInfo.Column("routeUid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("accountId", new TableInfo.Column("accountId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("externalId", new TableInfo.Column("externalId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("address", new TableInfo.Column("address", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("lat", new TableInfo.Column("lat", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("lng", new TableInfo.Column("lng", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("orderIndex", new TableInfo.Column("orderIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("contactName", new TableInfo.Column("contactName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("contactPhone", new TableInfo.Column("contactPhone", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("visitFrequency", new TableInfo.Column("visitFrequency", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("priority", new TableInfo.Column("priority", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("segment", new TableInfo.Column("segment", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("accountStatus", new TableInfo.Column("accountStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("openingHours", new TableInfo.Column("openingHours", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("visitedAt", new TableInfo.Column("visitedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("visitResult", new TableInfo.Column("visitResult", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("nextAction", new TableInfo.Column("nextAction", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("updatedAt", new TableInfo.Column("updatedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("deletedAt", new TableInfo.Column("deletedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsStops.put("syncedAt", new TableInfo.Column("syncedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStops = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStops = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStops = new TableInfo("stops", _columnsStops, _foreignKeysStops, _indicesStops);
        final TableInfo _existingStops = TableInfo.read(db, "stops");
        if (!_infoStops.equals(_existingStops)) {
          return new RoomOpenHelper.ValidationResult(false, "stops(com.pabl3st.rutapp.data.local.entity.StopEntity).\n"
                  + " Expected:\n" + _infoStops + "\n"
                  + " Found:\n" + _existingStops);
        }
        final HashMap<String, TableInfo.Column> _columnsSyncQueue = new HashMap<String, TableInfo.Column>(8);
        _columnsSyncQueue.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("entity", new TableInfo.Column("entity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("entityUid", new TableInfo.Column("entityUid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("operation", new TableInfo.Column("operation", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("payload", new TableInfo.Column("payload", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("attempts", new TableInfo.Column("attempts", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("lastError", new TableInfo.Column("lastError", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSyncQueue = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSyncQueue = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSyncQueue = new TableInfo("sync_queue", _columnsSyncQueue, _foreignKeysSyncQueue, _indicesSyncQueue);
        final TableInfo _existingSyncQueue = TableInfo.read(db, "sync_queue");
        if (!_infoSyncQueue.equals(_existingSyncQueue)) {
          return new RoomOpenHelper.ValidationResult(false, "sync_queue(com.pabl3st.rutapp.data.local.entity.SyncQueueEntity).\n"
                  + " Expected:\n" + _infoSyncQueue + "\n"
                  + " Found:\n" + _existingSyncQueue);
        }
        final HashMap<String, TableInfo.Column> _columnsDaySessions = new HashMap<String, TableInfo.Column>(10);
        _columnsDaySessions.put("routeUid", new TableInfo.Column("routeUid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDaySessions.put("dateStr", new TableInfo.Column("dateStr", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDaySessions.put("state", new TableInfo.Column("state", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDaySessions.put("startedAt", new TableInfo.Column("startedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDaySessions.put("pausedAt", new TableInfo.Column("pausedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDaySessions.put("elapsedMs", new TableInfo.Column("elapsedMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDaySessions.put("distanceKm", new TableInfo.Column("distanceKm", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDaySessions.put("lastLat", new TableInfo.Column("lastLat", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDaySessions.put("lastLng", new TableInfo.Column("lastLng", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDaySessions.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDaySessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDaySessions = new HashSet<TableInfo.Index>(1);
        _indicesDaySessions.add(new TableInfo.Index("index_day_sessions_dateStr", false, Arrays.asList("dateStr"), Arrays.asList("ASC")));
        final TableInfo _infoDaySessions = new TableInfo("day_sessions", _columnsDaySessions, _foreignKeysDaySessions, _indicesDaySessions);
        final TableInfo _existingDaySessions = TableInfo.read(db, "day_sessions");
        if (!_infoDaySessions.equals(_existingDaySessions)) {
          return new RoomOpenHelper.ValidationResult(false, "day_sessions(com.pabl3st.rutapp.data.local.entity.DaySessionEntity).\n"
                  + " Expected:\n" + _infoDaySessions + "\n"
                  + " Found:\n" + _existingDaySessions);
        }
        final HashMap<String, TableInfo.Column> _columnsKpiDefinitions = new HashMap<String, TableInfo.Column>(12);
        _columnsKpiDefinitions.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiDefinitions.put("accountId", new TableInfo.Column("accountId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiDefinitions.put("sector", new TableInfo.Column("sector", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiDefinitions.put("label", new TableInfo.Column("label", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiDefinitions.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiDefinitions.put("unit", new TableInfo.Column("unit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiDefinitions.put("options", new TableInfo.Column("options", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiDefinitions.put("required", new TableInfo.Column("required", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiDefinitions.put("visible", new TableInfo.Column("visible", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiDefinitions.put("orderIndex", new TableInfo.Column("orderIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiDefinitions.put("section", new TableInfo.Column("section", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiDefinitions.put("isSystem", new TableInfo.Column("isSystem", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysKpiDefinitions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesKpiDefinitions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoKpiDefinitions = new TableInfo("kpi_definitions", _columnsKpiDefinitions, _foreignKeysKpiDefinitions, _indicesKpiDefinitions);
        final TableInfo _existingKpiDefinitions = TableInfo.read(db, "kpi_definitions");
        if (!_infoKpiDefinitions.equals(_existingKpiDefinitions)) {
          return new RoomOpenHelper.ValidationResult(false, "kpi_definitions(com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity).\n"
                  + " Expected:\n" + _infoKpiDefinitions + "\n"
                  + " Found:\n" + _existingKpiDefinitions);
        }
        final HashMap<String, TableInfo.Column> _columnsBusinessProfiles = new HashMap<String, TableInfo.Column>(4);
        _columnsBusinessProfiles.put("accountId", new TableInfo.Column("accountId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBusinessProfiles.put("sector", new TableInfo.Column("sector", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBusinessProfiles.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBusinessProfiles.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBusinessProfiles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBusinessProfiles = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBusinessProfiles = new TableInfo("business_profiles", _columnsBusinessProfiles, _foreignKeysBusinessProfiles, _indicesBusinessProfiles);
        final TableInfo _existingBusinessProfiles = TableInfo.read(db, "business_profiles");
        if (!_infoBusinessProfiles.equals(_existingBusinessProfiles)) {
          return new RoomOpenHelper.ValidationResult(false, "business_profiles(com.pabl3st.rutapp.data.local.entity.BusinessProfileEntity).\n"
                  + " Expected:\n" + _infoBusinessProfiles + "\n"
                  + " Found:\n" + _existingBusinessProfiles);
        }
        final HashMap<String, TableInfo.Column> _columnsKpiValues = new HashMap<String, TableInfo.Column>(4);
        _columnsKpiValues.put("stopUid", new TableInfo.Column("stopUid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiValues.put("kpiId", new TableInfo.Column("kpiId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiValues.put("valueText", new TableInfo.Column("valueText", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKpiValues.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysKpiValues = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesKpiValues = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoKpiValues = new TableInfo("kpi_values", _columnsKpiValues, _foreignKeysKpiValues, _indicesKpiValues);
        final TableInfo _existingKpiValues = TableInfo.read(db, "kpi_values");
        if (!_infoKpiValues.equals(_existingKpiValues)) {
          return new RoomOpenHelper.ValidationResult(false, "kpi_values(com.pabl3st.rutapp.data.local.entity.KpiValueEntity).\n"
                  + " Expected:\n" + _infoKpiValues + "\n"
                  + " Found:\n" + _existingKpiValues);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "f8e68ca0072dd8f687315846dbbe6205", "53d9d6e587bba444231c43a5d91fb492");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "routes","stops","sync_queue","day_sessions","kpi_definitions","business_profiles","kpi_values");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `routes`");
      _db.execSQL("DELETE FROM `stops`");
      _db.execSQL("DELETE FROM `sync_queue`");
      _db.execSQL("DELETE FROM `day_sessions`");
      _db.execSQL("DELETE FROM `kpi_definitions`");
      _db.execSQL("DELETE FROM `business_profiles`");
      _db.execSQL("DELETE FROM `kpi_values`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(RouteDao.class, RouteDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(StopDao.class, StopDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SyncQueueDao.class, SyncQueueDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DaySessionDao.class, DaySessionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(KpiDefinitionDao.class, KpiDefinitionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BusinessProfileDao.class, BusinessProfileDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(KpiValueDao.class, KpiValueDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public RouteDao routeDao() {
    if (_routeDao != null) {
      return _routeDao;
    } else {
      synchronized(this) {
        if(_routeDao == null) {
          _routeDao = new RouteDao_Impl(this);
        }
        return _routeDao;
      }
    }
  }

  @Override
  public StopDao stopDao() {
    if (_stopDao != null) {
      return _stopDao;
    } else {
      synchronized(this) {
        if(_stopDao == null) {
          _stopDao = new StopDao_Impl(this);
        }
        return _stopDao;
      }
    }
  }

  @Override
  public SyncQueueDao syncQueueDao() {
    if (_syncQueueDao != null) {
      return _syncQueueDao;
    } else {
      synchronized(this) {
        if(_syncQueueDao == null) {
          _syncQueueDao = new SyncQueueDao_Impl(this);
        }
        return _syncQueueDao;
      }
    }
  }

  @Override
  public DaySessionDao daySessionDao() {
    if (_daySessionDao != null) {
      return _daySessionDao;
    } else {
      synchronized(this) {
        if(_daySessionDao == null) {
          _daySessionDao = new DaySessionDao_Impl(this);
        }
        return _daySessionDao;
      }
    }
  }

  @Override
  public KpiDefinitionDao kpiDefinitionDao() {
    if (_kpiDefinitionDao != null) {
      return _kpiDefinitionDao;
    } else {
      synchronized(this) {
        if(_kpiDefinitionDao == null) {
          _kpiDefinitionDao = new KpiDefinitionDao_Impl(this);
        }
        return _kpiDefinitionDao;
      }
    }
  }

  @Override
  public BusinessProfileDao businessProfileDao() {
    if (_businessProfileDao != null) {
      return _businessProfileDao;
    } else {
      synchronized(this) {
        if(_businessProfileDao == null) {
          _businessProfileDao = new BusinessProfileDao_Impl(this);
        }
        return _businessProfileDao;
      }
    }
  }

  @Override
  public KpiValueDao kpiValueDao() {
    if (_kpiValueDao != null) {
      return _kpiValueDao;
    } else {
      synchronized(this) {
        if(_kpiValueDao == null) {
          _kpiValueDao = new KpiValueDao_Impl(this);
        }
        return _kpiValueDao;
      }
    }
  }
}
