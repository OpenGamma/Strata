/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.basics.currency.Currency;

/**
 * Point sensitivity.
 * <p>
 * The sensitivity to a single point on a curve, surface or similar.
 * This is used within {@link PointSensitivities}.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface PointSensitivity
    extends ImmutableBean {

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
   * Compares two sensitivities, excluding the point sensitivity value.
   * <p>
   * If the other point sensitivity is of a different type, the comparison
   * must be based solely on the simple class name.
   * If the point sensitivity is of the same type, the comparison must
   * check the key, then the currency, then the date, then any other state.
   * <p>
   * The comparison by simple class name ensures that all instances of the same
   * type are ordered together.
   * 
   * @param other  the other sensitivity
   * @return positive if greater, zero if equal, negative if less
   */
  public abstract int compareExcludingSensitivity(PointSensitivity other);

}
