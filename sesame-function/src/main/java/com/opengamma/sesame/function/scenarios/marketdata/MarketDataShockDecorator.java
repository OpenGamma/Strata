/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.marketdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.PointsCurveNodeWithIdentifier;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Function that decorates {@link MarketDataFn} and applies shocks to the underlying market data.
 */
public class MarketDataShockDecorator implements MarketDataFn {

  private static final Logger s_logger = LoggerFactory.getLogger(MarketDataShockDecorator.class);

  /** The underlying market data function that this function decorates. */
  private final MarketDataFn _delegate;

  /**
   * @param delegate the function to decorate
   */
  public MarketDataShockDecorator(MarketDataFn delegate) {
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
  }

  @Override
  public Result<Double> getFxRate(Environment env, CurrencyPair currencyPair) {
    return _delegate.getFxRate(decorateDataSource(env), currencyPair);
  }

  @Override
  public Result<Double> getCurveNodeValue(Environment env, CurveNodeWithIdentifier node) {
    return _delegate.getCurveNodeValue(decorateDataSource(env), node);
  }

  @Override
  public Result<Double> getCurveNodeUnderlyingValue(Environment env, PointsCurveNodeWithIdentifier node) {
    return _delegate.getCurveNodeUnderlyingValue(decorateDataSource(env), node);
  }

  @Override
  public Result<Double> getMarketValue(Environment env, ExternalIdBundle id) {
    return _delegate.getMarketValue(decorateDataSource(env), id);
  }

  @Override
  public Result<?> getValue(Environment env, ExternalIdBundle id, FieldName fieldName) {
    return _delegate.getValue(decorateDataSource(env), id, fieldName);
  }

  private Environment decorateDataSource(Environment env) {
    Object arg = env.getScenarioArgument(this);

    if (arg == null) {
      s_logger.debug("null scenario argument");
      return env;
    }
    if (!(arg instanceof Shocks)) {
      s_logger.warn("Unexpected scenario argument for MarketDataShockDecorator, expected Shocks, got {}", arg);
      return env;
    }
    return env.withMarketData(new DataSourceDecorator(env.getMarketDataSource(), (Shocks) arg));
  }

  private class DataSourceDecorator implements MarketDataSource {

    /** The decorated data source. */
    private final MarketDataSource _delegate;

    /** The shocks to apply to the market data. */
    private final Shocks _shocks;

    private DataSourceDecorator(MarketDataSource delegate, Shocks shocks) {
      _delegate = delegate;
      _shocks = shocks;
    }

    @Override
    public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
      Result<?> result = _delegate.get(id, fieldName);

      if (!result.isSuccess()) {
        return result;
      }
      Object value = result.getValue();

      if (!(value instanceof Double)) {
        return Result.failure(FailureStatus.ERROR, "Market data shocks can only be applied to double values. Value " +
            "for {} is of type {}, value {}", id, value.getClass().getName(), value);
      }
      double shockedValue = (double) value;

      for (MarketDataShock shock : _shocks._shockList) {
        shockedValue = shock.apply(id, shockedValue);
      }
      return Result.success(shockedValue);
    }
  }

  /**
   * Creates an instance of shocks from some market data shocks.
   *
   * @param shocks the market data shocks
   * @return a {@link Shocks} instance wrapping the shocks
   */
  public static Shocks shocks(MarketDataShock... shocks) {
    List<MarketDataShock> shockList = new ArrayList<>(shocks.length);

    for (MarketDataShock shock : shocks) {
      if (shock == null) {
        throw new IllegalArgumentException("Null shocks not allowed");
      }
      shockList.add(shock);
    }
    return new Shocks(Collections.unmodifiableList(shockList));
  }

  /**
   * Wraps a list of {@link MarketDataShock} instances.
   */
  public static final class Shocks {

    private final List<MarketDataShock> _shockList;

    private Shocks(List<MarketDataShock> shockList) {
      _shockList = shockList;
    }
  }
}


