/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import static com.opengamma.strata.math.MathUtils.nearZero;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.pricer.model.SabrVolatilityFormula;

/**
 * Formulas related to the SABR implied normal volatility function.
 * <p>
 * Only the "beta = 0" versions take strike/forward with negative values. The other formulas have a barrier at 0.
 * <p>
 * Reference: Hagan, P.; Kumar, D.; Lesniewski, A. & Woodward, D. "Managing smile risk", Wilmott Magazine, 2002, September, 84-108
 * Note: Formula references are related to the preprint version, e.g. available at https://www.researchgate.net/publication/235622441_Managing_Smile_Risk
 */
public final class SabrHaganNormalVolatilityFormula
    implements SabrVolatilityFormula {

  /**
   * Default implementation.
   */
  public static final SabrHaganNormalVolatilityFormula DEFAULT = new SabrHaganNormalVolatilityFormula();

  /* internal parameters */
  private static final double SMALL_Z = 1e-6;
  private static final double RHO_EPS = 1e-5;
  
  // Private constructor
  private SabrHaganNormalVolatilityFormula() {
  }

  @Override
  public ValueType getVolatilityType() {
    return ValueType.NORMAL_VOLATILITY; // SABR implemented with normal/Bachelier implied volatility
  }

  //-------------------------------------------------------------------------

  @Override
  public double volatility(
      double forward,
      double strike,
      double timeToExpiry,
      double alpha,
      double beta,
      double rho,
      double nu) {

    if (beta == 0.0d) {
      return volatilityBeta0(forward, strike, timeToExpiry, alpha, rho, nu);
    } // else
    return volatilityBetaNonZero(forward, strike, timeToExpiry, alpha, beta, rho, nu);
  }

  @Override
  public ValueDerivatives volatilityAdjoint(
      double forward,
      double strike,
      double timeToExpiry,
      double alpha,
      double beta,
      double rho,
      double nu) {

    if (beta == 0.0d) {
      return volatilityBeta0Adjoint(forward, strike, timeToExpiry, alpha, rho, nu);
    } // else
    return volatilityBetaNonZeroAdjoint(forward, strike, timeToExpiry, alpha, beta, rho, nu);
  }
  
  /**
   * Returns the volatility using the generic formula with barrier at 0.
   * Formula B.69a in the preprint version
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value of the option
   * @param timeToExpiry  the time to expiry of the option
   * @param alpha  the SABR alpha value
   * @param beta  the SABR beta value
   * @param rho  the SABR rho value
   * @param nu  the SABR nu value
   * @return the volatility
   */
  public double volatilityBetaNonZero(
      double forward,
      double strike,
      double timeToExpiry,
      double alpha,
      double beta,
      double rho,
      double nu) {

    ArgChecker.isTrue(forward > 0.0d, "forward must be positive");
    ArgChecker.isTrue(strike > 0.0d, "strike must be positive");
    ArgChecker.isTrue(rho < 1.0d - RHO_EPS, "rho must be below 1 and not too close to 1");
    ArgChecker.isTrue(rho > -1.0d + RHO_EPS, "rho must be above -1 and not too close to -1");
    double logfK = Math.log(forward / strike);
    double logfK2 = logfK * logfK;
    double logfK4 = logfK2 * logfK2;
    double fK = forward * strike;
    double oneminusbeta = 1.0 - beta;
    double fKoneminusbeta = Math.pow(fK, oneminusbeta);
    double oneminusbeta2 = oneminusbeta * oneminusbeta;
    double oneminusbeta4 = oneminusbeta2 * oneminusbeta2;
    double zeta = nu / alpha * Math.pow(fK, 0.5d * oneminusbeta) * logfK;
    double term1 = alpha * Math.pow(fK, 0.5 * beta);
    double term2 = (1.0d + logfK2 / 24.0d + logfK4 / 1920.0d) /
        (1.0d + oneminusbeta2 * logfK2 / 24.0d + oneminusbeta4 * logfK4 / 1920.0d);
    double term3 = zetaOverXhat(zeta, rho);
    double term4 = 1.0d + (-beta * (2.0d - beta) * alpha * alpha / (24.0d * fKoneminusbeta) +
        rho * alpha * nu * beta / (4.0d * Math.sqrt(fKoneminusbeta)) + (2.0d - 3.0 * rho * rho) / 24.0d * nu * nu) *
        timeToExpiry;
    return term1 * term2 * term3 * term4;
  }

  /**
   * Returns the volatility using the generic formula with barrier at 0 at its derivatives.
   * Formula B.69a in the preprint version
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value of the option
   * @param timeToExpiry  the time to expiry of the option
   * @param alpha  the SABR alpha value
   * @param beta  the SABR beta value
   * @param rho  the SABR rho value
   * @param nu  the SABR nu value
   * @return the volatility
   */
  public ValueDerivatives volatilityBetaNonZeroAdjoint(
      double forward,
      double strike,
      double timeToExpiry,
      double alpha,
      double beta,
      double rho,
      double nu) {

    ArgChecker.isTrue(forward > 0.0d, "forward must be positive");
    ArgChecker.isTrue(strike > 0.0d, "strike must be positive");
    ArgChecker.isTrue(rho < 1.0d - RHO_EPS, "rho must be below 1 and not too close to 1");
    ArgChecker.isTrue(rho > -1.0d + RHO_EPS, "rho must be above -1 and not too close to -1");
    double logfK = Math.log(forward / strike);
    double logfK2 = logfK * logfK;
    double logfK4 = logfK2 * logfK2;
    double fK = forward * strike;
    double oneminusbeta = 1.0 - beta;
    double fKoneminusbeta = Math.pow(fK, oneminusbeta);
    double oneminusbeta2 = oneminusbeta * oneminusbeta;
    double oneminusbeta4 = oneminusbeta2 * oneminusbeta2;
    double zeta = nu / alpha * Math.pow(fK, 0.5d * oneminusbeta) * logfK;
    double term1 = alpha * Math.pow(fK, 0.5 * beta);
    double term2Num = 1.0d + logfK2 / 24.0d + logfK4 / 1920.0d;
    double term2Den = 1.0d + oneminusbeta2 * logfK2 / 24.0d + oneminusbeta4 * logfK4 / 1920.0d;
    double term2 = term2Num / term2Den;
    ValueDerivatives term3 = zetaOverXhatAdjoint(zeta, rho);
    double term4 = 1.0d +
        (-beta * (2.0d - beta) * alpha * alpha / (24.0d * fKoneminusbeta) +
            rho * alpha * nu * beta / (4.0d * Math.sqrt(fKoneminusbeta)) + (2.0d - 3.0 * rho * rho) / 24.0d * nu * nu) *
            timeToExpiry;
    double volatility = term1 * term2 * term3.getValue() * term4;
    // Backward sweep
    double term1Bar = term2 * term3.getValue() * term4;
    double term2Bar = term1 * term3.getValue() * term4;
    double term3Bar = term1 * term2 * term4;
    double term4Bar = term1 * term2 * term3.getValue();
    double betaBar = ((-2.0d + 2.0d * beta) * alpha * alpha / (24.0d * fKoneminusbeta) +
        rho * alpha * nu / (4.0d * Math.sqrt(fKoneminusbeta))) * timeToExpiry * term4Bar;
    double alphaBar = -beta * (2.0d - beta) * alpha / (12.0d * fKoneminusbeta) * timeToExpiry * term4Bar;
    alphaBar += rho * nu * beta / (4.0d * Math.sqrt(fKoneminusbeta)) * timeToExpiry * term4Bar;
    double fKoneminusbetaBar =
        beta * (2.0d - beta) * alpha * alpha / (24.0d * fKoneminusbeta * fKoneminusbeta) * timeToExpiry * term4Bar;
    fKoneminusbetaBar +=
        -0.5 * rho * alpha * nu * beta / (4.0d * Math.pow(fKoneminusbeta, 1.5d)) * timeToExpiry * term4Bar;
    double rhoBar = alpha * nu * beta / (4.0d * Math.sqrt(fKoneminusbeta)) * timeToExpiry * term4Bar;
    rhoBar += -6.0 * rho / 24.0d * nu * nu * timeToExpiry * term4Bar;
    double nuBar = rho * alpha * beta / (4.0d * Math.sqrt(fKoneminusbeta)) * timeToExpiry * term4Bar;
    nuBar += (2.0d - 3.0 * rho * rho) / 12.0d * nu * timeToExpiry * term4Bar;
    double zetaBar = term3.getDerivative(0) * term3Bar;
    rhoBar += term3.getDerivative(1) * term3Bar;
    double term2NumBar = 1.0d / term2Den * term2Bar;
    double term2DenBar = -term2Num / (term2Den * term2Den) * term2Bar;
    double oneminusbeta2Bar = logfK2 / 24.0d * term2DenBar;
    double logfK2Bar = oneminusbeta2 / 24.0d * term2DenBar;
    double oneminusbeta4Bar = logfK4 / 1920.0d * term2DenBar;
    double logfK4Bar = oneminusbeta4 / 1920.0d * term2DenBar;
    logfK2Bar += 1.0d / 24.0d * term2NumBar;
    logfK4Bar += 1.0d / 1920.0d * term2NumBar;
    alphaBar += Math.pow(fK, 0.5 * beta) * term1Bar;
    double fKBar = 0.5 * beta * alpha * Math.pow(fK, 0.5 * beta - 1.0d) * term1Bar;
    betaBar += alpha * Math.pow(fK, 0.5 * beta) * 0.5 * Math.log(fK) * term1Bar;
    nuBar += 1.0d / alpha * Math.pow(fK, 0.5d * oneminusbeta) * logfK * zetaBar;
    alphaBar += -nu / (alpha * alpha) * Math.pow(fK, 0.5d * oneminusbeta) * logfK * zetaBar;
    fKBar += 0.5d * oneminusbeta * nu / alpha * Math.pow(fK, 0.5d * oneminusbeta - 1.0d) * logfK * zetaBar;
    double oneminusbetaBar = nu / alpha * Math.pow(fK, 0.5d * oneminusbeta) * 0.5d * Math.log(fK) * logfK * zetaBar;
    double logfKBar = nu / alpha * Math.pow(fK, 0.5d * oneminusbeta) * zetaBar;
    oneminusbeta2Bar += 2.0d * oneminusbeta2 * oneminusbeta4Bar;
    oneminusbetaBar += 2.0d * oneminusbeta * oneminusbeta2Bar;
    oneminusbetaBar += Math.pow(fK, oneminusbeta) * Math.log(fK) * fKoneminusbetaBar;
    fKBar += oneminusbeta * Math.pow(fK, oneminusbeta - 1.0d) * fKoneminusbetaBar;
    betaBar += -oneminusbetaBar;
    double strikeBar = forward * fKBar;
    double forwardBar = strike * fKBar;
    logfK2Bar += 2.0d * logfK2 * logfK4Bar;
    logfKBar += 2.0d * logfK * logfK2Bar;
    forwardBar += 1.0d / forward * logfKBar;
    strikeBar += -1.0d / strike * logfKBar;
    return ValueDerivatives.of(volatility, DoubleArray.of(forwardBar, strikeBar, alphaBar, betaBar, rhoBar, nuBar));
  }

  /**
   * Calculates the normal implied volatility for the special case of beta=0.
   * <p>
   * The case beta=0 removes the barrier for forward/strike at 0 and lead to significant simplification 
   * in the implementation. 
   * <p>
   * Formula B.70a in the preprint version. Note that the preprint version has a typo (missing the zeta/xhat term).
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value of the option
   * @param timeToExpiry  the time to expiry of the option
   * @param alpha  the SABR alpha value
   * @param rho  the SABR rho value
   * @param nu  the SABR nu value
   * @return the volatility
   */
  public double volatilityBeta0(
      double forward,
      double strike,
      double timeToExpiry,
      double alpha,
      double rho,
      double nu) {

    ArgChecker.isTrue(rho < 1.0d - RHO_EPS, "rho must be below 1 and not too close to 1");
    ArgChecker.isTrue(rho > -1.0d + RHO_EPS, "rho must be above -1 and not too close to -1");
    double zeta = nu / alpha * (forward - strike);
    double term3 = zetaOverXhat(zeta, rho);
    double term4 = 1 + (2.0d - 3.0d * rho * rho) / 24.0d * nu * nu * timeToExpiry;
    double volatility = alpha * term3 * term4;
    return volatility;
  }

  /**
   * Calculates the normal implied volatility and its derivatives (w.r.t. to forward, strike and model parameters)
   * for the special case of beta=0.
   * <p>
   * The case beta=0 removes the barrier for forward/strike at 0 and lead to significant simplification 
   * in the implementation.
   * 
   * @param forward  the forward value of the underlying
   * @param strike  the strike value of the option
   * @param timeToExpiry  the time to expiry of the option
   * @param alpha  the SABR alpha value
   * @param rho  the SABR rho value
   * @param nu  the SABR nu value
   * @return the volatility and associated derivatives (forward, strike, alpha, beta, rho, nu)
   */
  public ValueDerivatives volatilityBeta0Adjoint(
      double forward,
      double strike,
      double timeToExpiry,
      double alpha,
      double rho,
      double nu) {

    ArgChecker.isTrue(rho < 1.0d - RHO_EPS, "rho must be below 1 and not too close to 1");
    ArgChecker.isTrue(rho > -1.0d + RHO_EPS, "rho must be above -1 and not too close to -1");
    double zeta = nu / alpha * (forward - strike);
    ValueDerivatives term3 = zetaOverXhatAdjoint(zeta, rho);
    double term4 = 1 + (2.0d - 3.0d * rho * rho) / 24.0d * nu * nu * timeToExpiry;
    double volatility = alpha * term3.getValue() * term4;
    // Backward sweep
    double alphaBar = term3.getValue() * term4;
    double term3Bar = alpha * term4;
    double term4Bar = alpha * term3.getValue();
    double rhoBar = -0.25 * rho * nu * nu * timeToExpiry * term4Bar;
    double nuBar = (2.0d - 3.0d * rho * rho) / 12.0d * nu * timeToExpiry * term4Bar;
    double zetaBar = term3.getDerivative(0) * term3Bar;
    rhoBar += term3.getDerivative(1) * term3Bar;
    nuBar += 1.0d / alpha * (forward - strike) * zetaBar;
    alphaBar += -nu / (alpha * alpha) * (forward - strike) * zetaBar;
    double forwardBar = nu / alpha * zetaBar;
    double strikeBar = -nu / alpha * zetaBar;
    return ValueDerivatives.of(volatility, DoubleArray.of(forwardBar, strikeBar, alphaBar, rhoBar, nuBar));
  }

  /**
   * Computes the ratio zeta over xHat. Special treatment is required for zeta close to 0 (ATM).
   * @param zeta  the zeta value
   * @param rho  the rho parameter value
   * @return the ratio
   */
  protected double zetaOverXhat(double zeta, double rho) {
    if (nearZero(zeta, SMALL_Z)) {
      return 1.0 - rho * zeta / 2.0;
    }
    double c0 = 1.0d - 2.0d * rho * zeta + zeta * zeta;
    double c1 = Math.sqrt(c0) - rho + zeta;
    double c2 = 1.0 - rho;
    double c3 = c1 / c2;
    double xhat = Math.log(c3);
    return zeta / xhat;
  }

  /**
   * Computes the ratio zeta over xHat and its derivatives. Special treatment is required for zeta close to 0 (ATM).
   * @param zeta  the zeta value
   * @param rho  the rho parameter value
   * @return the ratio
   */
  protected ValueDerivatives zetaOverXhatAdjoint(double zeta, double rho) {
    double zetaOverXhat;
    if (nearZero(zeta, SMALL_Z)) {
      zetaOverXhat = 1.0d - 0.5d * rho * zeta;
      // Backward sweep
      double rhoBar = -0.5d * zeta;
      double zetaBar = -0.5d * rho;
      return ValueDerivatives.of(zetaOverXhat, DoubleArray.of(zetaBar, rhoBar));
    } // else
    double c0 = 1.0d - 2.0d * rho * zeta + zeta * zeta;
    double c1 = Math.sqrt(c0) - rho + zeta;
    double c2 = 1.0 - rho;
    double c3 = c1 / c2;
    double xhat = Math.log(c3);
    zetaOverXhat = zeta / xhat;
    // Backward sweep
    double zetaBar = 1.0 / xhat;
    double xhatBar = -zeta / (xhat * xhat);
    double c3Bar = 1 / c3 * xhatBar;
    double c1Bar = 1 / c2 * c3Bar;
    double c2Bar = -c1 / (c2 * c2) * c3Bar;
    double rhoBar = -c2Bar;
    rhoBar += -c1Bar;
    zetaBar += c1Bar;
    double c0Bar = 0.5 / Math.sqrt(c0) * c1Bar;
    rhoBar += -2.0d * zeta * c0Bar;
    zetaBar += (-2.0d * rho + 2.0d * zeta) * c0Bar;
    return ValueDerivatives.of(zetaOverXhat, DoubleArray.of(zetaBar, rhoBar));
  }
  
}
