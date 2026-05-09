package com.pabl3st.rutapp.feature.auth;

import com.pabl3st.rutapp.data.repository.AuthRepository;
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<AuthRepository> repoProvider;

  private final Provider<SessionManager> sessionProvider;

  public AuthViewModel_Factory(Provider<AuthRepository> repoProvider,
      Provider<SessionManager> sessionProvider) {
    this.repoProvider = repoProvider;
    this.sessionProvider = sessionProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(repoProvider.get(), sessionProvider.get());
  }

  public static AuthViewModel_Factory create(Provider<AuthRepository> repoProvider,
      Provider<SessionManager> sessionProvider) {
    return new AuthViewModel_Factory(repoProvider, sessionProvider);
  }

  public static AuthViewModel newInstance(AuthRepository repo, SessionManager session) {
    return new AuthViewModel(repo, session);
  }
}
