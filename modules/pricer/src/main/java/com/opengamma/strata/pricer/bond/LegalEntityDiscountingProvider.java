/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.SecurityId;

/**
 * A provider of data for bond pricing, based on repo and issuer discounting.
 * <p>
 * This provides the environmental information against which bond pricing occurs,
 * which is the repo and issuer curves. If the bond is inflation linked, the
 * price index data is obtained from {@link RatesProvider}.
 * <p>
 * The standard independent implementation is {@link ImmutableLegalEntityDiscountingProvider}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface LegalEntityDiscountingProvider {

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
   * Gets the discount factors from a repo curve based on the security ID, issuer ID and currency.
   * <p>
   * This searches first for a curve associated with the security iD and currency,
   * and then for a curve associated with the issuer ID and currency.
   * <p>
   * If the valuation date is on or after the specified date, the discount factor is 1.
   * 
   * @param securityId  the standard ID of security to get the discount factors for
   * @param issuerId  the standard ID of legal entity to get the discount factors for
   * @param currency  the currency to get the discount factors for
   * @return the discount factors
   * @throws IllegalArgumentException if the discount factors are not available
   */
  public abstract RepoCurveDiscountFactors repoCurveDiscountFactors(
      SecurityId securityId,
      StandardId issuerId,
      Currency currency);

  /**
   * Gets the discount factors from an issuer based on the issuer ID and currency.
   * <p>
   * This searches for a curve associated with the issuer ID and currency.
   * <p>
   * If the valuation date is on or after the specified date, the discount factor is 1.
   * 
   * @param issuerId  the standard ID to get the discount factors for
   * @param currency  the currency to get the discount factors for
   * @return the discount factors
   * @throws IllegalArgumentException if the discount factors are not available
   */
  public abstract IssuerCurveDiscountFactors issuerCurveDiscountFactors(StandardId issuerId, Currency currency);

  //-------------------------------------------------------------------------
  /**
   * Computes the parameter sensitivity.
   * <p>
   * This computes the {@link CurrencyParameterSensitivities} associated with the {@link PointSensitivities}.
   * This corresponds to the projection of the point sensitivity to the curve internal parameters representation.
   * <p>
   * This method handles {@link RepoCurveZeroRateSensitivity} and {@link IssuerCurveZeroRateSensitivity}. 
   * For other sensitivity objects, see {@link RatesProvider#parameterSensitivity(PointSensitivities)}.
   * 
   * @param pointSensitivities  the point sensitivity
   * @return the sensitivity to the curve parameters
   */
  public abstract CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities);

  //-------------------------------------------------------------------------
  /**
   * Gets market data of a specific type.
   * <p>
   * This is a general purpose mechanism to obtain market data.
   * In general, it is desirable to pass the specific market data needed for pricing into
   * the pricing method. However, in some cases, notably swaps, this is not feasible.
   * It is strongly recommended to clearly state on pricing methods what data is required.
   * 
   * @param <T>  the type of the value
   * @param id  the identifier to find
   * @return the data associated with the key
   * @throws IllegalArgumentException if the data is not available
   */
  public abstract <T> T data(MarketDataId<T> id);

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
  public abstract <T> Optional<T> findData(MarketDataName<T> name);

  //-------------------------------------------------------------------------
  /**
   * Converts this provider to an equivalent {@code ImmutableLegalEntityDiscountingProvider}.
   * 
   * @return the equivalent immutable legal entity provider
   */
  public abstract ImmutableLegalEntityDiscountingProvider toImmutableLegalEntityDiscountingProvider();

}
