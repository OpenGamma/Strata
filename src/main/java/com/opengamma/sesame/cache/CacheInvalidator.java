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
 *
 */
public interface CacheInvalidator {

  void register(ExternalId id);

  void register(ExternalIdBundle bundle);

  void register(ObjectId id);

  void invalidate(ExternalId id);

  void invalidate(ObjectId id);

  void register(ValuationTimeCacheEntry entry);

  void invalidate(ZonedDateTime valuationTime);

  void setDataSource(MarketDataFactory marketDataFactory);
}
