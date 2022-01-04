/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;

/**
 * Tests {@link SabrInArrearsVolatilityFunction}
 */
public class SabrInArrearsVolatilityFunctionTest {

  private static final double NU = 0.8;
  private static final double RHO = -0.65;
  private static final double BETA = 0.76;
  private static final double ALPHA = 1.4;
  private static final SabrFormulaData SABR_DATA = SabrFormulaData.of(ALPHA, BETA, RHO, NU);
  private static final double TAU0_BEFORE = 1.0;
  private static final double TAU1_BEFORE = 1.5;
  private static final double TAU0_AFTER = -0.10;
  private static final double TAU1_AFTER = 0.40;
  private static final SabrInArrearsVolatilityFunction FUNCTION_DEFAULT =
      SabrInArrearsVolatilityFunction.DEFAULT;
  private static final double Q_OTHER = 1.1;
  private static final SabrInArrearsVolatilityFunction FUNCTION_OTHER =
      SabrInArrearsVolatilityFunction.of(Q_OTHER);

  private static final Offset<Double> TOLERANCE_PARAMETER = Offset.offset(1.0E-8);
  private static final Offset<Double> TOLERANCE_APPROX = Offset.offset(2.0E-3);
  private static final Offset<Double> TOLERANCE_PARAMETER_AD = Offset.offset(1.0E-6);

  /* Test the special case for alpha with q=1 (Piterbarg (2020) formula*/
  @Test
  public void alpha_q1() {
    double alphaTilde = ALPHA * Math.sqrt(1.0d + (TAU1_BEFORE - TAU0_BEFORE) / (3 * TAU0_BEFORE));
    SabrFormulaData parametersComputed = FUNCTION_DEFAULT.effectiveSabr(SABR_DATA, TAU0_BEFORE, TAU1_BEFORE);
    double alphaHatComputed = parametersComputed.getAlpha();
    assertThat(alphaTilde).isEqualTo(alphaHatComputed * Math.sqrt(TAU1_BEFORE / TAU0_BEFORE), TOLERANCE_APPROX);
    double tau = 2 * TAU0_BEFORE + TAU1_BEFORE;
    double tau2 = tau * tau;
    double tau3 = tau2 * tau;
    double tau02 = Math.pow(TAU0_BEFORE, 2);
    double tau03 = Math.pow(TAU0_BEFORE, 3);
    double tau12 = Math.pow(TAU1_BEFORE, 2);
    double tau13 = Math.pow(TAU1_BEFORE, 3);
    double gamma1 =
        tau * (2 * tau3 + tau13 + 2 * tau03 + 6 * tau02 * TAU1_BEFORE) / 21;
    double gamma2 = 3 * RHO * RHO * (TAU1_BEFORE - TAU0_BEFORE) * (TAU1_BEFORE - TAU0_BEFORE) *
        (3 * tau2 - tau12 + 5 * tau02 + 4 * TAU0_BEFORE * TAU1_BEFORE) / (7 * 25);
    double gamma = gamma1 + gamma2;
    double nuHat2 = NU * NU * gamma * 3 / (tau3 * TAU1_BEFORE);
    double h = NU * NU * (tau2 + 2 * tau02 + tau12) / (4 * TAU1_BEFORE * tau) - nuHat2;
    double adjustment = Math.exp(0.25 * h * TAU1_BEFORE);
    assertThat(alphaTilde * adjustment).isEqualTo(alphaHatComputed * Math.sqrt(TAU1_BEFORE / TAU0_BEFORE),
        TOLERANCE_PARAMETER);
  }

  /* Formula for tau_0 > 0 and q = 1. Local implementation.*/
  @Test
  public void reduced_before_formula_q1() {
    double tau = 2 * TAU0_BEFORE + TAU1_BEFORE;
    double tau2 = tau * tau;
    double tau3 = tau2 * tau;
    double tau02 = TAU0_BEFORE * TAU0_BEFORE;
    double tau03 = tau02 * TAU0_BEFORE;
    double tau12 = TAU1_BEFORE * TAU1_BEFORE;
    double tau13 = tau12 * TAU1_BEFORE;
    double gamma1 = tau * (2 * tau3 + tau13 + 2 * tau03 + 6 * tau02 * TAU1_BEFORE) / 21;
    double gamma2 = 3 * RHO * RHO * (TAU1_BEFORE - TAU0_BEFORE) * (TAU1_BEFORE - TAU0_BEFORE) *
        (3 * tau2 - tau12 + 5 * tau02 + 4 * TAU0_BEFORE * TAU1_BEFORE) / (7 * 25);
    double gamma = gamma1 + gamma2;
    double rhoHat = RHO * (3 * tau2 + 2 * tau02 + tau12) / (Math.sqrt(gamma) * 10);
    double nuHat2 = NU * NU * gamma * 3 / (tau3 * TAU1_BEFORE);
    double nuHat = Math.sqrt(nuHat2);
    double h = NU * NU * (tau2 + 2 * tau02 + tau12) / (4 * TAU1_BEFORE * tau) - nuHat2;
    double alphaHat2 = ALPHA * ALPHA / 3.0d * tau / TAU1_BEFORE * Math.exp(0.5 * h * TAU1_BEFORE);
    double alphaHat = Math.sqrt(alphaHat2);
    SabrFormulaData parametersComputed = FUNCTION_DEFAULT.effectiveSabr(SABR_DATA, TAU0_BEFORE, TAU1_BEFORE);
    assertThat(alphaHat).isEqualTo(parametersComputed.getAlpha(), TOLERANCE_PARAMETER);
    assertThat(BETA).isEqualTo(parametersComputed.getBeta(), TOLERANCE_PARAMETER);
    assertThat(rhoHat).isEqualTo(parametersComputed.getRho(), TOLERANCE_PARAMETER);
    assertThat(nuHat).isEqualTo(parametersComputed.getNu(), TOLERANCE_PARAMETER);
  }

  /* Formula for tau_0 > 0 and q != 1. Local implementation. */
  @Test
  public void reduced_before_formula_q0ther() {
    double tau = 2 * Q_OTHER * TAU0_BEFORE + TAU1_BEFORE;
    double tau2 = Math.pow(tau, 2);
    double tau3 = Math.pow(tau, 3);
    double tau02 = Math.pow(TAU0_BEFORE, 2);
    double tau03 = Math.pow(TAU0_BEFORE, 3);
    double tau12 = Math.pow(TAU1_BEFORE, 2);
    double tau13 = Math.pow(TAU1_BEFORE, 3);
    double gamma1 = tau *
        (2 * tau3 + tau13 + (4 * Math.pow(Q_OTHER, 2) - 2 * Q_OTHER) * tau03 + 6 * Q_OTHER * tau02 * TAU1_BEFORE) /
        ((4 * Q_OTHER + 3) * (2 * Q_OTHER + 1));
    double gamma2 = 3 * Q_OTHER * Math.pow(RHO, 2) * Math.pow(TAU1_BEFORE - TAU0_BEFORE, 2) *
        (3 * tau2 - tau12 + 5 * Q_OTHER * tau02 + 4 * TAU0_BEFORE * TAU1_BEFORE) /
        ((4 * Q_OTHER + 3) * Math.pow(3 * Q_OTHER + 2, 2));
    double gamma = gamma1 + gamma2;
    double rhoHat = RHO * (3 * tau2 + 2 * Q_OTHER * tau02 + tau12) / (Math.sqrt(gamma) * (6 * Q_OTHER + 4));
    double nuHat2 = NU * NU * gamma * (2 * Q_OTHER + 1) / (tau3 * TAU1_BEFORE);
    double nuHat = Math.sqrt(nuHat2);
    double h =
        Math.pow(NU, 2) * (tau2 + 2 * Q_OTHER * tau02 + tau12) / (2 * TAU1_BEFORE * tau * (Q_OTHER + 1)) - nuHat2;
    double alphaHat2 = ALPHA * ALPHA / (2 * Q_OTHER + 1) * tau / TAU1_BEFORE * Math.exp(0.5 * h * TAU1_BEFORE);
    double alphaHat = Math.sqrt(alphaHat2);
    SabrFormulaData parametersComputed = FUNCTION_OTHER.effectiveSabr(SABR_DATA, TAU0_BEFORE, TAU1_BEFORE);
    assertThat(alphaHat).isEqualTo(parametersComputed.getAlpha(), TOLERANCE_PARAMETER);
    assertThat(BETA).isEqualTo(parametersComputed.getBeta(), TOLERANCE_PARAMETER);
    assertThat(rhoHat).isEqualTo(parametersComputed.getRho(), TOLERANCE_PARAMETER);
    assertThat(nuHat).isEqualTo(parametersComputed.getNu(), TOLERANCE_PARAMETER);
  }

  /* Formula for tau_0 <= 0 and q != 1. Local implementation. */
  @Test
  public void after_formula() {
    double zeta = 3.0d / (4 * Q_OTHER + 3) *
        (1.0d / (2 * Q_OTHER + 1) + Math.pow(RHO, 2) * 2 * Q_OTHER / Math.pow(3 * Q_OTHER + 2, 2));
    double rhoHat = 2 * RHO / (Math.sqrt(zeta) * (3 * Q_OTHER + 2));
    double nuHat2 = Math.pow(NU, 2) * zeta * (2 * Q_OTHER + 1);
    double nuHat = Math.sqrt(nuHat2);
    double alphaHat2 = Math.pow(ALPHA, 2) /
        (2 * Q_OTHER + 1) * Math.pow(TAU1_AFTER / (TAU1_AFTER - TAU0_AFTER), 2 * Q_OTHER) *
        Math.exp(0.5 * (Math.pow(NU, 2) / (Q_OTHER + 1) - nuHat2) * TAU1_AFTER);
    double alphaHat = Math.sqrt(alphaHat2);
    SabrFormulaData parametersComputed = FUNCTION_OTHER.effectiveSabr(SABR_DATA, TAU0_AFTER, TAU1_AFTER);
    assertThat(alphaHat).isEqualTo(parametersComputed.getAlpha(), TOLERANCE_PARAMETER);
    assertThat(BETA).isEqualTo(parametersComputed.getBeta(), TOLERANCE_PARAMETER);
    assertThat(rhoHat).isEqualTo(parametersComputed.getRho(), TOLERANCE_PARAMETER);
    assertThat(nuHat).isEqualTo(parametersComputed.getNu(), TOLERANCE_PARAMETER);
  }

  @Test
  public void effectiveParamtersAd() {
    List<ValueDerivatives> adAfterGeneric = FUNCTION_OTHER.effectiveSabrAd(SABR_DATA, TAU0_AFTER, TAU1_AFTER);
    List<ValueDerivatives> adAfterSpecific =
        FUNCTION_OTHER.effectiveSabrAfterStartAd(SABR_DATA, TAU0_AFTER, TAU1_AFTER);
    assertThat(adAfterGeneric.size()).isEqualTo(adAfterSpecific.size());
    for (int i = 0; i < adAfterGeneric.size(); i++) {
      assertThat(adAfterGeneric.get(i).getValue()).isEqualTo(adAfterSpecific.get(i).getValue(), TOLERANCE_PARAMETER);
      assertThat(adAfterGeneric.get(i).getDerivatives()
          .equalWithTolerance(adAfterSpecific.get(i).getDerivatives(), TOLERANCE_PARAMETER_AD.value)).isTrue();
    }
    List<ValueDerivatives> adBeforeGeneric = FUNCTION_OTHER.effectiveSabrAd(SABR_DATA, TAU0_BEFORE, TAU1_BEFORE);
    List<ValueDerivatives> adBeforeSpecific =
        FUNCTION_OTHER.effectiveSabrBeforeStartAd(SABR_DATA, TAU0_BEFORE, TAU1_BEFORE);
    assertThat(adAfterGeneric.size()).isEqualTo(adAfterSpecific.size());
    for (int i = 0; i < adAfterGeneric.size(); i++) {
      assertThat(adBeforeGeneric.get(i).getValue()).isEqualTo(adBeforeSpecific.get(i).getValue(), TOLERANCE_PARAMETER);
      assertThat(adBeforeGeneric.get(i).getDerivatives()
          .equalWithTolerance(adBeforeSpecific.get(i).getDerivatives(), TOLERANCE_PARAMETER_AD.value)).isTrue();
    }
  }

  @Test
  public void afterStartAd() {
    SabrFormulaData value = FUNCTION_OTHER.effectiveSabrAfterStart(SABR_DATA, TAU0_AFTER, TAU1_AFTER);
    List<ValueDerivatives> ad = FUNCTION_OTHER.effectiveSabrAfterStartAd(SABR_DATA, TAU0_AFTER, TAU1_AFTER);
    double fdShift = 1.0E-8;
    // Alpha shift
    SabrFormulaData sabrAlphaShifted = SabrFormulaData.of(ALPHA + fdShift, BETA, RHO, NU);
    SabrFormulaData valueAlphaShifted =
        FUNCTION_OTHER.effectiveSabrAfterStart(sabrAlphaShifted, TAU0_AFTER, TAU1_AFTER);
    // Beta shift
    SabrFormulaData sabrBetaShifted = SabrFormulaData.of(ALPHA, BETA + fdShift, RHO, NU);
    SabrFormulaData valueBetaShifted = FUNCTION_OTHER.effectiveSabrAfterStart(sabrBetaShifted, TAU0_AFTER, TAU1_AFTER);
    // Rho shift
    SabrFormulaData sabrRhoShifted = SabrFormulaData.of(ALPHA, BETA, RHO + fdShift, NU);
    SabrFormulaData valueRhoShifted = FUNCTION_OTHER.effectiveSabrAfterStart(sabrRhoShifted, TAU0_AFTER, TAU1_AFTER);
    // Nu shift
    SabrFormulaData sabrNuShifted = SabrFormulaData.of(ALPHA, BETA, RHO, NU + fdShift);
    SabrFormulaData valueNuShifted = FUNCTION_OTHER.effectiveSabrAfterStart(sabrNuShifted, TAU0_AFTER, TAU1_AFTER);
    // Tau0 shift
    SabrFormulaData valueTau0Shifted =
        FUNCTION_OTHER.effectiveSabrAfterStart(SABR_DATA, TAU0_AFTER + fdShift, TAU1_AFTER);
    // Tau0 shift
    SabrFormulaData valueTau1Shifted =
        FUNCTION_OTHER.effectiveSabrAfterStart(SABR_DATA, TAU0_AFTER, TAU1_AFTER + fdShift);
    /* Alpha result */
    assertThat(value.getAlpha()).isEqualTo(ad.get(0).getValue(), TOLERANCE_PARAMETER);
    assertThat(ad.get(0).getDerivative(0))
        .isEqualTo((valueAlphaShifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(0).getDerivative(1))
        .isEqualTo((valueBetaShifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(0).getDerivative(2))
        .isEqualTo((valueRhoShifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(0).getDerivative(3))
        .isEqualTo((valueNuShifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(0).getDerivative(4))
        .isEqualTo((valueTau0Shifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(0).getDerivative(5))
        .isEqualTo((valueTau1Shifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    /* Beta result */
    assertThat(value.getBeta()).isEqualTo(ad.get(1).getValue(), TOLERANCE_PARAMETER);
    assertThat(ad.get(1).getDerivative(0))
        .isEqualTo((valueAlphaShifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(1).getDerivative(1))
        .isEqualTo((valueBetaShifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(1).getDerivative(2))
        .isEqualTo((valueRhoShifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(1).getDerivative(3))
        .isEqualTo((valueNuShifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(1).getDerivative(4))
        .isEqualTo((valueTau0Shifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(1).getDerivative(5))
        .isEqualTo((valueTau1Shifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    /* Rho result */
    assertThat(value.getRho()).isEqualTo(ad.get(2).getValue(), TOLERANCE_PARAMETER);
    assertThat(ad.get(2).getDerivative(0))
        .isEqualTo((valueAlphaShifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(2).getDerivative(1))
        .isEqualTo((valueBetaShifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(2).getDerivative(2))
        .isEqualTo((valueRhoShifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(2).getDerivative(3))
        .isEqualTo((valueNuShifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(2).getDerivative(4))
        .isEqualTo((valueTau0Shifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(2).getDerivative(5))
        .isEqualTo((valueTau1Shifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    /* Nu result */
    assertThat(value.getNu()).isEqualTo(ad.get(3).getValue(), TOLERANCE_PARAMETER);
    assertThat(ad.get(3).getDerivative(0))
        .isEqualTo((valueAlphaShifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(3).getDerivative(1))
        .isEqualTo((valueBetaShifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(3).getDerivative(2))
        .isEqualTo((valueRhoShifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(3).getDerivative(3))
        .isEqualTo((valueNuShifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(3).getDerivative(4))
        .isEqualTo((valueTau0Shifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(3).getDerivative(5))
        .isEqualTo((valueTau1Shifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
  }

  @Test
  public void beforeStartAd() {
    SabrFormulaData value = FUNCTION_OTHER.effectiveSabrBeforeStart(SABR_DATA, TAU0_BEFORE, TAU1_BEFORE);
    List<ValueDerivatives> ad = FUNCTION_OTHER.effectiveSabrBeforeStartAd(SABR_DATA, TAU0_BEFORE, TAU1_BEFORE);
    double fdShift = 1.0E-8;
    // Alpha shift
    SabrFormulaData sabrAlphaShifted = SabrFormulaData.of(ALPHA + fdShift, BETA, RHO, NU);
    SabrFormulaData valueAlphaShifted =
        FUNCTION_OTHER.effectiveSabrBeforeStart(sabrAlphaShifted, TAU0_BEFORE, TAU1_BEFORE);
    // Beta shift
    SabrFormulaData sabrBetaShifted = SabrFormulaData.of(ALPHA, BETA + fdShift, RHO, NU);
    SabrFormulaData valueBetaShifted =
        FUNCTION_OTHER.effectiveSabrBeforeStart(sabrBetaShifted, TAU0_BEFORE, TAU1_BEFORE);
    // Rho shift
    SabrFormulaData sabrRhoShifted = SabrFormulaData.of(ALPHA, BETA, RHO + fdShift, NU);
    SabrFormulaData valueRhoShifted = FUNCTION_OTHER.effectiveSabrBeforeStart(sabrRhoShifted, TAU0_BEFORE, TAU1_BEFORE);
    // Nu shift
    SabrFormulaData sabrNuShifted = SabrFormulaData.of(ALPHA, BETA, RHO, NU + fdShift);
    SabrFormulaData valueNuShifted = FUNCTION_OTHER.effectiveSabrBeforeStart(sabrNuShifted, TAU0_BEFORE, TAU1_BEFORE);
    // Tau0 shift
    SabrFormulaData valueTau0Shifted =
        FUNCTION_OTHER.effectiveSabrBeforeStart(SABR_DATA, TAU0_BEFORE + fdShift, TAU1_BEFORE);
    // Tau0 shift
    SabrFormulaData valueTau1Shifted =
        FUNCTION_OTHER.effectiveSabrBeforeStart(SABR_DATA, TAU0_BEFORE, TAU1_BEFORE + fdShift);
    /* Alpha result */
    assertThat(value.getAlpha()).isEqualTo(ad.get(0).getValue(), TOLERANCE_PARAMETER);
    assertThat(ad.get(0).getDerivative(0))
        .isEqualTo((valueAlphaShifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(0).getDerivative(1))
        .isEqualTo((valueBetaShifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(0).getDerivative(2))
        .isEqualTo((valueRhoShifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(0).getDerivative(3))
        .isEqualTo((valueNuShifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(0).getDerivative(4))
        .isEqualTo((valueTau0Shifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(0).getDerivative(5))
        .isEqualTo((valueTau1Shifted.getAlpha() - value.getAlpha()) / fdShift, TOLERANCE_PARAMETER_AD);
    /* Beta result */
    assertThat(value.getBeta()).isEqualTo(ad.get(1).getValue(), TOLERANCE_PARAMETER);
    assertThat(ad.get(1).getDerivative(0))
        .isEqualTo((valueAlphaShifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(1).getDerivative(1))
        .isEqualTo((valueBetaShifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(1).getDerivative(2))
        .isEqualTo((valueRhoShifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(1).getDerivative(3))
        .isEqualTo((valueNuShifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(1).getDerivative(4))
        .isEqualTo((valueTau0Shifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(1).getDerivative(5))
        .isEqualTo((valueTau1Shifted.getBeta() - value.getBeta()) / fdShift, TOLERANCE_PARAMETER_AD);
    /* Rho result */
    assertThat(value.getRho()).isEqualTo(ad.get(2).getValue(), TOLERANCE_PARAMETER);
    assertThat(ad.get(2).getDerivative(0))
        .isEqualTo((valueAlphaShifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(2).getDerivative(1))
        .isEqualTo((valueBetaShifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(2).getDerivative(2))
        .isEqualTo((valueRhoShifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(2).getDerivative(3))
        .isEqualTo((valueNuShifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(2).getDerivative(4))
        .isEqualTo((valueTau0Shifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(2).getDerivative(5))
        .isEqualTo((valueTau1Shifted.getRho() - value.getRho()) / fdShift, TOLERANCE_PARAMETER_AD);
    /* Nu result */
    assertThat(value.getNu()).isEqualTo(ad.get(3).getValue(), TOLERANCE_PARAMETER);
    assertThat(ad.get(3).getDerivative(0))
        .isEqualTo((valueAlphaShifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(3).getDerivative(1))
        .isEqualTo((valueBetaShifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(3).getDerivative(2))
        .isEqualTo((valueRhoShifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(3).getDerivative(3))
        .isEqualTo((valueNuShifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(3).getDerivative(4))
        .isEqualTo((valueTau0Shifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
    assertThat(ad.get(3).getDerivative(5))
        .isEqualTo((valueTau1Shifted.getNu() - value.getNu()) / fdShift, TOLERANCE_PARAMETER_AD);
  }

}
