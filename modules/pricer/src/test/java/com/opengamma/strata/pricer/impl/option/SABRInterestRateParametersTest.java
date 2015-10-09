/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SABRFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SABRHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.impl.volatility.smile.function.VolatilityFunctionProvider;

/**
 * Test {@link SABRInterestRateParameters}.
 */
@Test
public class SABRInterestRateParametersTest {

  private static final LinearInterpolator1D LINEAR = new LinearInterpolator1D();
  private static final GridInterpolator2D GRID = new GridInterpolator2D(LINEAR, LINEAR);
  private static final SurfaceMetadata METADATA = DefaultSurfaceMetadata.of("surface");
  private static final InterpolatedNodalSurface ALPHA_SURFACE = InterpolatedNodalSurface.of(METADATA,
      new double[] {0.0, 10, 0.0, 10 }, new double[] {0, 0, 10, 10 }, new double[] {0.2, 0.2, 0.2, 0.2 }, GRID);
  private static final InterpolatedNodalSurface BETA_SURFACE = InterpolatedNodalSurface.of(METADATA,
      new double[] {0.0, 10, 0.0, 10 }, new double[] {0, 0, 10, 10 }, new double[] {1, 1, 1, 1 }, GRID);
  private static final InterpolatedNodalSurface RHO_SURFACE = InterpolatedNodalSurface.of(METADATA,
      new double[] {0.0, 10, 0.0, 10 }, new double[] {0, 0, 10, 10 }, new double[] {-0.5, -0.5, -0.5, -0.5 }, GRID);
  private static final InterpolatedNodalSurface NU_SURFACE = InterpolatedNodalSurface.of(METADATA,
      new double[] {0.0, 10, 0.0, 10 }, new double[] {0, 0, 10, 10 }, new double[] {0.5, 0.5, 0.5, 0.5 }, GRID);
  private static final SABRHaganVolatilityFunctionProvider FUNCTION = SABRHaganVolatilityFunctionProvider.DEFAULT;
  private static final SABRInterestRateParameters PARAMETERS =
      SABRInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, FUNCTION);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullAlpha() {
    NodalSurface surface = null;
    SABRInterestRateParameters.of(surface, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, FUNCTION);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullBeta() {
    NodalSurface surface = null;
    SABRInterestRateParameters.of(ALPHA_SURFACE, surface, RHO_SURFACE, NU_SURFACE, FUNCTION);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullRho() {
    NodalSurface surface = null;
    SABRInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, surface, NU_SURFACE, FUNCTION);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullNu() {
    NodalSurface surface = null;
    SABRInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, surface, FUNCTION);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    SABRInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE,
        (VolatilityFunctionProvider<SABRFormulaData>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData() {
    PARAMETERS.getVolatility(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWrongData() {
    PARAMETERS.getVolatility(new double[] {1, 2, 3 });
  }

  @Test
  public void hashEqualGetter() {
    assertEquals(PARAMETERS.getAlphaSurface(), ALPHA_SURFACE);
    assertEquals(PARAMETERS.getBetaSurface(), BETA_SURFACE);
    assertEquals(PARAMETERS.getRhoSurface(), RHO_SURFACE);
    assertEquals(PARAMETERS.getNuSurface(), NU_SURFACE);
    assertEquals(PARAMETERS.getSabrFunctionProvider(), FUNCTION);
    double expiry = 2.0;
    double tenor = 3.0;
    DoublesPair sample = DoublesPair.of(expiry, tenor);
    assertEquals(PARAMETERS.getAlpha(sample), ALPHA_SURFACE.zValue(sample));
    assertEquals(PARAMETERS.getBeta(sample), BETA_SURFACE.zValue(sample));
    assertEquals(PARAMETERS.getRho(sample), RHO_SURFACE.zValue(sample));
    assertEquals(PARAMETERS.getNu(sample), NU_SURFACE.zValue(sample));
    double strike = 1.1;
    double forward = 1.05;
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, PutCall.CALL);
    SABRFormulaData data = SABRFormulaData.of(
        ALPHA_SURFACE.zValue(sample), BETA_SURFACE.zValue(sample), RHO_SURFACE.zValue(sample), NU_SURFACE.zValue(sample));
    assertEquals(PARAMETERS.getVolatility(expiry, tenor, strike, forward), FUNCTION.getVolatility(option, forward, data));
    assertEquals(PARAMETERS.getVolatility(new double[] {expiry, tenor, strike, forward }),
        FUNCTION.getVolatility(option, forward, data));
    SABRInterestRateParameters other =
        SABRInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, FUNCTION);
    assertEquals(PARAMETERS, other);
    assertEquals(PARAMETERS.hashCode(), other.hashCode());
    other = SABRInterestRateParameters.of(BETA_SURFACE, BETA_SURFACE, RHO_SURFACE, NU_SURFACE, FUNCTION);
    assertFalse(other.equals(PARAMETERS));
    other = SABRInterestRateParameters.of(ALPHA_SURFACE, ALPHA_SURFACE, RHO_SURFACE, NU_SURFACE, FUNCTION);
    assertFalse(other.equals(PARAMETERS));
    other = SABRInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, ALPHA_SURFACE, NU_SURFACE, FUNCTION);
    assertFalse(other.equals(PARAMETERS));
    other = SABRInterestRateParameters.of(ALPHA_SURFACE, BETA_SURFACE, RHO_SURFACE, ALPHA_SURFACE, FUNCTION);
    assertFalse(other.equals(PARAMETERS));
    assertFalse(other.equals(PARAMETERS));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(PARAMETERS);
    InterpolatedNodalSurface surface = InterpolatedNodalSurface.of(METADATA,
        new double[] {0.0, 5, 0.0, 5 }, new double[] {0, 0, 8, 8 }, new double[] {0.2, 0.3, 0.2, 0.3 }, GRID);
    SABRInterestRateParameters other = SABRInterestRateParameters.of(surface, surface, surface, surface, FUNCTION);
    coverBeanEquals(PARAMETERS, other);
  }

}
