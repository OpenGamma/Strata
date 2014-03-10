/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.util.ArgumentChecker;

/**
 * Object associated with a cache entry that depends on the valuation time.
 * When the valuation time changes entries are checked against the new time and if they're no longer valid
 * the corresponding cache entry is cleared.
 */
public interface ValuationTimeCacheEntry {

  /**
   * Returns true if the cache entry is valid at the valuation time.
   * @param valuationTime The new valuation time
   * @return true if the cache entry is valid at the specified time
   */
  boolean isValidAt(ZonedDateTime valuationTime);

  public static final class ValidAtCalculationInstant implements ValuationTimeCacheEntry {

    private final ZonedDateTime _calculationTime;

    public ValidAtCalculationInstant(ZonedDateTime calculationTime) {
      _calculationTime = ArgumentChecker.notNull(calculationTime, "calculationTime");
    }

    @Override
    public boolean isValidAt(ZonedDateTime valuationTime) {
      return _calculationTime.equals(valuationTime);
    }
  }

  public static final class ValidOnCalculationDay implements ValuationTimeCacheEntry {

    private final LocalDate _calculationDate;

    public ValidOnCalculationDay(LocalDate calculationDate) {
      _calculationDate = ArgumentChecker.notNull(calculationDate, "calculationDate");
    }

    @Override
    public boolean isValidAt(ZonedDateTime valuationTime) {
      return _calculationDate.equals(valuationTime.toLocalDate());
    }
  }
}
