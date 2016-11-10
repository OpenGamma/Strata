/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.MarketDataView;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Provides access to rates for a currency pair.
 * <p>
 * This provides forward rates for a single {@link Currency pair}, such as 'EUR/GBP'.
 * The forward rate is the conversion rate between two currencies on a fixing date in the future.
 */
public interface FxForwardRates
    extends MarketDataView, ParameterizedData {

  /**
   * Gets the currency pair.
   * <p>
   * The the currency pair that the forward rates are for.
   * 
   * @return the currency pair
   */
  public abstract CurrencyPair getCurrencyPair();

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this provider is calibrated for this date.
   * 
   * @return the valuation date
   */
  @Override
  public abstract LocalDate getValuationDate();

  @Override
  public abstract FxForwardRates withParameter(int parameterIndex, double newValue);

  @Override
  public abstract FxForwardRates withPerturbation(ParameterPerturbation perturbation);

  //-------------------------------------------------------------------------
  /**
   * Gets the forward rate at the specified payment date.
   * <p>
   * The exchange rate of the currency pair varies over time.
   * This method obtains the estimated rate for the payment date.
   * <p>
   * This method specifies which of the two currencies in the currency pair is to be treated
   * as the base currency for the purposes of the returned rate.
   * If the specified base currency equals the base currency of the currency pair, then
   * the rate is simply returned. If the specified base currency equals the counter currency
   * of the currency pair, then the inverse rate is returned.
   * As such, an amount in the specified base currency can be directly multiplied by the
   * returned FX rate to perform FX conversion.
   * <p>
   * To convert an amount in the specified base currency to the other currency,
   * multiply it by the returned FX rate.
   * 
   * @param baseCurrency  the base currency that the rate should be expressed against
   * @param referenceDate  the date to query the rate for
   * @return the forward rate of the currency pair
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double rate(Currency baseCurrency, LocalDate referenceDate);

  /**
   * Calculates the point sensitivity of the forward rate at the specified payment date.
   * <p>
   * This returns a sensitivity instance referring to the points that were queried in the market data.
   * The sensitivity refers to the result of {@link #rate(Currency, LocalDate)}.
   * 
   * @param baseCurrency  the base currency that the rate should be expressed against
   * @param referenceDate  the date to find the sensitivity for
   * @return the point sensitivity of the rate
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract PointSensitivityBuilder ratePointSensitivity(Currency baseCurrency, LocalDate referenceDate);

  /**
   * Calculates the sensitivity of the forward rate to the current FX rate.
   * <p>
   * This returns the sensitivity to the current FX rate that was used to determine the FX forward rate.
   * The sensitivity refers to the result of {@link #rate(Currency, LocalDate)}.
   * 
   * @param baseCurrency  the base currency that the rate should be expressed against
   * @param referenceDate  the date to find the sensitivity for
   * @return the sensitivity of the FX forward rate to the current FX rate
   * @throws RuntimeException if the value cannot be obtained
   */
  public abstract double rateFxSpotSensitivity(Currency baseCurrency, LocalDate referenceDate);

  //-------------------------------------------------------------------------
  /**
   * Calculates the parameter sensitivity from the point sensitivity.
   * <p>
   * This is used to convert a single point sensitivity to parameter sensitivity.
   * 
   * @param pointSensitivity  the point sensitivity to convert
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract CurrencyParameterSensitivities parameterSensitivity(FxForwardSensitivity pointSensitivity);

  /**
   * Calculates the currency exposure from the point sensitivity.
   * <p>
   * This is used to convert a single point sensitivity to currency exposure.
   * 
   * @param pointSensitivity  the point sensitivity to convert
   * @return the currency exposure
   * @throws RuntimeException if the result cannot be calculated
   */
  public abstract MultiCurrencyAmount currencyExposure(FxForwardSensitivity pointSensitivity);

}
