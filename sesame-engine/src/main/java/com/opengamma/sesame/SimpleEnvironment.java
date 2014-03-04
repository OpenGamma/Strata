/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.joda.beans.PropertyDefinition;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.ArgumentChecker;

/**
 * Simple immutable {@link Environment} implementation.
 */
public final class SimpleEnvironment implements Environment {

  /** The valuation time. */
  @PropertyDefinition(validate = "notNull")
  private final ZonedDateTime _valuationTime;

  /** The function that provides market data. */
  @PropertyDefinition(validate = "notNull")
  private final MarketDataSource _marketDataSource;

  @Override
  public LocalDate getValuationDate() {
    return _valuationTime.toLocalDate();
  }

  @Override
  public ZonedDateTime getValuationTime() {
    return _valuationTime;
  }

  @Override
  public MarketDataSource getMarketDataSource() {
    return _marketDataSource;
  }

  @Override
  public Environment withValuationTime(ZonedDateTime valuationTime) {
    return new SimpleEnvironment(ArgumentChecker.notNull(valuationTime, "valuationTime"), _marketDataSource);
  }

  @Override
  public Environment withMarketData(MarketDataSource marketData) {
    return new SimpleEnvironment(_valuationTime, ArgumentChecker.notNull(marketData, "marketData"));
  }

  @Override
  public Environment with(ZonedDateTime valuationTime, MarketDataSource marketData) {
    return new SimpleEnvironment(valuationTime, marketData);
  }

  public SimpleEnvironment(ZonedDateTime valuationTime, MarketDataSource marketDataSource) {
    _valuationTime = ArgumentChecker.notNull(valuationTime, "valuationTime");
    _marketDataSource = ArgumentChecker.notNull(marketDataSource, "marketDataSource");
  }
}
