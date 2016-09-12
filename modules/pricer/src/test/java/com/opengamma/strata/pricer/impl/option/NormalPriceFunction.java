/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import java.util.function.Function;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Computes the price of an option in the normally distributed assets hypothesis (Bachelier model).
 */
public final class NormalPriceFunction {
  // this class has been replaced by NormalFormulaRepository
  // it is retained for testing purposes

  /**
   * Gets the price function for the option.
   * 
   * @param option  the option description
   * @return the price function
   */
  public Function<NormalFunctionData, Double> getPriceFunction(EuropeanVanillaOption option) {
    ArgChecker.notNull(option, "option");
    return new Function<NormalFunctionData, Double>() {

      @SuppressWarnings("synthetic-access")
      @Override
      public Double apply(NormalFunctionData data) {
        ArgChecker.notNull(data, "data");
        return data.getNumeraire() *
            NormalFormulaRepository.price(
                data.getForward(),
                option.getStrike(),
                option.getTimeToExpiry(),
                data.getNormalVolatility(),
                option.getPutCall());
      }
    };
  }

  /**
   * Computes the price of an option in the normally distributed assets hypothesis (Bachelier model).
   * The first order price derivatives are also provided.
   * 
   * @param option  the option description
   * @param data  the model data
   * @return a {@link ValueDerivatives} with the price in the value and the derivatives with
   *  respect to [0] the forward, [1] the volatility and [2] the strike
   */
  public ValueDerivatives getPriceAdjoint(EuropeanVanillaOption option, NormalFunctionData data) {
    ArgChecker.notNull(option, "option");
    ArgChecker.notNull(data, "data");
    return NormalFormulaRepository.priceAdjoint(
        data.getForward(),
        option.getStrike(),
        option.getTimeToExpiry(),
        data.getNormalVolatility(),
        data.getNumeraire(),
        option.getPutCall());
  }

  /**
   * Computes forward delta of an option in the normally distributed assets hypothesis (Bachelier model).
   * 
   * @param option  the option description
   * @param data  the model data
   * @return delta
   */
  public double getDelta(EuropeanVanillaOption option, NormalFunctionData data) {
    ArgChecker.notNull(option, "option");
    ArgChecker.notNull(data, "data");
    return data.getNumeraire() *
        NormalFormulaRepository.delta(
            data.getForward(),
            option.getStrike(),
            option.getTimeToExpiry(),
            data.getNormalVolatility(),
            option.getPutCall());
  }

  /**
   * Computes forward gamma of an option in the normally distributed assets hypothesis (Bachelier model).
   * 
   * @param option  the option description
   * @param data  the model data
   * @return gamma
   */
  public double getGamma(EuropeanVanillaOption option, NormalFunctionData data) {
    ArgChecker.notNull(option, "option");
    ArgChecker.notNull(data, "data");
    return data.getNumeraire() *
        NormalFormulaRepository.gamma(
            data.getForward(),
            option.getStrike(),
            option.getTimeToExpiry(),
            data.getNormalVolatility(),
            option.getPutCall());
  }

  /**
   * Computes vega of an option in the normally distributed assets hypothesis (Bachelier model).
   * 
   * @param option  the option description
   * @param data  the model data
   * @return vega
   */
  public double getVega(EuropeanVanillaOption option, NormalFunctionData data) {
    ArgChecker.notNull(option, "option");
    ArgChecker.notNull(data, "data");
    return data.getNumeraire() *
        NormalFormulaRepository.vega(
            data.getForward(),
            option.getStrike(),
            option.getTimeToExpiry(),
            data.getNormalVolatility(),
            option.getPutCall());
  }

  /**
   * Computes theta of an option in the normally distributed assets hypothesis (Bachelier model).
   * 
   * @param option  the option description
   * @param data  the model data
   * @return theta
   */
  public double getTheta(EuropeanVanillaOption option, NormalFunctionData data) {
    ArgChecker.notNull(option, "option");
    ArgChecker.notNull(data, "data");
    return data.getNumeraire() *
        NormalFormulaRepository.theta(
            data.getForward(),
            option.getStrike(),
            option.getTimeToExpiry(),
            data.getNormalVolatility(),
            option.getPutCall());
  }

}
