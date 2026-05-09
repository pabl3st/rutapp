package com.pabl3st.rutapp.feature.perfil;

import com.pabl3st.rutapp.data.repository.BusinessProfileRepository;
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
public final class BusinessProfileViewModel_Factory implements Factory<BusinessProfileViewModel> {
  private final Provider<BusinessProfileRepository> repoProvider;

  public BusinessProfileViewModel_Factory(Provider<BusinessProfileRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public BusinessProfileViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static BusinessProfileViewModel_Factory create(
      Provider<BusinessProfileRepository> repoProvider) {
    return new BusinessProfileViewModel_Factory(repoProvider);
  }

  public static BusinessProfileViewModel newInstance(BusinessProfileRepository repo) {
    return new BusinessProfileViewModel(repo);
  }
}
