/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.model;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

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
@Test
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

  public void getter() {
    assertEquals(PARAMETERS.getAlphaCurve(), ALPHA_CURVE);
    assertEquals(PARAMETERS.getBetaCurve(), BETA_CURVE);
    assertEquals(PARAMETERS.getRhoCurve(), RHO_CURVE);
    assertEquals(PARAMETERS.getNuCurve(), NU_CURVE);
    assertEquals(PARAMETERS.getSabrVolatilityFormula(), FORMULA);
    assertEquals(PARAMETERS.getShiftCurve().getName(), CurveName.of("Zero shift"));
    assertEquals(PARAMETERS.getDayCount(), ACT_ACT_ISDA);
    assertEquals(PARAMETERS.getParameterCount(), 9);
    double expiry = 2.0;
    double alpha = ALPHA_CURVE.yValue(expiry);
    double beta = BETA_CURVE.yValue(expiry);
    double rho = RHO_CURVE.yValue(expiry);
    double nu = NU_CURVE.yValue(expiry);
    assertEquals(PARAMETERS.alpha(expiry), alpha);
    assertEquals(PARAMETERS.beta(expiry), beta);
    assertEquals(PARAMETERS.rho(expiry), rho);
    assertEquals(PARAMETERS.nu(expiry), nu);
    double strike = 1.1;
    double forward = 1.05;
    assertEquals(PARAMETERS.volatility(expiry, strike, forward),
        FORMULA.volatility(forward, strike, expiry, alpha, beta, rho, nu));
    double[] adjCmp = PARAMETERS.volatilityAdjoint(expiry, strike, forward).getDerivatives().toArray();
    double[] adjExp = FORMULA.volatilityAdjoint(forward, strike, expiry, alpha, beta, rho, nu).getDerivatives().toArray();
    for (int i = 0; i < 6; ++i) {
      assertEquals(adjCmp[i], adjExp[i]);
    }
    for (int i = 0; i < 9; ++i) {
      if (i < 2) {
        assertEquals(PARAMETERS.getParameterMetadata(i), ALPHA_CURVE.getParameterMetadata(i));
        assertEquals(PARAMETERS.getParameter(i), ALPHA_CURVE.getParameter(i));
      } else if (i < 4) {
        assertEquals(PARAMETERS.getParameterMetadata(i), BETA_CURVE.getParameterMetadata(i - 2));
        assertEquals(PARAMETERS.getParameter(i), BETA_CURVE.getParameter(i - 2));
      } else if (i < 6) {
        assertEquals(PARAMETERS.getParameterMetadata(i), RHO_CURVE.getParameterMetadata(i - 4));
        assertEquals(PARAMETERS.getParameter(i), RHO_CURVE.getParameter(i - 4));
      } else if (i < 8) {
        assertEquals(PARAMETERS.getParameterMetadata(i), NU_CURVE.getParameterMetadata(i - 6));
        assertEquals(PARAMETERS.getParameter(i), NU_CURVE.getParameter(i - 6));
      } else {
        assertEquals(PARAMETERS.getParameterMetadata(i), ParameterMetadata.empty());
        assertEquals(PARAMETERS.getParameter(i), 0d);
      }
    }
  }

  public void negativeRates() {
    double shift = 0.05;
    Curve surface = ConstantCurve.of("shfit", shift);
    SabrParameters params =
        SabrParameters.of(ALPHA_CURVE, BETA_CURVE, RHO_CURVE, NU_CURVE, surface, FORMULA);
    double expiry = 2.0;
    assertEquals(params.alpha(expiry), ALPHA_CURVE.yValue(expiry));
    assertEquals(params.beta(expiry), BETA_CURVE.yValue(expiry));
    assertEquals(params.rho(expiry), RHO_CURVE.yValue(expiry));
    assertEquals(params.nu(expiry), NU_CURVE.yValue(expiry));
    double strike = -0.02;
    double forward = 0.015;
    double alpha = ALPHA_CURVE.yValue(expiry);
    double beta = BETA_CURVE.yValue(expiry);
    double rho = RHO_CURVE.yValue(expiry);
    double nu = NU_CURVE.yValue(expiry);
    assertEquals(params.volatility(expiry, strike, forward),
        FORMULA.volatility(forward + shift, strike + shift, expiry, alpha, beta, rho, nu));
    double[] adjCmp = params.volatilityAdjoint(expiry, strike, forward).getDerivatives().toArray();
    double[] adjExp = FORMULA.volatilityAdjoint(
        forward + shift, strike + shift, expiry, alpha, beta, rho, nu).getDerivatives().toArray();
    for (int i = 0; i < 4; ++i) {
      assertEquals(adjCmp[i], adjExp[i]);
    }
  }

  public void perturbation() {
    SabrParameters test = PARAMETERS.withPerturbation((i, v, m) -> (2d + i) * v);
    SabrParameters expected = PARAMETERS;
    for (int i = 0; i < PARAMETERS.getParameterCount(); ++i) {
      expected = expected.withParameter(i, (2d + i) * expected.getParameter(i));
    }
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(PARAMETERS);
  }

}
