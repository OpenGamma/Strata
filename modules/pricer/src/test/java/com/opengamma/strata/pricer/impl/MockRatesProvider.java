/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.view.DiscountFactors;
import com.opengamma.strata.market.view.FxForwardRates;
import com.opengamma.strata.market.view.FxIndexRates;
import com.opengamma.strata.market.view.IborIndexRates;
import com.opengamma.strata.market.view.OvernightIndexRates;
import com.opengamma.strata.market.view.PriceIndexValues;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Mock implementation of rate provider.
 * Throws exceptions for most methods.
 */
public class MockRatesProvider
    implements RatesProvider {

  /**
   * The FX rate.
   */
  public static final double RATE = 1.6d;

  /**
   * The valuation date.
   */
  private final LocalDate valuationDate;

  /**
   * Creates an instance.
   */
  public MockRatesProvider() {
    this.valuationDate = null;
  }

  /**
   * Creates an instance.
   * 
   * @param valuationDate  the valuation date
   */
  public MockRatesProvider(LocalDate valuationDate) {
    this.valuationDate = valuationDate;
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T data(MarketDataKey<T> key) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    return baseCurrency.equals(counterCurrency) ? 1 : RATE;
  }

  //-------------------------------------------------------------------------
  @Override
  public DiscountFactors discountFactors(Currency currency) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexRates fxIndexRates(FxIndex index) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public FxForwardRates fxForwardRates(CurrencyPair currencyPair) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public IborIndexRates iborIndexRates(IborIndex index) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightIndexRates overnightIndexRates(OvernightIndex index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PriceIndexValues priceIndexValues(PriceIndex index) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public CurveCurrencyParameterSensitivities curveParameterSensitivity(PointSensitivities pointSensitivities) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MultiCurrencyAmount currencyExposure(PointSensitivities pointSensitivities) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    if (valuationDate == null) {
      throw new UnsupportedOperationException();
    }
    return valuationDate;
  }

  //-------------------------------------------------------------------------
  @Override
  public Optional<Curve> findCurve(CurveName name) {
    return Optional.empty();
  }

  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
    throw new UnsupportedOperationException();
  }

}
