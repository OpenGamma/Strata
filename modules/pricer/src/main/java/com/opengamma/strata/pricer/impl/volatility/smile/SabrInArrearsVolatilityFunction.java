/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Adjustments to the SABR parameters to accommodate the pricing of in-arrears caplets.
 * <p>
 * The parameter "q" in the formula is a parameter such that 1-q is closely related to a mean reversion.
 * It is defaulted to q=1 in the default instance of the formula.
 * <p>
 * Reference: Willems, Sander. SABR smiles for RFR caplets. August 2020. 
 * Electronic copy available at: https://ssrn.com/abstract=3567655
 */
@BeanDefinition(factoryName = "of")
public final class SabrInArrearsVolatilityFunction
    implements ImmutableBean, Serializable {

  /** The default parameter for q, with 1-q related to mean-reversion. The default
   * Correspond to no mean reversion in the in-arrears period. */
  private static final double DEFAULT_Q = 1.0;

  /** The mean reversion related parameter. */
  @PropertyDefinition
  private final double q;

  /**
   * Default implementation with q = 1;
   */
  public static final SabrInArrearsVolatilityFunction DEFAULT = new SabrInArrearsVolatilityFunction(DEFAULT_Q);

  /**
   * The effective SABR parameters from the raw SABR parameters and the times. Formula in the case tau0 > 0.
   * <p>
   * Theorem 4.2 in reference.
   * 
   * @param parameters  the raw SABR parameters
   * @param tau0  the accumulation period start time
   * @param tau1  the accumulation period end time
   * @return  the effective SABR parameters
   */
  public SabrFormulaData effectiveSabrBeforeStart(SabrFormulaData parameters, double tau0, double tau1) {
    double alpha = parameters.getAlpha();
    double beta = parameters.getBeta();
    double rho = parameters.getRho();
    double nu = parameters.getNu();
    double tau = 2 * q * tau0 + tau1;
    double tauP2 = tau * tau;
    double tauP3 = tauP2 * tau;
    double tau0P2 = tau0 * tau0;
    double tau0P3 = tau0P2 * tau0;
    double tau1P2 = tau1 * tau1;
    double tau1P3 = tau1P2 * tau1;
    double gamma1 =
        tau * (2 * tauP3 + tau1P3 + q * (4 * q - 2) * tau0P3 + 6 * q * tau0P2 * tau1) / ((4 * q + 3) * (2 * q + 1));
    double gamma2 = 3 * q * rho * rho * (tau1 - tau0) * (tau1 - tau0) *
        (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) / ((4 * q + 3) * (3 * q + 2) * (3 * q + 2));
    double gamma = gamma1 + gamma2;
    double rhoHat = rho * (3 * tauP2 + 2 * q * tau0P2 + tau1P2) / (Math.sqrt(gamma) * (6 * q + 4));
    double nuHatP2 = nu * nu * gamma * (2 * q + 1) / (tauP3 * tau1);
    double nuHat = Math.sqrt(nuHatP2);
    double h = nu * nu * (tauP2 + 2 * q * tau0P2 + tau1P2) / (2 * tau1 * tau * (q + 1)) - nuHatP2;
    double alphaHatP2 = alpha * alpha / (2 * q + 1) * tau / tau1 * Math.exp(0.5 * h * tau1);
    double alphaHat = Math.sqrt(alphaHatP2);
    return SabrFormulaData.of(alphaHat, beta, rhoHat, nuHat);
  }

  /**
   * The effective SABR parameters from the raw SABR parameters and the times. Formula in the case tau0 <= 0.
   * <p>
   * Theorem 4.1 in reference.
   * 
   * @param parameters  the raw SABR parameters
   * @param tau0  the accumulation period start time
   * @param tau1  the accumulation period end time
   * @return  the effective SABR parameters
   */
  public SabrFormulaData effectiveSabrAfterStart(SabrFormulaData parameters, double tau0, double tau1) {
    double alpha = parameters.getAlpha();
    double beta = parameters.getBeta();
    double rho = parameters.getRho();
    double nu = parameters.getNu();
    double zeta = 3.0d / (4 * q + 3) * (1.0d / (2 * q + 1) + rho * rho * 2 * q / ((3 * q + 2) * (3 * q + 2)));
    double rhoHat = 2 * rho / (Math.sqrt(zeta) * (3 * q + 2));
    double nuHat2 = nu * nu * zeta * (2 * q + 1);
    double nuHat = Math.sqrt(nuHat2);
    double alphaHat2 = alpha * alpha / (2 * q + 1) * Math.pow(tau1 / (tau1 - tau0), 2 * q) *
        Math.exp(0.5 * (nu * nu / (q + 1) - nuHat2) * tau1);
    double alphaHat = Math.sqrt(alphaHat2);
    return SabrFormulaData.of(alphaHat, beta, rhoHat, nuHat);
  }

  /**
   * The effective SABR parameters from the raw SABR parameters and the times.
   * 
   * @param parameters  the raw SABR parameters
   * @param tau0  the accumulation period start time
   * @param tau1  the accumulation period end time
   * @return  the effective SABR parameters
   */
  public SabrFormulaData effectiveSabr(SabrFormulaData parameters, double tau0, double tau1) {
    if (tau0 <= 0.0d) {
      return effectiveSabrAfterStart(parameters, tau0, tau1);
    }
    return effectiveSabrBeforeStart(parameters, tau0, tau1);
  }

  /**
   * The effective SABR parameters and their derivatives from the raw SABR parameters and the times. 
   * Formula in the case tau0 <= 0;
   * <p>
   * The results are provided as a list of ValueDerivatives corresponding to alpha, beta, rho and nu and 
   * with each derivatives part containing the derivatives with respect to alpha, beta, rho, nu, tau0 and tau1.
   * 
   * @param parameters  the raw SABR parameters
   * @param tau0  the accumulation period start time
   * @param tau1  the accumulation period end time
   * @return  the effective SABR parameters
   */
  public List<ValueDerivatives> effectiveSabrAfterStartAd(SabrFormulaData parameters, double tau0, double tau1) {
    double alpha = parameters.getAlpha();
    double beta = parameters.getBeta();
    double rho = parameters.getRho();
    double nu = parameters.getNu();
    double zeta = 3.0d / (4 * q + 3) * (1.0d / (2 * q + 1) + rho * rho * 2 * q / ((3 * q + 2) * (3 * q + 2)));
    double rhoHat = 2 * rho / (Math.sqrt(zeta) * (3 * q + 2));
    double nuHat2 = nu * nu * zeta * (2 * q + 1);
    double nuHat = Math.sqrt(nuHat2);
    double alphaHat2 = alpha * alpha / (2 * q + 1) * Math.pow(tau1 / (tau1 - tau0), 2 * q) *
        Math.exp(0.5 * (nu * nu / (q + 1) - nuHat2) * tau1);
    double alphaHat = Math.sqrt(alphaHat2);
    /* Backward sweep */
    List<ValueDerivatives> results = new ArrayList<>();
    // alpha
    double alphaHatBarAlpha = 1.0;
    double alphaHat2BarAlpha = 0.5 / alphaHat * alphaHatBarAlpha;
    double alphaBarAlpha = 2 * alphaHat2 / alpha * alphaHat2BarAlpha;
    double tau1BarAlpha = alphaHat2 * 0.5 * (nu * nu / (q + 1) - nuHat2) * alphaHat2BarAlpha;
    tau1BarAlpha += alpha * alpha / (2 * q + 1) *
        Math.exp(0.5 * (nu * nu / (q + 1) - nuHat2) * tau1) * 2 * q * Math.pow(tau1 / (tau1 - tau0), 2 * q - 1) *
        (1.0d / (tau1 - tau0) - tau1 / ((tau1 - tau0) * (tau1 - tau0))) * alphaHat2BarAlpha;
    double tau0BarAlpha =
        2 * q * alphaHat2 / (tau1 / (tau1 - tau0)) * tau1 / ((tau1 - tau0) * (tau1 - tau0)) * alphaHat2BarAlpha;
    double nuBarAlpha = alphaHat2 * nu / (q + 1) * tau1 * alphaHat2BarAlpha;
    double nuHat2BarAlpha = alphaHat2 * -0.5 * tau1 * alphaHat2BarAlpha;
    nuBarAlpha += nuHat2 * 2 / nu * nuHat2BarAlpha;
    double zetaBarAlpha = nuHat2 / zeta * nuHat2BarAlpha;
    double rhoBarAlpha = 3.0d / (4 * q + 3) * rho * 4 * q / ((3 * q + 2) * (3 * q + 2)) * zetaBarAlpha;
    results.add(ValueDerivatives // alpha and derivatives
        .of(alphaHat, DoubleArray.of(alphaBarAlpha, 0, rhoBarAlpha, nuBarAlpha, tau0BarAlpha, tau1BarAlpha)));
    // beta
    results.add(ValueDerivatives // beta and derivatives
        .of(beta, DoubleArray.of(0, 1.0d, 0, 0, 0, 0)));
    // rho
    double rhoHatBarRho = 1.0;
    double rhoBarRho = rhoHat / rho * rhoHatBarRho;
    double zetaBarRho = -0.5 * rhoHat / zeta * rhoHatBarRho;
    rhoBarRho += 3.0d / (4 * q + 3) * rho * 4 * q / ((3 * q + 2) * (3 * q + 2)) * zetaBarRho;
    results.add(ValueDerivatives // rho and derivatives
        .of(rhoHat, DoubleArray.of(0, 0, rhoBarRho, 0, 0, 0)));
    // nu
    double nuHatBarNu = 1.0;
    double nuHat2BarNu = 0.5 / nuHat * nuHatBarNu;
    double nuBarNu = 2 * nuHat2 / nu * nuHat2BarNu;
    double zetaBarNu = nuHat2 / zeta * nuHat2BarNu;
    double rhoBarNu = 3.0d / (4 * q + 3) * rho * 4 * q / ((3 * q + 2) * (3 * q + 2)) * zetaBarNu;
    results.add(ValueDerivatives // nu and derivatives
        .of(nuHat, DoubleArray.of(0, 0, rhoBarNu, nuBarNu, 0, 0)));
    return results;
  }

  /**
   * The effective SABR parameters and their derivatives from the raw SABR parameters and the times. 
   * Formula in the case tau0 > 0;
   * <p>
   * The results are provided as a list of ValueDerivatives corresponding to alpha, beta, rho and nu and 
   * with each derivatives part containing the derivatives with respect to alpha, beta, rho, nu, tau0 and tau1.
   * 
   * @param parameters  the raw SABR parameters
   * @param tau0  the accumulation period start time
   * @param tau1  the accumulation period end time
   * @return  the effective SABR parameters
   */
  public List<ValueDerivatives> effectiveSabrBeforeStartAd(SabrFormulaData parameters, double tau0, double tau1) {
    double alpha = parameters.getAlpha();
    double beta = parameters.getBeta();
    double rho = parameters.getRho();
    double nu = parameters.getNu();
    double tau = 2 * q * tau0 + tau1;
    double tauP2 = tau * tau;
    double tauP3 = tauP2 * tau;
    double tau0P2 = tau0 * tau0;
    double tau0P3 = tau0P2 * tau0;
    double tau1P2 = tau1 * tau1;
    double tau1P3 = tau1P2 * tau1;
    double gamma1 =
        tau * (2 * tauP3 + tau1P3 + q * (4 * q - 2) * tau0P3 + 6 * q * tau0P2 * tau1) / ((4 * q + 3) * (2 * q + 1));
    double gamma2 = 3 * q * rho * rho * (tau1 - tau0) * (tau1 - tau0) *
        (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) / ((4 * q + 3) * (3 * q + 2) * (3 * q + 2));
    double gamma = gamma1 + gamma2;
    double rhoHat = rho * (3 * tauP2 + 2 * q * tau0P2 + tau1P2) / (Math.sqrt(gamma) * (6 * q + 4));
    double nuHatP2 = nu * nu * gamma * (2 * q + 1) / (tauP3 * tau1);
    double nuHat = Math.sqrt(nuHatP2);
    double h = nu * nu * (tauP2 + 2 * q * tau0P2 + tau1P2) / (2 * tau1 * tau * (q + 1)) - nuHatP2;
    double alphaHat2 = alpha * alpha / (2 * q + 1) * tau / tau1 * Math.exp(0.5 * h * tau1);
    double alphaHat = Math.sqrt(alphaHat2);
    /* Backward sweep */
    List<ValueDerivatives> results = new ArrayList<>();
    // alpha
    double alphaHatBarAlpha = 1.0;
    double alphaHat2BarAlpha = 0.5 / alphaHat * alphaHatBarAlpha;
    double alphaBarAlpha = 2 * alphaHat2 / alpha * alphaHat2BarAlpha;
    double tauBarAlpha = alphaHat2 / tau * alphaHat2BarAlpha;
    double tau1BarAlpha = -alphaHat2 / tau1 * alphaHat2BarAlpha;
    tau1BarAlpha += alphaHat2 * 0.5 * h * alphaHat2BarAlpha; // OK
    double hBarAlpha = alphaHat2 * 0.5 * tau1 * alphaHat2BarAlpha;
    double nuBarAlpha = 2 * (h + nuHatP2) / nu * hBarAlpha;
    double tauP2BarAlpha = nu * nu / (2 * tau1 * tau * (q + 1)) * hBarAlpha;
    double tau0P2BarAlpha = nu * nu * 2 * q / (2 * tau1 * tau * (q + 1)) * hBarAlpha;
    double tau1P2BarAlpha = nu * nu / (2 * tau1 * tau * (q + 1)) * hBarAlpha;
    tau1BarAlpha += -(h + nuHatP2) / tau1 * hBarAlpha; // OK
    tauBarAlpha += -(h + nuHatP2) / tau * hBarAlpha;
    double nuHat2BarAlpha = -hBarAlpha;
    nuBarAlpha += 2 * nuHatP2 / nu * nuHat2BarAlpha;
    double gammaBarAlpha = nuHatP2 / gamma * nuHat2BarAlpha;
    double tauP3BarAlpha = -nuHatP2 / tauP3 * nuHat2BarAlpha;
    tau1BarAlpha += -nuHatP2 / tau1 * nuHat2BarAlpha; // OK
    double gamma1BarAlpha = gammaBarAlpha;
    double gamma2BarAlpha = gammaBarAlpha;
    double rhoBarAlpha = 2 * gamma2 / rho * gamma2BarAlpha;
    tau1BarAlpha += 2 * gamma2 / (tau1 - tau0) * gamma2BarAlpha;
    tau1BarAlpha += gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 4 * tau0 * gamma2BarAlpha;
    double tau0BarAlpha = -2 * gamma2 / (tau1 - tau0) * gamma2BarAlpha;
    tauP2BarAlpha += gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 3 * gamma2BarAlpha;
    tau1P2BarAlpha += -gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * gamma2BarAlpha;
    tau0P2BarAlpha += gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 5 * q * gamma2BarAlpha;
    tau0BarAlpha += gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 4 * tau1 * gamma2BarAlpha;
    tauBarAlpha += gamma1 / tau * gamma1BarAlpha;
    tauP3BarAlpha += tau * 2 / ((4 * q + 3) * (2 * q + 1)) * gamma1BarAlpha;
    double tau1P3BarAlpha = tau / ((4 * q + 3) * (2 * q + 1)) * gamma1BarAlpha;
    double tau0P3BarAlpha = tau * q * (4 * q - 2) / ((4 * q + 3) * (2 * q + 1)) * gamma1BarAlpha;
    tau0P2BarAlpha += tau * 6 * q * tau1 / ((4 * q + 3) * (2 * q + 1)) * gamma1BarAlpha;
    tau1BarAlpha += tau * 6 * q * tau0P2 / ((4 * q + 3) * (2 * q + 1)) * gamma1BarAlpha;
    tau1P2BarAlpha += tau1 * tau1P3BarAlpha;
    tau1BarAlpha += tau1P2 * tau1P3BarAlpha;
    tau1BarAlpha += 2 * tau1 * tau1P2BarAlpha;
    tau0P2BarAlpha += tau0 * tau0P3BarAlpha;
    tau0BarAlpha += tau0P2 * tau0P3BarAlpha;
    tau0BarAlpha += 2 * tau0 * tau0P2BarAlpha;
    tauP2BarAlpha += tau * tauP3BarAlpha;
    tauBarAlpha += tauP2 * tauP3BarAlpha;
    tauBarAlpha += 2 * tau * tauP2BarAlpha;
    tau0BarAlpha += 2 * q * tauBarAlpha;
    tau1BarAlpha += tauBarAlpha;
    results.add(ValueDerivatives // alpha and derivatives
        .of(alphaHat, DoubleArray.of(alphaBarAlpha, 0, rhoBarAlpha, nuBarAlpha, tau0BarAlpha, tau1BarAlpha)));
    // beta
    results.add(ValueDerivatives // beta and derivatives
        .of(beta, DoubleArray.of(0, 1.0d, 0, 0, 0, 0)));
    // rho
    double rhoHatBarRho = 1.0;
    double rhoBarRho = rhoHat / rho * rhoHatBarRho;
    double tau2BarRho = rho * 3 / (Math.sqrt(gamma) * (6 * q + 4)) * rhoHatBarRho;
    double tau0P2BarRho = rho * 2 * q / (Math.sqrt(gamma) * (6 * q + 4)) * rhoHatBarRho;
    double tau1P2BarRho = rho / (Math.sqrt(gamma) * (6 * q + 4)) * rhoHatBarRho;
    double gammaBarRho = -0.5 * rhoHat / gamma * rhoHatBarRho;
    double gamma1BarRho = gammaBarRho;
    double gamma2BarRho = gammaBarRho;
    rhoBarRho += 2 * gamma2 / rho * gamma2BarRho;
    double tau1BarRho = 2 * gamma2 / (tau1 - tau0) * gamma2BarRho;
    tau1BarRho += gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 4 * tau0 * gamma2BarRho;
    double tau0BarRho = -2 * gamma2 / (tau1 - tau0) * gamma2BarRho;
    tau2BarRho += gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 3 * gamma2BarRho;
    tau1P2BarRho += -gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * gamma2BarRho;
    tau0P2BarRho += gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 5 * q * gamma2BarRho;
    tau0BarRho += gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 4 * tau1 * gamma2BarRho;
    double tauBarRho = gamma1 / tau * gamma1BarRho;
    double tau3BarRho = tau * 2 / ((4 * q + 3) * (2 * q + 1)) * gamma1BarRho;
    double tau13BarRho = tau / ((4 * q + 3) * (2 * q + 1)) * gamma1BarRho;
    double tau03BarRho = tau * q * (4 * q - 2) / ((4 * q + 3) * (2 * q + 1)) * gamma1BarRho;
    tau0P2BarRho += tau * 6 * q * tau1 / ((4 * q + 3) * (2 * q + 1)) * gamma1BarRho;
    tau1BarRho += tau * 6 * q * tau0P2 / ((4 * q + 3) * (2 * q + 1)) * gamma1BarRho;
    tau1P2BarRho += tau1 * tau13BarRho;
    tau1BarRho += tau1P2 * tau13BarRho;
    tau1BarRho += 2 * tau1 * tau1P2BarRho;
    tau0P2BarRho += tau0 * tau03BarRho;
    tau0BarRho += tau0P2 * tau03BarRho;
    tau0BarRho += 2 * tau0 * tau0P2BarRho;
    tau2BarRho += tau * tau3BarRho;
    tauBarRho += tauP2 * tau3BarRho;
    tauBarRho += 2 * tau * tau2BarRho;
    tau0BarRho += 2 * q * tauBarRho;
    tau1BarRho += tauBarRho;
    results.add(ValueDerivatives // rho and derivatives
        .of(rhoHat, DoubleArray.of(0, 0, rhoBarRho, 0, tau0BarRho, tau1BarRho)));
    // nu
    double nuHatBarNu = 1.0;
    double nuHat2BarNu = 0.5 / nuHat * nuHatBarNu;
    double nuBarNu = 2 * nuHatP2 / nu * nuHat2BarNu;
    double gammaBarNu = nuHatP2 / gamma * nuHat2BarNu;
    double tau1BarNu = -nuHatP2 / tau1 * nuHat2BarNu;
    double tauP3BarNu = -nuHatP2 / tauP3 * nuHat2BarNu;
    double gamma1BarNu = gammaBarNu;
    double gamma2BarNu = gammaBarNu;
    double rhoBarNu = 2 * gamma2 / rho * gamma2BarNu;
    tau1BarNu += 2 * gamma2 / (tau1 - tau0) * gamma2BarNu;
    tau1BarNu += gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 4 * tau0 * gamma2BarNu;
    double tau0BarNu = -2 * gamma2 / (tau1 - tau0) * gamma2BarNu;
    double tauP2BarNu = gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 3 * gamma2BarNu;
    double tau1P2BarNu = -gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * gamma2BarNu;
    double tau0P2BarNu = gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 5 * q * gamma2BarNu;
    tau0BarNu += gamma2 / (3 * tauP2 - tau1P2 + 5 * q * tau0P2 + 4 * tau0 * tau1) * 4 * tau1 * gamma2BarNu;
    double tauBarNu = gamma1 / tau * gamma1BarNu;
    tauP3BarNu += tau * 2 / ((4 * q + 3) * (2 * q + 1)) * gamma1BarNu;
    double tau1P3BarNu = tau / ((4 * q + 3) * (2 * q + 1)) * gamma1BarNu;
    double tau0P3BarNu = tau * q * (4 * q - 2) / ((4 * q + 3) * (2 * q + 1)) * gamma1BarNu;
    tau0P2BarNu += tau * 6 * q * tau1 / ((4 * q + 3) * (2 * q + 1)) * gamma1BarNu;
    tau1BarNu += tau * 6 * q * tau0P2 / ((4 * q + 3) * (2 * q + 1)) * gamma1BarNu;
    tau1P2BarNu += tau1 * tau1P3BarNu;
    tau1BarNu += tau1P2 * tau1P3BarNu;
    tau1BarNu += 2 * tau1 * tau1P2BarNu;
    tau0P2BarNu += tau0 * tau0P3BarNu;
    tau0BarNu += tau0P2 * tau0P3BarNu;
    tau0BarNu += 2 * tau0 * tau0P2BarNu;
    tauP2BarNu += tau * tauP3BarNu;
    tauBarNu += tauP2 * tauP3BarNu;
    tauBarNu += 2 * tau * tauP2BarNu;
    tau0BarNu += 2 * q * tauBarNu;
    tau1BarNu += tauBarNu;
    results.add(ValueDerivatives // nu and derivatives
        .of(nuHat, DoubleArray.of(0, 0, rhoBarNu, nuBarNu, tau0BarNu, tau1BarNu)));
    return results;
  }

  /**
   * The effective SABR parameters from the raw SABR parameters and the times.
   * 
   * @param parameters  the raw SABR parameters
   * @param tau0  the accumulation period start time
   * @param tau1  the accumulation period end time
   * @return  the effective SABR parameters
   */
  public List<ValueDerivatives> effectiveSabrAd(SabrFormulaData parameters, double tau0, double tau1) {
    if (tau0 <= 0.0d) {
      return effectiveSabrAfterStartAd(parameters, tau0, tau1);
    }
    return effectiveSabrBeforeStartAd(parameters, tau0, tau1);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SabrInArrearsVolatilityFunction}.
   * @return the meta-bean, not null
   */
  public static SabrInArrearsVolatilityFunction.Meta meta() {
    return SabrInArrearsVolatilityFunction.Meta.INSTANCE;
  }

  static {
    MetaBean.register(SabrInArrearsVolatilityFunction.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Obtains an instance.
   * @param q  the value of the property
   * @return the instance
   */
  public static SabrInArrearsVolatilityFunction of(
      double q) {
    return new SabrInArrearsVolatilityFunction(
      q);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SabrInArrearsVolatilityFunction.Builder builder() {
    return new SabrInArrearsVolatilityFunction.Builder();
  }

  private SabrInArrearsVolatilityFunction(
      double q) {
    this.q = q;
  }

  @Override
  public SabrInArrearsVolatilityFunction.Meta metaBean() {
    return SabrInArrearsVolatilityFunction.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the mean reversion related parameter.
   * @return the value of the property
   */
  public double getQ() {
    return q;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SabrInArrearsVolatilityFunction other = (SabrInArrearsVolatilityFunction) obj;
      return JodaBeanUtils.equal(q, other.q);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(q);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("SabrInArrearsVolatilityFunction{");
    buf.append("q").append('=').append(JodaBeanUtils.toString(q));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SabrInArrearsVolatilityFunction}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code q} property.
     */
    private final MetaProperty<Double> q = DirectMetaProperty.ofImmutable(
        this, "q", SabrInArrearsVolatilityFunction.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "q");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113:  // q
          return q;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public SabrInArrearsVolatilityFunction.Builder builder() {
      return new SabrInArrearsVolatilityFunction.Builder();
    }

    @Override
    public Class<? extends SabrInArrearsVolatilityFunction> beanType() {
      return SabrInArrearsVolatilityFunction.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code q} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> q() {
      return q;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113:  // q
          return ((SabrInArrearsVolatilityFunction) bean).getQ();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code SabrInArrearsVolatilityFunction}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<SabrInArrearsVolatilityFunction> {

    private double q;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(SabrInArrearsVolatilityFunction beanToCopy) {
      this.q = beanToCopy.getQ();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113:  // q
          return q;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 113:  // q
          this.q = (Double) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public SabrInArrearsVolatilityFunction build() {
      return new SabrInArrearsVolatilityFunction(
          q);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the mean reversion related parameter.
     * @param q  the new value
     * @return this, for chaining, not null
     */
    public Builder q(double q) {
      this.q = q;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("SabrInArrearsVolatilityFunction.Builder{");
      buf.append("q").append('=').append(JodaBeanUtils.toString(q));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
