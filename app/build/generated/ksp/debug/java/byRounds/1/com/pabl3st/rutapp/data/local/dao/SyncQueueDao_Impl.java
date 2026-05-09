package com.pabl3st.rutapp.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pabl3st.rutapp.data.local.entity.SyncQueueEntity;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SyncQueueDao_Impl implements SyncQueueDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SyncQueueEntity> __insertionAdapterOfSyncQueueEntity;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  private final SharedSQLiteStatement __preparedStmtOfMarkFailed;

  public SyncQueueDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSyncQueueEntity = new EntityInsertionAdapter<SyncQueueEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sync_queue` (`id`,`entity`,`entityUid`,`operation`,`payload`,`createdAt`,`attempts`,`lastError`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SyncQueueEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getEntity());
        statement.bindString(3, entity.getEntityUid());
        statement.bindString(4, entity.getOperation());
        statement.bindString(5, entity.getPayload());
        statement.bindLong(6, entity.getCreatedAt());
        statement.bindLong(7, entity.getAttempts());
        if (entity.getLastError() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getLastError());
        }
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sync_queue WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkFailed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sync_queue SET attempts = attempts + 1, lastError = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object enqueue(final SyncQueueEntity item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSyncQueueEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final int id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markFailed(final int id, final String error,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkFailed.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, error);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfMarkFailed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getNext50(final Continuation<? super List<SyncQueueEntity>> $completion) {
    final String _sql = "SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT 50";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SyncQueueEntity>>() {
      @Override
      @NonNull
      public List<SyncQueueEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEntity = CursorUtil.getColumnIndexOrThrow(_cursor, "entity");
          final int _cursorIndexOfEntityUid = CursorUtil.getColumnIndexOrThrow(_cursor, "entityUid");
          final int _cursorIndexOfOperation = CursorUtil.getColumnIndexOrThrow(_cursor, "operation");
          final int _cursorIndexOfPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "payload");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "attempts");
          final int _cursorIndexOfLastError = CursorUtil.getColumnIndexOrThrow(_cursor, "lastError");
          final List<SyncQueueEntity> _result = new ArrayList<SyncQueueEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SyncQueueEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpEntity;
            _tmpEntity = _cursor.getString(_cursorIndexOfEntity);
            final String _tmpEntityUid;
            _tmpEntityUid = _cursor.getString(_cursorIndexOfEntityUid);
            final String _tmpOperation;
            _tmpOperation = _cursor.getString(_cursorIndexOfOperation);
            final String _tmpPayload;
            _tmpPayload = _cursor.getString(_cursorIndexOfPayload);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final int _tmpAttempts;
            _tmpAttempts = _cursor.getInt(_cursorIndexOfAttempts);
            final String _tmpLastError;
            if (_cursor.isNull(_cursorIndexOfLastError)) {
              _tmpLastError = null;
            } else {
              _tmpLastError = _cursor.getString(_cursorIndexOfLastError);
            }
            _item = new SyncQueueEntity(_tmpId,_tmpEntity,_tmpEntityUid,_tmpOperation,_tmpPayload,_tmpCreatedAt,_tmpAttempts,_tmpLastError);
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
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM sync_queue";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
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
