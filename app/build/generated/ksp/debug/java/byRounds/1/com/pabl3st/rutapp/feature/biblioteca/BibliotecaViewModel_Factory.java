package com.pabl3st.rutapp.feature.biblioteca;

import com.pabl3st.rutapp.data.repository.StopRepository;
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
public final class BibliotecaViewModel_Factory implements Factory<BibliotecaViewModel> {
  private final Provider<StopRepository> stopRepoProvider;

  private final Provider<SessionManager> sessionProvider;

  public BibliotecaViewModel_Factory(Provider<StopRepository> stopRepoProvider,
      Provider<SessionManager> sessionProvider) {
    this.stopRepoProvider = stopRepoProvider;
    this.sessionProvider = sessionProvider;
  }

  @Override
  public BibliotecaViewModel get() {
    return newInstance(stopRepoProvider.get(), sessionProvider.get());
  }

  public static BibliotecaViewModel_Factory create(Provider<StopRepository> stopRepoProvider,
      Provider<SessionManager> sessionProvider) {
    return new BibliotecaViewModel_Factory(stopRepoProvider, sessionProvider);
  }

  public static BibliotecaViewModel newInstance(StopRepository stopRepo, SessionManager session) {
    return new BibliotecaViewModel(stopRepo, session);
  }
}
