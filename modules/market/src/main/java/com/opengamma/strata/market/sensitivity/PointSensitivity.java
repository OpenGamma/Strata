/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;

/**
 * Point sensitivity.
 * <p>
 * The sensitivity to a single point on a curve, surface or similar.
 * This is used within {@link PointSensitivities}.
 * <p>
 * Each implementation of this interface will consist of two distinct parts.
 * The first is a set of information that identifies the point.
 * The second is the sensitivity value.
 * <p>
 * For example, when an Ibor index is queried, the implementation would typically contain
 * the Ibor index, fixing date and the sensitivity value.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface PointSensitivity
    extends FxConvertible<PointSensitivity> {

  /**
   * Gets the currency of the point sensitivity.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  /**
   * Gets the point sensitivity value.
   * 
   * @return the sensitivity
   */
  public abstract double getSensitivity();

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified sensitivity currency set.
   * <p>
   * The result will consists of the same points, but with the sensitivity currency altered.
   * 
   * @param currency  the new currency
   * @return an instance based on this sensitivity with the specified currency
   */
  public abstract PointSensitivity withCurrency(Currency currency);

  /**
   * Returns an instance with the new point sensitivity value.
   * 
   * @param sensitivity  the new sensitivity
   * @return an instance based on this sensitivity with the specified sensitivity
   */
  public abstract PointSensitivity withSensitivity(double sensitivity);

  /**
   * Compares the key of two sensitivities, excluding the point sensitivity value.
   * <p>
   * If the other point sensitivity is of a different type, the comparison
   * is based solely on the simple class name.
   * If the point sensitivity is of the same type, the comparison must
   * check the key, then the currency, then the date, then any other state.
   * <p>
   * The comparison by simple class name ensures that all instances of the same
   * type are ordered together.
   * 
   * @param other  the other sensitivity
   * @return positive if greater, zero if equal, negative if less
   */
  public abstract int compareKey(PointSensitivity other);

  /**
   * Converts this instance to an equivalent amount in the specified currency.
   * <p>
   * The result will be expressed in terms of the given currency.
   * Any FX conversion that is required will use rates from the provider.
   * 
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the converted instance, which should be expressed in the specified currency
   * @throws RuntimeException if no FX rate could be found
   */
  @Override
  public default PointSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    if (getCurrency().equals(resultCurrency)) {
      return this;
    }
    double fxRate = rateProvider.fxRate(getCurrency(), resultCurrency);
    return withCurrency(resultCurrency).withSensitivity(fxRate * getSensitivity());
  }

}
