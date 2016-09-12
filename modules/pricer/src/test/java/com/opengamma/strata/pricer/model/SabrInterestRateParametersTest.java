/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;

/**
 * Test {@link SabrInterestRateParameters}.
 */
@Test
public class SabrInterestRateParametersTest {

  private static final GridSurfaceInterpolator GRID = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final InterpolatedNodalSurface ALPHA_SURFACE = InterpolatedNodalSurface.of(
      Surfaces.sabrParameterByExpiryTenor("SabrAlpha", ACT_ACT_ISDA, ValueType.SABR_ALPHA),
      DoubleArray.of(0, 0, 10, 10), DoubleArray.of(0, 10, 0, 10), DoubleArray.of(0.2, 0.2, 0.2, 0.2), GRID);
  private static final InterpolatedNodalSurface BETA_SURFACE = InterpolatedNodalSurface.of(
      Surfaces.sabrParameterByExpiryTenor("SabrBeta", ACT_ACT_ISDA, ValueType.SABR_BETA),
      DoubleArray.of(0, 0, 10, 10), DoubleArray.of(0, 10, 0, 10), DoubleArray.of(1, 1, 1, 1), GRID);
  private static final InterpolatedNodalSurface RHO_SURFACE = InterpolatedNodalSurface.of(
      Surfaces.sabrParameterByExpiryTenor("SabrRho", ACT_ACT_ISDA, ValueType.SABR_RHO),
      DoubleArray.of(0, 0, 10, 10), DoubleArray.of(0, 10, 0, 10), DoubleArray.of(-0.5, -0.5, -0.5, -0.5), GRID);
  private static final InterpolatedNodalSurface NU_SURFACE = InterpolatedNodalSurface.of(
      Surfaces.sabrParameterByExpiryTenor("SabrNu", ACT_ACT_ISDA, ValueType.SABR_NU),
      DoubleArray.of(0, 0, 10, 10), DoubleArray.of(0, 10, 0, 10), DoubleArray.of(0.5, 0.5, 0.5, 0.5), GRID);
  private static final SabrVolatilityFormula FORMULA = SabrVolatilityFormula.hagan();
  private static final SabrInterestRateParameters PARAMETERS =
      SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, FORMULA);

  public void hashEqualGetter() {
    assertEquals(PARAMETERS.getAlphaSurface(), ALPHA_SURFACE);
    assertEquals(PARAMETERS.getBetaSurface(), BETA_SURFACE);
    assertEquals(PARAMETERS.getRhoSurface(), RHO_SURFACE);
    assertEquals(PARAMETERS.getNuSurface(), NU_SURFACE);
    assertEquals(PARAMETERS.getSabrVolatilityFormula(), FORMULA);
    assertEquals(PARAMETERS.getShiftSurface().getName(), SurfaceName.of("Zero shift"));
    double expiry = 2.0;
    double tenor = 3.0;
    double alpha = ALPHA_SURFACE.zValue(expiry, tenor);
    double beta = BETA_SURFACE.zValue(expiry, tenor);
    double rho = RHO_SURFACE.zValue(expiry, tenor);
    double nu = NU_SURFACE.zValue(expiry, tenor);
    assertEquals(PARAMETERS.alpha(expiry, tenor), alpha);
    assertEquals(PARAMETERS.beta(expiry, tenor), beta);
    assertEquals(PARAMETERS.rho(expiry, tenor), rho);
    assertEquals(PARAMETERS.nu(expiry, tenor), nu);
    double strike = 1.1;
    double forward = 1.05;
    assertEquals(PARAMETERS.volatility(expiry, tenor, strike, forward),
        FORMULA.volatility(forward, strike, expiry, alpha, beta, rho, nu));
    double[] adjCmp = PARAMETERS.volatilityAdjoint(expiry, tenor, strike, forward).getDerivatives().toArray();
    double[] adjExp = FORMULA.volatilityAdjoint(forward, strike, expiry, alpha, beta, rho, nu).getDerivatives().toArray();
    for (int i = 0; i < 6; ++i) {
      assertEquals(adjCmp[i], adjExp[i]);
    }
    SabrInterestRateParameters other =
        SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, FORMULA);
    assertEquals(PARAMETERS, other);
    assertEquals(PARAMETERS.hashCode(), other.hashCode());
  }

  public void negativeRates() {
    double shift = 0.05;
    Surface surface = ConstantSurface.of("shfit", shift);
    SabrInterestRateParameters params =
        SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, surface, FORMULA);
    double expiry = 2.0;
    double tenor = 3.0;
    assertEquals(params.alpha(expiry, tenor), ALPHA_SURFACE.zValue(expiry, tenor));
    assertEquals(params.beta(expiry, tenor), BETA_SURFACE.zValue(expiry, tenor));
    assertEquals(params.rho(expiry, tenor), RHO_SURFACE.zValue(expiry, tenor));
    assertEquals(params.nu(expiry, tenor), NU_SURFACE.zValue(expiry, tenor));
    double strike = -0.02;
    double forward = 0.015;
    double alpha = ALPHA_SURFACE.zValue(expiry, tenor);
    double beta = BETA_SURFACE.zValue(expiry, tenor);
    double rho = RHO_SURFACE.zValue(expiry, tenor);
    double nu = NU_SURFACE.zValue(expiry, tenor);
    assertEquals(params.volatility(expiry, tenor, strike, forward),
        FORMULA.volatility(forward + shift, strike + shift, expiry, alpha, beta, rho, nu));
    double[] adjCmp = params.volatilityAdjoint(expiry, tenor, strike, forward).getDerivatives().toArray();
    double[] adjExp = FORMULA.volatilityAdjoint(
        forward + shift, strike + shift, expiry, alpha, beta, rho, nu).getDerivatives().toArray();
    for (int i = 0; i < 4; ++i) {
      assertEquals(adjCmp[i], adjExp[i]);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(PARAMETERS);
  }

}
