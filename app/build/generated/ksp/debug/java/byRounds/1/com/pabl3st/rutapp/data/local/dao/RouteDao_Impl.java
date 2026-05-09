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
import com.pabl3st.rutapp.data.local.entity.RouteEntity;
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
public final class RouteDao_Impl implements RouteDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSyncStatus;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByUid;

  private final EntityUpsertionAdapter<RouteEntity> __upsertionAdapterOfRouteEntity;

  public RouteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfUpdateSyncStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE routes SET syncStatus = ?, syncedAt = ? WHERE uid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByUid = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM routes WHERE uid = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfRouteEntity = new EntityUpsertionAdapter<RouteEntity>(new EntityInsertionAdapter<RouteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `routes` (`uid`,`serverId`,`accountId`,`userId`,`name`,`dateAssigned`,`status`,`notes`,`createdAt`,`updatedAt`,`deletedAt`,`syncStatus`,`syncedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RouteEntity entity) {
        statement.bindString(1, entity.getUid());
        if (entity.getServerId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getServerId());
        }
        statement.bindLong(3, entity.getAccountId());
        statement.bindLong(4, entity.getUserId());
        statement.bindString(5, entity.getName());
        statement.bindString(6, entity.getDateAssigned());
        statement.bindString(7, entity.getStatus());
        if (entity.getNotes() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getNotes());
        }
        statement.bindString(9, entity.getCreatedAt());
        statement.bindString(10, entity.getUpdatedAt());
        if (entity.getDeletedAt() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getDeletedAt());
        }
        statement.bindString(12, entity.getSyncStatus());
        if (entity.getSyncedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getSyncedAt());
        }
      }
    }, new EntityDeletionOrUpdateAdapter<RouteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `routes` SET `uid` = ?,`serverId` = ?,`accountId` = ?,`userId` = ?,`name` = ?,`dateAssigned` = ?,`status` = ?,`notes` = ?,`createdAt` = ?,`updatedAt` = ?,`deletedAt` = ?,`syncStatus` = ?,`syncedAt` = ? WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RouteEntity entity) {
        statement.bindString(1, entity.getUid());
        if (entity.getServerId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getServerId());
        }
        statement.bindLong(3, entity.getAccountId());
        statement.bindLong(4, entity.getUserId());
        statement.bindString(5, entity.getName());
        statement.bindString(6, entity.getDateAssigned());
        statement.bindString(7, entity.getStatus());
        if (entity.getNotes() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getNotes());
        }
        statement.bindString(9, entity.getCreatedAt());
        statement.bindString(10, entity.getUpdatedAt());
        if (entity.getDeletedAt() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getDeletedAt());
        }
        statement.bindString(12, entity.getSyncStatus());
        if (entity.getSyncedAt() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getSyncedAt());
        }
        statement.bindString(14, entity.getUid());
      }
    });
  }

  @Override
  public Object updateSyncStatus(final String uid, final String status, final String at,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSyncStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        if (at == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, at);
        }
        _argIndex = 3;
        _stmt.bindString(_argIndex, uid);
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
          __preparedStmtOfUpdateSyncStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByUid(final String uid, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByUid.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, uid);
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
          __preparedStmtOfDeleteByUid.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final RouteEntity route, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfRouteEntity.upsert(route);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<RouteEntity> routes,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfRouteEntity.upsert(routes);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RouteEntity>> observeByUser(final int userId) {
    final String _sql = "SELECT * FROM routes WHERE deletedAt IS NULL AND userId = ? ORDER BY dateAssigned DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"routes"}, new Callable<List<RouteEntity>>() {
      @Override
      @NonNull
      public List<RouteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDateAssigned = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAssigned");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<RouteEntity> _result = new ArrayList<RouteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RouteEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final int _tmpUserId;
            _tmpUserId = _cursor.getInt(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDateAssigned;
            _tmpDateAssigned = _cursor.getString(_cursorIndexOfDateAssigned);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getString(_cursorIndexOfSyncedAt);
            }
            _item = new RouteEntity(_tmpUid,_tmpServerId,_tmpAccountId,_tmpUserId,_tmpName,_tmpDateAssigned,_tmpStatus,_tmpNotes,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
  public Flow<List<RouteEntity>> observeByAccount(final int accountId) {
    final String _sql = "SELECT * FROM routes WHERE deletedAt IS NULL AND accountId = ? ORDER BY dateAssigned DESC, userId ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"routes"}, new Callable<List<RouteEntity>>() {
      @Override
      @NonNull
      public List<RouteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDateAssigned = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAssigned");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<RouteEntity> _result = new ArrayList<RouteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RouteEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final int _tmpUserId;
            _tmpUserId = _cursor.getInt(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDateAssigned;
            _tmpDateAssigned = _cursor.getString(_cursorIndexOfDateAssigned);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getString(_cursorIndexOfSyncedAt);
            }
            _item = new RouteEntity(_tmpUid,_tmpServerId,_tmpAccountId,_tmpUserId,_tmpName,_tmpDateAssigned,_tmpStatus,_tmpNotes,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
  public Flow<List<RouteEntity>> observeByDate(final int userId, final String date) {
    final String _sql = "SELECT * FROM routes WHERE deletedAt IS NULL AND userId = ? AND dateAssigned = ? ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    _argIndex = 2;
    _statement.bindString(_argIndex, date);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"routes"}, new Callable<List<RouteEntity>>() {
      @Override
      @NonNull
      public List<RouteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDateAssigned = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAssigned");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<RouteEntity> _result = new ArrayList<RouteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RouteEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final int _tmpUserId;
            _tmpUserId = _cursor.getInt(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDateAssigned;
            _tmpDateAssigned = _cursor.getString(_cursorIndexOfDateAssigned);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getString(_cursorIndexOfSyncedAt);
            }
            _item = new RouteEntity(_tmpUid,_tmpServerId,_tmpAccountId,_tmpUserId,_tmpName,_tmpDateAssigned,_tmpStatus,_tmpNotes,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
  public Flow<List<RouteEntity>> observeByDateForAccount(final int accountId, final String date) {
    final String _sql = "SELECT * FROM routes WHERE deletedAt IS NULL AND accountId = ? AND dateAssigned = ? ORDER BY userId ASC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindString(_argIndex, date);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"routes"}, new Callable<List<RouteEntity>>() {
      @Override
      @NonNull
      public List<RouteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDateAssigned = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAssigned");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<RouteEntity> _result = new ArrayList<RouteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RouteEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final int _tmpUserId;
            _tmpUserId = _cursor.getInt(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDateAssigned;
            _tmpDateAssigned = _cursor.getString(_cursorIndexOfDateAssigned);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getString(_cursorIndexOfSyncedAt);
            }
            _item = new RouteEntity(_tmpUid,_tmpServerId,_tmpAccountId,_tmpUserId,_tmpName,_tmpDateAssigned,_tmpStatus,_tmpNotes,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
  public Object getByUid(final String uid, final Continuation<? super RouteEntity> $completion) {
    final String _sql = "SELECT * FROM routes WHERE uid = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, uid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RouteEntity>() {
      @Override
      @Nullable
      public RouteEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDateAssigned = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAssigned");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final RouteEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final int _tmpUserId;
            _tmpUserId = _cursor.getInt(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDateAssigned;
            _tmpDateAssigned = _cursor.getString(_cursorIndexOfDateAssigned);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getString(_cursorIndexOfSyncedAt);
            }
            _result = new RouteEntity(_tmpUid,_tmpServerId,_tmpAccountId,_tmpUserId,_tmpName,_tmpDateAssigned,_tmpStatus,_tmpNotes,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
  public Object getPendingSync(final Continuation<? super List<RouteEntity>> $completion) {
    final String _sql = "SELECT * FROM routes WHERE syncStatus = 'pending' OR syncStatus = 'error'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RouteEntity>>() {
      @Override
      @NonNull
      public List<RouteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDateAssigned = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAssigned");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<RouteEntity> _result = new ArrayList<RouteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RouteEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final int _tmpUserId;
            _tmpUserId = _cursor.getInt(_cursorIndexOfUserId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDateAssigned;
            _tmpDateAssigned = _cursor.getString(_cursorIndexOfDateAssigned);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getString(_cursorIndexOfDeletedAt);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getString(_cursorIndexOfSyncedAt);
            }
            _item = new RouteEntity(_tmpUid,_tmpServerId,_tmpAccountId,_tmpUserId,_tmpName,_tmpDateAssigned,_tmpStatus,_tmpNotes,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
