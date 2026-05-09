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
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pabl3st.rutapp.data.local.entity.StopEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
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
public final class StopDao_Impl implements StopDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSyncStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStatus;

  private final SharedSQLiteStatement __preparedStmtOfMarkVisiting;

  private final SharedSQLiteStatement __preparedStmtOfUpdateCoords;

  private final SharedSQLiteStatement __preparedStmtOfUpdateVisitResult;

  private final SharedSQLiteStatement __preparedStmtOfUpdateOrderIndex;

  private final EntityUpsertionAdapter<StopEntity> __upsertionAdapterOfStopEntity;

  public StopDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfUpdateSyncStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE stops SET syncStatus = ?, syncedAt = ? WHERE uid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE stops SET status = ?, visitedAt = ?, updatedAt = ?, syncStatus = 'pending' WHERE uid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkVisiting = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE stops SET status = 'visiting', syncStatus = 'pending' WHERE uid = ? AND status = 'pending'";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateCoords = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE stops SET lat = ?, lng = ?, updatedAt = ?, syncStatus = 'pending' WHERE uid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateVisitResult = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE stops SET status = 'done', visitedAt = ?, visitResult = ?,\n"
                + "        notes = ?, nextAction = ?, updatedAt = ?, syncStatus = 'pending'\n"
                + "        WHERE uid = ?\n"
                + "    ";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateOrderIndex = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE stops SET orderIndex = ?, updatedAt = ?, syncStatus = 'pending' WHERE uid = ?";
        return _query;
      }
    };
    this.__upsertionAdapterOfStopEntity = new EntityUpsertionAdapter<StopEntity>(new EntityInsertionAdapter<StopEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `stops` (`uid`,`serverId`,`routeUid`,`accountId`,`name`,`externalId`,`address`,`lat`,`lng`,`orderIndex`,`contactName`,`contactPhone`,`visitFrequency`,`priority`,`segment`,`accountStatus`,`openingHours`,`status`,`notes`,`visitedAt`,`visitResult`,`nextAction`,`createdAt`,`updatedAt`,`deletedAt`,`syncStatus`,`syncedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StopEntity entity) {
        statement.bindString(1, entity.getUid());
        if (entity.getServerId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getServerId());
        }
        statement.bindString(3, entity.getRouteUid());
        statement.bindLong(4, entity.getAccountId());
        statement.bindString(5, entity.getName());
        if (entity.getExternalId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getExternalId());
        }
        if (entity.getAddress() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getAddress());
        }
        if (entity.getLat() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getLat());
        }
        if (entity.getLng() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getLng());
        }
        statement.bindLong(10, entity.getOrderIndex());
        if (entity.getContactName() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getContactName());
        }
        if (entity.getContactPhone() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getContactPhone());
        }
        if (entity.getVisitFrequency() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getVisitFrequency());
        }
        statement.bindLong(14, entity.getPriority());
        if (entity.getSegment() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getSegment());
        }
        statement.bindString(16, entity.getAccountStatus());
        if (entity.getOpeningHours() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getOpeningHours());
        }
        statement.bindString(18, entity.getStatus());
        if (entity.getNotes() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getNotes());
        }
        if (entity.getVisitedAt() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getVisitedAt());
        }
        if (entity.getVisitResult() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getVisitResult());
        }
        if (entity.getNextAction() == null) {
          statement.bindNull(22);
        } else {
          statement.bindString(22, entity.getNextAction());
        }
        statement.bindString(23, entity.getCreatedAt());
        statement.bindString(24, entity.getUpdatedAt());
        if (entity.getDeletedAt() == null) {
          statement.bindNull(25);
        } else {
          statement.bindString(25, entity.getDeletedAt());
        }
        statement.bindString(26, entity.getSyncStatus());
        if (entity.getSyncedAt() == null) {
          statement.bindNull(27);
        } else {
          statement.bindString(27, entity.getSyncedAt());
        }
      }
    }, new EntityDeletionOrUpdateAdapter<StopEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `stops` SET `uid` = ?,`serverId` = ?,`routeUid` = ?,`accountId` = ?,`name` = ?,`externalId` = ?,`address` = ?,`lat` = ?,`lng` = ?,`orderIndex` = ?,`contactName` = ?,`contactPhone` = ?,`visitFrequency` = ?,`priority` = ?,`segment` = ?,`accountStatus` = ?,`openingHours` = ?,`status` = ?,`notes` = ?,`visitedAt` = ?,`visitResult` = ?,`nextAction` = ?,`createdAt` = ?,`updatedAt` = ?,`deletedAt` = ?,`syncStatus` = ?,`syncedAt` = ? WHERE `uid` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final StopEntity entity) {
        statement.bindString(1, entity.getUid());
        if (entity.getServerId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getServerId());
        }
        statement.bindString(3, entity.getRouteUid());
        statement.bindLong(4, entity.getAccountId());
        statement.bindString(5, entity.getName());
        if (entity.getExternalId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getExternalId());
        }
        if (entity.getAddress() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getAddress());
        }
        if (entity.getLat() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getLat());
        }
        if (entity.getLng() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getLng());
        }
        statement.bindLong(10, entity.getOrderIndex());
        if (entity.getContactName() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getContactName());
        }
        if (entity.getContactPhone() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getContactPhone());
        }
        if (entity.getVisitFrequency() == null) {
          statement.bindNull(13);
        } else {
          statement.bindLong(13, entity.getVisitFrequency());
        }
        statement.bindLong(14, entity.getPriority());
        if (entity.getSegment() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getSegment());
        }
        statement.bindString(16, entity.getAccountStatus());
        if (entity.getOpeningHours() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getOpeningHours());
        }
        statement.bindString(18, entity.getStatus());
        if (entity.getNotes() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getNotes());
        }
        if (entity.getVisitedAt() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getVisitedAt());
        }
        if (entity.getVisitResult() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getVisitResult());
        }
        if (entity.getNextAction() == null) {
          statement.bindNull(22);
        } else {
          statement.bindString(22, entity.getNextAction());
        }
        statement.bindString(23, entity.getCreatedAt());
        statement.bindString(24, entity.getUpdatedAt());
        if (entity.getDeletedAt() == null) {
          statement.bindNull(25);
        } else {
          statement.bindString(25, entity.getDeletedAt());
        }
        statement.bindString(26, entity.getSyncStatus());
        if (entity.getSyncedAt() == null) {
          statement.bindNull(27);
        } else {
          statement.bindString(27, entity.getSyncedAt());
        }
        statement.bindString(28, entity.getUid());
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
  public Object updateStatus(final String uid, final String status, final String at,
      final String updatedAt, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        if (at == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, at);
        }
        _argIndex = 3;
        _stmt.bindString(_argIndex, updatedAt);
        _argIndex = 4;
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
          __preparedStmtOfUpdateStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markVisiting(final String uid, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkVisiting.acquire();
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
          __preparedStmtOfMarkVisiting.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateCoords(final String uid, final double lat, final double lng, final String at,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateCoords.acquire();
        int _argIndex = 1;
        _stmt.bindDouble(_argIndex, lat);
        _argIndex = 2;
        _stmt.bindDouble(_argIndex, lng);
        _argIndex = 3;
        _stmt.bindString(_argIndex, at);
        _argIndex = 4;
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
          __preparedStmtOfUpdateCoords.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateVisitResult(final String uid, final String result, final String notes,
      final String nextAction, final String at, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateVisitResult.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, at);
        _argIndex = 2;
        _stmt.bindString(_argIndex, result);
        _argIndex = 3;
        if (notes == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, notes);
        }
        _argIndex = 4;
        if (nextAction == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, nextAction);
        }
        _argIndex = 5;
        _stmt.bindString(_argIndex, at);
        _argIndex = 6;
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
          __preparedStmtOfUpdateVisitResult.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateOrderIndex(final String uid, final int orderIndex, final String at,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateOrderIndex.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, orderIndex);
        _argIndex = 2;
        _stmt.bindString(_argIndex, at);
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
          __preparedStmtOfUpdateOrderIndex.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final StopEntity stop, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfStopEntity.upsert(stop);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertAll(final List<StopEntity> stops,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfStopEntity.upsert(stops);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<StopEntity>> observeByRoute(final String routeUid) {
    final String _sql = "SELECT * FROM stops WHERE routeUid = ? AND deletedAt IS NULL ORDER BY orderIndex ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, routeUid);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"stops"}, new Callable<List<StopEntity>>() {
      @Override
      @NonNull
      public List<StopEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfRouteUid = CursorUtil.getColumnIndexOrThrow(_cursor, "routeUid");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfExternalId = CursorUtil.getColumnIndexOrThrow(_cursor, "externalId");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfContactPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "contactPhone");
          final int _cursorIndexOfVisitFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "visitFrequency");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfSegment = CursorUtil.getColumnIndexOrThrow(_cursor, "segment");
          final int _cursorIndexOfAccountStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "accountStatus");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfVisitedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "visitedAt");
          final int _cursorIndexOfVisitResult = CursorUtil.getColumnIndexOrThrow(_cursor, "visitResult");
          final int _cursorIndexOfNextAction = CursorUtil.getColumnIndexOrThrow(_cursor, "nextAction");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<StopEntity> _result = new ArrayList<StopEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StopEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final String _tmpRouteUid;
            _tmpRouteUid = _cursor.getString(_cursorIndexOfRouteUid);
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpExternalId;
            if (_cursor.isNull(_cursorIndexOfExternalId)) {
              _tmpExternalId = null;
            } else {
              _tmpExternalId = _cursor.getString(_cursorIndexOfExternalId);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpContactPhone;
            if (_cursor.isNull(_cursorIndexOfContactPhone)) {
              _tmpContactPhone = null;
            } else {
              _tmpContactPhone = _cursor.getString(_cursorIndexOfContactPhone);
            }
            final Integer _tmpVisitFrequency;
            if (_cursor.isNull(_cursorIndexOfVisitFrequency)) {
              _tmpVisitFrequency = null;
            } else {
              _tmpVisitFrequency = _cursor.getInt(_cursorIndexOfVisitFrequency);
            }
            final int _tmpPriority;
            _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            final String _tmpSegment;
            if (_cursor.isNull(_cursorIndexOfSegment)) {
              _tmpSegment = null;
            } else {
              _tmpSegment = _cursor.getString(_cursorIndexOfSegment);
            }
            final String _tmpAccountStatus;
            _tmpAccountStatus = _cursor.getString(_cursorIndexOfAccountStatus);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpVisitedAt;
            if (_cursor.isNull(_cursorIndexOfVisitedAt)) {
              _tmpVisitedAt = null;
            } else {
              _tmpVisitedAt = _cursor.getString(_cursorIndexOfVisitedAt);
            }
            final String _tmpVisitResult;
            if (_cursor.isNull(_cursorIndexOfVisitResult)) {
              _tmpVisitResult = null;
            } else {
              _tmpVisitResult = _cursor.getString(_cursorIndexOfVisitResult);
            }
            final String _tmpNextAction;
            if (_cursor.isNull(_cursorIndexOfNextAction)) {
              _tmpNextAction = null;
            } else {
              _tmpNextAction = _cursor.getString(_cursorIndexOfNextAction);
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
            _item = new StopEntity(_tmpUid,_tmpServerId,_tmpRouteUid,_tmpAccountId,_tmpName,_tmpExternalId,_tmpAddress,_tmpLat,_tmpLng,_tmpOrderIndex,_tmpContactName,_tmpContactPhone,_tmpVisitFrequency,_tmpPriority,_tmpSegment,_tmpAccountStatus,_tmpOpeningHours,_tmpStatus,_tmpNotes,_tmpVisitedAt,_tmpVisitResult,_tmpNextAction,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
  public Flow<List<StopEntity>> observeByRouteUids(final List<String> routeUids) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM stops WHERE routeUid IN (");
    final int _inputSize = routeUids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") AND deletedAt IS NULL ORDER BY orderIndex ASC");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : routeUids) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"stops"}, new Callable<List<StopEntity>>() {
      @Override
      @NonNull
      public List<StopEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfRouteUid = CursorUtil.getColumnIndexOrThrow(_cursor, "routeUid");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfExternalId = CursorUtil.getColumnIndexOrThrow(_cursor, "externalId");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfContactPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "contactPhone");
          final int _cursorIndexOfVisitFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "visitFrequency");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfSegment = CursorUtil.getColumnIndexOrThrow(_cursor, "segment");
          final int _cursorIndexOfAccountStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "accountStatus");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfVisitedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "visitedAt");
          final int _cursorIndexOfVisitResult = CursorUtil.getColumnIndexOrThrow(_cursor, "visitResult");
          final int _cursorIndexOfNextAction = CursorUtil.getColumnIndexOrThrow(_cursor, "nextAction");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<StopEntity> _result = new ArrayList<StopEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StopEntity _item_1;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final String _tmpRouteUid;
            _tmpRouteUid = _cursor.getString(_cursorIndexOfRouteUid);
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpExternalId;
            if (_cursor.isNull(_cursorIndexOfExternalId)) {
              _tmpExternalId = null;
            } else {
              _tmpExternalId = _cursor.getString(_cursorIndexOfExternalId);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpContactPhone;
            if (_cursor.isNull(_cursorIndexOfContactPhone)) {
              _tmpContactPhone = null;
            } else {
              _tmpContactPhone = _cursor.getString(_cursorIndexOfContactPhone);
            }
            final Integer _tmpVisitFrequency;
            if (_cursor.isNull(_cursorIndexOfVisitFrequency)) {
              _tmpVisitFrequency = null;
            } else {
              _tmpVisitFrequency = _cursor.getInt(_cursorIndexOfVisitFrequency);
            }
            final int _tmpPriority;
            _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            final String _tmpSegment;
            if (_cursor.isNull(_cursorIndexOfSegment)) {
              _tmpSegment = null;
            } else {
              _tmpSegment = _cursor.getString(_cursorIndexOfSegment);
            }
            final String _tmpAccountStatus;
            _tmpAccountStatus = _cursor.getString(_cursorIndexOfAccountStatus);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpVisitedAt;
            if (_cursor.isNull(_cursorIndexOfVisitedAt)) {
              _tmpVisitedAt = null;
            } else {
              _tmpVisitedAt = _cursor.getString(_cursorIndexOfVisitedAt);
            }
            final String _tmpVisitResult;
            if (_cursor.isNull(_cursorIndexOfVisitResult)) {
              _tmpVisitResult = null;
            } else {
              _tmpVisitResult = _cursor.getString(_cursorIndexOfVisitResult);
            }
            final String _tmpNextAction;
            if (_cursor.isNull(_cursorIndexOfNextAction)) {
              _tmpNextAction = null;
            } else {
              _tmpNextAction = _cursor.getString(_cursorIndexOfNextAction);
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
            _item_1 = new StopEntity(_tmpUid,_tmpServerId,_tmpRouteUid,_tmpAccountId,_tmpName,_tmpExternalId,_tmpAddress,_tmpLat,_tmpLng,_tmpOrderIndex,_tmpContactName,_tmpContactPhone,_tmpVisitFrequency,_tmpPriority,_tmpSegment,_tmpAccountStatus,_tmpOpeningHours,_tmpStatus,_tmpNotes,_tmpVisitedAt,_tmpVisitResult,_tmpNextAction,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
            _result.add(_item_1);
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
  public Flow<List<StopEntity>> observeWithGpsByRouteUids(final List<String> routeUids) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("\n");
    _stringBuilder.append("        SELECT * FROM stops");
    _stringBuilder.append("\n");
    _stringBuilder.append("        WHERE routeUid IN (");
    final int _inputSize = routeUids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    _stringBuilder.append("\n");
    _stringBuilder.append("          AND deletedAt IS NULL");
    _stringBuilder.append("\n");
    _stringBuilder.append("          AND lat IS NOT NULL AND lng IS NOT NULL");
    _stringBuilder.append("\n");
    _stringBuilder.append("          AND lat != 0.0   AND lng != 0.0");
    _stringBuilder.append("\n");
    _stringBuilder.append("        ORDER BY status ASC, orderIndex ASC");
    _stringBuilder.append("\n");
    _stringBuilder.append("    ");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : routeUids) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"stops"}, new Callable<List<StopEntity>>() {
      @Override
      @NonNull
      public List<StopEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfRouteUid = CursorUtil.getColumnIndexOrThrow(_cursor, "routeUid");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfExternalId = CursorUtil.getColumnIndexOrThrow(_cursor, "externalId");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfContactPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "contactPhone");
          final int _cursorIndexOfVisitFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "visitFrequency");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfSegment = CursorUtil.getColumnIndexOrThrow(_cursor, "segment");
          final int _cursorIndexOfAccountStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "accountStatus");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfVisitedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "visitedAt");
          final int _cursorIndexOfVisitResult = CursorUtil.getColumnIndexOrThrow(_cursor, "visitResult");
          final int _cursorIndexOfNextAction = CursorUtil.getColumnIndexOrThrow(_cursor, "nextAction");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<StopEntity> _result = new ArrayList<StopEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StopEntity _item_1;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final String _tmpRouteUid;
            _tmpRouteUid = _cursor.getString(_cursorIndexOfRouteUid);
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpExternalId;
            if (_cursor.isNull(_cursorIndexOfExternalId)) {
              _tmpExternalId = null;
            } else {
              _tmpExternalId = _cursor.getString(_cursorIndexOfExternalId);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpContactPhone;
            if (_cursor.isNull(_cursorIndexOfContactPhone)) {
              _tmpContactPhone = null;
            } else {
              _tmpContactPhone = _cursor.getString(_cursorIndexOfContactPhone);
            }
            final Integer _tmpVisitFrequency;
            if (_cursor.isNull(_cursorIndexOfVisitFrequency)) {
              _tmpVisitFrequency = null;
            } else {
              _tmpVisitFrequency = _cursor.getInt(_cursorIndexOfVisitFrequency);
            }
            final int _tmpPriority;
            _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            final String _tmpSegment;
            if (_cursor.isNull(_cursorIndexOfSegment)) {
              _tmpSegment = null;
            } else {
              _tmpSegment = _cursor.getString(_cursorIndexOfSegment);
            }
            final String _tmpAccountStatus;
            _tmpAccountStatus = _cursor.getString(_cursorIndexOfAccountStatus);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpVisitedAt;
            if (_cursor.isNull(_cursorIndexOfVisitedAt)) {
              _tmpVisitedAt = null;
            } else {
              _tmpVisitedAt = _cursor.getString(_cursorIndexOfVisitedAt);
            }
            final String _tmpVisitResult;
            if (_cursor.isNull(_cursorIndexOfVisitResult)) {
              _tmpVisitResult = null;
            } else {
              _tmpVisitResult = _cursor.getString(_cursorIndexOfVisitResult);
            }
            final String _tmpNextAction;
            if (_cursor.isNull(_cursorIndexOfNextAction)) {
              _tmpNextAction = null;
            } else {
              _tmpNextAction = _cursor.getString(_cursorIndexOfNextAction);
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
            _item_1 = new StopEntity(_tmpUid,_tmpServerId,_tmpRouteUid,_tmpAccountId,_tmpName,_tmpExternalId,_tmpAddress,_tmpLat,_tmpLng,_tmpOrderIndex,_tmpContactName,_tmpContactPhone,_tmpVisitFrequency,_tmpPriority,_tmpSegment,_tmpAccountStatus,_tmpOpeningHours,_tmpStatus,_tmpNotes,_tmpVisitedAt,_tmpVisitResult,_tmpNextAction,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
            _result.add(_item_1);
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
  public Flow<List<StopEntity>> observeAll(final int accountId) {
    final String _sql = "SELECT * FROM stops WHERE accountId = ? AND deletedAt IS NULL ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"stops"}, new Callable<List<StopEntity>>() {
      @Override
      @NonNull
      public List<StopEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfRouteUid = CursorUtil.getColumnIndexOrThrow(_cursor, "routeUid");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfExternalId = CursorUtil.getColumnIndexOrThrow(_cursor, "externalId");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfContactPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "contactPhone");
          final int _cursorIndexOfVisitFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "visitFrequency");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfSegment = CursorUtil.getColumnIndexOrThrow(_cursor, "segment");
          final int _cursorIndexOfAccountStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "accountStatus");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfVisitedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "visitedAt");
          final int _cursorIndexOfVisitResult = CursorUtil.getColumnIndexOrThrow(_cursor, "visitResult");
          final int _cursorIndexOfNextAction = CursorUtil.getColumnIndexOrThrow(_cursor, "nextAction");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<StopEntity> _result = new ArrayList<StopEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StopEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final String _tmpRouteUid;
            _tmpRouteUid = _cursor.getString(_cursorIndexOfRouteUid);
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpExternalId;
            if (_cursor.isNull(_cursorIndexOfExternalId)) {
              _tmpExternalId = null;
            } else {
              _tmpExternalId = _cursor.getString(_cursorIndexOfExternalId);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpContactPhone;
            if (_cursor.isNull(_cursorIndexOfContactPhone)) {
              _tmpContactPhone = null;
            } else {
              _tmpContactPhone = _cursor.getString(_cursorIndexOfContactPhone);
            }
            final Integer _tmpVisitFrequency;
            if (_cursor.isNull(_cursorIndexOfVisitFrequency)) {
              _tmpVisitFrequency = null;
            } else {
              _tmpVisitFrequency = _cursor.getInt(_cursorIndexOfVisitFrequency);
            }
            final int _tmpPriority;
            _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            final String _tmpSegment;
            if (_cursor.isNull(_cursorIndexOfSegment)) {
              _tmpSegment = null;
            } else {
              _tmpSegment = _cursor.getString(_cursorIndexOfSegment);
            }
            final String _tmpAccountStatus;
            _tmpAccountStatus = _cursor.getString(_cursorIndexOfAccountStatus);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpVisitedAt;
            if (_cursor.isNull(_cursorIndexOfVisitedAt)) {
              _tmpVisitedAt = null;
            } else {
              _tmpVisitedAt = _cursor.getString(_cursorIndexOfVisitedAt);
            }
            final String _tmpVisitResult;
            if (_cursor.isNull(_cursorIndexOfVisitResult)) {
              _tmpVisitResult = null;
            } else {
              _tmpVisitResult = _cursor.getString(_cursorIndexOfVisitResult);
            }
            final String _tmpNextAction;
            if (_cursor.isNull(_cursorIndexOfNextAction)) {
              _tmpNextAction = null;
            } else {
              _tmpNextAction = _cursor.getString(_cursorIndexOfNextAction);
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
            _item = new StopEntity(_tmpUid,_tmpServerId,_tmpRouteUid,_tmpAccountId,_tmpName,_tmpExternalId,_tmpAddress,_tmpLat,_tmpLng,_tmpOrderIndex,_tmpContactName,_tmpContactPhone,_tmpVisitFrequency,_tmpPriority,_tmpSegment,_tmpAccountStatus,_tmpOpeningHours,_tmpStatus,_tmpNotes,_tmpVisitedAt,_tmpVisitResult,_tmpNextAction,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
  public Flow<List<StopEntity>> observeWithoutGps(final int accountId) {
    final String _sql = "SELECT * FROM stops WHERE accountId = ? AND deletedAt IS NULL AND (lat IS NULL OR lng IS NULL OR lat = 0.0 OR lng = 0.0) ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"stops"}, new Callable<List<StopEntity>>() {
      @Override
      @NonNull
      public List<StopEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfRouteUid = CursorUtil.getColumnIndexOrThrow(_cursor, "routeUid");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfExternalId = CursorUtil.getColumnIndexOrThrow(_cursor, "externalId");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfContactPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "contactPhone");
          final int _cursorIndexOfVisitFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "visitFrequency");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfSegment = CursorUtil.getColumnIndexOrThrow(_cursor, "segment");
          final int _cursorIndexOfAccountStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "accountStatus");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfVisitedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "visitedAt");
          final int _cursorIndexOfVisitResult = CursorUtil.getColumnIndexOrThrow(_cursor, "visitResult");
          final int _cursorIndexOfNextAction = CursorUtil.getColumnIndexOrThrow(_cursor, "nextAction");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<StopEntity> _result = new ArrayList<StopEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StopEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final String _tmpRouteUid;
            _tmpRouteUid = _cursor.getString(_cursorIndexOfRouteUid);
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpExternalId;
            if (_cursor.isNull(_cursorIndexOfExternalId)) {
              _tmpExternalId = null;
            } else {
              _tmpExternalId = _cursor.getString(_cursorIndexOfExternalId);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpContactPhone;
            if (_cursor.isNull(_cursorIndexOfContactPhone)) {
              _tmpContactPhone = null;
            } else {
              _tmpContactPhone = _cursor.getString(_cursorIndexOfContactPhone);
            }
            final Integer _tmpVisitFrequency;
            if (_cursor.isNull(_cursorIndexOfVisitFrequency)) {
              _tmpVisitFrequency = null;
            } else {
              _tmpVisitFrequency = _cursor.getInt(_cursorIndexOfVisitFrequency);
            }
            final int _tmpPriority;
            _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            final String _tmpSegment;
            if (_cursor.isNull(_cursorIndexOfSegment)) {
              _tmpSegment = null;
            } else {
              _tmpSegment = _cursor.getString(_cursorIndexOfSegment);
            }
            final String _tmpAccountStatus;
            _tmpAccountStatus = _cursor.getString(_cursorIndexOfAccountStatus);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpVisitedAt;
            if (_cursor.isNull(_cursorIndexOfVisitedAt)) {
              _tmpVisitedAt = null;
            } else {
              _tmpVisitedAt = _cursor.getString(_cursorIndexOfVisitedAt);
            }
            final String _tmpVisitResult;
            if (_cursor.isNull(_cursorIndexOfVisitResult)) {
              _tmpVisitResult = null;
            } else {
              _tmpVisitResult = _cursor.getString(_cursorIndexOfVisitResult);
            }
            final String _tmpNextAction;
            if (_cursor.isNull(_cursorIndexOfNextAction)) {
              _tmpNextAction = null;
            } else {
              _tmpNextAction = _cursor.getString(_cursorIndexOfNextAction);
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
            _item = new StopEntity(_tmpUid,_tmpServerId,_tmpRouteUid,_tmpAccountId,_tmpName,_tmpExternalId,_tmpAddress,_tmpLat,_tmpLng,_tmpOrderIndex,_tmpContactName,_tmpContactPhone,_tmpVisitFrequency,_tmpPriority,_tmpSegment,_tmpAccountStatus,_tmpOpeningHours,_tmpStatus,_tmpNotes,_tmpVisitedAt,_tmpVisitResult,_tmpNextAction,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
  public Flow<List<StopEntity>> observeOrphaned(final int accountId) {
    final String _sql = "SELECT * FROM stops WHERE accountId = ? AND deletedAt IS NULL AND routeUid NOT IN (SELECT uid FROM routes WHERE deletedAt IS NULL) ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"stops",
        "routes"}, new Callable<List<StopEntity>>() {
      @Override
      @NonNull
      public List<StopEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfRouteUid = CursorUtil.getColumnIndexOrThrow(_cursor, "routeUid");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfExternalId = CursorUtil.getColumnIndexOrThrow(_cursor, "externalId");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfContactPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "contactPhone");
          final int _cursorIndexOfVisitFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "visitFrequency");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfSegment = CursorUtil.getColumnIndexOrThrow(_cursor, "segment");
          final int _cursorIndexOfAccountStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "accountStatus");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfVisitedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "visitedAt");
          final int _cursorIndexOfVisitResult = CursorUtil.getColumnIndexOrThrow(_cursor, "visitResult");
          final int _cursorIndexOfNextAction = CursorUtil.getColumnIndexOrThrow(_cursor, "nextAction");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<StopEntity> _result = new ArrayList<StopEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StopEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final String _tmpRouteUid;
            _tmpRouteUid = _cursor.getString(_cursorIndexOfRouteUid);
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpExternalId;
            if (_cursor.isNull(_cursorIndexOfExternalId)) {
              _tmpExternalId = null;
            } else {
              _tmpExternalId = _cursor.getString(_cursorIndexOfExternalId);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpContactPhone;
            if (_cursor.isNull(_cursorIndexOfContactPhone)) {
              _tmpContactPhone = null;
            } else {
              _tmpContactPhone = _cursor.getString(_cursorIndexOfContactPhone);
            }
            final Integer _tmpVisitFrequency;
            if (_cursor.isNull(_cursorIndexOfVisitFrequency)) {
              _tmpVisitFrequency = null;
            } else {
              _tmpVisitFrequency = _cursor.getInt(_cursorIndexOfVisitFrequency);
            }
            final int _tmpPriority;
            _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            final String _tmpSegment;
            if (_cursor.isNull(_cursorIndexOfSegment)) {
              _tmpSegment = null;
            } else {
              _tmpSegment = _cursor.getString(_cursorIndexOfSegment);
            }
            final String _tmpAccountStatus;
            _tmpAccountStatus = _cursor.getString(_cursorIndexOfAccountStatus);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpVisitedAt;
            if (_cursor.isNull(_cursorIndexOfVisitedAt)) {
              _tmpVisitedAt = null;
            } else {
              _tmpVisitedAt = _cursor.getString(_cursorIndexOfVisitedAt);
            }
            final String _tmpVisitResult;
            if (_cursor.isNull(_cursorIndexOfVisitResult)) {
              _tmpVisitResult = null;
            } else {
              _tmpVisitResult = _cursor.getString(_cursorIndexOfVisitResult);
            }
            final String _tmpNextAction;
            if (_cursor.isNull(_cursorIndexOfNextAction)) {
              _tmpNextAction = null;
            } else {
              _tmpNextAction = _cursor.getString(_cursorIndexOfNextAction);
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
            _item = new StopEntity(_tmpUid,_tmpServerId,_tmpRouteUid,_tmpAccountId,_tmpName,_tmpExternalId,_tmpAddress,_tmpLat,_tmpLng,_tmpOrderIndex,_tmpContactName,_tmpContactPhone,_tmpVisitFrequency,_tmpPriority,_tmpSegment,_tmpAccountStatus,_tmpOpeningHours,_tmpStatus,_tmpNotes,_tmpVisitedAt,_tmpVisitResult,_tmpNextAction,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
  public Object getByUid(final String uid, final Continuation<? super StopEntity> $completion) {
    final String _sql = "SELECT * FROM stops WHERE uid = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, uid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<StopEntity>() {
      @Override
      @Nullable
      public StopEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfRouteUid = CursorUtil.getColumnIndexOrThrow(_cursor, "routeUid");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfExternalId = CursorUtil.getColumnIndexOrThrow(_cursor, "externalId");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfContactPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "contactPhone");
          final int _cursorIndexOfVisitFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "visitFrequency");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfSegment = CursorUtil.getColumnIndexOrThrow(_cursor, "segment");
          final int _cursorIndexOfAccountStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "accountStatus");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfVisitedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "visitedAt");
          final int _cursorIndexOfVisitResult = CursorUtil.getColumnIndexOrThrow(_cursor, "visitResult");
          final int _cursorIndexOfNextAction = CursorUtil.getColumnIndexOrThrow(_cursor, "nextAction");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final StopEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final String _tmpRouteUid;
            _tmpRouteUid = _cursor.getString(_cursorIndexOfRouteUid);
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpExternalId;
            if (_cursor.isNull(_cursorIndexOfExternalId)) {
              _tmpExternalId = null;
            } else {
              _tmpExternalId = _cursor.getString(_cursorIndexOfExternalId);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpContactPhone;
            if (_cursor.isNull(_cursorIndexOfContactPhone)) {
              _tmpContactPhone = null;
            } else {
              _tmpContactPhone = _cursor.getString(_cursorIndexOfContactPhone);
            }
            final Integer _tmpVisitFrequency;
            if (_cursor.isNull(_cursorIndexOfVisitFrequency)) {
              _tmpVisitFrequency = null;
            } else {
              _tmpVisitFrequency = _cursor.getInt(_cursorIndexOfVisitFrequency);
            }
            final int _tmpPriority;
            _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            final String _tmpSegment;
            if (_cursor.isNull(_cursorIndexOfSegment)) {
              _tmpSegment = null;
            } else {
              _tmpSegment = _cursor.getString(_cursorIndexOfSegment);
            }
            final String _tmpAccountStatus;
            _tmpAccountStatus = _cursor.getString(_cursorIndexOfAccountStatus);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpVisitedAt;
            if (_cursor.isNull(_cursorIndexOfVisitedAt)) {
              _tmpVisitedAt = null;
            } else {
              _tmpVisitedAt = _cursor.getString(_cursorIndexOfVisitedAt);
            }
            final String _tmpVisitResult;
            if (_cursor.isNull(_cursorIndexOfVisitResult)) {
              _tmpVisitResult = null;
            } else {
              _tmpVisitResult = _cursor.getString(_cursorIndexOfVisitResult);
            }
            final String _tmpNextAction;
            if (_cursor.isNull(_cursorIndexOfNextAction)) {
              _tmpNextAction = null;
            } else {
              _tmpNextAction = _cursor.getString(_cursorIndexOfNextAction);
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
            _result = new StopEntity(_tmpUid,_tmpServerId,_tmpRouteUid,_tmpAccountId,_tmpName,_tmpExternalId,_tmpAddress,_tmpLat,_tmpLng,_tmpOrderIndex,_tmpContactName,_tmpContactPhone,_tmpVisitFrequency,_tmpPriority,_tmpSegment,_tmpAccountStatus,_tmpOpeningHours,_tmpStatus,_tmpNotes,_tmpVisitedAt,_tmpVisitResult,_tmpNextAction,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
  public Object getPendingSync(final Continuation<? super List<StopEntity>> $completion) {
    final String _sql = "SELECT * FROM stops WHERE syncStatus = 'pending' OR syncStatus = 'error'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<StopEntity>>() {
      @Override
      @NonNull
      public List<StopEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUid = CursorUtil.getColumnIndexOrThrow(_cursor, "uid");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfRouteUid = CursorUtil.getColumnIndexOrThrow(_cursor, "routeUid");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfExternalId = CursorUtil.getColumnIndexOrThrow(_cursor, "externalId");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfOrderIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "orderIndex");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfContactPhone = CursorUtil.getColumnIndexOrThrow(_cursor, "contactPhone");
          final int _cursorIndexOfVisitFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "visitFrequency");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfSegment = CursorUtil.getColumnIndexOrThrow(_cursor, "segment");
          final int _cursorIndexOfAccountStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "accountStatus");
          final int _cursorIndexOfOpeningHours = CursorUtil.getColumnIndexOrThrow(_cursor, "openingHours");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfVisitedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "visitedAt");
          final int _cursorIndexOfVisitResult = CursorUtil.getColumnIndexOrThrow(_cursor, "visitResult");
          final int _cursorIndexOfNextAction = CursorUtil.getColumnIndexOrThrow(_cursor, "nextAction");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final List<StopEntity> _result = new ArrayList<StopEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final StopEntity _item;
            final String _tmpUid;
            _tmpUid = _cursor.getString(_cursorIndexOfUid);
            final Integer _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getInt(_cursorIndexOfServerId);
            }
            final String _tmpRouteUid;
            _tmpRouteUid = _cursor.getString(_cursorIndexOfRouteUid);
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpExternalId;
            if (_cursor.isNull(_cursorIndexOfExternalId)) {
              _tmpExternalId = null;
            } else {
              _tmpExternalId = _cursor.getString(_cursorIndexOfExternalId);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final int _tmpOrderIndex;
            _tmpOrderIndex = _cursor.getInt(_cursorIndexOfOrderIndex);
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final String _tmpContactPhone;
            if (_cursor.isNull(_cursorIndexOfContactPhone)) {
              _tmpContactPhone = null;
            } else {
              _tmpContactPhone = _cursor.getString(_cursorIndexOfContactPhone);
            }
            final Integer _tmpVisitFrequency;
            if (_cursor.isNull(_cursorIndexOfVisitFrequency)) {
              _tmpVisitFrequency = null;
            } else {
              _tmpVisitFrequency = _cursor.getInt(_cursorIndexOfVisitFrequency);
            }
            final int _tmpPriority;
            _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            final String _tmpSegment;
            if (_cursor.isNull(_cursorIndexOfSegment)) {
              _tmpSegment = null;
            } else {
              _tmpSegment = _cursor.getString(_cursorIndexOfSegment);
            }
            final String _tmpAccountStatus;
            _tmpAccountStatus = _cursor.getString(_cursorIndexOfAccountStatus);
            final String _tmpOpeningHours;
            if (_cursor.isNull(_cursorIndexOfOpeningHours)) {
              _tmpOpeningHours = null;
            } else {
              _tmpOpeningHours = _cursor.getString(_cursorIndexOfOpeningHours);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpVisitedAt;
            if (_cursor.isNull(_cursorIndexOfVisitedAt)) {
              _tmpVisitedAt = null;
            } else {
              _tmpVisitedAt = _cursor.getString(_cursorIndexOfVisitedAt);
            }
            final String _tmpVisitResult;
            if (_cursor.isNull(_cursorIndexOfVisitResult)) {
              _tmpVisitResult = null;
            } else {
              _tmpVisitResult = _cursor.getString(_cursorIndexOfVisitResult);
            }
            final String _tmpNextAction;
            if (_cursor.isNull(_cursorIndexOfNextAction)) {
              _tmpNextAction = null;
            } else {
              _tmpNextAction = _cursor.getString(_cursorIndexOfNextAction);
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
            _item = new StopEntity(_tmpUid,_tmpServerId,_tmpRouteUid,_tmpAccountId,_tmpName,_tmpExternalId,_tmpAddress,_tmpLat,_tmpLng,_tmpOrderIndex,_tmpContactName,_tmpContactPhone,_tmpVisitFrequency,_tmpPriority,_tmpSegment,_tmpAccountStatus,_tmpOpeningHours,_tmpStatus,_tmpNotes,_tmpVisitedAt,_tmpVisitResult,_tmpNextAction,_tmpCreatedAt,_tmpUpdatedAt,_tmpDeletedAt,_tmpSyncStatus,_tmpSyncedAt);
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
