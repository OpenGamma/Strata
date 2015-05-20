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
import com.opengamma.strata.market.curve.DiscountFactors;
import com.opengamma.strata.market.curve.DiscountFxIndexRates;
import com.opengamma.strata.market.curve.DiscountIborIndexRates;
import com.opengamma.strata.market.curve.DiscountOvernightIndexRates;
import com.opengamma.strata.market.curve.FxIndexRates;
import com.opengamma.strata.market.curve.IborIndexRates;
import com.opengamma.strata.market.curve.OvernightIndexRates;
import com.opengamma.strata.market.curve.ZeroRateDiscountFactors;
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
  public DiscountFactors discountFactors(Currency currency) {
    YieldCurve yieldCurve = marketData.getValue(DiscountingCurveKey.of(currency));
    return ZeroRateDiscountFactors.of(currency, getValuationDate(), dayCount, yieldCurve);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexRates fxIndexRates(FxIndex index) {
    CurrencyPair pair = index.getCurrencyPair();
    DiscountFactors base = discountFactors(pair.getBase());
    DiscountFactors counter = discountFactors(pair.getCounter());
    FxRate fxRate = FxRate.of(pair, fxRate(pair));
    return DiscountFxIndexRates.of(index, timeSeries(index), fxRate, base, counter);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborIndexRates iborIndexRates(IborIndex index) {
    LocalDateDoubleTimeSeries timeSeries = timeSeries(index);
    YieldCurve curve = marketData.getValue(RateIndexCurveKey.of(index));
    DiscountFactors dfc = ZeroRateDiscountFactors.of(index.getCurrency(), getValuationDate(), dayCount, curve);
    return DiscountIborIndexRates.of(index, timeSeries, dfc);
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightIndexRates overnightIndexRates(OvernightIndex index) {
    LocalDateDoubleTimeSeries timeSeries = timeSeries(index);
    YieldCurve curve = marketData.getValue(RateIndexCurveKey.of(index));
    DiscountFactors dfc = ZeroRateDiscountFactors.of(index.getCurrency(), getValuationDate(), dayCount, curve);
    return DiscountOvernightIndexRates.of(index, timeSeries, dfc);
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
