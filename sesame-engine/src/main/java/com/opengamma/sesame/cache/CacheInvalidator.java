/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import java.util.Collection;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.marketdata.MarketDataFn;

/**
 * TODO bulk register methods?
 */
public interface CacheInvalidator {

  void register(ExternalId id);

  void register(ExternalIdBundle bundle);

  void register(ObjectId id);

  void register(ValuationTimeCacheEntry entry);

  void invalidate(MarketDataFn marketDataFactory,
                  ZonedDateTime valuationTime,
                  VersionCorrection configVersionCorrection,
                  Collection<ExternalId> marketData,
                  Collection<ObjectId> dbData);
}
