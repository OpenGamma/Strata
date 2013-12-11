/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.util.ArgumentChecker;

/**
 * {@link ValuationTimeFn} implementation that tells a {@link DefaultCacheInvalidator} when its methods are called.
 * This allows the engine to infer which values in the cache depend on the valuation time so it can invalidate
 * them when the valuation time changes. The valuation time is sourced from an underlying {@link ValuationTimeFn}.
 */
public class CacheAwareValuationTimeFn implements ValuationTimeFn {

  private final ValuationTimeFn _delegate;
  private final CacheInvalidator _cacheInvalidator;

  /* package */
  public CacheAwareValuationTimeFn(ValuationTimeFn delegate, CacheInvalidator cacheInvalidator) {
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
    _cacheInvalidator = ArgumentChecker.notNull(cacheInvalidator, "cacheInvalidator");
  }

  @Override
  public LocalDate getDate() {
    _cacheInvalidator.register(new ValuationTimeCacheEntry.ValidOnCalculationDay(_delegate.getDate()));
    return _delegate.getDate();
  }

  @Override
  public ZonedDateTime getTime() {
    _cacheInvalidator.register(new ValuationTimeCacheEntry.ValidAtCalculationInstant(_delegate.getTime()));
    return _delegate.getTime();
  }
}
