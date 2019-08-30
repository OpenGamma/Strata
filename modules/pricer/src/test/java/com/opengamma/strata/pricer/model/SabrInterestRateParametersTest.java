/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;

/**
 * Test {@link SabrInterestRateParameters}.
 */
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

  @Test
  public void hashEqualGetter() {
    assertThat(PARAMETERS.getAlphaSurface()).isEqualTo(ALPHA_SURFACE);
    assertThat(PARAMETERS.getBetaSurface()).isEqualTo(BETA_SURFACE);
    assertThat(PARAMETERS.getRhoSurface()).isEqualTo(RHO_SURFACE);
    assertThat(PARAMETERS.getNuSurface()).isEqualTo(NU_SURFACE);
    assertThat(PARAMETERS.getSabrVolatilityFormula()).isEqualTo(FORMULA);
    assertThat(PARAMETERS.getShiftSurface().getName()).isEqualTo(SurfaceName.of("Zero shift"));
    double expiry = 2.0;
    double tenor = 3.0;
    double alpha = ALPHA_SURFACE.zValue(expiry, tenor);
    double beta = BETA_SURFACE.zValue(expiry, tenor);
    double rho = RHO_SURFACE.zValue(expiry, tenor);
    double nu = NU_SURFACE.zValue(expiry, tenor);
    assertThat(PARAMETERS.alpha(expiry, tenor)).isEqualTo(alpha);
    assertThat(PARAMETERS.beta(expiry, tenor)).isEqualTo(beta);
    assertThat(PARAMETERS.rho(expiry, tenor)).isEqualTo(rho);
    assertThat(PARAMETERS.nu(expiry, tenor)).isEqualTo(nu);
    double strike = 1.1;
    double forward = 1.05;
    assertThat(PARAMETERS.volatility(expiry, tenor, strike, forward)).isEqualTo(FORMULA.volatility(forward, strike, expiry, alpha, beta, rho, nu));
    double[] adjCmp = PARAMETERS.volatilityAdjoint(expiry, tenor, strike, forward).getDerivatives().toArray();
    double[] adjExp = FORMULA.volatilityAdjoint(forward, strike, expiry, alpha, beta, rho, nu).getDerivatives().toArray();
    for (int i = 0; i < 6; ++i) {
      assertThat(adjCmp[i]).isEqualTo(adjExp[i]);
    }
    SabrInterestRateParameters other =
        SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, FORMULA);
    assertThat(PARAMETERS).isEqualTo(other);
    assertThat(PARAMETERS.hashCode()).isEqualTo(other.hashCode());
  }

  @Test
  public void negativeRates() {
    double shift = 0.05;
    Surface surface = ConstantSurface.of("shfit", shift);
    SabrInterestRateParameters params =
        SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, surface, FORMULA);
    double expiry = 2.0;
    double tenor = 3.0;
    assertThat(params.alpha(expiry, tenor)).isEqualTo(ALPHA_SURFACE.zValue(expiry, tenor));
    assertThat(params.beta(expiry, tenor)).isEqualTo(BETA_SURFACE.zValue(expiry, tenor));
    assertThat(params.rho(expiry, tenor)).isEqualTo(RHO_SURFACE.zValue(expiry, tenor));
    assertThat(params.nu(expiry, tenor)).isEqualTo(NU_SURFACE.zValue(expiry, tenor));
    double strike = -0.02;
    double forward = 0.015;
    double alpha = ALPHA_SURFACE.zValue(expiry, tenor);
    double beta = BETA_SURFACE.zValue(expiry, tenor);
    double rho = RHO_SURFACE.zValue(expiry, tenor);
    double nu = NU_SURFACE.zValue(expiry, tenor);
    assertThat(params.volatility(expiry, tenor, strike, forward)).isEqualTo(FORMULA.volatility(forward + shift, strike + shift, expiry, alpha, beta, rho, nu));
    double[] adjCmp = params.volatilityAdjoint(expiry, tenor, strike, forward).getDerivatives().toArray();
    double[] adjExp = FORMULA.volatilityAdjoint(
        forward + shift, strike + shift, expiry, alpha, beta, rho, nu).getDerivatives().toArray();
    for (int i = 0; i < 4; ++i) {
      assertThat(adjCmp[i]).isEqualTo(adjExp[i]);
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(PARAMETERS);
  }

}
