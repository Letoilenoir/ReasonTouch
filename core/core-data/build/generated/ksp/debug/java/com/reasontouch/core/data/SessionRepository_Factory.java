package com.reasontouch.core.data;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class SessionRepository_Factory implements Factory<SessionRepository> {
  private final Provider<SessionDao> sessionDaoProvider;

  private final Provider<MidiTrackDao> trackDaoProvider;

  private final Provider<NoteEventDao> noteDaoProvider;

  private final Provider<ChordEventDao> chordDaoProvider;

  private final Provider<StrumPatternDao> strumPatternDaoProvider;

  private final Provider<StrumStepDao> strumStepDaoProvider;

  public SessionRepository_Factory(Provider<SessionDao> sessionDaoProvider,
      Provider<MidiTrackDao> trackDaoProvider, Provider<NoteEventDao> noteDaoProvider,
      Provider<ChordEventDao> chordDaoProvider, Provider<StrumPatternDao> strumPatternDaoProvider,
      Provider<StrumStepDao> strumStepDaoProvider) {
    this.sessionDaoProvider = sessionDaoProvider;
    this.trackDaoProvider = trackDaoProvider;
    this.noteDaoProvider = noteDaoProvider;
    this.chordDaoProvider = chordDaoProvider;
    this.strumPatternDaoProvider = strumPatternDaoProvider;
    this.strumStepDaoProvider = strumStepDaoProvider;
  }

  @Override
  public SessionRepository get() {
    return newInstance(sessionDaoProvider.get(), trackDaoProvider.get(), noteDaoProvider.get(), chordDaoProvider.get(), strumPatternDaoProvider.get(), strumStepDaoProvider.get());
  }

  public static SessionRepository_Factory create(Provider<SessionDao> sessionDaoProvider,
      Provider<MidiTrackDao> trackDaoProvider, Provider<NoteEventDao> noteDaoProvider,
      Provider<ChordEventDao> chordDaoProvider, Provider<StrumPatternDao> strumPatternDaoProvider,
      Provider<StrumStepDao> strumStepDaoProvider) {
    return new SessionRepository_Factory(sessionDaoProvider, trackDaoProvider, noteDaoProvider, chordDaoProvider, strumPatternDaoProvider, strumStepDaoProvider);
  }

  public static SessionRepository newInstance(SessionDao sessionDao, MidiTrackDao trackDao,
      NoteEventDao noteDao, ChordEventDao chordDao, StrumPatternDao strumPatternDao,
      StrumStepDao strumStepDao) {
    return new SessionRepository(sessionDao, trackDao, noteDao, chordDao, strumPatternDao, strumStepDao);
  }
}
