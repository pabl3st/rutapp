package com.pabl3st.rutapp.feature.perfil;

import com.pabl3st.rutapp.data.repository.AuthRepository;
import com.pabl3st.rutapp.data.repository.BusinessProfileRepository;
import com.pabl3st.rutapp.data.repository.UserPrefsRepository;
import com.pabl3st.rutapp.data.session.SessionManager;
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
public final class PerfilViewModel_Factory implements Factory<PerfilViewModel> {
  private final Provider<SessionManager> sessionProvider;

  private final Provider<AuthRepository> authRepoProvider;

  private final Provider<BusinessProfileRepository> profileRepoProvider;

  private final Provider<UserPrefsRepository> prefsRepoProvider;

  public PerfilViewModel_Factory(Provider<SessionManager> sessionProvider,
      Provider<AuthRepository> authRepoProvider,
      Provider<BusinessProfileRepository> profileRepoProvider,
      Provider<UserPrefsRepository> prefsRepoProvider) {
    this.sessionProvider = sessionProvider;
    this.authRepoProvider = authRepoProvider;
    this.profileRepoProvider = profileRepoProvider;
    this.prefsRepoProvider = prefsRepoProvider;
  }

  @Override
  public PerfilViewModel get() {
    return newInstance(sessionProvider.get(), authRepoProvider.get(), profileRepoProvider.get(), prefsRepoProvider.get());
  }

  public static PerfilViewModel_Factory create(Provider<SessionManager> sessionProvider,
      Provider<AuthRepository> authRepoProvider,
      Provider<BusinessProfileRepository> profileRepoProvider,
      Provider<UserPrefsRepository> prefsRepoProvider) {
    return new PerfilViewModel_Factory(sessionProvider, authRepoProvider, profileRepoProvider, prefsRepoProvider);
  }

  public static PerfilViewModel newInstance(SessionManager session, AuthRepository authRepo,
      BusinessProfileRepository profileRepo, UserPrefsRepository prefsRepo) {
    return new PerfilViewModel(session, authRepo, profileRepo, prefsRepo);
  }
}
