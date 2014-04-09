/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.swaption;

import com.opengamma.analytics.financial.interestrate.PresentValueSABRSensitivityDataBundle;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.financial.security.option.SwaptionSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Calculate analytics values for a swaption.
 */
public interface SwaptionFn {

  /**
   * Calculate the present value for a Swaption security.
   *
   * @param env the environment used for calculation
   * @param security the Swaption to calculate the PV for
   * @return result containing the present value if successful, a Failure otherwise
   */
  @Output(value = OutputNames.PRESENT_VALUE)
  Result<MultipleCurrencyAmount> calculatePV(Environment env, SwaptionSecurity security);

  /**
   * Calculate the implied volatility for a Swaption security.
   *
   * @param security the Swaption to calculate the implied volatility for
   * @return result containing the implied volatility if successful, a Failure otherwise
   */
  @Output(value = OutputNames.IMPLIED_VOLATILITY)
  Result<Double> calculateImpliedVolatility(Environment env, SwaptionSecurity security);

  /**
   * Calculate the bucketed PV01 for a Swaption security.
   *
   * @param env the environment used for calculation
   * @param security the Swaption to calculate the bucketed PV01 for
   * @return result containing the bucketed PV01 if successful, a Failure otherwise
   */
  @Output(value = OutputNames.BUCKETED_PV01)
  Result<MultipleCurrencyParameterSensitivity> calculateBucketedPV01(Environment env, SwaptionSecurity security);

  /**
   * Calculate the bucketed SABR risk for a Swaption security.
   *
   * @param env the environment used for calculation
   * @param security the Swaption to calculate the bucketed SABR risk for
   * @return result containing the bucketed SABR risk if successful, a Failure otherwise
   */
  // todo - maybe split into the individual elements (alpha, beta, rho, nu risk)?
  // todo - this is obviously SABR specific -shouldn't be on a general interface?
  @Output(value = OutputNames.BUCKETED_SABR_RISK)
  Result<PresentValueSABRSensitivityDataBundle> calculateBucketedSABRRisk(Environment env, SwaptionSecurity security);
}
