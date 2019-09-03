/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.market.model.SabrParameterType.ALPHA;
import static com.opengamma.strata.market.model.SabrParameterType.BETA;
import static com.opengamma.strata.market.model.SabrParameterType.NU;
import static com.opengamma.strata.market.model.SabrParameterType.RHO;
import static com.opengamma.strata.market.model.SabrParameterType.SHIFT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.pricer.model.SabrParameters;

/**
 * Test {@link SabrParametersIborCapletFloorletVolatilities}.
 */
public class SabrParametersIborCapletFloorletVolatilitiesTest {

  private static final LocalDate DATE = LocalDate.of(2014, 1, 3);
  private static final LocalTime TIME = LocalTime.of(10, 0);
  private static final ZoneId ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime DATE_TIME = DATE.atTime(TIME).atZone(ZONE);
  private static final SabrParameters PARAM = IborCapletFloorletSabrRateVolatilityDataSet.SABR_PARAM;

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY = new ZonedDateTime[] {
      dateUtc(2014, 1, 3), dateUtc(2015, 1, 3), dateUtc(2016, 4, 21), dateUtc(2017, 1, 3)};
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final double TEST_FORWARD = 0.025;
  private static final double[] TEST_STRIKE = new double[] {0.02, 0.025, 0.03};
  private static final int NB_STRIKE = TEST_STRIKE.length;
  static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletSabrRateVolatilityDataSet.NAME;
  static final IborCapletFloorletVolatilitiesName NAME2 = IborCapletFloorletVolatilitiesName.of("Test-SABR2");
  private static final double TOLERANCE_VOL = 1.0E-10;

  @Test
  public void test_of() {
    SabrParametersIborCapletFloorletVolatilities test =
        SabrParametersIborCapletFloorletVolatilities.of(NAME, EUR_EURIBOR_3M, DATE_TIME, PARAM);
    assertThat(test.getIndex()).isEqualTo(EUR_EURIBOR_3M);
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getParameters()).isEqualTo(PARAM);
    assertThat(test.getValuationDateTime()).isEqualTo(DATE_TIME);
    assertThat(test.getParameterCount()).isEqualTo(PARAM.getParameterCount());
    int nParams = PARAM.getParameterCount();
    double newValue = 152d;
    for (int i = 0; i < nParams; ++i) {
      assertThat(test.getParameter(i)).isEqualTo(PARAM.getParameter(i));
      assertThat(test.getParameterMetadata(i)).isEqualTo(PARAM.getParameterMetadata(i));
      assertThat(test.withParameter(i, newValue)).isEqualTo(SabrParametersIborCapletFloorletVolatilities.of(
          NAME, EUR_EURIBOR_3M, DATE_TIME, PARAM.withParameter(i, newValue)));
      assertThat(test.withPerturbation((n, v, m) -> 2d * v)).isEqualTo(SabrParametersIborCapletFloorletVolatilities.of(
          NAME, EUR_EURIBOR_3M, DATE_TIME, PARAM.withPerturbation((n, v, m) -> 2d * v)));
    }
  }

  @Test
  public void test_findData() {
    SabrParametersIborCapletFloorletVolatilities test =
        SabrParametersIborCapletFloorletVolatilities.of(NAME, EUR_EURIBOR_3M, DATE_TIME, PARAM);
    assertThat(test.findData(PARAM.getAlphaCurve().getName())).isEqualTo(Optional.of(PARAM.getAlphaCurve()));
    assertThat(test.findData(PARAM.getBetaCurve().getName())).isEqualTo(Optional.of(PARAM.getBetaCurve()));
    assertThat(test.findData(PARAM.getRhoCurve().getName())).isEqualTo(Optional.of(PARAM.getRhoCurve()));
    assertThat(test.findData(PARAM.getNuCurve().getName())).isEqualTo(Optional.of(PARAM.getNuCurve()));
    assertThat(test.findData(PARAM.getShiftCurve().getName())).isEqualTo(Optional.of(PARAM.getShiftCurve()));
    assertThat(test.findData(SurfaceName.of("Rubbish"))).isEqualTo(Optional.empty());
  }

  @Test
  public void test_calc() {
    SabrParametersIborCapletFloorletVolatilities test =
        SabrParametersIborCapletFloorletVolatilities.of(NAME, EUR_EURIBOR_3M, DATE_TIME, PARAM);
    assertThat(test.alpha(1.56)).isEqualTo(PARAM.alpha(1.56));
    assertThat(test.beta(1.56)).isEqualTo(PARAM.beta(1.56));
    assertThat(test.rho(1.56)).isEqualTo(PARAM.rho(1.56));
    assertThat(test.nu(1.56)).isEqualTo(PARAM.nu(1.56));
    assertThat(test.shift(1.56)).isEqualTo(PARAM.shift(1.56));
  }

  @Test
  public void test_relativeTime() {
    SabrParametersIborCapletFloorletVolatilities prov =
        SabrParametersIborCapletFloorletVolatilities.of(NAME, EUR_EURIBOR_3M, DATE_TIME, PARAM);
    double test1 = prov.relativeTime(DATE_TIME);
    assertThat(test1).isEqualTo(0d);
    double test2 = prov.relativeTime(DATE_TIME.plusYears(2));
    double test3 = prov.relativeTime(DATE_TIME.minusYears(2));
    assertThat(test2).isCloseTo(-test3, offset(1e-2));
  }

  @Test
  public void test_volatility() {
    SabrParametersIborCapletFloorletVolatilities prov =
        SabrParametersIborCapletFloorletVolatilities.of(NAME, EUR_EURIBOR_3M, DATE_TIME, PARAM);
    for (int i = 0; i < NB_TEST; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        double expiryTime = prov.relativeTime(TEST_OPTION_EXPIRY[i]);
        double volExpected = PARAM.volatility(expiryTime, TEST_STRIKE[j], TEST_FORWARD);
        double volComputed = prov.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[j], TEST_FORWARD);
        assertThat(volComputed).isCloseTo(volExpected, offset(TOLERANCE_VOL));
        ValueDerivatives volAdjExpected = PARAM.volatilityAdjoint(expiryTime, TEST_STRIKE[j], TEST_FORWARD);
        ValueDerivatives volAdjComputed = prov.volatilityAdjoint(expiryTime, TEST_STRIKE[j], TEST_FORWARD);
        assertThat(volAdjComputed.getValue()).isCloseTo(volExpected, offset(TOLERANCE_VOL));
        assertThat(DoubleArrayMath.fuzzyEquals(
            volAdjComputed.getDerivatives().toArray(), volAdjExpected.getDerivatives().toArray(), TOLERANCE_VOL)).isTrue();
      }
    }
  }

  @Test
  public void test_parameterSensitivity() {
    double alphaSensi = 2.24, betaSensi = 3.45, rhoSensi = -2.12, nuSensi = -0.56, shiftSensi = 2.5;
    SabrParametersIborCapletFloorletVolatilities prov =
        SabrParametersIborCapletFloorletVolatilities.of(NAME, EUR_EURIBOR_3M, DATE_TIME, PARAM);
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = prov.relativeTime(TEST_OPTION_EXPIRY[i]);
      PointSensitivities point = PointSensitivities.of(
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime, ALPHA, EUR, alphaSensi),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime, BETA, EUR, betaSensi),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime, RHO, EUR, rhoSensi),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime, NU, EUR, nuSensi),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime, SHIFT, EUR, shiftSensi));
      CurrencyParameterSensitivities sensiComputed = prov.parameterSensitivity(point);
      UnitParameterSensitivity alphaSensitivities = prov.getParameters().getAlphaCurve()
          .yValueParameterSensitivity(expiryTime);
      UnitParameterSensitivity betaSensitivities = prov.getParameters().getBetaCurve()
          .yValueParameterSensitivity(expiryTime);
      UnitParameterSensitivity rhoSensitivities = prov.getParameters().getRhoCurve()
          .yValueParameterSensitivity(expiryTime);
      UnitParameterSensitivity nuSensitivities = prov.getParameters().getNuCurve()
          .yValueParameterSensitivity(expiryTime);
      UnitParameterSensitivity shiftSensitivities = prov.getParameters().getShiftCurve()
          .yValueParameterSensitivity(expiryTime);
      CurrencyParameterSensitivity alphaSensiObj = sensiComputed.getSensitivity(
          IborCapletFloorletSabrRateVolatilityDataSet.META_ALPHA.getCurveName(), EUR);
      CurrencyParameterSensitivity betaSensiObj = sensiComputed.getSensitivity(
          IborCapletFloorletSabrRateVolatilityDataSet.META_BETA.getCurveName(), EUR);
      CurrencyParameterSensitivity rhoSensiObj = sensiComputed.getSensitivity(
          IborCapletFloorletSabrRateVolatilityDataSet.META_RHO.getCurveName(), EUR);
      CurrencyParameterSensitivity nuSensiObj = sensiComputed.getSensitivity(
          IborCapletFloorletSabrRateVolatilityDataSet.META_NU.getCurveName(), EUR);
      CurrencyParameterSensitivity shiftSensiObj = sensiComputed.getSensitivity(
          IborCapletFloorletSabrRateVolatilityDataSet.META_SHIFT.getCurveName(), EUR);
      DoubleArray alphaNodeSensiComputed = alphaSensiObj.getSensitivity();
      DoubleArray betaNodeSensiComputed = betaSensiObj.getSensitivity();
      DoubleArray rhoNodeSensiComputed = rhoSensiObj.getSensitivity();
      DoubleArray nuNodeSensiComputed = nuSensiObj.getSensitivity();
      DoubleArray shiftNodeSensiComputed = shiftSensiObj.getSensitivity();
      assertThat(alphaSensitivities.getSensitivity().size()).isEqualTo(alphaNodeSensiComputed.size());
      assertThat(betaSensitivities.getSensitivity().size()).isEqualTo(betaNodeSensiComputed.size());
      assertThat(rhoSensitivities.getSensitivity().size()).isEqualTo(rhoNodeSensiComputed.size());
      assertThat(nuSensitivities.getSensitivity().size()).isEqualTo(nuNodeSensiComputed.size());
      assertThat(shiftSensitivities.getSensitivity().size()).isEqualTo(shiftNodeSensiComputed.size());
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
      for (int k = 0; k < shiftNodeSensiComputed.size(); ++k) {
        assertThat(shiftNodeSensiComputed.get(k)).isCloseTo(shiftSensitivities.getSensitivity().get(k) * shiftSensi, offset(TOLERANCE_VOL));
      }
    }
  }

  @Test
  public void test_parameterSensitivity_multi() {
    double[] points1 = new double[] {2.24, 3.45, -2.12, -0.56};
    double[] points2 = new double[] {-0.145, 1.01, -5.0, -11.0};
    double[] points3 = new double[] {1.3, -4.32, 2.1, -7.18};
    SabrParametersIborCapletFloorletVolatilities prov =
        SabrParametersIborCapletFloorletVolatilities.of(NAME, EUR_EURIBOR_3M, DATE_TIME, PARAM);
    double expiryTime0 = prov.relativeTime(TEST_OPTION_EXPIRY[0]);
    double expiryTime3 = prov.relativeTime(TEST_OPTION_EXPIRY[3]);
    for (int i = 0; i < NB_TEST; i++) {
      PointSensitivities sensi1 = PointSensitivities.of(
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime0, ALPHA, EUR, points1[0]),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime0, BETA, EUR, points1[1]),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime0, RHO, EUR, points1[2]),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime0, NU, EUR, points1[3]));
      PointSensitivities sensi2 = PointSensitivities.of(
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime0, ALPHA, EUR, points2[0]),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime0, BETA, EUR, points2[1]),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime0, RHO, EUR, points2[2]),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime0, NU, EUR, points2[3]));
      PointSensitivities sensi3 = PointSensitivities.of(
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime3, ALPHA, EUR, points3[0]),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime3, BETA, EUR, points3[1]),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime3, RHO, EUR, points3[2]),
          IborCapletFloorletSabrSensitivity.of(NAME, expiryTime3, NU, EUR, points3[3]));
      PointSensitivities sensis = sensi1.combinedWith(sensi2).combinedWith(sensi3).normalized();
      CurrencyParameterSensitivities computed = prov.parameterSensitivity(sensis);
      CurrencyParameterSensitivities expected = prov.parameterSensitivity(sensi1)
          .combinedWith(prov.parameterSensitivity(sensi2))
          .combinedWith(prov.parameterSensitivity(sensi3));
      DoubleArrayMath.fuzzyEquals(
          computed.getSensitivity(PARAM.getAlphaCurve().getName(), EUR).getSensitivity().toArray(),
          expected.getSensitivity(PARAM.getAlphaCurve().getName(), EUR).getSensitivity().toArray(),
          TOLERANCE_VOL);
      DoubleArrayMath.fuzzyEquals(
          computed.getSensitivity(PARAM.getBetaCurve().getName(), EUR).getSensitivity().toArray(),
          expected.getSensitivity(PARAM.getBetaCurve().getName(), EUR).getSensitivity().toArray(),
          TOLERANCE_VOL);
      DoubleArrayMath.fuzzyEquals(
          computed.getSensitivity(PARAM.getRhoCurve().getName(), EUR).getSensitivity().toArray(),
          expected.getSensitivity(PARAM.getRhoCurve().getName(), EUR).getSensitivity().toArray(),
          TOLERANCE_VOL);
      DoubleArrayMath.fuzzyEquals(
          computed.getSensitivity(PARAM.getNuCurve().getName(), EUR).getSensitivity().toArray(),
          expected.getSensitivity(PARAM.getNuCurve().getName(), EUR).getSensitivity().toArray(),
          TOLERANCE_VOL);
    }
  }

  @Test
  public void coverage() {
    SabrParametersIborCapletFloorletVolatilities test1 =
        SabrParametersIborCapletFloorletVolatilities.of(NAME, EUR_EURIBOR_3M, DATE_TIME, PARAM);
    coverImmutableBean(test1);
    SabrParametersIborCapletFloorletVolatilities test2 = SabrParametersIborCapletFloorletVolatilities.of(
        NAME2, IborIndices.EUR_LIBOR_3M, DATE_TIME.plusDays(1), IborCapletFloorletSabrRateVolatilityDataSet.SABR_PARAM_FLAT);
    coverBeanEquals(test1, test2);
  }

}
