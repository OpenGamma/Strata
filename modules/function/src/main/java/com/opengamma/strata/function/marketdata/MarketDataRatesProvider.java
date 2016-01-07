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
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.OvernightIndexCurveKey;
import com.opengamma.strata.market.key.PriceIndexCurveKey;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.DiscountFxForwardRates;
import com.opengamma.strata.market.value.DiscountFxIndexRates;
import com.opengamma.strata.market.value.ForwardPriceIndexValues;
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
  private final MarketData marketData;

  /**
   * Creates an instance.
   * 
   * @param marketData  the underlying market data
   */
  public MarketDataRatesProvider(MarketData marketData) {
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
    return marketData.getTimeSeries(IndexRateKey.of(index));
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
    Curve curve = marketData.getValue(DiscountCurveKey.of(currency));
    return DiscountFactors.of(currency, getValuationDate(), curve);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexRates fxIndexRates(FxIndex index) {
    FxForwardRates fxForwardRates = fxForwardRates(index.getCurrencyPair());
    return DiscountFxIndexRates.of(index, fxForwardRates, timeSeries(index));
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
    Curve curve = marketData.getValue(IborIndexCurveKey.of(index));
    return IborIndexRates.of(index, getValuationDate(), curve, timeSeries(index));
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightIndexRates overnightIndexRates(OvernightIndex index) {
    Curve curve = marketData.getValue(OvernightIndexCurveKey.of(index));
    return OvernightIndexRates.of(index, getValuationDate(), curve, timeSeries(index));
  }

  //-------------------------------------------------------------------------
  @Override
  public PriceIndexValues priceIndexValues(PriceIndex index) {
    Curve curve = marketData.getValue(PriceIndexCurveKey.of(index));
    if (!(curve instanceof InterpolatedNodalCurve)) {
      throw new IllegalArgumentException("Curve must be an InterpolatedNodalCurve: " + index);
    }
    return ForwardPriceIndexValues.of(index, getValuationDate(), (InterpolatedNodalCurve) curve, timeSeries(index));
  }

}
