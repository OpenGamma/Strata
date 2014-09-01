/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata.spec;

import java.io.Serializable;

import org.threeten.bp.LocalDate;

import com.opengamma.id.UniqueId;
import com.opengamma.util.ArgumentChecker;

/**
 * Marker interface for all market data specifications.
 */
public interface MarketDataSpecification extends Serializable {

  /**
   * Gets a live market data specification.
   * 
   * @return the live market data specification
   */
  public static LiveMarketDataSpecification live() {
    return LiveMarketDataSpecification.LIVE_SPEC;
  }

  /**
   * Gets a live market data specification for a specific data source.
   * 
   * @param dataSource  the name of the data source, not null
   * @return the live market data specification, not null
   */
  public static LiveMarketDataSpecification live(String dataSource) {
    ArgumentChecker.notNull(dataSource, "dataSource");
    return LiveMarketDataSpecification.of(dataSource.intern());
  }

  /**
   * Gets a historical market data specification.
   * 
   * @param date  the date, not null
   * @param timeSeriesResolverKey time series resolver key, or null for the system default
   * @return the historical market data specification, not null
   */
  public static HistoricalMarketDataSpecification historical(LocalDate date, String timeSeriesResolverKey) {
    return new FixedHistoricalMarketDataSpecification(timeSeriesResolverKey, date);
  }

  /**
   * Gets a user market data specification.
   * 
   * @param snapshotId  the unique identifier of the snapshot, not null
   * @return the user market data specification, not null
   */
  public static UserMarketDataSpecification user(UniqueId snapshotId) {
    return UserMarketDataSpecification.of(snapshotId);
  }

}
