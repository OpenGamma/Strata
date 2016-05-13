/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.surface.ConstantNodalSurface;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.impl.volatility.smile.function.VolatilityFunctionProvider;

/**
 * Test {@link SabrInterestRateParameters}.
 */
@Test
public class SabrInterestRateParametersTest {

  private static final LinearInterpolator1D LINEAR = new LinearInterpolator1D();
  private static final GridInterpolator2D GRID = new GridInterpolator2D(LINEAR, LINEAR);
  private static final InterpolatedNodalSurface ALPHA_SURFACE = InterpolatedNodalSurface.of(
      Surfaces.swaptionSabrExpiryTenor("SabrAlpha", ACT_ACT_ISDA, USD_FIXED_6M_LIBOR_3M, ValueType.SABR_ALPHA),
      DoubleArray.of(0.0, 10, 0.0, 10), DoubleArray.of(0, 0, 10, 10), DoubleArray.of(0.2, 0.2, 0.2, 0.2), GRID);
  private static final InterpolatedNodalSurface BETA_SURFACE = InterpolatedNodalSurface.of(
      Surfaces.swaptionSabrExpiryTenor("SabrBeta", ACT_ACT_ISDA, USD_FIXED_6M_LIBOR_3M, ValueType.SABR_BETA),
      DoubleArray.of(0.0, 10, 0.0, 10), DoubleArray.of(0, 0, 10, 10), DoubleArray.of(1, 1, 1, 1), GRID);
  private static final InterpolatedNodalSurface RHO_SURFACE = InterpolatedNodalSurface.of(
      Surfaces.swaptionSabrExpiryTenor("SabrRho", ACT_ACT_ISDA, USD_FIXED_6M_LIBOR_3M, ValueType.SABR_RHO),
      DoubleArray.of(0.0, 10, 0.0, 10), DoubleArray.of(0, 0, 10, 10), DoubleArray.of(-0.5, -0.5, -0.5, -0.5), GRID);
  private static final InterpolatedNodalSurface NU_SURFACE = InterpolatedNodalSurface.of(
      Surfaces.swaptionSabrExpiryTenor("SabrNu", ACT_ACT_ISDA, USD_FIXED_6M_LIBOR_3M, ValueType.SABR_NU),
      DoubleArray.of(0.0, 10, 0.0, 10), DoubleArray.of(0, 0, 10, 10), DoubleArray.of(0.5, 0.5, 0.5, 0.5), GRID);
  private static final SabrHaganVolatilityFunctionProvider FUNCTION = SabrHaganVolatilityFunctionProvider.DEFAULT;
  private static final SabrInterestRateParameters PARAMETERS =
      SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, FUNCTION);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullAlpha() {
    NodalSurface surface = null;
    SabrInterestRateParameters.of(surface, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, FUNCTION);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullBeta() {
    NodalSurface surface = null;
    SabrInterestRateParameters.of(ALPHA_SURFACE, surface, RHO_SURFACE, NU_SURFACE, FUNCTION);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullRho() {
    NodalSurface surface = null;
    SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, surface, NU_SURFACE, FUNCTION);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullNu() {
    NodalSurface surface = null;
    SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, surface, FUNCTION);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullShift() {
    NodalSurface surface = null;
    SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, surface, FUNCTION);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE,
        (VolatilityFunctionProvider<SabrFormulaData>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData() {
    PARAMETERS.volatility(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWrongData() {
    PARAMETERS.volatility(DoubleArray.of(1, 2, 3));
  }

  @Test
  public void hashEqualGetter() {
    assertEquals(PARAMETERS.getAlphaSurface(), ALPHA_SURFACE);
    assertEquals(PARAMETERS.getBetaSurface(), BETA_SURFACE);
    assertEquals(PARAMETERS.getRhoSurface(), RHO_SURFACE);
    assertEquals(PARAMETERS.getNuSurface(), NU_SURFACE);
    assertEquals(PARAMETERS.getSabrFunctionProvider(), FUNCTION);
    assertEquals(PARAMETERS.getShiftSurface().getName(), SurfaceName.of("Zero shift"));
    double expiry = 2.0;
    double tenor = 3.0;
    assertEquals(PARAMETERS.alpha(expiry, tenor), ALPHA_SURFACE.zValue(expiry, tenor));
    assertEquals(PARAMETERS.beta(expiry, tenor), BETA_SURFACE.zValue(expiry, tenor));
    assertEquals(PARAMETERS.rho(expiry, tenor), RHO_SURFACE.zValue(expiry, tenor));
    assertEquals(PARAMETERS.nu(expiry, tenor), NU_SURFACE.zValue(expiry, tenor));
    double strike = 1.1;
    double forward = 1.05;
    SabrFormulaData data = SabrFormulaData.of(
        ALPHA_SURFACE.zValue(expiry, tenor),
        BETA_SURFACE.zValue(expiry, tenor),
        RHO_SURFACE.zValue(expiry, tenor),
        NU_SURFACE.zValue(expiry, tenor));
    assertEquals(PARAMETERS.volatility(expiry, tenor, strike, forward),
        FUNCTION.getVolatility(forward, strike, expiry, data));
    assertEquals(PARAMETERS.volatility(DoubleArray.of(expiry, tenor, strike, forward)),
        FUNCTION.getVolatility(forward, strike, expiry, data));
    double[] adjCmp = PARAMETERS.volatilityAdjoint(expiry, tenor, strike, forward).getDerivatives().toArray();
    double[] adjExp = FUNCTION.getVolatilityAdjoint(forward, strike, expiry, data).getDerivatives().toArray();
    for (int i = 0; i < 6; ++i) {
      assertEquals(adjCmp[i], adjExp[i]);
    }
    SabrInterestRateParameters other =
        SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, FUNCTION);
    assertEquals(PARAMETERS, other);
    assertEquals(PARAMETERS.hashCode(), other.hashCode());
  }

  @Test
  public void negativeRates() {
    double shift = 0.05;
    NodalSurface surface = ConstantNodalSurface.of("shfit", shift);
    SabrInterestRateParameters params =
        SabrInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, surface, FUNCTION);
    double expiry = 2.0;
    double tenor = 3.0;
    assertEquals(params.alpha(expiry, tenor), ALPHA_SURFACE.zValue(expiry, tenor));
    assertEquals(params.beta(expiry, tenor), BETA_SURFACE.zValue(expiry, tenor));
    assertEquals(params.rho(expiry, tenor), RHO_SURFACE.zValue(expiry, tenor));
    assertEquals(params.nu(expiry, tenor), NU_SURFACE.zValue(expiry, tenor));
    double strike = -0.02;
    double forward = 0.015;
    SabrFormulaData data = SabrFormulaData.of(
        ALPHA_SURFACE.zValue(expiry, tenor),
        BETA_SURFACE.zValue(expiry, tenor),
        RHO_SURFACE.zValue(expiry, tenor),
        NU_SURFACE.zValue(expiry, tenor));
    assertEquals(params.volatility(expiry, tenor, strike, forward),
        FUNCTION.getVolatility(forward + shift, strike + shift, expiry, data));
    assertEquals(params.volatility(DoubleArray.of(expiry, tenor, strike, forward)),
        FUNCTION.getVolatility(forward + shift, strike + shift, expiry, data));
    double[] adjCmp = params.volatilityAdjoint(expiry, tenor, strike, forward).getDerivatives().toArray();
    double[] adjExp = FUNCTION.getVolatilityAdjoint(forward + shift, strike + shift, expiry, data).getDerivatives().toArray();
    for (int i = 0; i < 4; ++i) {
      assertEquals(adjCmp[i], adjExp[i]);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(PARAMETERS);
  }

}
