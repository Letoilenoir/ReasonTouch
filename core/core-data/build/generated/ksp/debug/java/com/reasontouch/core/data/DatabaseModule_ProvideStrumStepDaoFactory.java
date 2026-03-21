package com.reasontouch.core.data;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DatabaseModule_ProvideStrumStepDaoFactory implements Factory<StrumStepDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideStrumStepDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public StrumStepDao get() {
    return provideStrumStepDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideStrumStepDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideStrumStepDaoFactory(dbProvider);
  }

  public static StrumStepDao provideStrumStepDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideStrumStepDao(db));
  }
}
