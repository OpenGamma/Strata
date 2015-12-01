/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.MarketDataValue;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.key.FxIndexRatesKey;
import com.opengamma.strata.market.sensitivity.FxIndexSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides access to rates for an FX index.
 * <p>
 * This provides historic and forward rates for a single {@link FxIndex}, such as 'EUR/GBP-ECB'.
 * An FX rate is the conversion rate between two currencies. An FX index is the rate
 * as published by a specific organization, typically at a well-known time-of-day.
 */
public interface FxIndexRates
    extends MarketDataValue<FxIndexRates> {

  /**
   * Gets the market data key.
   * <p>
   * This returns the {@link FxIndexRatesKey} that identifies this instance.
   * 
   * @return the market data key
   */
  @Override
  public default FxIndexRatesKey getKey() {
    return FxIndexRatesKey.of(getIndex());
  }
  /**
   * Gets the FX index.
   * <p>
   * The index that the rates are for.
   * 
   * @return the FX index
   */
  public abstract FxIndex getIndex();

  /**
   * Gets the time-series of fixings for the index.
   * <p>
   * The time-series contains historic fixings of the index.
   * It may be empty if the data is not available.
   * 
   * @return the time-series fixings
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries();

  /**
   * Gets the underlying FX forward rates.  
   * 
   * @return the FX forward rates
   */
  public abstract FxForwardRates getFxForwardRates();

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate at the specified fixing date.
   * <p>
   * The rate of the FX index varies over time.
   * This method obtains the actual or estimated rate for the fixing date.
   * <p>
   * This retrieves the actual rate if the fixing date is before the valuation date,
   * or the estimated rate if the fixing date is after the valuation date.
   * If the fixing date equals the valuation date, then the best available rate is returned.
   * <p>
   * The index defines the conversion rate for a specific currency pair.
   * This method specifies which of the two currencies in the index is to be treated
   * as the base currency for the purposes of the returned rate.
   * If the specified base currency equals the base currency of the index, then
   * the rate is simply returned. If the specified base currency equals the counter currency
   * of the index, then the inverse rate is returned.
   * As such, an amount in the specified base currency can be directly multiplied by the
   * returned FX rate to perform FX conversion.
   * <p>
   * To convert an amount in the specified base currency to the other currency,
   * multiply it by the returned FX rate.
   * 
   * @param baseCurrency  the base currency that the rate should be expressed against
   * @param fixingDate  the fixing date to query the rate for
   * @return the rate of the index, either historic or forward
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double rate(Currency baseCurrency, LocalDate fixingDate);

  /**
   * Calculates the point sensitivity of the historic or forward rate at the specified fixing date.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * If a time-series was used, then there is no sensitivity.
   * The sensitivity refers to the result of {@link #rate(Currency, LocalDate)}.
   * 
   * @param baseCurrency  the base currency that the rate should be expressed against
   * @param fixingDate  the fixing date to find the sensitivity for
   * @return the point sensitivity of the rate
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract PointSensitivityBuilder ratePointSensitivity(Currency baseCurrency, LocalDate fixingDate);

  //-------------------------------------------------------------------------
  /**
   * Calculates the curve parameter sensitivity from the point sensitivity.
   * <p>
   * This is used to convert a single point sensitivity to curve parameter sensitivity.
   * 
   * @param pointSensitivity  the point sensitivity to convert
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract CurveCurrencyParameterSensitivities curveParameterSensitivity(FxIndexSensitivity pointSensitivity);

  /**
   * Calculates the currency exposure from the point sensitivity.
   * <p>
   * This is used to convert a single point sensitivity to currency exposure.
   * 
   * @param pointSensitivity  the point sensitivity to convert
   * @return the currency exposure
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract MultiCurrencyAmount currencyExposure(FxIndexSensitivity pointSensitivity);

}
