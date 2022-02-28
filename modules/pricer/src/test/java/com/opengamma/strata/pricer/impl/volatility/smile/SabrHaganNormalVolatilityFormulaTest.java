/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;

/**
 * Tests {@link SabrHaganNormalVolatilityFormula}.
 */
public class SabrHaganNormalVolatilityFormulaTest {

  private static final SabrHaganNormalVolatilityFormula FORMULA = SabrHaganNormalVolatilityFormula.DEFAULT;

  private static final double FORWARD = 0.01;
  private static final double TIME = 10.0;
  private static final double ALPHA = 0.01;
  private static final double BETA = 0.5;
  private static final double RHO = 0.50;
  private static final double NU = 0.25;

  private static final Offset<Double> TOLERANCE_VOL_BETA_0 = Offset.offset(2.0E-5);
  private static final Offset<Double> TOLERANCE_VOL_HARDCODED = Offset.offset(3.0E-6);
  private static final Offset<Double> TOLERANCE_1 = Offset.offset(1.0E-6);
  private static final Offset<Double> TOLERANCE_2 = Offset.offset(1.0E-5);
  private static final Offset<Double> TOLERANCE_3 = Offset.offset(1.0E-10);

  /* Tests the formula versus external hard-coded data */
  @Test
  public void externalData() {
    double forward = 0.00901;
    double[] strikes = {0.00, 0.0050, 0.00901, 0.0100, 0.0150, 0.0200};
    double time = 9.993155373;
    double alpha = 0.006234762;
    double rho = 0.535215894;
    double nu = 0.273841661;
    double[] volatilityExpected = {59.00d, 61.74, 64.57, 65.33, 69.46, 73.89};
    double bp1 = 1.0E-4;
    for (int i = 0; i < strikes.length; i++) {
      double volatilityComputed = FORMULA.volatilityBeta0(forward, strikes[i], time, alpha, rho, nu);
      assertThat(volatilityComputed).isCloseTo(volatilityExpected[i] * bp1, TOLERANCE_VOL_HARDCODED);
    }
  }

  /* Tests the formula versus external hard-coded data, negative forward */
  @Test
  public void externalData2() {
    double forward = -0.00256;
    double[] strikes = {-0.00256, 0.0, 0.005, 0.01, 0.015, 0.02};
    double time = 0.999315537;
    double alpha = 0.003946629;
    double rho = 0.831235327;
    double nu = 1.204374903;
    double[] volatilityExpected = {39.29, 51.62, 73.11, 92.29, 110.05, 126.82};
    double bp1 = 1.0E-4;
    for (int i = 0; i < strikes.length; i++) {
      double volatilityComputed = FORMULA.volatilityBeta0(forward, strikes[i], time, alpha, rho, nu);
      assertThat(volatilityComputed).isCloseTo(volatilityExpected[i] * bp1, TOLERANCE_VOL_HARDCODED);
    }
  }

  /* Tests the specific formula for beta = 0 versus the generic formula. The results are not perfectly
   * identical as the generic version has a barrier at 0. */
  @Test
  public void beta0() {
    double[] strikes = {0.0050, 0.0100, 0.0150, 0.0200};
    for (int i = 0; i < strikes.length; i++) {
      double volatilityGeneric = FORMULA.volatility(FORWARD, strikes[i], TIME, ALPHA, 0.0, RHO, NU);
      double volatilitySpecific = FORMULA.volatilityBeta0(FORWARD, strikes[i], TIME, ALPHA, RHO, NU);
      assertThat(volatilityGeneric).isCloseTo(volatilitySpecific, TOLERANCE_VOL_BETA_0);
    }
  }

  @Test
  public void zetaOverXhatAtm() {
    double[] zeta = {-1.0E-7, 0.0, 1E-7, 2.0E-6};
    for (int loopzeta = 0; loopzeta < zeta.length; loopzeta++) {
      double ratioComputed = FORMULA.zetaOverXhat(zeta[loopzeta], RHO);
      double ratioExpected = 1.0d - 0.5d * RHO * zeta[loopzeta];
      assertThat(ratioComputed).isCloseTo(ratioExpected, TOLERANCE_3);
    }
  }

  @Test
  public void zetaOverXhatOtm() {
    double[] zeta = {-1.0E-1, 2.0E-6, 2.0E-1};
    for (int loopzeta = 0; loopzeta < zeta.length; loopzeta++) {
      double ratioComputed = FORMULA.zetaOverXhat(zeta[loopzeta], RHO);
      double ratioExpected = zeta[loopzeta] /
          Math.log(
              (Math.sqrt(1.0d - 2.0d * RHO * zeta[loopzeta] + zeta[loopzeta] * zeta[loopzeta]) - RHO + zeta[loopzeta]) /
                  (1.0 - RHO));
      assertThat(ratioComputed).isCloseTo(ratioExpected, TOLERANCE_3);
    }
  }

  /* Test against local implementation */
  @Test
  public void volatilityBetaNonZero() {
    double[] forward = {0.0100, 0.0150};
    double[] strikes = {0.0050, 0.0100, 0.0150, 0.0200};
    double[] time = {5.0, 10.0};
    double[] alpha = {0.01, 0.02};
    double[] rho = {-0.25, 0.50};
    double[] nu = {0.25, 0.50};
    for (int loopforward = 0; loopforward < forward.length; loopforward++) {
      for (int loopstrikes = 0; loopstrikes < strikes.length; loopstrikes++) {
        for (int looptime = 0; looptime < time.length; looptime++) {
          for (int loopalpha = 0; loopalpha < alpha.length; loopalpha++) {
            for (int looprho = 0; looprho < rho.length; looprho++) {
              for (int loopnu = 0; loopnu < nu.length; loopnu++) {
                double fK = forward[loopforward] * strikes[loopstrikes];
                double fKBeta05 = Math.pow(fK, 0.5 * BETA);
                double logSK = Math.log(forward[loopforward] / strikes[loopstrikes]);
                double oneminusbeta = 1.0d - BETA;
                double term1 = alpha[loopalpha] * fKBeta05;
                double term2 = (1.0 + logSK * logSK / 24.0 + Math.pow(logSK, 4) / 1920.0) /
                    (1.0 + oneminusbeta * oneminusbeta * logSK * logSK / 24.0 + Math.pow(oneminusbeta * logSK, 4) / 1920.0);
                double zeta = nu[loopnu] / alpha[loopalpha] *
                    Math.pow(forward[loopforward] * strikes[loopstrikes], 0.5 * oneminusbeta) * logSK;
                double term3 = FORMULA.zetaOverXhat(zeta, rho[looprho]);
                double term4 = time[looptime] *
                    ((Math.pow(oneminusbeta, 2.0d) - 1.0d) * alpha[loopalpha] * alpha[loopalpha] /
                        (24 * Math.pow(forward[loopforward] * strikes[loopstrikes], oneminusbeta)) +
                        rho[looprho] * BETA * nu[loopnu] * alpha[loopalpha] /
                            (4.0d * Math.pow(forward[loopforward] * strikes[loopstrikes], 0.5 * oneminusbeta)) +
                        (2.0d - 3.0d * rho[looprho] * rho[looprho]) * nu[loopnu] * nu[loopnu] / 24.0d);
                double volatilityExpected = term1 * term2 * term3 * (1.0d + term4);
                double volatilityComputed = FORMULA.volatility(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], BETA, rho[looprho], nu[loopnu]);
                assertThat(volatilityExpected).isCloseTo(volatilityComputed, TOLERANCE_1);
              }
            }
          }
        }
      }
    }
  }

  @Test
  public void zetaOverXhatAdjoint() {
    double shiftFd = 1.0E-6;
    double[] zeta = {-1.5, -0.5, 0.0, 0.5, 1.0, 2.0};
    double[] rho = {-0.5, 0.0, 0.5};
    for (int loopzeta = 0; loopzeta < zeta.length; loopzeta++) {
      for (int looprho = 0; looprho < rho.length; looprho++) {
        ValueDerivatives resultComputed = FORMULA.zetaOverXhatAdjoint(zeta[loopzeta], rho[looprho]);
        double valueExpected = FORMULA.zetaOverXhat(zeta[loopzeta], rho[looprho]);
        assertThat(resultComputed.getValue()).isCloseTo(valueExpected, TOLERANCE_1);
        double valueZetaP = FORMULA.zetaOverXhat(zeta[loopzeta] + shiftFd, rho[looprho]);
        double derivativeZetaExpected = (valueZetaP - valueExpected) / shiftFd;
        assertThat(resultComputed.getDerivative(0)).isCloseTo(derivativeZetaExpected, TOLERANCE_1);
        double valueRhoP = FORMULA.zetaOverXhat(zeta[loopzeta], rho[looprho] + shiftFd);
        double derivativeRhoExpected = (valueRhoP - valueExpected) / shiftFd;
        assertThat(resultComputed.getDerivative(1)).isCloseTo(derivativeRhoExpected, TOLERANCE_1);
      }
    }
  }

  /* Test the adjoint version of the normal implied volatility for beta=0 versus finite difference. */
  @Test
  public void volatilityBeta0Adjoint() {
    double shiftFd = 1.0E-8;
    double[] forward = {-0.0025, 0.00, 0.0100};
    double[] strikes = {-0.0050, 0.0050, 0.0100, 0.0150};
    double[] time = {5.0, 10.0};
    double[] alpha = {0.01, 0.02};
    double[] rho = {-0.25, 0.50};
    double[] nu = {0.25, 0.50};
    for (int loopforward = 0; loopforward < forward.length; loopforward++) {
      for (int loopstrikes = 0; loopstrikes < strikes.length; loopstrikes++) {
        for (int looptime = 0; looptime < time.length; looptime++) {
          for (int loopalpha = 0; loopalpha < alpha.length; loopalpha++) {
            for (int looprho = 0; looprho < rho.length; looprho++) {
              for (int loopnu = 0; loopnu < nu.length; loopnu++) {
                ValueDerivatives resultComputed = FORMULA.volatilityBeta0Adjoint(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], rho[looprho], nu[loopnu]);
                double valueExpected = FORMULA.volatilityBeta0(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], rho[looprho], nu[loopnu]);
                assertThat(resultComputed.getValue()).isCloseTo(valueExpected, TOLERANCE_1);
                double valueForwardP = FORMULA.volatilityBeta0(
                    forward[loopforward] + shiftFd, strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], rho[looprho], nu[loopnu]);
                double derivativeForwardExpected = (valueForwardP - valueExpected) / shiftFd;
                assertThat(resultComputed.getDerivative(0)).isCloseTo(derivativeForwardExpected, TOLERANCE_1);
                double valueStrikesP = FORMULA.volatilityBeta0(
                    forward[loopforward], strikes[loopstrikes] + shiftFd, time[looptime],
                    alpha[loopalpha], rho[looprho], nu[loopnu]);
                double derivativeStrikesExpected = (valueStrikesP - valueExpected) / shiftFd;
                assertThat(resultComputed.getDerivative(1)).isCloseTo(derivativeStrikesExpected, TOLERANCE_1);
                double valueAlphaP = FORMULA.volatilityBeta0(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha] + shiftFd, rho[looprho], nu[loopnu]);
                double derivativeAlphaExpected = (valueAlphaP - valueExpected) / shiftFd;
                assertThat(resultComputed.getDerivative(2)).isCloseTo(derivativeAlphaExpected, TOLERANCE_1);
                double valueRhoP = FORMULA.volatilityBeta0(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], rho[looprho] + shiftFd, nu[loopnu]);
                double derivativeRhoExpected = (valueRhoP - valueExpected) / shiftFd;
                assertThat(resultComputed.getDerivative(3)).isCloseTo(derivativeRhoExpected, TOLERANCE_1);
                double valueNuP = FORMULA.volatilityBeta0(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], rho[looprho], nu[loopnu] + shiftFd);
                double derivativeNuExpected = (valueNuP - valueExpected) / shiftFd;
                assertThat(resultComputed.getDerivative(4)).isCloseTo(derivativeNuExpected, TOLERANCE_1);
              }
            }
          }
        }
      }
    }
  }

  /* Test the adjoint version of the normal implied volatility versus finite difference. */
  @Test
  public void volatilityAdjoint() {
    double shiftFd = 1.0E-9;
    double[] forward = {0.0100, 0.0150};
    double[] strikes = {0.0050, 0.0100, 0.0150, 0.0200};
    double[] time = {5.0, 10.0};
    double[] alpha = {0.01, 0.02};
    double beta = 0.5;
    double[] rho = {-0.25, 0.50};
    double[] nu = {0.25, 0.50};
    for (int loopforward = 0; loopforward < forward.length; loopforward++) {
      for (int loopstrikes = 0; loopstrikes < strikes.length; loopstrikes++) {
        for (int looptime = 0; looptime < time.length; looptime++) {
          for (int loopalpha = 0; loopalpha < alpha.length; loopalpha++) {
            for (int looprho = 0; looprho < rho.length; looprho++) {
              for (int loopnu = 0; loopnu < nu.length; loopnu++) {
                ValueDerivatives resultComputed = FORMULA.volatilityAdjoint(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], beta, rho[looprho], nu[loopnu]);
                double valueExpected = FORMULA.volatility(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], beta, rho[looprho], nu[loopnu]);
                assertThat(resultComputed.getValue()).isCloseTo(valueExpected, TOLERANCE_1);
                double valueForwardP = FORMULA.volatility(
                    forward[loopforward] + shiftFd, strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], beta, rho[looprho], nu[loopnu]);
                double derivativeForwardExpected = (valueForwardP - valueExpected) / shiftFd;
                assertThat(resultComputed.getDerivative(0)).isCloseTo(derivativeForwardExpected, TOLERANCE_2);
                double valueStrikesP = FORMULA.volatility(
                    forward[loopforward], strikes[loopstrikes] + shiftFd, time[looptime],
                    alpha[loopalpha], beta, rho[looprho], nu[loopnu]);
                double derivativeStrikesExpected = (valueStrikesP - valueExpected) / shiftFd;
                assertThat(resultComputed.getDerivative(1)).isCloseTo(derivativeStrikesExpected, TOLERANCE_2);
                double valueAlphaP = FORMULA.volatility(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha] + shiftFd, beta, rho[looprho], nu[loopnu]);
                double derivativeAlphaExpected = (valueAlphaP - valueExpected) / shiftFd;
                assertThat(resultComputed.getDerivative(2)).isCloseTo(derivativeAlphaExpected, TOLERANCE_2);
                double valueBetaP = FORMULA.volatility(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], beta + shiftFd, rho[looprho], nu[loopnu]);
                double derivativeBetaExpected = (valueBetaP - valueExpected) / shiftFd;
                assertThat(resultComputed.getDerivative(3)).isCloseTo(derivativeBetaExpected, TOLERANCE_2);
                double valueRhoP = FORMULA.volatility(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], beta, rho[looprho] + shiftFd, nu[loopnu]);
                double derivativeRhoExpected = (valueRhoP - valueExpected) / shiftFd;
                assertThat(resultComputed.getDerivative(4)).isCloseTo(derivativeRhoExpected, TOLERANCE_1);
                double valueNuP = FORMULA.volatility(
                    forward[loopforward], strikes[loopstrikes], time[looptime],
                    alpha[loopalpha], beta, rho[looprho], nu[loopnu] + shiftFd);
                double derivativeNuExpected = (valueNuP - valueExpected) / shiftFd;
                assertThat(resultComputed.getDerivative(5)).isCloseTo(derivativeNuExpected, TOLERANCE_1);

              }
            }
          }
        }
      }
    }
  }

  @Test
  public void testNegativeForward() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FORMULA.volatility(-0.01, 0.01, TIME, ALPHA, 0.5, RHO, NU));
  }

  @Test
  public void testNegativeStrike() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FORMULA.volatility(0.01, -0.01, TIME, ALPHA, 0.5, RHO, NU));
  }

  @Test
  public void testNegativeRho1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FORMULA.volatility(0.01, 0.01, TIME, ALPHA, 0.5, 1.0, NU));
  }

  @Test
  public void testNegativeRho_1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FORMULA.volatility(0.01, 0.01, TIME, ALPHA, 0.5, -1.0, NU));
  }

}
