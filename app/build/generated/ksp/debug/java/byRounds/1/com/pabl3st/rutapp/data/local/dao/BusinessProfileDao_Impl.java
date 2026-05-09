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
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pabl3st.rutapp.data.local.entity.BusinessProfileEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class BusinessProfileDao_Impl implements BusinessProfileDao {
  private final RoomDatabase __db;

  private final EntityUpsertionAdapter<BusinessProfileEntity> __upsertionAdapterOfBusinessProfileEntity;

  public BusinessProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__upsertionAdapterOfBusinessProfileEntity = new EntityUpsertionAdapter<BusinessProfileEntity>(new EntityInsertionAdapter<BusinessProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `business_profiles` (`accountId`,`sector`,`name`,`updatedAt`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BusinessProfileEntity entity) {
        statement.bindLong(1, entity.getAccountId());
        statement.bindString(2, entity.getSector());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getUpdatedAt());
      }
    }, new EntityDeletionOrUpdateAdapter<BusinessProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `business_profiles` SET `accountId` = ?,`sector` = ?,`name` = ?,`updatedAt` = ? WHERE `accountId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BusinessProfileEntity entity) {
        statement.bindLong(1, entity.getAccountId());
        statement.bindString(2, entity.getSector());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getUpdatedAt());
        statement.bindLong(5, entity.getAccountId());
      }
    });
  }

  @Override
  public Object upsert(final BusinessProfileEntity profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfBusinessProfileEntity.upsert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<BusinessProfileEntity> observe(final int accountId) {
    final String _sql = "SELECT * FROM business_profiles WHERE accountId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"business_profiles"}, new Callable<BusinessProfileEntity>() {
      @Override
      @Nullable
      public BusinessProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfSector = CursorUtil.getColumnIndexOrThrow(_cursor, "sector");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final BusinessProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpSector;
            _tmpSector = _cursor.getString(_cursorIndexOfSector);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new BusinessProfileEntity(_tmpAccountId,_tmpSector,_tmpName,_tmpUpdatedAt);
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
  public Object get(final int accountId,
      final Continuation<? super BusinessProfileEntity> $completion) {
    final String _sql = "SELECT * FROM business_profiles WHERE accountId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BusinessProfileEntity>() {
      @Override
      @Nullable
      public BusinessProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfSector = CursorUtil.getColumnIndexOrThrow(_cursor, "sector");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final BusinessProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpAccountId;
            _tmpAccountId = _cursor.getInt(_cursorIndexOfAccountId);
            final String _tmpSector;
            _tmpSector = _cursor.getString(_cursorIndexOfSector);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new BusinessProfileEntity(_tmpAccountId,_tmpSector,_tmpName,_tmpUpdatedAt);
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
