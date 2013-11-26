/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.util.ArgumentChecker;

// TODO this will need to support thread local binding for full reval
// TODO cache invalidation. decorate or put it in here?
public class ValuationTimeProvider implements ValuationTimeProviderFunction {

  private Instant _valuationTime;

  public ValuationTimeProvider(Instant valuationTime) {
    _valuationTime = ArgumentChecker.notNull(valuationTime, "valuationTime");
  }

  @Override
  public Instant getValuationTime() {
    return _valuationTime;
  }

  @Override
  public ZonedDateTime getZonedDateTime() {
    return ZonedDateTime.ofInstant(_valuationTime, ZoneOffset.UTC);
  }

  @Override
  public LocalDate getLocalDate() {
    return LocalDate.from(getZonedDateTime());
  }

  public void setValuationTime(Instant valuationTime) {
    _valuationTime = ArgumentChecker.notNull(valuationTime, "valuationTime");
  }
}
