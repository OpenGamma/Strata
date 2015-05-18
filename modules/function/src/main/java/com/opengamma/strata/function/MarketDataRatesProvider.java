/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;

import java.io.Serializable;
import java.time.LocalDate;

import org.joda.beans.JodaBeanUtils;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.market.curve.DiscountFactorCurve;
import com.opengamma.strata.market.curve.DiscountFxIndexCurve;
import com.opengamma.strata.market.curve.DiscountIborIndexCurve;
import com.opengamma.strata.market.curve.DiscountOvernightIndexCurve;
import com.opengamma.strata.market.curve.FxIndexCurve;
import com.opengamma.strata.market.curve.IborIndexCurve;
import com.opengamma.strata.market.curve.OvernightIndexCurve;
import com.opengamma.strata.market.curve.ZeroRateDiscountFactorCurve;
import com.opengamma.strata.market.key.DiscountingCurveKey;
import com.opengamma.strata.market.key.FxRateKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.RateIndexCurveKey;
import com.opengamma.strata.pricer.rate.AbstractRatesProvider;

/**
 * A rates provider based on market data from the engine.
 * <p>
 * This provides the environmental information against which pricing occurs.
 * This includes FX rates, discount factors and forward curves.
 */
public final class MarketDataRatesProvider
    extends AbstractRatesProvider
    implements Serializable {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The day count applicable to the models.
   */
  private final DayCount dayCount = ACT_ACT_ISDA;
  /**
   * The set of market data for the calculations.
   */
  private final SingleCalculationMarketData marketData;

  public MarketDataRatesProvider(SingleCalculationMarketData marketData) {
    JodaBeanUtils.notNull(marketData, "marketData");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.marketData = marketData;
  }

  @Override
  public LocalDate getValuationDate() {
    return marketData.getValuationDate();
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T data(Class<T> type) {
    throw new IllegalArgumentException("Unknown type: " + type.getName());
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
    ArgChecker.notNull(index, "index");
    LocalDateDoubleTimeSeries series = marketData.getTimeSeries(IndexRateKey.of(index));
    if (series == null) {
      throw new IllegalArgumentException("Unknown index: " + index.getName());
    }
    return series;
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    ArgChecker.notNull(baseCurrency, "baseCurrency");
    ArgChecker.notNull(counterCurrency, "counterCurrency");
    if (baseCurrency.equals(counterCurrency)) {
      return 1d;
    }
    return marketData.getValue(FxRateKey.of(baseCurrency, counterCurrency));
  }

  //-------------------------------------------------------------------------
  @Override
  public DiscountFactorCurve discountCurve(Currency currency) {
    YieldCurve yieldCurve = marketData.getValue(DiscountingCurveKey.of(currency));
    return ZeroRateDiscountFactorCurve.of(currency, getValuationDate(), dayCount, yieldCurve);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexCurve fxIndexCurve(FxIndex index) {
    CurrencyPair pair = index.getCurrencyPair();
    DiscountFactorCurve base = discountCurve(pair.getBase());
    DiscountFactorCurve counter = discountCurve(pair.getCounter());
    FxRate fxRate = FxRate.of(pair, fxRate(pair));
    return DiscountFxIndexCurve.of(index, timeSeries(index), fxRate, base, counter);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborIndexCurve iborIndexCurve(IborIndex index) {
    LocalDateDoubleTimeSeries timeSeries = timeSeries(index);
    YieldCurve curve = marketData.getValue(RateIndexCurveKey.of(index));
    DiscountFactorCurve dfc = ZeroRateDiscountFactorCurve.of(index.getCurrency(), getValuationDate(), dayCount, curve);
    return DiscountIborIndexCurve.of(index, timeSeries, dfc);
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightIndexCurve overnightIndexCurve(OvernightIndex index) {
    LocalDateDoubleTimeSeries timeSeries = timeSeries(index);
    YieldCurve curve = marketData.getValue(RateIndexCurveKey.of(index));
    DiscountFactorCurve dfc = ZeroRateDiscountFactorCurve.of(index.getCurrency(), getValuationDate(), dayCount, curve);
    return DiscountOvernightIndexCurve.of(index, timeSeries, dfc);
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(LocalDate date) {
    ArgChecker.notNull(date, "date");
    return (date.isBefore(marketData.getValuationDate()) ?
        -dayCount.yearFraction(date, marketData.getValuationDate()) :
        dayCount.yearFraction(marketData.getValuationDate(), date));
  }
}
