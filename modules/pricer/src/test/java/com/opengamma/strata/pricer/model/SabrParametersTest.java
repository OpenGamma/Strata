/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.model;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link SabrParameters}.
 */
public class SabrParametersTest {

  private static final InterpolatedNodalCurve ALPHA_CURVE =
      InterpolatedNodalCurve.of(
          Curves.sabrParameterByExpiry("SabrAlpha", ACT_ACT_ISDA, ValueType.SABR_ALPHA),
          DoubleArray.of(0, 10),
          DoubleArray.of(0.2, 0.2),
          LINEAR);
  private static final InterpolatedNodalCurve BETA_CURVE =
      InterpolatedNodalCurve.of(Curves.sabrParameterByExpiry("SabrBeta", ACT_ACT_ISDA, ValueType.SABR_BETA),
          DoubleArray.of(0, 10),
          DoubleArray.of(1, 1),
          LINEAR);
  private static final InterpolatedNodalCurve RHO_CURVE =
      InterpolatedNodalCurve.of(
          Curves.sabrParameterByExpiry("SabrRho", ACT_ACT_ISDA, ValueType.SABR_RHO),
          DoubleArray.of(0, 10),
          DoubleArray.of(-0.5, -0.5),
          LINEAR);
  private static final InterpolatedNodalCurve NU_CURVE =
      InterpolatedNodalCurve.of(
          Curves.sabrParameterByExpiry("SabrNu", ACT_ACT_ISDA, ValueType.SABR_NU),
          DoubleArray.of(0, 10),
          DoubleArray.of(0.5, 0.5),
          LINEAR);
  private static final SabrVolatilityFormula FORMULA = SabrVolatilityFormula.hagan();
  private static final SabrParameters PARAMETERS =
      SabrParameters.of(ALPHA_CURVE, BETA_CURVE, RHO_CURVE, NU_CURVE, FORMULA);

  @Test
  public void getter() {
    assertThat(PARAMETERS.getAlphaCurve()).isEqualTo(ALPHA_CURVE);
    assertThat(PARAMETERS.getBetaCurve()).isEqualTo(BETA_CURVE);
    assertThat(PARAMETERS.getRhoCurve()).isEqualTo(RHO_CURVE);
    assertThat(PARAMETERS.getNuCurve()).isEqualTo(NU_CURVE);
    assertThat(PARAMETERS.getSabrVolatilityFormula()).isEqualTo(FORMULA);
    assertThat(PARAMETERS.getShiftCurve().getName()).isEqualTo(CurveName.of("Zero shift"));
    assertThat(PARAMETERS.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(PARAMETERS.getParameterCount()).isEqualTo(9);
    double expiry = 2.0;
    double alpha = ALPHA_CURVE.yValue(expiry);
    double beta = BETA_CURVE.yValue(expiry);
    double rho = RHO_CURVE.yValue(expiry);
    double nu = NU_CURVE.yValue(expiry);
    assertThat(PARAMETERS.alpha(expiry)).isEqualTo(alpha);
    assertThat(PARAMETERS.beta(expiry)).isEqualTo(beta);
    assertThat(PARAMETERS.rho(expiry)).isEqualTo(rho);
    assertThat(PARAMETERS.nu(expiry)).isEqualTo(nu);
    double strike = 1.1;
    double forward = 1.05;
    assertThat(PARAMETERS.volatility(expiry, strike, forward)).isEqualTo(FORMULA.volatility(forward, strike, expiry, alpha, beta, rho, nu));
    double[] adjCmp = PARAMETERS.volatilityAdjoint(expiry, strike, forward).getDerivatives().toArray();
    double[] adjExp = FORMULA.volatilityAdjoint(forward, strike, expiry, alpha, beta, rho, nu).getDerivatives().toArray();
    for (int i = 0; i < 6; ++i) {
      assertThat(adjCmp[i]).isEqualTo(adjExp[i]);
    }
    for (int i = 0; i < 9; ++i) {
      if (i < 2) {
        assertThat(PARAMETERS.getParameterMetadata(i)).isEqualTo(ALPHA_CURVE.getParameterMetadata(i));
        assertThat(PARAMETERS.getParameter(i)).isEqualTo(ALPHA_CURVE.getParameter(i));
      } else if (i < 4) {
        assertThat(PARAMETERS.getParameterMetadata(i)).isEqualTo(BETA_CURVE.getParameterMetadata(i - 2));
        assertThat(PARAMETERS.getParameter(i)).isEqualTo(BETA_CURVE.getParameter(i - 2));
      } else if (i < 6) {
        assertThat(PARAMETERS.getParameterMetadata(i)).isEqualTo(RHO_CURVE.getParameterMetadata(i - 4));
        assertThat(PARAMETERS.getParameter(i)).isEqualTo(RHO_CURVE.getParameter(i - 4));
      } else if (i < 8) {
        assertThat(PARAMETERS.getParameterMetadata(i)).isEqualTo(NU_CURVE.getParameterMetadata(i - 6));
        assertThat(PARAMETERS.getParameter(i)).isEqualTo(NU_CURVE.getParameter(i - 6));
      } else {
        assertThat(PARAMETERS.getParameterMetadata(i)).isEqualTo(ParameterMetadata.empty());
        assertThat(PARAMETERS.getParameter(i)).isEqualTo(0d);
      }
    }
  }

  @Test
  public void negativeRates() {
    double shift = 0.05;
    Curve surface = ConstantCurve.of("shfit", shift);
    SabrParameters params =
        SabrParameters.of(ALPHA_CURVE, BETA_CURVE, RHO_CURVE, NU_CURVE, surface, FORMULA);
    double expiry = 2.0;
    assertThat(params.alpha(expiry)).isEqualTo(ALPHA_CURVE.yValue(expiry));
    assertThat(params.beta(expiry)).isEqualTo(BETA_CURVE.yValue(expiry));
    assertThat(params.rho(expiry)).isEqualTo(RHO_CURVE.yValue(expiry));
    assertThat(params.nu(expiry)).isEqualTo(NU_CURVE.yValue(expiry));
    double strike = -0.02;
    double forward = 0.015;
    double alpha = ALPHA_CURVE.yValue(expiry);
    double beta = BETA_CURVE.yValue(expiry);
    double rho = RHO_CURVE.yValue(expiry);
    double nu = NU_CURVE.yValue(expiry);
    assertThat(params.volatility(expiry, strike, forward)).isEqualTo(FORMULA.volatility(forward + shift, strike + shift, expiry, alpha, beta, rho, nu));
    double[] adjCmp = params.volatilityAdjoint(expiry, strike, forward).getDerivatives().toArray();
    double[] adjExp = FORMULA.volatilityAdjoint(
        forward + shift, strike + shift, expiry, alpha, beta, rho, nu).getDerivatives().toArray();
    for (int i = 0; i < 4; ++i) {
      assertThat(adjCmp[i]).isEqualTo(adjExp[i]);
    }
  }

  @Test
  public void perturbation() {
    SabrParameters test = PARAMETERS.withPerturbation((i, v, m) -> (2d + i) * v);
    SabrParameters expected = PARAMETERS;
    for (int i = 0; i < PARAMETERS.getParameterCount(); ++i) {
      expected = expected.withParameter(i, (2d + i) * expected.getParameter(i));
    }
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(PARAMETERS);
  }

}
