/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.marketdata.MarketDataFactory;

/**
 * {@link CacheInvalidator} that does nothing, used when caching is disabled.
 */
public class NoOpCacheInvalidator implements CacheInvalidator {

  @Override
  public void register(ExternalId id) {
  }

  @Override
  public void register(ExternalIdBundle bundle) {
  }

  @Override
  public void register(ObjectId id) {
  }

  @Override
  public void invalidate(ExternalId id) {
  }

  @Override
  public void invalidate(ObjectId id) {
  }

  @Override
  public void register(ValuationTimeCacheEntry entry) {
  }

  @Override
  public void invalidate(ZonedDateTime valuationTime) {
  }

  @Override
  public void setDataSource(MarketDataFactory marketDataFactory) {
  }
}
