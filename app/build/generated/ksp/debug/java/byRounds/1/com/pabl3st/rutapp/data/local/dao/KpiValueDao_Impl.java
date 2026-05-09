package com.pabl3st.rutapp.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
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
import com.pabl3st.rutapp.data.local.entity.KpiValueEntity;
import java.lang.Class;
import java.lang.Exception;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class KpiValueDao_Impl implements KpiValueDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByStop;

  private final EntityUpsertionAdapter<KpiValueEntity> __upsertionAdapterOfKpiValueEntity;

  public KpiValueDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE kpi_values SET syncStatus = 'synced' WHERE stopUid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByStop = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM kpi_values WHERE stopUid = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfKpiValueEntity = new EntityUpsertionAdapter<KpiValueEntity>(new EntityInsertionAdapter<KpiValueEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `kpi_values` (`stopUid`,`kpiId`,`valueText`,`syncStatus`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final KpiValueEntity entity) {
        statement.bindString(1, entity.getStopUid());
        statement.bindString(2, entity.getKpiId());
        statement.bindString(3, entity.getValueText());
        statement.bindString(4, entity.getSyncStatus());
      }
    }, new EntityDeletionOrUpdateAdapter<KpiValueEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `kpi_values` SET `stopUid` = ?,`kpiId` = ?,`valueText` = ?,`syncStatus` = ? WHERE `stopUid` = ? AND `kpiId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final KpiValueEntity entity) {
        statement.bindString(1, entity.getStopUid());
        statement.bindString(2, entity.getKpiId());
        statement.bindString(3, entity.getValueText());
        statement.bindString(4, entity.getSyncStatus());
        statement.bindString(5, entity.getStopUid());
        statement.bindString(6, entity.getKpiId());
      }
    });
  }

  @Override
  public Object markSynced(final String stopUid, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, stopUid);
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
          __preparedStmtOfMarkSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByStop(final String stopUid, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByStop.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, stopUid);
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
          __preparedStmtOfDeleteByStop.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<KpiValueEntity> values,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfKpiValueEntity.upsert(values);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final KpiValueEntity value, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfKpiValueEntity.upsert(value);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByStop(final String stopUid,
      final Continuation<? super List<KpiValueEntity>> $completion) {
    final String _sql = "SELECT * FROM kpi_values WHERE stopUid = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, stopUid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<KpiValueEntity>>() {
      @Override
      @NonNull
      public List<KpiValueEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfStopUid = CursorUtil.getColumnIndexOrThrow(_cursor, "stopUid");
          final int _cursorIndexOfKpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "kpiId");
          final int _cursorIndexOfValueText = CursorUtil.getColumnIndexOrThrow(_cursor, "valueText");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final List<KpiValueEntity> _result = new ArrayList<KpiValueEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KpiValueEntity _item;
            final String _tmpStopUid;
            _tmpStopUid = _cursor.getString(_cursorIndexOfStopUid);
            final String _tmpKpiId;
            _tmpKpiId = _cursor.getString(_cursorIndexOfKpiId);
            final String _tmpValueText;
            _tmpValueText = _cursor.getString(_cursorIndexOfValueText);
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new KpiValueEntity(_tmpStopUid,_tmpKpiId,_tmpValueText,_tmpSyncStatus);
            _result.add(_item);
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
  public Object getPendingSync(final Continuation<? super List<KpiValueEntity>> $completion) {
    final String _sql = "SELECT * FROM kpi_values WHERE syncStatus = 'pending'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<KpiValueEntity>>() {
      @Override
      @NonNull
      public List<KpiValueEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfStopUid = CursorUtil.getColumnIndexOrThrow(_cursor, "stopUid");
          final int _cursorIndexOfKpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "kpiId");
          final int _cursorIndexOfValueText = CursorUtil.getColumnIndexOrThrow(_cursor, "valueText");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final List<KpiValueEntity> _result = new ArrayList<KpiValueEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KpiValueEntity _item;
            final String _tmpStopUid;
            _tmpStopUid = _cursor.getString(_cursorIndexOfStopUid);
            final String _tmpKpiId;
            _tmpKpiId = _cursor.getString(_cursorIndexOfKpiId);
            final String _tmpValueText;
            _tmpValueText = _cursor.getString(_cursorIndexOfValueText);
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            _item = new KpiValueEntity(_tmpStopUid,_tmpKpiId,_tmpValueText,_tmpSyncStatus);
            _result.add(_item);
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
