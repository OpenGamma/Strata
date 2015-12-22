/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Volatility for swaptions in the log-normal or Black model.
 */
public interface BlackSwaptionVolatilities {

  /**
   * Gets the convention of the swap for which the data is valid.
   * 
   * @return the convention
   */
  public FixedIborSwapConvention getConvention();

  /**
   * Gets the valuation date-time.
   * <p>
   * The raw data in this provider is calibrated for this date-time.
   * 
   * @return the valuation date-time
   */
  public ZonedDateTime getValuationDateTime();

  //-------------------------------------------------------------------------
  /**
   * Gets the volatility at the specified date-time.
   * 
   * @param expiryDate  the option expiry
   * @param tenor  the swaption tenor in years
   * @param strike  the option strike rate
   * @param forwardRate  the forward rate of the underlying swap
   * @return the volatility
   * @throws RuntimeException if the value cannot be obtained
   */
  public double volatility(ZonedDateTime expiryDate, double tenor, double strike, double forwardRate);

  /**
   * Calculates the surface parameter sensitivity from the point sensitivity.
   * <p>
   * This is used to convert a single point sensitivity to surface parameter sensitivity.
   * 
   * @param pointSensitivity  the point sensitivity to convert
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public SurfaceCurrencyParameterSensitivity surfaceCurrencyParameterSensitivity(SwaptionSensitivity pointSensitivity);

  //-------------------------------------------------------------------------
  /**
   * Converts a time and date to a relative year fraction. 
   * <p>
   * When the date is after the valuation date (and potentially time), the returned number is negative.
   * 
   * @param date  the date/time to find the relative year fraction of
   * @return the relative year fraction
   */
  public abstract double relativeTime(ZonedDateTime date);

  /**
   * Calculates the tenor of the swap based on its start date and end date.
   * 
   * @param startDate  the start date
   * @param endDate  the end date
   * @return the tenor
   */
  public abstract double tenor(LocalDate startDate, LocalDate endDate);

}
