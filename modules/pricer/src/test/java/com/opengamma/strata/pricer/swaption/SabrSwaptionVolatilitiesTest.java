/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.market.model.SabrParameterType.ALPHA;
import static com.opengamma.strata.market.model.SabrParameterType.BETA;
import static com.opengamma.strata.market.model.SabrParameterType.NU;
import static com.opengamma.strata.market.model.SabrParameterType.RHO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.param.PointShifts;
import com.opengamma.strata.market.param.PointShiftsBuilder;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.pricer.model.SabrInterestRateParameters;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Test {@link SabrSwaptionVolatilities}.
 */
public class SabrSwaptionVolatilitiesTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE = LocalDate.of(2014, 1, 3);
  private static final LocalTime TIME = LocalTime.of(10, 0);
  private static final ZoneId ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime DATE_TIME = DATE.atTime(TIME).atZone(ZONE);
  private static final SabrInterestRateParameters PARAM = SwaptionSabrRateVolatilityDataSet.SABR_PARAM_SHIFT_USD;
  private static final FixedIborSwapConvention CONV = SwaptionSabrRateVolatilityDataSet.SWAP_CONVENTION_USD;

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY = new ZonedDateTime[] {
      dateUtc(2014, 1, 3), dateUtc(2014, 1, 3), dateUtc(2015, 1, 3), dateUtc(2017, 1, 3)};
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final double[] TEST_TENOR = new double[] {2.0, 6.0, 7.0, 15.0};
  private static final double TEST_FORWARD = 0.025;
  private static final double[] TEST_STRIKE = new double[] {0.02, 0.025, 0.03};
  private static final int NB_STRIKE = TEST_STRIKE.length;
  static final SwaptionVolatilitiesName NAME = SwaptionVolatilitiesName.of("Test-SABR");
  static final SwaptionVolatilitiesName NAME2 = SwaptionVolatilitiesName.of("Test-SABR2");

  private static final double TOLERANCE_VOL = 1.0E-10;

  @Test
  public void test_of() {
    SabrParametersSwaptionVolatilities test = SabrParametersSwaptionVolatilities.of(NAME, CONV, DATE_TIME, PARAM);
    assertThat(test.getConvention()).isEqualTo(CONV);
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getParameters()).isEqualTo(PARAM);
    assertThat(test.getValuationDateTime()).isEqualTo(DATE_TIME);
  }

  @Test
  public void test_findData() {
    SabrParametersSwaptionVolatilities test = SabrParametersSwaptionVolatilities.of(NAME, CONV, DATE_TIME, PARAM);
    assertThat(test.findData(PARAM.getAlphaSurface().getName())).isEqualTo(Optional.of(PARAM.getAlphaSurface()));
    assertThat(test.findData(PARAM.getBetaSurface().getName())).isEqualTo(Optional.of(PARAM.getBetaSurface()));
    assertThat(test.findData(PARAM.getRhoSurface().getName())).isEqualTo(Optional.of(PARAM.getRhoSurface()));
    assertThat(test.findData(PARAM.getNuSurface().getName())).isEqualTo(Optional.of(PARAM.getNuSurface()));
    assertThat(test.findData(PARAM.getShiftSurface().getName())).isEqualTo(Optional.of(PARAM.getShiftSurface()));
    assertThat(test.findData(SurfaceName.of("Rubbish"))).isEqualTo(Optional.empty());
  }

  @Test
  public void test_calc() {
    SabrParametersSwaptionVolatilities test = SabrParametersSwaptionVolatilities.of(NAME, CONV, DATE_TIME, PARAM);
    assertThat(test.alpha(1d, 2d)).isEqualTo(PARAM.alpha(1d, 2d));
    assertThat(test.beta(1d, 2d)).isEqualTo(PARAM.beta(1d, 2d));
    assertThat(test.rho(1d, 2d)).isEqualTo(PARAM.rho(1d, 2d));
    assertThat(test.nu(1d, 2d)).isEqualTo(PARAM.nu(1d, 2d));
    assertThat(test.shift(1d, 2d)).isEqualTo(PARAM.shift(1d, 2d));
  }

  @Test
  public void test_tenor() {
    SabrParametersSwaptionVolatilities prov = SabrParametersSwaptionVolatilities.of(NAME, CONV, DATE_TIME, PARAM);
    double test1 = prov.tenor(DATE, DATE);
    assertThat(test1).isEqualTo(0d);
    double test2 = prov.tenor(DATE, DATE.plusYears(2));
    double test3 = prov.tenor(DATE, DATE.minusYears(2));
    assertThat(test2).isEqualTo(-test3);
    double test4 = prov.tenor(DATE, LocalDate.of(2019, 2, 2));
    double test5 = prov.tenor(DATE, LocalDate.of(2018, 12, 31));
    assertThat(test4).isEqualTo(5d);
    assertThat(test5).isEqualTo(5d);
  }

  @Test
  public void test_relativeTime() {
    SabrParametersSwaptionVolatilities prov = SabrParametersSwaptionVolatilities.of(NAME, CONV, DATE_TIME, PARAM);
    double test1 = prov.relativeTime(DATE_TIME);
    assertThat(test1).isEqualTo(0d);
    double test2 = prov.relativeTime(DATE_TIME.plusYears(2));
    double test3 = prov.relativeTime(DATE_TIME.minusYears(2));
    assertThat(test2).isCloseTo(-test3, offset(1e-2));
  }

  @Test
  public void test_volatility() {
    SabrParametersSwaptionVolatilities prov = SabrParametersSwaptionVolatilities.of(NAME, CONV, DATE_TIME, PARAM);
    for (int i = 0; i < NB_TEST; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        double expiryTime = prov.relativeTime(TEST_OPTION_EXPIRY[i]);
        double volExpected = PARAM.volatility(expiryTime, TEST_TENOR[i], TEST_STRIKE[j], TEST_FORWARD);
        double volComputed = prov.volatility(TEST_OPTION_EXPIRY[i], TEST_TENOR[i], TEST_STRIKE[j], TEST_FORWARD);
        assertThat(volComputed).isCloseTo(volExpected, offset(TOLERANCE_VOL));
      }
    }
  }

  @Test
  public void test_parameterSensitivity() {
    double alphaSensi = 2.24, betaSensi = 3.45, rhoSensi = -2.12, nuSensi = -0.56;
    SabrParametersSwaptionVolatilities prov = SabrParametersSwaptionVolatilities.of(NAME, CONV, DATE_TIME, PARAM);
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = prov.relativeTime(TEST_OPTION_EXPIRY[i]);
      PointSensitivities point = PointSensitivities.of(
          SwaptionSabrSensitivity.of(NAME, expiryTime, TEST_TENOR[i], ALPHA, USD, alphaSensi),
          SwaptionSabrSensitivity.of(NAME, expiryTime, TEST_TENOR[i], BETA, USD, betaSensi),
          SwaptionSabrSensitivity.of(NAME, expiryTime, TEST_TENOR[i], RHO, USD, rhoSensi),
          SwaptionSabrSensitivity.of(NAME, expiryTime, TEST_TENOR[i], NU, USD, nuSensi));
      CurrencyParameterSensitivities sensiComputed = prov.parameterSensitivity(point);
      UnitParameterSensitivity alphaSensitivities = prov.getParameters().getAlphaSurface()
          .zValueParameterSensitivity(expiryTime, TEST_TENOR[i]);
      UnitParameterSensitivity betaSensitivities = prov.getParameters().getBetaSurface()
          .zValueParameterSensitivity(expiryTime, TEST_TENOR[i]);
      UnitParameterSensitivity rhoSensitivities = prov.getParameters().getRhoSurface()
          .zValueParameterSensitivity(expiryTime, TEST_TENOR[i]);
      UnitParameterSensitivity nuSensitivities = prov.getParameters().getNuSurface()
          .zValueParameterSensitivity(expiryTime, TEST_TENOR[i]);
      CurrencyParameterSensitivity alphaSensiObj = sensiComputed.getSensitivity(
          SwaptionSabrRateVolatilityDataSet.META_ALPHA.getSurfaceName(), USD);
      CurrencyParameterSensitivity betaSensiObj = sensiComputed.getSensitivity(
          SwaptionSabrRateVolatilityDataSet.META_BETA_USD.getSurfaceName(), USD);
      CurrencyParameterSensitivity rhoSensiObj = sensiComputed.getSensitivity(
          SwaptionSabrRateVolatilityDataSet.META_RHO.getSurfaceName(), USD);
      CurrencyParameterSensitivity nuSensiObj = sensiComputed.getSensitivity(
          SwaptionSabrRateVolatilityDataSet.META_NU.getSurfaceName(), USD);
      DoubleArray alphaNodeSensiComputed = alphaSensiObj.getSensitivity();
      DoubleArray betaNodeSensiComputed = betaSensiObj.getSensitivity();
      DoubleArray rhoNodeSensiComputed = rhoSensiObj.getSensitivity();
      DoubleArray nuNodeSensiComputed = nuSensiObj.getSensitivity();
      assertThat(alphaSensitivities.getSensitivity().size()).isEqualTo(alphaNodeSensiComputed.size());
      assertThat(betaSensitivities.getSensitivity().size()).isEqualTo(betaNodeSensiComputed.size());
      assertThat(rhoSensitivities.getSensitivity().size()).isEqualTo(rhoNodeSensiComputed.size());
      assertThat(nuSensitivities.getSensitivity().size()).isEqualTo(nuNodeSensiComputed.size());
      for (int k = 0; k < alphaNodeSensiComputed.size(); ++k) {
        assertThat(alphaNodeSensiComputed.get(k)).isCloseTo(alphaSensitivities.getSensitivity().get(k) * alphaSensi, offset(TOLERANCE_VOL));
      }
      for (int k = 0; k < betaNodeSensiComputed.size(); ++k) {
        assertThat(betaNodeSensiComputed.get(k)).isCloseTo(betaSensitivities.getSensitivity().get(k) * betaSensi, offset(TOLERANCE_VOL));
      }
      for (int k = 0; k < rhoNodeSensiComputed.size(); ++k) {
        assertThat(rhoNodeSensiComputed.get(k)).isCloseTo(rhoSensitivities.getSensitivity().get(k) * rhoSensi, offset(TOLERANCE_VOL));
      }
      for (int k = 0; k < nuNodeSensiComputed.size(); ++k) {
        assertThat(nuNodeSensiComputed.get(k)).isCloseTo(nuSensitivities.getSensitivity().get(k) * nuSensi, offset(TOLERANCE_VOL));
      }
    }
  }

  @Test
  public void test_parameterSensitivity_multi() {
    double[] points1 = new double[] {2.24, 3.45, -2.12, -0.56};
    double[] points2 = new double[] {-0.145, 1.01, -5.0, -11.0};
    double[] points3 = new double[] {1.3, -4.32, 2.1, -7.18};
    SabrParametersSwaptionVolatilities prov = SabrParametersSwaptionVolatilities.of(NAME, CONV, DATE_TIME, PARAM);
    double expiryTime0 = prov.relativeTime(TEST_OPTION_EXPIRY[0]);
    double expiryTime3 = prov.relativeTime(TEST_OPTION_EXPIRY[3]);
    for (int i = 0; i < NB_TEST; i++) {
      PointSensitivities sensi1 = PointSensitivities.of(
          SwaptionSabrSensitivity.of(NAME, expiryTime0, TEST_TENOR[i], ALPHA, USD, points1[0]),
          SwaptionSabrSensitivity.of(NAME, expiryTime0, TEST_TENOR[i], BETA, USD, points1[1]),
          SwaptionSabrSensitivity.of(NAME, expiryTime0, TEST_TENOR[i], RHO, USD, points1[2]),
          SwaptionSabrSensitivity.of(NAME, expiryTime0, TEST_TENOR[i], NU, USD, points1[3]));
      PointSensitivities sensi2 = PointSensitivities.of(
          SwaptionSabrSensitivity.of(NAME, expiryTime0, TEST_TENOR[i], ALPHA, USD, points2[0]),
          SwaptionSabrSensitivity.of(NAME, expiryTime0, TEST_TENOR[i], BETA, USD, points2[1]),
          SwaptionSabrSensitivity.of(NAME, expiryTime0, TEST_TENOR[i], RHO, USD, points2[2]),
          SwaptionSabrSensitivity.of(NAME, expiryTime0, TEST_TENOR[i], NU, USD, points2[3]));
      PointSensitivities sensi3 = PointSensitivities.of(
          SwaptionSabrSensitivity.of(NAME, expiryTime3, TEST_TENOR[i], ALPHA, USD, points3[0]),
          SwaptionSabrSensitivity.of(NAME, expiryTime3, TEST_TENOR[i], BETA, USD, points3[1]),
          SwaptionSabrSensitivity.of(NAME, expiryTime3, TEST_TENOR[i], RHO, USD, points3[2]),
          SwaptionSabrSensitivity.of(NAME, expiryTime3, TEST_TENOR[i], NU, USD, points3[3]));
      PointSensitivities sensis = sensi1.combinedWith(sensi2).combinedWith(sensi3).normalized();
      CurrencyParameterSensitivities computed = prov.parameterSensitivity(sensis);
      CurrencyParameterSensitivities expected = prov.parameterSensitivity(sensi1)
          .combinedWith(prov.parameterSensitivity(sensi2))
          .combinedWith(prov.parameterSensitivity(sensi3));
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

  @Test
  public void test_pointShifts() {
    SabrParametersSwaptionVolatilities base = SabrParametersSwaptionVolatilities.of(NAME, CONV, DATE_TIME, PARAM);
    PointShiftsBuilder builder = PointShifts.builder(ShiftType.ABSOLUTE);
    for (int i = 0; i < base.getParameterCount(); ++i) {
      builder.addShift(0, base.getParameterMetadata(i).getIdentifier(), 0.1d * (i + 1d));
      builder.addShift(1, base.getParameterMetadata(i).getIdentifier(), 10d * (i + 1d));
    }
    PointShifts shifts = builder.build();
    MarketDataBox<ParameterizedData> resBox = shifts.applyTo(MarketDataBox.ofSingleValue(base), REF_DATA);
    SabrParametersSwaptionVolatilities computed0 = (SabrParametersSwaptionVolatilities) resBox.getValue(0);
    SabrParametersSwaptionVolatilities computed1 = (SabrParametersSwaptionVolatilities) resBox.getValue(1);
    for (int i = 0; i < base.getParameterCount(); ++i) {
      assertThat(computed0.getParameter(i)).isEqualTo(base.getParameter(i) + 0.1d * (i + 1d));
      assertThat(computed1.getParameter(i)).isEqualTo(base.getParameter(i) + 10d * (i + 1d));
    }
  }

  @Test
  public void coverage() {
    SabrParametersSwaptionVolatilities test1 = SabrParametersSwaptionVolatilities.of(NAME, CONV, DATE_TIME, PARAM);
    coverImmutableBean(test1);
    SabrParametersSwaptionVolatilities test2 = SabrParametersSwaptionVolatilities.of(
        NAME2,
        SwaptionSabrRateVolatilityDataSet.SWAP_CONVENTION_EUR,
        DATE_TIME.plusDays(1),
        SwaptionSabrRateVolatilityDataSet.SABR_PARAM_USD);
    coverBeanEquals(test1, test2);
  }
}
