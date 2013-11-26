/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import javax.inject.Provider;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

public interface ValuationTimeProviderFunction extends Provider<ZonedDateTime> {

  /** @deprecated use {@link #get()} and call {@code toInstant()}. */
  @Deprecated
  Instant getValuationTime();

  /** @deprecated use {@link #get()}. */
  @Deprecated
  ZonedDateTime getZonedDateTime();

  /** @deprecated use {@link #get()} and call {@code toLocalDate()}. */
  @Deprecated
  LocalDate getLocalDate();
}
