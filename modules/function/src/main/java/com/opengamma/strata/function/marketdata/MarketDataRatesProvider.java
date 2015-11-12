/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata;

import java.io.Serializable;
import java.time.LocalDate;

import org.joda.beans.JodaBeanUtils;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.calc.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IborIndexRatesKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.OvernightIndexRatesKey;
import com.opengamma.strata.market.key.PriceIndexValuesKey;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.DiscountFxForwardRates;
import com.opengamma.strata.market.value.DiscountFxIndexRates;
import com.opengamma.strata.market.value.FxForwardRates;
import com.opengamma.strata.market.value.FxIndexRates;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.market.value.OvernightIndexRates;
import com.opengamma.strata.market.value.PriceIndexValues;
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
   * The set of market data for the calculations.
   */
  private final SingleCalculationMarketData marketData;

  /**
   * Creates an instance.
   * 
   * @param marketData  the underlying market data
   */
  public MarketDataRatesProvider(SingleCalculationMarketData marketData) {
    JodaBeanUtils.notNull(marketData, "marketData");
    this.marketData = marketData;
  }

  @Override
  public LocalDate getValuationDate() {
    return marketData.getValuationDate();
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T data(MarketDataKey<T> key) {
    return marketData.getValue(key);
  }

  //-------------------------------------------------------------------------
  // finds the time-series
  private LocalDateDoubleTimeSeries timeSeries(Index index) {
    LocalDateDoubleTimeSeries series = marketData.getTimeSeries(IndexRateKey.of(index));
    if (series == null) {
      return LocalDateDoubleTimeSeries.empty();
    }
    return series;
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    if (baseCurrency.equals(counterCurrency)) {
      return 1d;
    }
    return marketData.getValue(FxRateKey.of(baseCurrency, counterCurrency)).fxRate(baseCurrency, counterCurrency);
  }

  //-------------------------------------------------------------------------
  @Override
  public DiscountFactors discountFactors(Currency currency) {
    return marketData.getValue(DiscountFactorsKey.of(currency));
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexRates fxIndexRates(FxIndex index) {
    LocalDateDoubleTimeSeries timeSeries = timeSeries(index);
    FxForwardRates fxForwardRates = fxForwardRates(index.getCurrencyPair());
    return DiscountFxIndexRates.of(index, timeSeries, fxForwardRates);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxForwardRates fxForwardRates(CurrencyPair currencyPair) {
    DiscountFactors base = discountFactors(currencyPair.getBase());
    DiscountFactors counter = discountFactors(currencyPair.getCounter());
    FxRate fxRate = FxRate.of(currencyPair, fxRate(currencyPair));
    return DiscountFxForwardRates.of(currencyPair, fxRate, base, counter);
  };

  //-------------------------------------------------------------------------
  @Override
  public IborIndexRates iborIndexRates(IborIndex index) {
    return marketData.getValue(IborIndexRatesKey.of(index));
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightIndexRates overnightIndexRates(OvernightIndex index) {
    return marketData.getValue(OvernightIndexRatesKey.of(index));
  }

  //-------------------------------------------------------------------------
  @Override
  public PriceIndexValues priceIndexValues(PriceIndex index) {
    return marketData.getValue(PriceIndexValuesKey.of(index));
  }

}
