package com.pabl3st.rutapp.sync;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.pabl3st.rutapp.data.repository.SyncRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class SyncWorker_Factory {
  private final Provider<SyncRepository> syncRepositoryProvider;

  public SyncWorker_Factory(Provider<SyncRepository> syncRepositoryProvider) {
    this.syncRepositoryProvider = syncRepositoryProvider;
  }

  public SyncWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, syncRepositoryProvider.get());
  }

  public static SyncWorker_Factory create(Provider<SyncRepository> syncRepositoryProvider) {
    return new SyncWorker_Factory(syncRepositoryProvider);
  }

  public static SyncWorker newInstance(Context appContext, WorkerParameters workerParams,
      SyncRepository syncRepository) {
    return new SyncWorker(appContext, workerParams, syncRepository);
  }
}
