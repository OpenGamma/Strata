/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_E_360;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivities;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivities;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceUnitParameterSensitivity;
import com.opengamma.strata.pricer.impl.option.SabrInterestRateParameters;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link SabrSwaptionVolatilities}.
 */
@Test
public class SabrSwaptionVolatilitiesTest {

  private static final LocalDate DATE = LocalDate.of(2014, 1, 3);
  private static final LocalTime TIME = LocalTime.of(10, 0);
  private static final ZoneId ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime DATE_TIME = DATE.atTime(TIME).atZone(ZONE);
  private static final SabrInterestRateParameters PARAM = SwaptionSabrRateVolatilityDataSet.SABR_PARAM_SHIFT_USD;
  private static final FixedIborSwapConvention CONV = FixedIborSwapConventions.JPY_FIXED_6M_TIBORJ_3M;

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY = new ZonedDateTime[] {
      dateUtc(2014, 1, 3), dateUtc(2014, 1, 3), dateUtc(2015, 1, 3), dateUtc(2017, 1, 3)};
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final double[] TEST_TENOR = new double[] {2.0, 6.0, 7.0, 15.0};
  private static final double TEST_FORWARD = 0.025;
  private static final double[] TEST_STRIKE = new double[] {0.02, 0.025, 0.03};
  private static final int NB_STRIKE = TEST_STRIKE.length;

  private static final double TOLERANCE_VOL = 1.0E-10;

  public void test_of() {
    SabrParametersSwaptionVolatilities test = SabrParametersSwaptionVolatilities.of(PARAM, CONV, DATE_TIME, ACT_ACT_ISDA);
    assertEquals(test.getConvention(), CONV);
    assertEquals(test.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test.getParameters(), PARAM);
    assertEquals(test.getValuationDateTime(), DATE_TIME);
  }

  public void test_of_dateTimeZone() {
    SabrParametersSwaptionVolatilities test1 =
        SabrParametersSwaptionVolatilities.of(PARAM, CONV, DATE, TIME, ZONE, ACT_ACT_ISDA);
    assertEquals(test1.getConvention(), CONV);
    assertEquals(test1.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test1.getParameters(), PARAM);
    assertEquals(test1.getValuationDateTime(), DATE.atTime(TIME).atZone(ZONE));
    SabrParametersSwaptionVolatilities test2 = SabrParametersSwaptionVolatilities.of(PARAM, CONV, DATE_TIME, ACT_ACT_ISDA);
    assertEquals(test1, test2);
  }

  public void test_tenor() {
    SabrParametersSwaptionVolatilities prov = SabrParametersSwaptionVolatilities.of(PARAM, CONV, DATE_TIME, ACT_ACT_ISDA);
    double test1 = prov.tenor(DATE, DATE);
    assertEquals(test1, 0d);
    double test2 = prov.tenor(DATE, DATE.plusYears(2));
    double test3 = prov.tenor(DATE, DATE.minusYears(2));
    assertEquals(test2, -test3);
    double test4 = prov.tenor(DATE, LocalDate.of(2019, 2, 2));
    double test5 = prov.tenor(DATE, LocalDate.of(2018, 12, 31));
    assertEquals(test4, 5d);
    assertEquals(test5, 5d);
  }

  public void test_relativeTime() {
    SabrParametersSwaptionVolatilities prov = SabrParametersSwaptionVolatilities.of(PARAM, CONV, DATE_TIME, THIRTY_E_360);
    double test1 = prov.relativeTime(DATE_TIME);
    assertEquals(test1, 0d);
    double test2 = prov.relativeTime(DATE_TIME.plusYears(2));
    double test3 = prov.relativeTime(DATE_TIME.minusYears(2));
    assertEquals(test2, -test3);
  }

  public void test_volatility() {
    SabrParametersSwaptionVolatilities prov = SabrParametersSwaptionVolatilities.of(PARAM, CONV, DATE_TIME, ACT_ACT_ISDA);
    for (int i = 0; i < NB_TEST; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        double expiryTime = prov.relativeTime(TEST_OPTION_EXPIRY[i]);
        double volExpected = PARAM.volatility(expiryTime, TEST_TENOR[i], TEST_STRIKE[j], TEST_FORWARD);
        double volComputed = prov.volatility(TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE[j], TEST_FORWARD);
        assertEquals(volComputed, volExpected, TOLERANCE_VOL);
      }
    }
  }

  public void test_surfaceCurrencyParameterSensitivity() {
    double alphaSensi = 2.24, betaSensi = 3.45, rhoSensi = -2.12, nuSensi = -0.56;
    SabrParametersSwaptionVolatilities prov = SabrParametersSwaptionVolatilities.of(PARAM, CONV, DATE_TIME, ACT_ACT_ISDA);
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = prov.relativeTime(TEST_OPTION_EXPIRY[i]);
      SwaptionSabrSensitivity point = SwaptionSabrSensitivity.of(CONV, TEST_OPTION_EXPIRY[i], TEST_TENOR[i], USD,
          alphaSensi, betaSensi, rhoSensi, nuSensi);
      SurfaceCurrencyParameterSensitivities sensiComputed = prov.surfaceCurrencyParameterSensitivity(point);
      SurfaceUnitParameterSensitivity alphaSensitivities = prov.getParameters().getAlphaSurface()
          .zValueParameterSensitivity(expiryTime, TEST_TENOR[i]);
      SurfaceUnitParameterSensitivity betaSensitivities = prov.getParameters().getBetaSurface()
          .zValueParameterSensitivity(expiryTime, TEST_TENOR[i]);
      SurfaceUnitParameterSensitivity rhoSensitivities = prov.getParameters().getRhoSurface()
          .zValueParameterSensitivity(expiryTime, TEST_TENOR[i]);
      SurfaceUnitParameterSensitivity nuSensitivities = prov.getParameters().getNuSurface()
          .zValueParameterSensitivity(expiryTime, TEST_TENOR[i]);
      SurfaceCurrencyParameterSensitivity alphaSensiObj = sensiComputed.getSensitivity(
          SwaptionSabrRateVolatilityDataSet.META_ALPHA.getSurfaceName(), USD);
      SurfaceCurrencyParameterSensitivity betaSensiObj = sensiComputed.getSensitivity(
          SwaptionSabrRateVolatilityDataSet.META_BETA_USD.getSurfaceName(), USD);
      SurfaceCurrencyParameterSensitivity rhoSensiObj = sensiComputed.getSensitivity(
          SwaptionSabrRateVolatilityDataSet.META_RHO.getSurfaceName(), USD);
      SurfaceCurrencyParameterSensitivity nuSensiObj = sensiComputed.getSensitivity(
          SwaptionSabrRateVolatilityDataSet.META_NU.getSurfaceName(), USD);
      DoubleArray alphaNodeSensiComputed = alphaSensiObj.getSensitivity();
      DoubleArray betaNodeSensiComputed = betaSensiObj.getSensitivity();
      DoubleArray rhoNodeSensiComputed = rhoSensiObj.getSensitivity();
      DoubleArray nuNodeSensiComputed = nuSensiObj.getSensitivity();
      assertEquals(alphaSensitivities.getSensitivity().size(), alphaNodeSensiComputed.size());
      assertEquals(betaSensitivities.getSensitivity().size(), betaNodeSensiComputed.size());
      assertEquals(rhoSensitivities.getSensitivity().size(), rhoNodeSensiComputed.size());
      assertEquals(nuSensitivities.getSensitivity().size(), nuNodeSensiComputed.size());
      for (int k = 0; k < alphaNodeSensiComputed.size(); ++k) {
        assertEquals(alphaNodeSensiComputed.get(k), alphaSensitivities.getSensitivity().get(k) * alphaSensi, TOLERANCE_VOL);
      }
      for (int k = 0; k < betaNodeSensiComputed.size(); ++k) {
        assertEquals(betaNodeSensiComputed.get(k), betaSensitivities.getSensitivity().get(k) * betaSensi, TOLERANCE_VOL);
      }
      for (int k = 0; k < rhoNodeSensiComputed.size(); ++k) {
        assertEquals(rhoNodeSensiComputed.get(k), rhoSensitivities.getSensitivity().get(k) * rhoSensi, TOLERANCE_VOL);
      }
      for (int k = 0; k < nuNodeSensiComputed.size(); ++k) {
        assertEquals(nuNodeSensiComputed.get(k), nuSensitivities.getSensitivity().get(k) * nuSensi, TOLERANCE_VOL);
      }
    }
  }

  public void test_surfaceCurrencyParameterSensitivity_multi() {
    double[] points1 = new double[] {2.24, 3.45, -2.12, -0.56};
    double[] points2 = new double[] {-0.145, 1.01, -5.0, -11.0};
    double[] points3 = new double[] {1.3, -4.32, 2.1, -7.18};
    SabrParametersSwaptionVolatilities prov =
        SabrParametersSwaptionVolatilities.of(PARAM, CONV, DATE_TIME, ACT_ACT_ISDA);
    for (int i = 0; i < NB_TEST; i++) {
      SwaptionSabrSensitivity sensi1 = SwaptionSabrSensitivity.of(
          CONV, TEST_OPTION_EXPIRY[0], TEST_TENOR[i], USD, points1[0], points1[1], points1[2], points1[3]);
      SwaptionSabrSensitivity sensi2 = SwaptionSabrSensitivity.of(
          CONV, TEST_OPTION_EXPIRY[0], TEST_TENOR[i], USD, points2[0], points2[1], points2[2], points2[3]);
      SwaptionSabrSensitivity sensi3 = SwaptionSabrSensitivity.of(
          CONV, TEST_OPTION_EXPIRY[3], TEST_TENOR[i], USD, points3[0], points3[1], points3[2], points3[3]);
      SwaptionSabrSensitivities sensis = SwaptionSabrSensitivities.of(Arrays.asList(sensi1, sensi2, sensi3)).normalize();
      SurfaceCurrencyParameterSensitivities computed = prov.surfaceCurrencyParameterSensitivity(sensis);
      SurfaceCurrencyParameterSensitivities expected = prov.surfaceCurrencyParameterSensitivity(sensi1)
          .combinedWith(prov.surfaceCurrencyParameterSensitivity(sensi2))
          .combinedWith(prov.surfaceCurrencyParameterSensitivity(sensi3));
      DoubleArrayMath.fuzzyEquals(
          computed.getSensitivity(PARAM.getAlphaSurface().getName(), USD).getSensitivity().toArray(),
          expected.getSensitivity(PARAM.getAlphaSurface().getName(), USD).getSensitivity().toArray(),
          TOLERANCE_VOL);
      DoubleArrayMath.fuzzyEquals(
          computed.getSensitivity(PARAM.getBetaSurface().getName(), USD).getSensitivity().toArray(),
          expected.getSensitivity(PARAM.getBetaSurface().getName(), USD).getSensitivity().toArray(),
          TOLERANCE_VOL);
      DoubleArrayMath.fuzzyEquals(
          computed.getSensitivity(PARAM.getRhoSurface().getName(), USD).getSensitivity().toArray(),
          expected.getSensitivity(PARAM.getRhoSurface().getName(), USD).getSensitivity().toArray(),
          TOLERANCE_VOL);
      DoubleArrayMath.fuzzyEquals(
          computed.getSensitivity(PARAM.getNuSurface().getName(), USD).getSensitivity().toArray(),
          expected.getSensitivity(PARAM.getNuSurface().getName(), USD).getSensitivity().toArray(),
          TOLERANCE_VOL);
    }
  }

  public void coverage() {
    SabrParametersSwaptionVolatilities test1 = SabrParametersSwaptionVolatilities.of(PARAM, CONV, DATE_TIME, ACT_ACT_ISDA);
    coverImmutableBean(test1);
    SabrParametersSwaptionVolatilities test2 = SabrParametersSwaptionVolatilities.of(
        SwaptionSabrRateVolatilityDataSet.SABR_PARAM_USD,
        FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M, DATE_TIME.plusDays(1), THIRTY_E_360);
    coverBeanEquals(test1, test2);
  }
}
