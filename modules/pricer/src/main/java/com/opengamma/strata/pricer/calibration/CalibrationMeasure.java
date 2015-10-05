/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Provides access to the measures needed to perform curve calibration for a single type of trade.
 * <p>
 * The most commonly used measures are par spread and converted present value.
 * <p>
 * See {@link TradeCalibrationMeasure} for constants defining measures for common trade types.
 * 
 * @param <T> the trade type
 */
public interface CalibrationMeasure<T extends Trade> {

  /**
   * Gets the trade type of the calibrator.
   * 
   * @return the trade type
   */
  public abstract Class<T> getTradeType();

  /**
   * Calculates the value, such as par spread.
   * <p>
   * The value must be calculated using the specified rates provider.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the sensitivity
   * @throws IllegalArgumentException if the trade cannot be valued
   */
  public abstract double value(T trade, ImmutableRatesProvider provider);

  /**
   * Calculates the parameter sensitivities that relate to the value.
   * <p>
   * The sensitivities must be calculated using the specified rates provider.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the sensitivity
   * @throws IllegalArgumentException if the trade cannot be valued
   */
  public abstract CurveCurrencyParameterSensitivities sensitivities(T trade, ImmutableRatesProvider provider);

}
