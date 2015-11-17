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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivities;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.meta.SwaptionSurfaceExpiryTenorNodeMetadata;
import com.opengamma.strata.pricer.impl.option.SabrInterestRateParameters;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link SabrVolatilitySwaptionProvider}.
 */
@Test
public class SabrVolatilitySwaptionProviderTest {
  private static final LocalDate DATE = LocalDate.of(2014, 1, 3);
  private static final LocalTime TIME = LocalTime.of(10, 0);
  private static final ZoneId ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime DATE_TIME = DATE.atTime(TIME).atZone(ZONE);
  private static final SabrInterestRateParameters PARAM = SwaptionSabrRateVolatilityDataSet.SABR_PARAM_SHIFT_USD;
  private static final FixedIborSwapConvention CONV = FixedIborSwapConventions.JPY_FIXED_6M_TIBORJ_3M;

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY = new ZonedDateTime[] {
    dateUtc(2014, 1, 3), dateUtc(2014, 1, 3), dateUtc(2015, 1, 3), dateUtc(2017, 1, 3) };
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final double[] TEST_TENOR = new double[] {2.0, 6.0, 7.0, 15.0 };
  private static final double TEST_FORWARD = 0.025;
  private static final double[] TEST_STRIKE = new double[] {0.02, 0.025, 0.03 };
  private static final int NB_STRIKE = TEST_STRIKE.length;

  private static final double TOLERANCE_VOL = 1.0E-10;

  public void test_of() {
    SabrVolatilitySwaptionProvider test = SabrVolatilitySwaptionProvider.of(PARAM, CONV, ACT_ACT_ISDA, DATE);
    assertEquals(test.getConvention(), CONV);
    assertEquals(test.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test.getParameters(), PARAM);
    assertEquals(test.getValuationDateTime(), DATE.atTime(LocalTime.NOON).atZone(ZoneOffset.UTC));
  }

  public void test_of_withTimeZone() {
    SabrVolatilitySwaptionProvider test1 = SabrVolatilitySwaptionProvider.of(PARAM, CONV, ACT_ACT_ISDA, DATE, TIME, ZONE);
    assertEquals(test1.getConvention(), CONV);
    assertEquals(test1.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test1.getParameters(), PARAM);
    assertEquals(test1.getValuationDateTime(), DATE.atTime(TIME).atZone(ZONE));
    SabrVolatilitySwaptionProvider test2 = SabrVolatilitySwaptionProvider.of(PARAM, CONV, ACT_ACT_ISDA, DATE_TIME);
    assertEquals(test1, test2);
  }

  public void test_tenor() {
    SabrVolatilitySwaptionProvider prov = SabrVolatilitySwaptionProvider.of(PARAM, CONV, ACT_ACT_ISDA, DATE);
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
    SabrVolatilitySwaptionProvider prov = SabrVolatilitySwaptionProvider.of(PARAM, CONV, THIRTY_E_360, DATE);
    double test1 = prov.relativeTime(DATE_TIME);
    assertEquals(test1, 0d);
    double test2 = prov.relativeTime(DATE_TIME.plusYears(2));
    double test3 = prov.relativeTime(DATE_TIME.minusYears(2));
    assertEquals(test2, -test3);
  }

  public void test_volatility() {
    SabrVolatilitySwaptionProvider prov = SabrVolatilitySwaptionProvider.of(PARAM, CONV, ACT_ACT_ISDA, DATE);
    for (int i = 0; i < NB_TEST; i++) {
      for (int j=0;j<NB_STRIKE;++j) {
        double expiryTime = prov.relativeTime(TEST_OPTION_EXPIRY[i]);
        double volExpected = PARAM.getVolatility(expiryTime, TEST_TENOR[i], TEST_STRIKE[j], TEST_FORWARD);
        double volComputed = prov.getVolatility(TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE[j], TEST_FORWARD);
        assertEquals(volComputed, volExpected, TOLERANCE_VOL);
      }
    }
  }

  public void test_surfaceCurrencyParameterSensitivity() {
    double alphaSensi = 2.24, betaSensi = 3.45, rhoSensi = -2.12, nuSensi = -0.56;
    SabrVolatilitySwaptionProvider prov = SabrVolatilitySwaptionProvider.of(PARAM, CONV, ACT_ACT_ISDA, DATE);
    for (int i = 0; i < NB_TEST; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        double expiryTime = prov.relativeTime(TEST_OPTION_EXPIRY[i]);
        SwaptionSabrSensitivity point = SwaptionSabrSensitivity.of(CONV, TEST_OPTION_EXPIRY[i], TEST_TENOR[i],
            TEST_STRIKE[j], TEST_FORWARD, USD, alphaSensi, betaSensi, rhoSensi, nuSensi);
        SurfaceCurrencyParameterSensitivities sensiComputed = prov.surfaceCurrencyParameterSensitivity(point);
        Map<DoublesPair, Double> alphaMap = prov.getParameters().getAlphaSurface()
            .zValueParameterSensitivity(expiryTime, TEST_TENOR[i]);
        Map<DoublesPair, Double> betaMap = prov.getParameters().getBetaSurface()
            .zValueParameterSensitivity(expiryTime, TEST_TENOR[i]);
        Map<DoublesPair, Double> rhoMap = prov.getParameters().getRhoSurface()
            .zValueParameterSensitivity(expiryTime, TEST_TENOR[i]);
        Map<DoublesPair, Double> nuMap = prov.getParameters().getNuSurface()
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
        assertEquals(alphaMap.size(), alphaNodeSensiComputed.size());
        assertEquals(betaMap.size(), betaNodeSensiComputed.size());
        assertEquals(rhoMap.size(), rhoNodeSensiComputed.size());
        assertEquals(nuMap.size(), nuNodeSensiComputed.size());
        for (int k = 0; k < alphaNodeSensiComputed.size(); ++k) {
          SwaptionSurfaceExpiryTenorNodeMetadata meta = (SwaptionSurfaceExpiryTenorNodeMetadata)
              alphaSensiObj.getMetadata().getParameterMetadata().get().get(k);
          DoublesPair pair = DoublesPair.of(meta.getYearFraction(), meta.getTenor());
          assertEquals(alphaNodeSensiComputed.get(k), alphaMap.get(pair) * alphaSensi, TOLERANCE_VOL);
        }
        for (int k = 0; k < betaNodeSensiComputed.size(); ++k) {
          SwaptionSurfaceExpiryTenorNodeMetadata meta = (SwaptionSurfaceExpiryTenorNodeMetadata)
              betaSensiObj.getMetadata().getParameterMetadata().get().get(k);
          DoublesPair pair = DoublesPair.of(meta.getYearFraction(), meta.getTenor());
          assertEquals(betaNodeSensiComputed.get(k), betaMap.get(pair) * betaSensi, TOLERANCE_VOL);
        }
        for (int k = 0; k < rhoNodeSensiComputed.size(); ++k) {
          SwaptionSurfaceExpiryTenorNodeMetadata meta = (SwaptionSurfaceExpiryTenorNodeMetadata)
              rhoSensiObj.getMetadata().getParameterMetadata().get().get(k);
          DoublesPair pair = DoublesPair.of(meta.getYearFraction(), meta.getTenor());
          assertEquals(rhoNodeSensiComputed.get(k), rhoMap.get(pair) * rhoSensi, TOLERANCE_VOL);
        }
        for (int k = 0; k < nuNodeSensiComputed.size(); ++k) {
          SwaptionSurfaceExpiryTenorNodeMetadata meta = (SwaptionSurfaceExpiryTenorNodeMetadata)
              nuSensiObj.getMetadata().getParameterMetadata().get().get(k);
          DoublesPair pair = DoublesPair.of(meta.getYearFraction(), meta.getTenor());
          assertEquals(nuNodeSensiComputed.get(k), nuMap.get(pair) * nuSensi, TOLERANCE_VOL);
        }
      }
    }
  }

  public void coverage() {
    SabrVolatilitySwaptionProvider test1 = SabrVolatilitySwaptionProvider.of(PARAM, CONV, ACT_ACT_ISDA, DATE);
    coverImmutableBean(test1);
    SabrVolatilitySwaptionProvider test2 = SabrVolatilitySwaptionProvider.of(
        SwaptionSabrRateVolatilityDataSet.SABR_PARAM_USD,
        FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M, THIRTY_E_360, LocalDate.of(2013, 2, 15));
    coverBeanEquals(test1, test2);
  }
}
