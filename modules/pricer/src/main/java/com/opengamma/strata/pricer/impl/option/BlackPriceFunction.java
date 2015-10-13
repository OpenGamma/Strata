/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;

/**
 * Computes the price of an option in the Black model.
 */
public final class BlackPriceFunction {

  /**
   * The comparison value used to determine near-zero.
   */
  private static final double NEAR_ZERO = 1e-16;
  /**
   * The normal distribution implementation.
   */
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);

  /**
   * Gets the price function for the option.
   * 
   * @param option  the option description
   * @return the price function
   */
  public Function1D<BlackFunctionData, Double> getPriceFunction(final EuropeanVanillaOption option) {
    ArgChecker.notNull(option, "option");
    double k = option.getStrike();
    double t = option.getTimeToExpiry();
    boolean isCall = option.isCall();
    return new Function1D<BlackFunctionData, Double>() {
      @Override
      public Double evaluate(BlackFunctionData data) {
        ArgChecker.notNull(data, "data");
        double forward = data.getForward();
        double sigma = data.getBlackVolatility();
        double df = data.getNumeraire();
        return df * BlackFormulaRepository.price(forward, k, t, sigma, isCall);
      }
    };
  }

  /**
   * Computes the Black price and its derivatives.
   * 
   * @param option  the option description
   * @param data  the model data
   * @return a {@link ValueDerivatives} with the price in the value and the derivatives with
   *  respect to [0] the forward, [1] the volatility and [2] the strike
   */
  public ValueDerivatives getPriceAdjoint(EuropeanVanillaOption option, BlackFunctionData data) {
    double strike = option.getStrike();
    double timeToExpiry = option.getTimeToExpiry();
    double vol = data.getBlackVolatility();
    double forward = data.getForward();
    boolean isCall = option.isCall();
    double discountFactor = data.getNumeraire();
    double sqrttheta = Math.sqrt(timeToExpiry);
    double omega = isCall ? 1 : -1;
    // Implementation Note: Forward sweep.
    double volblack = 0, kappa = 0, d1 = 0, d2 = 0;
    double x = 0;
    double price;
    if (strike < NEAR_ZERO || sqrttheta < NEAR_ZERO) {
      x = omega * (forward - strike);
      price = (x > 0 ? discountFactor * x : 0.0);
    } else {
      volblack = vol * sqrttheta;
      kappa = Math.log(forward / strike) / volblack - 0.5 * volblack;
      d1 = NORMAL.getCDF(omega * (kappa + volblack));
      d2 = NORMAL.getCDF(omega * kappa);
      price = discountFactor * omega * (forward * d1 - strike * d2);
    }
    // Implementation Note: Backward sweep.
    double pBar = 1.0;
    double forwardBar = 0, strikeBar = 0, volblackBar = 0, volatilityBar = 0;
    if (strike < NEAR_ZERO || sqrttheta < NEAR_ZERO) {
      forwardBar = (x > 0 ? discountFactor * omega : 0.0);
      strikeBar = (x > 0 ? -discountFactor * omega : 0.0);
    } else {
      double d1Bar = discountFactor * omega * forward * pBar;
      double density1 = NORMAL.getPDF(omega * (kappa + volblack));
      // Implementation Note: kappa_bar = 0; no need to implement it.
      // Methodology Note: kappa_bar is optimal exercise boundary. The
      // derivative at the optimal point is 0.
      forwardBar = discountFactor * omega * d1 * pBar;
      strikeBar = -discountFactor * omega * d2 * pBar;
      volblackBar = density1 * omega * d1Bar;
      volatilityBar = sqrttheta * volblackBar;
    }
    double[] priceAdjoint = new double[3];
    priceAdjoint[0] = forwardBar;
    priceAdjoint[1] = volatilityBar;
    priceAdjoint[2] = strikeBar;
    return ValueDerivatives.of(price, priceAdjoint);
  }

  /**
   * Return the Black price and its first and second order derivatives.
   * 
   * @param option  the option description
   * @param data  the model data
   * @param bsD  an array containing the price derivative [0] the derivative with respect to the forward,
   *  [1] the derivative with respect to the volatility and [2] the derivative with respect to the strike.
   * @param bsD2  an array of array containing the price second order derivatives.
   *  Second order derivatives with respect to:
   *  [0][0] forward-forward [0][1] forward-volatility [0][2] forward-strike
   *  [1][1]volatility-volatility, [1][2] volatility-strike, [2][2] strike-strike
   * @return the price
   */
  public double getPriceAdjoint2(EuropeanVanillaOption option, BlackFunctionData data, double[] bsD, double[][] bsD2) {
    // Forward sweep
    double strike = option.getStrike();
    double timeToExpiry = option.getTimeToExpiry();
    double vol = data.getBlackVolatility();
    double forward = data.getForward();
    boolean isCall = option.isCall();
    double discountFactor = data.getNumeraire();
    double sqrttheta = Math.sqrt(timeToExpiry);
    double omega = isCall ? 1 : -1;
    // Implementation Note: Forward sweep.
    double volblack = 0, kappa = 0, d1 = 0, d2 = 0;
    double x = 0;
    double p;
    if (strike < NEAR_ZERO || sqrttheta < NEAR_ZERO) {
      x = omega * (forward - strike);
      p = (x > 0 ? discountFactor * x : 0.0);
      volblack = sqrttheta < NEAR_ZERO ? 0 : (vol * sqrttheta);
    } else {
      volblack = vol * sqrttheta;
      kappa = Math.log(forward / strike) / volblack - 0.5 * volblack;
      d1 = NORMAL.getCDF(omega * (kappa + volblack));
      d2 = NORMAL.getCDF(omega * kappa);
      p = discountFactor * omega * (forward * d1 - strike * d2);
    }
    // Implementation Note: Backward sweep.
    double pBar = 1.0;
    double density1 = 0.0;
    double d1Bar = 0.0;
    double forwardBar = 0, strikeBar = 0, volblackBar = 0, volatilityBar = 0;
    if (strike < NEAR_ZERO || sqrttheta < NEAR_ZERO) {
      forwardBar = (x > 0 ? discountFactor * omega : 0.0);
      strikeBar = (x > 0 ? -discountFactor * omega : 0.0);
    } else {
      d1Bar = discountFactor * omega * forward * pBar;
      density1 = NORMAL.getPDF(omega * (kappa + volblack));
      // Implementation Note: kappa_bar = 0; no need to implement it.
      // Methodology Note: kappa_bar is optimal exercise boundary. The
      // derivative at the optimal point is 0.
      forwardBar = discountFactor * omega * d1 * pBar;
      strikeBar = -discountFactor * omega * d2 * pBar;
      volblackBar = density1 * omega * d1Bar;
      volatilityBar = sqrttheta * volblackBar;
    }
    bsD[0] = forwardBar;
    bsD[1] = volatilityBar;
    bsD[2] = strikeBar;
    if (strike < NEAR_ZERO || sqrttheta < NEAR_ZERO) {
      return p;
    }
    // Backward sweep: second derivative
    double d2Bar = -discountFactor * omega * strike;
    double density2 = NORMAL.getPDF(omega * kappa);
    double d1Kappa = omega * density1;
    double d1KappaKappa = -(kappa + volblack) * d1Kappa;
    double d2Kappa = omega * density2;
    double d2KappaKappa = -kappa * d2Kappa;
    double kappaKappaBar2 = d1KappaKappa * d1Bar + d2KappaKappa * d2Bar;
    double kappaV = -Math.log(forward / strike) / (volblack * volblack) - 0.5;
    double kappaVV = 2 * Math.log(forward / strike) / (volblack * volblack * volblack);
    double d1TotVV = density1 * omega * (-(kappa + volblack) * (kappaV + 1) * (kappaV + 1) + kappaVV);
    double d2TotVV = d2KappaKappa * kappaV * kappaV + d2Kappa * kappaVV;
    double vVbar2 = d1Bar * d1TotVV + d2Bar * d2TotVV;
    double volVolBar2 = vVbar2 * timeToExpiry;
    double kappaStrikeBar2 = -discountFactor * omega * d2Kappa;
    double kappaStrike = -1.0 / (strike * volblack);
    double strikeStrikeBar2 = (kappaKappaBar2 * kappaStrike + 2 * kappaStrikeBar2) * kappaStrike;
    double kappaStrikeV = 1.0 / strike / (volblack * volblack);
    double d1VK = -omega * (kappa + volblack) * density1 * (kappaV + 1) * kappaStrike + omega * density1 * kappaStrikeV;
    double d2V = d2Kappa * kappaV;
    double d2VK = -omega * kappa * density2 * kappaV * kappaStrike + omega * density2 * kappaStrikeV;
    double strikeD2Bar2 = -discountFactor * omega;
    double strikeVolblackBar2 = strikeD2Bar2 * d2V + d1Bar * d1VK + d2Bar * d2VK;
    double strikeVolBar2 = strikeVolblackBar2 * sqrttheta;
    double kappaForward = 1.0 / (forward * volblack);
    double forwardForwardBar2 = discountFactor * omega * d1Kappa * kappaForward;
    double strikeForwardBar2 = discountFactor * omega * d1Kappa * kappaStrike;
    double volForwardBar2 = discountFactor * omega * d1Kappa * (kappaV + 1) * sqrttheta;
    bsD2[0][0] = forwardForwardBar2;
    bsD2[0][1] = volForwardBar2;
    bsD2[1][0] = volForwardBar2;
    bsD2[0][2] = strikeForwardBar2;
    bsD2[2][0] = strikeForwardBar2;
    bsD2[1][1] = volVolBar2;
    bsD2[1][2] = strikeVolBar2;
    bsD2[2][1] = strikeVolBar2;
    bsD2[2][2] = strikeStrikeBar2;
    return p;
  }

  /**
   * Gets the vega function for the option.
   * 
   * @param option  the option description
   * @return the vega function
   */
  public Function1D<BlackFunctionData, Double> getVegaFunction(EuropeanVanillaOption option) {
    ArgChecker.notNull(option, "option");
    double k = option.getStrike();
    double t = option.getTimeToExpiry();
    return new Function1D<BlackFunctionData, Double>() {
      @Override
      public Double evaluate(BlackFunctionData data) {
        ArgChecker.notNull(data, "data");
        double sigma = data.getBlackVolatility();
        double f = data.getForward();
        double discountFactor = data.getNumeraire();
        return discountFactor * BlackFormulaRepository.vega(f, k, t, sigma);
      }
    };
  }

}
