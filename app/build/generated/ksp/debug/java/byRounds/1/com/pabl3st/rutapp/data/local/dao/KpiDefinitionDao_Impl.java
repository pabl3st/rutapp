package com.pabl3st.rutapp.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pabl3st.rutapp.data.local.entity.KpiDefinitionEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class KpiDefinitionDao_Impl implements KpiDefinitionDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfDeleteCustom;

  private final SharedSQLiteStatement __preparedStmtOfSetVisible;

  private final SharedSQLiteStatement __preparedStmtOfSetOrder;

  private final EntityUpsertionAdapter<KpiDefinitionEntity> __upsertionAdapterOfKpiDefinitionEntity;

  public KpiDefinitionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfDeleteCustom = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM kpi_definitions WHERE id = ? AND isSystem = 0";
        return _query;
      }
    };
    this.__preparedStmtOfSetVisible = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE kpi_definitions SET visible = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetOrder = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE kpi_definitions SET orderIndex = ? WHERE id = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfKpiDefinitionEntity = new EntityUpsertionAdapter<KpiDefinitionEntity>(new EntityInsertionAdapter<KpiDefinitionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `kpi_definitions` (`id`,`accountId`,`sector`,`label`,`type`,`unit`,`options`,`required`,`visible`,`orderIndex`,`section`,`isSystem`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final KpiDefinitionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getAccountId());
        statement.bindString(3, entity.getSector());
        statement.bindString(4, entity.getLabel());
        statement.bindString(5, entity.getType());
        if (entity.getUnit() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getUnit());
        }
        if (entity.getOptions() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getOptions());
        }
        final int _tmp = entity.getRequired() ? 1 : 0;
        statement.bindLong(8, _tmp);
        final int _tmp_1 = entity.getVisible() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        statement.bindLong(10, entity.getOrderIndex());
        statement.bindString(11, entity.getSection());
        final int _tmp_2 = entity.isSystem() ? 1 : 0;
        statement.bindLong(12, _tmp_2);
      }
    }, new EntityDeletionOrUpdateAdapter<KpiDefinitionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `kpi_definitions` SET `id` = ?,`accountId` = ?,`sector` = ?,`label` = ?,`type` = ?,`unit` = ?,`options` = ?,`required` = ?,`visible` = ?,`orderIndex` = ?,`section` = ?,`isSystem` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final KpiDefinitionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getAccountId());
        statement.bindString(3, entity.getSector());
        statement.bindString(4, entity.getLabel());
        statement.bindString(5, entity.getType());
        if (entity.getUnit() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getUnit());
        }
        if (entity.getOptions() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getOptions());
        }
        final int _tmp = entity.getRequired() ? 1 : 0;
        statement.bindLong(8, _tmp);
        final int _tmp_1 = entity.getVisible() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        statement.bindLong(10, entity.getOrderIndex());
        statement.bindString(11, entity.getSection());
        final int _tmp_2 = entity.isSystem() ? 1 : 0;
        statement.bindLong(12, _tmp_2);
        statement.bindString(13, entity.getId());
      }
    });
  }

  @Override
  public Object deleteCustom(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCustom.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteCustom.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setVisible(final String id, final boolean visible,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetVisible.acquire();
        int _argIndex = 1;
        final int _tmp = visible ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetVisible.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setOrder(final String id, final int order,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetOrder.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, order);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetOrder.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final KpiDefinitionEntity kpi,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfKpiDefinitionEntity.upsert(kpi);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<KpiDefinitionEntity> kpis,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfKpiDefinitionEntity.upsert(kpis);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<KpiDefinitionEntity>> observeActive(final int accountId, final String sector) {
    final String _sql = "\n"
            + "        SELECT * FROM kpi_definitions\n"
            + "        WHERE (accountId = 0 OR accountId = ?)\n"
            + "          AND (sector = ? OR sector = 'common')\n"
            + "          AND visible = 1\n"
            + "        ORDER BY orderIndex ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindString(_argIndex, sector);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"kpi_definitions"}, new Callable<List<KpiDefinitionEntity>>() {
      @Override
      @NonNull
      public List<KpiDefinitionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfSector = CursorUtil.getColumnIndexOrThrow(_cursor, "sector");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "options");
          final int _cursorIndexOfRequired = CursorUtil.getColumnIndexOrThrow(_cursor, "required");
          final int _cursorIndexOfVisible = CursorUtil.getColumnIndexOrThrow(_cursor, "visible");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final int _cursorIndexOfSection = CursorUtil.getColumnIndexOrThrow(_cursor, "section");
          final int _cursorIndexOfIsSystem = CursorUtil.getColumnIndexOrThrow(_cursor, "isSystem");
          final List<KpiDefinitionEntity> _result = new ArrayList<KpiDefinitionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KpiDefinitionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpSector;
            _tmpSector = _cursor.getString(_cursorIndexOfSector);
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final String _tmpOptions;
            if (_cursor.isNull(_cursorIndexOfOptions)) {
              _tmpOptions = null;
            } else {
              _tmpOptions = _cursor.getString(_cursorIndexOfOptions);
            }
            final boolean _tmpRequired;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRequired);
            _tmpRequired = _tmp != 0;
            final boolean _tmpVisible;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfVisible);
            _tmpVisible = _tmp_1 != 0;
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final String _tmpSection;
            _tmpSection = _cursor.getString(_cursorIndexOfSection);
            final boolean _tmpIsSystem;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsSystem);
            _tmpIsSystem = _tmp_2 != 0;
            _item = new KpiDefinitionEntity(_tmpId,_tmpAccountId,_tmpSector,_tmpLabel,_tmpType,_tmpUnit,_tmpOptions,_tmpRequired,_tmpVisible,_tmpOrderIndex,_tmpSection,_tmpIsSystem);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<KpiDefinitionEntity>> observeAll(final int accountId, final String sector) {
    final String _sql = "\n"
            + "        SELECT * FROM kpi_definitions\n"
            + "        WHERE (accountId = 0 OR accountId = ?)\n"
            + "          AND (sector = ? OR sector = 'common')\n"
            + "        ORDER BY section ASC, orderIndex ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindString(_argIndex, sector);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"kpi_definitions"}, new Callable<List<KpiDefinitionEntity>>() {
      @Override
      @NonNull
      public List<KpiDefinitionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfSector = CursorUtil.getColumnIndexOrThrow(_cursor, "sector");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "options");
          final int _cursorIndexOfRequired = CursorUtil.getColumnIndexOrThrow(_cursor, "required");
          final int _cursorIndexOfVisible = CursorUtil.getColumnIndexOrThrow(_cursor, "visible");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final int _cursorIndexOfSection = CursorUtil.getColumnIndexOrThrow(_cursor, "section");
          final int _cursorIndexOfIsSystem = CursorUtil.getColumnIndexOrThrow(_cursor, "isSystem");
          final List<KpiDefinitionEntity> _result = new ArrayList<KpiDefinitionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KpiDefinitionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpSector;
            _tmpSector = _cursor.getString(_cursorIndexOfSector);
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final String _tmpOptions;
            if (_cursor.isNull(_cursorIndexOfOptions)) {
              _tmpOptions = null;
            } else {
              _tmpOptions = _cursor.getString(_cursorIndexOfOptions);
            }
            final boolean _tmpRequired;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRequired);
            _tmpRequired = _tmp != 0;
            final boolean _tmpVisible;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfVisible);
            _tmpVisible = _tmp_1 != 0;
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final String _tmpSection;
            _tmpSection = _cursor.getString(_cursorIndexOfSection);
            final boolean _tmpIsSystem;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsSystem);
            _tmpIsSystem = _tmp_2 != 0;
            _item = new KpiDefinitionEntity(_tmpId,_tmpAccountId,_tmpSector,_tmpLabel,_tmpType,_tmpUnit,_tmpOptions,_tmpRequired,_tmpVisible,_tmpOrderIndex,_tmpSection,_tmpIsSystem);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final String id,
      final Continuation<? super KpiDefinitionEntity> $completion) {
    final String _sql = "SELECT * FROM kpi_definitions WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<KpiDefinitionEntity>() {
      @Override
      @Nullable
      public KpiDefinitionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfSector = CursorUtil.getColumnIndexOrThrow(_cursor, "sector");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "options");
          final int _cursorIndexOfRequired = CursorUtil.getColumnIndexOrThrow(_cursor, "required");
          final int _cursorIndexOfVisible = CursorUtil.getColumnIndexOrThrow(_cursor, "visible");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final int _cursorIndexOfSection = CursorUtil.getColumnIndexOrThrow(_cursor, "section");
          final int _cursorIndexOfIsSystem = CursorUtil.getColumnIndexOrThrow(_cursor, "isSystem");
          final KpiDefinitionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpSector;
            _tmpSector = _cursor.getString(_cursorIndexOfSector);
            final String _tmpLabel;
            _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final String _tmpOptions;
            if (_cursor.isNull(_cursorIndexOfOptions)) {
              _tmpOptions = null;
            } else {
              _tmpOptions = _cursor.getString(_cursorIndexOfOptions);
            }
            final boolean _tmpRequired;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRequired);
            _tmpRequired = _tmp != 0;
            final boolean _tmpVisible;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfVisible);
            _tmpVisible = _tmp_1 != 0;
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final String _tmpSection;
            _tmpSection = _cursor.getString(_cursorIndexOfSection);
            final boolean _tmpIsSystem;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsSystem);
            _tmpIsSystem = _tmp_2 != 0;
            _result = new KpiDefinitionEntity(_tmpId,_tmpAccountId,_tmpSector,_tmpLabel,_tmpType,_tmpUnit,_tmpOptions,_tmpRequired,_tmpVisible,_tmpOrderIndex,_tmpSection,_tmpIsSystem);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object countSystem(final String sector, final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM kpi_definitions WHERE accountId = 0 AND sector = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sector);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
