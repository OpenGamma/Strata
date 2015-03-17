/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import java.time.LocalDate;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.currency.CurrencyPair;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.basics.index.FxIndex;
import com.opengamma.basics.index.IborIndex;
import com.opengamma.basics.index.Index;
import com.opengamma.basics.index.OvernightIndex;
import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Mock implementation of pricing environment.
 * Throws exceptions for all methods.
 */
public class MockPricingEnvironment
    implements PricingEnvironment {

  /**
   * The valuation date.
   */
  private final LocalDate valuationDate;

  /**
   * Creates an instance.
   */
  public MockPricingEnvironment() {
    this.valuationDate = null;
  }

  /**
   * Creates an instance.
   * 
   * @param valuationDate  the valuation date
   */
  public MockPricingEnvironment(LocalDate valuationDate) {
    this.valuationDate = valuationDate;
  }

  //-------------------------------------------------------------------------
  @Override
  @SuppressWarnings("unchecked")
  public <T> T rawData(Class<T> cls) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxRate(CurrencyPair currencyPair) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount fxConvert(MultiCurrencyAmount amount, Currency currency) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public double discountFactor(Currency currency, LocalDate date) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PointSensitivityBuilder discountFactorZeroRateSensitivity(Currency currency, LocalDate date) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxIndexRate(FxIndex index, Currency baseCurrency, LocalDate fixingDate) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public double iborIndexRate(IborIndex index, LocalDate fixingDate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PointSensitivityBuilder iborIndexRateSensitivity(IborIndex index, LocalDate fixingDate) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public double overnightIndexRate(OvernightIndex index, LocalDate fixingDate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PointSensitivityBuilder overnightIndexRateSensitivity(OvernightIndex index, LocalDate fixingDate) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public double overnightIndexRatePeriod(OvernightIndex index, LocalDate startDate, LocalDate endDate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PointSensitivityBuilder overnightIndexRatePeriodSensitivity(OvernightIndex index, LocalDate startDate, LocalDate endDate) {
    throw new UnsupportedOperationException();
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(LocalDate date) {
    throw new UnsupportedOperationException();
  }

  @Override
  public LocalDate getValuationDate() {
    if (valuationDate == null) {
      throw new UnsupportedOperationException();
    }
    return valuationDate;
  }

}
