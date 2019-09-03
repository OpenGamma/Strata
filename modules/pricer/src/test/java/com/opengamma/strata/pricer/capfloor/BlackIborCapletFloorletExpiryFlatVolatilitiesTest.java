/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.TenorParameterMetadata;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link BlackIborCapletFloorletExpiryFlatVolatilities}.
 */
public class BlackIborCapletFloorletExpiryFlatVolatilitiesTest {

  private static final DoubleArray TIME = DoubleArray.of(0.25, 0.5, 1.0, 2.0);
  private static final DoubleArray VOL = DoubleArray.of(0.14, 0.15, 0.12, 0.1);
  private static final ImmutableList<Tenor> TENOR = ImmutableList.of(
      Tenor.TENOR_3M, Tenor.TENOR_6M, Tenor.TENOR_1Y, Tenor.TENOR_2Y);
  private static final CurveMetadata METADATA;
  static {
    List<TenorParameterMetadata> list = new ArrayList<TenorParameterMetadata>();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      TenorParameterMetadata parameterMetadata = TenorParameterMetadata.of(TENOR.get(i));
      list.add(parameterMetadata);
    }
    METADATA = Curves.blackVolatilityByExpiry("CAP_VOL", ACT_365F).withParameterMetadata(list);
  }
  private static final InterpolatedNodalCurve CURVE = InterpolatedNodalCurve.of(METADATA, TIME, VOL, LINEAR);
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final BlackIborCapletFloorletExpiryFlatVolatilities VOLS =
      BlackIborCapletFloorletExpiryFlatVolatilities.of(GBP_LIBOR_3M, VAL_DATE_TIME, CURVE);

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY = new ZonedDateTime[] {
      dateUtc(2015, 2, 17), dateUtc(2015, 5, 17), dateUtc(2015, 6, 17), dateUtc(2017, 2, 17), dateUtc(2017, 5, 30)};
  private static final int NB_TEST_EXPIRY = TEST_OPTION_EXPIRY.length;
  private static final double[] TEST_STRIKE = new double[] {0.001, 0.01, 0.022, 0.042};
  private static final double[] TEST_SENSITIVITY = new double[] {1.0, -34.0, 12.0, -2.01};
  private static final int NB_TEST_STRIKE_SENSI = TEST_STRIKE.length;
  private static final double TEST_FORWARD = 0.015; // not used internally

  private static final double TOLERANCE_VOL = 1.0E-10;

  @Test
  public void test_getter() {
    assertThat(VOLS.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(VOLS.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(VOLS.getCurve()).isEqualTo(CURVE);
  }

  @Test
  public void test_price_formula() {
    double sampleVol = 0.2;
    for (int i = 0; i < NB_TEST_EXPIRY; i++) {
      double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
      for (int j = 0; j < NB_TEST_STRIKE_SENSI; j++) {
        for (PutCall putCall : new PutCall[] {PutCall.CALL, PutCall.PUT}) {
          double price = VOLS.price(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double delta = VOLS.priceDelta(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double gamma = VOLS.priceGamma(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double theta = VOLS.priceTheta(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double vega = VOLS.priceVega(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          assertThat(price).isEqualTo(BlackFormulaRepository.price(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol, putCall.isCall()));
          assertThat(delta).isEqualTo(BlackFormulaRepository.delta(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol, putCall.isCall()));
          assertThat(gamma).isEqualTo(BlackFormulaRepository.gamma(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol));
          assertThat(theta).isEqualTo(BlackFormulaRepository.driftlessTheta(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol));
          assertThat(vega).isEqualTo(BlackFormulaRepository.vega(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol));
        }
      }
    }
  }

  @Test
  public void test_relativeTime() {
    double test1 = VOLS.relativeTime(VAL_DATE_TIME);
    assertThat(test1).isEqualTo(0d);
    double test2 = VOLS.relativeTime(date(2018, 2, 17).atStartOfDay(LONDON_ZONE));
    double test3 = VOLS.relativeTime(date(2012, 2, 17).atStartOfDay(LONDON_ZONE));
    assertThat(test2).isEqualTo(-test3); // consistency checked
  }

  @Test
  public void test_volatility() {
    for (int i = 0; i < NB_TEST_EXPIRY; i++) {
      double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
      for (int j = 0; j < NB_TEST_STRIKE_SENSI; ++j) {
        double volExpected = CURVE.yValue(expiryTime);
        double volComputed = VOLS.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[j], TEST_FORWARD);
        assertThat(volComputed).isCloseTo(volExpected, offset(TOLERANCE_VOL));
      }
    }
  }

  @Test
  public void test_volatility_sensitivity() {
    double eps = 1.0e-6;
    int nData = TIME.size();
    for (int i = 0; i < NB_TEST_EXPIRY; i++) {
      for (int k = 0; k < NB_TEST_STRIKE_SENSI; k++) {
        double expiryTime = VOLS.relativeTime(TEST_OPTION_EXPIRY[i]);
        IborCapletFloorletSensitivity point = IborCapletFloorletSensitivity.of(
            VOLS.getName(), expiryTime, TEST_STRIKE[k], TEST_FORWARD, GBP, TEST_SENSITIVITY[k]);
        double[] sensFd = new double[nData];
        for (int j = 0; j < nData; j++) {
          DoubleArray volDataUp = VOL.subArray(0, nData).with(j, VOL.get(j) + eps);
          DoubleArray volDataDw = VOL.subArray(0, nData).with(j, VOL.get(j) - eps);
          InterpolatedNodalCurve paramUp =
              InterpolatedNodalCurve.of(METADATA, TIME, volDataUp, LINEAR);
          InterpolatedNodalCurve paramDw =
              InterpolatedNodalCurve.of(METADATA, TIME, volDataDw, LINEAR);
          BlackIborCapletFloorletExpiryFlatVolatilities provUp =
              BlackIborCapletFloorletExpiryFlatVolatilities.of(GBP_LIBOR_3M, VAL_DATE_TIME, paramUp);
          BlackIborCapletFloorletExpiryFlatVolatilities provDw =
              BlackIborCapletFloorletExpiryFlatVolatilities.of(GBP_LIBOR_3M, VAL_DATE_TIME, paramDw);
          double volUp = provUp.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[k], TEST_FORWARD);
          double volDw = provDw.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[k], TEST_FORWARD);
          double fd = 0.5 * (volUp - volDw) / eps;
          sensFd[j] = fd * TEST_SENSITIVITY[k];
        }
        CurrencyParameterSensitivity sensActual = VOLS.parameterSensitivity(point).getSensitivities().get(0);
        double[] computed = sensActual.getSensitivity().toArray();
        assertThat(DoubleArrayMath.fuzzyEquals(computed, sensFd, eps)).isTrue();
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(VOLS);
    coverBeanEquals(VOLS, VOLS);
  }

}
