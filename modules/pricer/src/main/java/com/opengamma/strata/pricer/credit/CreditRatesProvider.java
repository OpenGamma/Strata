/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * The rates provider, used to calculate analytic measures.
 * <p>
 * The primary usage of this provider is to price credit default swaps on a legal entity.
 * This includes credit curves, discounting curves and recovery rate curves.
 */
public interface CreditRatesProvider {

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this provider is calibrated for this date.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

  //-------------------------------------------------------------------------
  /**
  * Gets the survival probabilities for a standard ID and a currency.
  * <p>
  * If both the standard ID and currency are matched, the relevant {@code LegalEntitySurvivalProbabilities} is returned. 
  * <p>
  * If the valuation date is on the specified date, the survival probability is 1.
  * 
  * @param legalEntityId  the standard ID of legal entity to get the discount factors for
  * @param currency  the currency to get the discount factors for
  * @return the survival probabilities 
  * @throws IllegalArgumentException if the survival probabilities are not available
  */
  public abstract LegalEntitySurvivalProbabilities survivalProbabilities(StandardId legalEntityId, Currency currency);

  /**
  * Gets the discount factors for a currency. 
  * <p>
  * The discount factor represents the time value of money for the specified currency 
  * when comparing the valuation date to the specified date. 
  * <p>
  * If the valuation date is on the specified date, the discount factor is 1.
  * 
  * @param currency  the currency to get the discount factors for
  * @return the discount factors for the specified currency
  */
  public abstract CreditDiscountFactors discountFactors(Currency currency);

  /**
  * Gets the recovery rates for a standard ID.
  * <p>
  * If both the standard ID and currency are matched, the relevant {@code RecoveryRates} is returned. 
  * 
  * @param legalEntityId  the standard ID of legal entity to get the discount factors for
  * @return the recovery rates
  * @throws IllegalArgumentException if the recovery rates are not available
  */
  public abstract RecoveryRates recoveryRates(StandardId legalEntityId);

  //-------------------------------------------------------------------------
  /**
  * Computes the parameter sensitivity.
  * <p>
  * This computes the {@link CurrencyParameterSensitivities} associated with the {@link PointSensitivities}.
  * This corresponds to the projection of the point sensitivity to the curve internal parameters representation.
  * <p>
  * The sensitivities handled here are {@link CreditCurveZeroRateSensitivity}, {@link ZeroRateSensitivity}. 
  * For the other sensitivity objects, use {@link RatesProvider} instead.
  * 
  * @param pointSensitivities  the point sensitivity
  * @return the sensitivity to the curve parameters
  */
  public abstract CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities);

  /**
   * Computes the parameter sensitivity for a specific credit curve.
   * <p>
   * The credit curve is specified by {@code legalEntityId} and {@code currency}.
   * 
   * @param pointSensitivities  the point sensitivity
   * @param legalEntityId  the legal entity
   * @param currency  the currency
   * @return the sensitivity to the curve parameters
   */
  public abstract CurrencyParameterSensitivity singleCreditCurveParameterSensitivity(
      PointSensitivities pointSensitivities,
      StandardId legalEntityId,
      Currency currency);

  /**
   * Computes the parameter sensitivity for a specific discount curve.
   * <p>
   * The discount curve is specified by {@code currency}.
   * 
   * @param pointSensitivities  the point sensitivity
   * @param currency  the currency
   * @return the sensitivity to the curve parameters
   */
  public abstract CurrencyParameterSensitivity singleDiscountCurveParameterSensitivity(
      PointSensitivities pointSensitivities,
      Currency currency);

  //-------------------------------------------------------------------------
  /**
  * Finds the market data with the specified name.
  * <p>
  * This is most commonly used to find a {@link Curve} using a {@link CurveName}.
  * If the market data cannot be found, empty is returned.
  * 
  * @param <T>  the type of the market data value
  * @param name  the name to find
  * @return the market data value, empty if not found
  */
  public <T> Optional<T> findData(MarketDataName<T> name);

  //-------------------------------------------------------------------------
  /**
   * Converts this provider to an equivalent {@code ImmutableCreditRatesProvider}.
   * 
   * @return the equivalent immutable legal entity provider
   */
  public abstract ImmutableCreditRatesProvider toImmutableCreditRatesProvider();

}
