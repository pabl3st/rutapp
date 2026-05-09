package com.pabl3st.rutapp.feature.visita;

import androidx.lifecycle.SavedStateHandle;
import com.pabl3st.rutapp.data.local.dao.KpiValueDao;
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository;
import com.pabl3st.rutapp.data.repository.StopRepository;
import com.pabl3st.rutapp.data.repository.UserPrefsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class VisitaViewModel_Factory implements Factory<VisitaViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<StopRepository> stopRepoProvider;

  private final Provider<BusinessProfileRepository> profileRepoProvider;

  private final Provider<KpiValueDao> kpiValueDaoProvider;

  private final Provider<UserPrefsRepository> prefsRepoProvider;

  public VisitaViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<StopRepository> stopRepoProvider,
      Provider<BusinessProfileRepository> profileRepoProvider,
      Provider<KpiValueDao> kpiValueDaoProvider, Provider<UserPrefsRepository> prefsRepoProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.stopRepoProvider = stopRepoProvider;
    this.profileRepoProvider = profileRepoProvider;
    this.kpiValueDaoProvider = kpiValueDaoProvider;
    this.prefsRepoProvider = prefsRepoProvider;
  }

  @Override
  public VisitaViewModel get() {
    return newInstance(savedStateHandleProvider.get(), stopRepoProvider.get(), profileRepoProvider.get(), kpiValueDaoProvider.get(), prefsRepoProvider.get());
  }

  public static VisitaViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<StopRepository> stopRepoProvider,
      Provider<BusinessProfileRepository> profileRepoProvider,
      Provider<KpiValueDao> kpiValueDaoProvider, Provider<UserPrefsRepository> prefsRepoProvider) {
    return new VisitaViewModel_Factory(savedStateHandleProvider, stopRepoProvider, profileRepoProvider, kpiValueDaoProvider, prefsRepoProvider);
  }

  public static VisitaViewModel newInstance(SavedStateHandle savedStateHandle,
      StopRepository stopRepo, BusinessProfileRepository profileRepo, KpiValueDao kpiValueDao,
      UserPrefsRepository prefsRepo) {
    return new VisitaViewModel(savedStateHandle, stopRepo, profileRepo, kpiValueDao, prefsRepo);
  }
}
