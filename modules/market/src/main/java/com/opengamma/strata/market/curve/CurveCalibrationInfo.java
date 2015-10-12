/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

/**
 * Information about curve calibration.
 * <p>
 * Implementations of this interface provide information that was obtained during curve calibration.
 */
public interface CurveCalibrationInfo {

  /**
   * Converts this instance to the specified type.
   * <p>
   * This returns an instance, intended to be {@code this}, as the specified type.
   * If the type does not match, {@link ClassCastException} is thrown.
   * This method exists primarily to provide a better error message, however it does
   * allow the potential for more complex conversions.
   * 
   * @param type  the type to cast to
   * @return the cast instance
   * @throws ClassCastException if the type cannot be converted
   */
  public default <T> T convertTo(Class<T> type) {
    return type.cast(this);
  }

}
