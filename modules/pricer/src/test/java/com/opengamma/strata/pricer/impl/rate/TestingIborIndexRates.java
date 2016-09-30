/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;

/**
* Test implementation of {@link IborIndexRates}.
*/
class TestingIborIndexRates implements IborIndexRates {

  private final IborIndex index;
  private final LocalDate valuationDate;
  private final LocalDateDoubleTimeSeries rates;
  private final LocalDateDoubleTimeSeries fixings;
  private final IborRateSensitivity sens;

  public TestingIborIndexRates(
      IborIndex index,
      LocalDate valuationDate,
      LocalDateDoubleTimeSeries rates,
      LocalDateDoubleTimeSeries fixings) {

    this.index = index;
    this.valuationDate = valuationDate;
    this.rates = rates;
    this.fixings = fixings;
    this.sens = null;
  }

  public TestingIborIndexRates(
      IborIndex index,
      LocalDate valuationDate,
      LocalDateDoubleTimeSeries rates,
      LocalDateDoubleTimeSeries fixings,
      IborRateSensitivity sens) {

    this.index = index;
    this.valuationDate = valuationDate;
    this.rates = rates;
    this.fixings = fixings;
    this.sens = sens;
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  @Override
  public IborIndex getIndex() {
    return index;
  }

  @Override
  public LocalDateDoubleTimeSeries getFixings() {
    return fixings;
  }

  @Override
  public double rate(IborIndexObservation observation) {
    LocalDate fixingDate = observation.getFixingDate();
    if (fixingDate.equals(valuationDate) && fixings.containsDate(fixingDate)) {
      return fixings.get(fixingDate).getAsDouble();
    }
    return rates.get(fixingDate).getAsDouble();
  }

  @Override
  public PointSensitivityBuilder ratePointSensitivity(IborIndexObservation observation) {
    return sens;
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getParameterCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public double getParameter(int parameterIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IborIndexRates withParameter(int parameterIndex, double newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IborIndexRates withPerturbation(ParameterPerturbation perturbation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public double rateIgnoringFixings(IborIndexObservation observation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public PointSensitivityBuilder rateIgnoringFixingsPointSensitivity(IborIndexObservation observation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(IborRateSensitivity pointSensitivity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CurrencyParameterSensitivities createParameterSensitivity(Currency currency, DoubleArray sensitivities) {
    throw new UnsupportedOperationException();
  }

}
