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
import com.pabl3st.rutapp.data.local.entity.DaySessionEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DaySessionDao_Impl implements DaySessionDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfUpdateState;

  private final SharedSQLiteStatement __preparedStmtOfUpdateDistance;

  private final EntityUpsertionAdapter<DaySessionEntity> __upsertionAdapterOfDaySessionEntity;

  public DaySessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfUpdateState = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE day_sessions SET state = ?, pausedAt = ?, elapsedMs = ?, updatedAt = ? WHERE routeUid = ? AND dateStr = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateDistance = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE day_sessions SET distanceKm = ?, lastLat = ?, lastLng = ?, updatedAt = ? WHERE routeUid = ? AND dateStr = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfDaySessionEntity = new EntityUpsertionAdapter<DaySessionEntity>(new EntityInsertionAdapter<DaySessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `day_sessions` (`routeUid`,`dateStr`,`state`,`startedAt`,`pausedAt`,`elapsedMs`,`distanceKm`,`lastLat`,`lastLng`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DaySessionEntity entity) {
        statement.bindString(1, entity.getRouteUid());
        statement.bindString(2, entity.getDateStr());
        statement.bindString(3, entity.getState());
        if (entity.getStartedAt() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getStartedAt());
        }
        if (entity.getPausedAt() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getPausedAt());
        }
        statement.bindLong(6, entity.getElapsedMs());
        statement.bindDouble(7, entity.getDistanceKm());
        if (entity.getLastLat() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getLastLat());
        }
        if (entity.getLastLng() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getLastLng());
        }
        statement.bindLong(10, entity.getUpdatedAt());
      }
    }, new EntityDeletionOrUpdateAdapter<DaySessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `day_sessions` SET `routeUid` = ?,`dateStr` = ?,`state` = ?,`startedAt` = ?,`pausedAt` = ?,`elapsedMs` = ?,`distanceKm` = ?,`lastLat` = ?,`lastLng` = ?,`updatedAt` = ? WHERE `routeUid` = ? AND `dateStr` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DaySessionEntity entity) {
        statement.bindString(1, entity.getRouteUid());
        statement.bindString(2, entity.getDateStr());
        statement.bindString(3, entity.getState());
        if (entity.getStartedAt() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getStartedAt());
        }
        if (entity.getPausedAt() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getPausedAt());
        }
        statement.bindLong(6, entity.getElapsedMs());
        statement.bindDouble(7, entity.getDistanceKm());
        if (entity.getLastLat() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getLastLat());
        }
        if (entity.getLastLng() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getLastLng());
        }
        statement.bindLong(10, entity.getUpdatedAt());
        statement.bindString(11, entity.getRouteUid());
        statement.bindString(12, entity.getDateStr());
      }
    });
  }

  @Override
  public Object updateState(final String routeUid, final String dateStr, final String state,
      final Long pausedAt, final long elapsedMs, final long now,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateState.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, state);
        _argIndex = 2;
        if (pausedAt == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, pausedAt);
        }
        _argIndex = 3;
        _stmt.bindLong(_argIndex, elapsedMs);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, now);
        _argIndex = 5;
        _stmt.bindString(_argIndex, routeUid);
        _argIndex = 6;
        _stmt.bindString(_argIndex, dateStr);
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
          __preparedStmtOfUpdateState.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDistance(final String routeUid, final String dateStr, final double km,
      final double lat, final double lng, final long now,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateDistance.acquire();
        int _argIndex = 1;
        _stmt.bindDouble(_argIndex, km);
        _argIndex = 2;
        _stmt.bindDouble(_argIndex, lat);
        _argIndex = 3;
        _stmt.bindDouble(_argIndex, lng);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, now);
        _argIndex = 5;
        _stmt.bindString(_argIndex, routeUid);
        _argIndex = 6;
        _stmt.bindString(_argIndex, dateStr);
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
          __preparedStmtOfUpdateDistance.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final DaySessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfDaySessionEntity.upsert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<DaySessionEntity> observe(final String routeUid, final String dateStr) {
    final String _sql = "SELECT * FROM day_sessions WHERE routeUid = ? AND dateStr = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, routeUid);
    _argIndex = 2;
    _statement.bindString(_argIndex, dateStr);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"day_sessions"}, new Callable<DaySessionEntity>() {
      @Override
      @Nullable
      public DaySessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRouteUid = CursorUtil.getColumnIndexOrThrow(_cursor, "routeUid");
          final int _cursorIndexOfDateStr = CursorUtil.getColumnIndexOrThrow(_cursor, "dateStr");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfPausedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedAt");
          final int _cursorIndexOfElapsedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "elapsedMs");
          final int _cursorIndexOfDistanceKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceKm");
          final int _cursorIndexOfLastLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lastLat");
          final int _cursorIndexOfLastLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lastLng");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final DaySessionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpRouteUid;
            _tmpRouteUid = _cursor.getString(_cursorIndexOfRouteUid);
            final String _tmpDateStr;
            _tmpDateStr = _cursor.getString(_cursorIndexOfDateStr);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final Long _tmpStartedAt;
            if (_cursor.isNull(_cursorIndexOfStartedAt)) {
              _tmpStartedAt = null;
            } else {
              _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            }
            final Long _tmpPausedAt;
            if (_cursor.isNull(_cursorIndexOfPausedAt)) {
              _tmpPausedAt = null;
            } else {
              _tmpPausedAt = _cursor.getLong(_cursorIndexOfPausedAt);
            }
            final long _tmpElapsedMs;
            _tmpElapsedMs = _cursor.getLong(_cursorIndexOfElapsedMs);
            final double _tmpDistanceKm;
            _tmpDistanceKm = _cursor.getDouble(_cursorIndexOfDistanceKm);
            final Double _tmpLastLat;
            if (_cursor.isNull(_cursorIndexOfLastLat)) {
              _tmpLastLat = null;
            } else {
              _tmpLastLat = _cursor.getDouble(_cursorIndexOfLastLat);
            }
            final Double _tmpLastLng;
            if (_cursor.isNull(_cursorIndexOfLastLng)) {
              _tmpLastLng = null;
            } else {
              _tmpLastLng = _cursor.getDouble(_cursorIndexOfLastLng);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new DaySessionEntity(_tmpRouteUid,_tmpDateStr,_tmpState,_tmpStartedAt,_tmpPausedAt,_tmpElapsedMs,_tmpDistanceKm,_tmpLastLat,_tmpLastLng,_tmpUpdatedAt);
          } else {
            _result = null;
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
  public Object get(final String routeUid, final String dateStr,
      final Continuation<? super DaySessionEntity> $completion) {
    final String _sql = "SELECT * FROM day_sessions WHERE routeUid = ? AND dateStr = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, routeUid);
    _argIndex = 2;
    _statement.bindString(_argIndex, dateStr);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DaySessionEntity>() {
      @Override
      @Nullable
      public DaySessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfRouteUid = CursorUtil.getColumnIndexOrThrow(_cursor, "routeUid");
          final int _cursorIndexOfDateStr = CursorUtil.getColumnIndexOrThrow(_cursor, "dateStr");
          final int _cursorIndexOfState = CursorUtil.getColumnIndexOrThrow(_cursor, "state");
          final int _cursorIndexOfStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "startedAt");
          final int _cursorIndexOfPausedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "pausedAt");
          final int _cursorIndexOfElapsedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "elapsedMs");
          final int _cursorIndexOfDistanceKm = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceKm");
          final int _cursorIndexOfLastLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lastLat");
          final int _cursorIndexOfLastLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lastLng");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final DaySessionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpRouteUid;
            _tmpRouteUid = _cursor.getString(_cursorIndexOfRouteUid);
            final String _tmpDateStr;
            _tmpDateStr = _cursor.getString(_cursorIndexOfDateStr);
            final String _tmpState;
            _tmpState = _cursor.getString(_cursorIndexOfState);
            final Long _tmpStartedAt;
            if (_cursor.isNull(_cursorIndexOfStartedAt)) {
              _tmpStartedAt = null;
            } else {
              _tmpStartedAt = _cursor.getLong(_cursorIndexOfStartedAt);
            }
            final Long _tmpPausedAt;
            if (_cursor.isNull(_cursorIndexOfPausedAt)) {
              _tmpPausedAt = null;
            } else {
              _tmpPausedAt = _cursor.getLong(_cursorIndexOfPausedAt);
            }
            final long _tmpElapsedMs;
            _tmpElapsedMs = _cursor.getLong(_cursorIndexOfElapsedMs);
            final double _tmpDistanceKm;
            _tmpDistanceKm = _cursor.getDouble(_cursorIndexOfDistanceKm);
            final Double _tmpLastLat;
            if (_cursor.isNull(_cursorIndexOfLastLat)) {
              _tmpLastLat = null;
            } else {
              _tmpLastLat = _cursor.getDouble(_cursorIndexOfLastLat);
            }
            final Double _tmpLastLng;
            if (_cursor.isNull(_cursorIndexOfLastLng)) {
              _tmpLastLng = null;
            } else {
              _tmpLastLng = _cursor.getDouble(_cursorIndexOfLastLng);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new DaySessionEntity(_tmpRouteUid,_tmpDateStr,_tmpState,_tmpStartedAt,_tmpPausedAt,_tmpElapsedMs,_tmpDistanceKm,_tmpLastLat,_tmpLastLng,_tmpUpdatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
