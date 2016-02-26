/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.sensitivity.IborCapletFloorletSensitivity;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.surface.meta.GenericVolatilitySurfaceYearFractionMetadata;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;

/**
 * Test {@link BlackIborCapletFloorletExpiryStrikeVolatilities}.
 */
@Test
public class BlackIborCapletFloorletExpiryStrikeVolatilitiesTest {

  private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolator.of(
      CurveInterpolators.LINEAR.getName(), CurveExtrapolators.FLAT.getName(), CurveExtrapolators.FLAT.getName());
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final DoubleArray TIME =
      DoubleArray.of(0.25, 0.5, 1.0, 0.25, 0.5, 1.0, 0.25, 0.5, 1.0, 0.25, 0.5, 1.0);
  private static final DoubleArray STRIKE =
      DoubleArray.of(0.005, 0.005, 0.005, 0.01, 0.01, 0.01, 0.02, 0.02, 0.02, 0.035, 0.035, 0.035);
  private static final DoubleArray VOL =
      DoubleArray.of(0.14, 0.12, 0.1, 0.14, 0.13, 0.12, 0.13, 0.12, 0.11, 0.12, 0.11, 0.1);
  private static final SurfaceMetadata METADATA_WITH_PARAM;
  private static final SurfaceMetadata METADATA;
  static {
    List<GenericVolatilitySurfaceYearFractionMetadata> list =
        new ArrayList<GenericVolatilitySurfaceYearFractionMetadata>();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      GenericVolatilitySurfaceYearFractionMetadata parameterMetadata =
          GenericVolatilitySurfaceYearFractionMetadata.of(TIME.get(i), SimpleStrike.of(STRIKE.get(i)));
      list.add(parameterMetadata);
    }
    METADATA_WITH_PARAM = DefaultSurfaceMetadata.builder()
        .dayCount(ACT_365F)
        .parameterMetadata(list)
        .surfaceName(SurfaceName.of("CAP_VOL"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .build();
    METADATA = DefaultSurfaceMetadata.builder()
        .dayCount(ACT_365F)
        .surfaceName(SurfaceName.of("CAP_VOL"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .build();
  }
  private static final InterpolatedNodalSurface SURFACE_WITH_PARAM =
      InterpolatedNodalSurface.of(METADATA_WITH_PARAM, TIME, STRIKE, VOL, INTERPOLATOR_2D);
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIME, STRIKE, VOL, INTERPOLATOR_2D);
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities PROVIDER_WITH_PARAM =
      BlackIborCapletFloorletExpiryStrikeVolatilities.of(
          SURFACE_WITH_PARAM, GBP_LIBOR_3M, VAL_DATE, VAL_TIME, LONDON_ZONE, ACT_365F);
  private static final BlackIborCapletFloorletExpiryStrikeVolatilities PROVIDER =
      BlackIborCapletFloorletExpiryStrikeVolatilities.of(SURFACE, GBP_LIBOR_3M, VAL_DATE_TIME, ACT_365F);

  private static final ZonedDateTime[] TEST_OPTION_EXPIRY =
      new ZonedDateTime[] {dateUtc(2015, 2, 17), dateUtc(2015, 5, 17), dateUtc(2015, 6, 17), dateUtc(2017, 2, 17)};
  private static final int NB_TEST = TEST_OPTION_EXPIRY.length;
  private static final double[] TEST_STRIKE = new double[] {0.001, 0.01, 0.022, 0.042};
  private static final double[] TEST_SENSITIVITY = new double[] {1.0, -34.0, 12.0, 0.1};
  private static final double TEST_FORWARD = 0.015; // not used internally

  private static final double TOLERANCE_VOL = 1.0E-10;

  public void test_getter() {
    assertEquals(PROVIDER_WITH_PARAM.getValuationDate(), VAL_DATE);
    assertEquals(PROVIDER_WITH_PARAM.getIndex(), GBP_LIBOR_3M);
    assertEquals(PROVIDER_WITH_PARAM.getSurface(), SURFACE_WITH_PARAM);
    assertEquals(PROVIDER_WITH_PARAM.getDayCount(), ACT_365F);
  }

  public void test_price_formula() {
    double sampleVol = 0.2;
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = PROVIDER_WITH_PARAM.relativeTime(TEST_OPTION_EXPIRY[i]);
      for (int j = 0; j < NB_TEST; j++) {
        for (PutCall putCall : new PutCall[] {PutCall.CALL, PutCall.PUT}) {
          double price = PROVIDER.price(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double delta = PROVIDER.priceDelta(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double gamma = PROVIDER.priceGamma(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double theta = PROVIDER.priceTheta(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          double vega = PROVIDER.priceVega(expiryTime, putCall, TEST_STRIKE[j], TEST_FORWARD, sampleVol);
          assertEquals(price,
              BlackFormulaRepository.price(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol, putCall.isCall()));
          assertEquals(delta,
              BlackFormulaRepository.delta(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol, putCall.isCall()));
          assertEquals(gamma,
              BlackFormulaRepository.gamma(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol));
          assertEquals(theta,
              BlackFormulaRepository.driftlessTheta(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol));
          assertEquals(vega,
              BlackFormulaRepository.vega(TEST_FORWARD, TEST_STRIKE[j], expiryTime, sampleVol));
        }
      }
    }
  }

  public void test_relativeTime() {
    double test1 = PROVIDER_WITH_PARAM.relativeTime(VAL_DATE_TIME);
    assertEquals(test1, 0d);
    double test2 = PROVIDER_WITH_PARAM.relativeTime(date(2018, 2, 17).atStartOfDay(LONDON_ZONE));
    double test3 = PROVIDER_WITH_PARAM.relativeTime(date(2012, 2, 17).atStartOfDay(LONDON_ZONE));
    assertEquals(test2, -test3); // consistency checked
  }

  public void test_volatility() {
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = PROVIDER_WITH_PARAM.relativeTime(TEST_OPTION_EXPIRY[i]);
      for (int j = 0; j < NB_TEST; ++j) {
        double volExpected = SURFACE_WITH_PARAM.zValue(expiryTime, TEST_STRIKE[j]);
        double volComputed = PROVIDER_WITH_PARAM.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[j], TEST_FORWARD);
        assertEquals(volComputed, volExpected, TOLERANCE_VOL);
      }
    }
  }

  public void test_volatility_sensitivity() {
    double eps = 1.0e-6;
    int nData = TIME.size();
    for (int i = 0; i < NB_TEST; i++) {
      for (int k = 0; k < NB_TEST; k++) {
        IborCapletFloorletSensitivity point = IborCapletFloorletSensitivity.of(
            GBP_LIBOR_3M, TEST_OPTION_EXPIRY[i], TEST_STRIKE[k], TEST_FORWARD, GBP, TEST_SENSITIVITY[i]);
        SurfaceCurrencyParameterSensitivity sensi = PROVIDER_WITH_PARAM.surfaceCurrencyParameterSensitivity(point);
        Map<DoublesPair, Double> map = new HashMap<DoublesPair, Double>();
        for (int j = 0; j < nData; ++j) {
          DoubleArray volDataUp = VOL.subArray(0, nData).with(j, VOL.get(j) + eps);
          DoubleArray volDataDw = VOL.subArray(0, nData).with(j, VOL.get(j) - eps);
          InterpolatedNodalSurface paramUp =
              InterpolatedNodalSurface.of(METADATA_WITH_PARAM, TIME, STRIKE, volDataUp, INTERPOLATOR_2D);
          InterpolatedNodalSurface paramDw =
              InterpolatedNodalSurface.of(METADATA_WITH_PARAM, TIME, STRIKE, volDataDw, INTERPOLATOR_2D);
          BlackIborCapletFloorletExpiryStrikeVolatilities provUp =
              BlackIborCapletFloorletExpiryStrikeVolatilities.of(paramUp, GBP_LIBOR_3M, VAL_DATE_TIME, ACT_365F);
          BlackIborCapletFloorletExpiryStrikeVolatilities provDw =
              BlackIborCapletFloorletExpiryStrikeVolatilities.of(paramDw, GBP_LIBOR_3M, VAL_DATE_TIME, ACT_365F);
          double volUp = provUp.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[k], TEST_FORWARD);
          double volDw = provDw.volatility(TEST_OPTION_EXPIRY[i], TEST_STRIKE[k], TEST_FORWARD);
          double fd = 0.5 * (volUp - volDw) / eps;
          map.put(DoublesPair.of(TIME.get(j), STRIKE.get(j)), fd * TEST_SENSITIVITY[i]);
        }
        SurfaceCurrencyParameterSensitivity sensiFromNoMetadata = PROVIDER.surfaceCurrencyParameterSensitivity(point);
        List<SurfaceParameterMetadata> list = sensi.getMetadata().getParameterMetadata().get();
        DoubleArray computed = sensi.getSensitivity();
        assertEquals(computed.size(), nData);
        for (int j = 0; j < list.size(); ++j) {
          GenericVolatilitySurfaceYearFractionMetadata metadata =
              (GenericVolatilitySurfaceYearFractionMetadata) list.get(i);
          double expected = map.get(DoublesPair.of(metadata.getYearFraction(), metadata.getStrike().getValue()));
          assertEquals(computed.get(i), expected, eps);
          assertTrue(sensiFromNoMetadata.getMetadata().getParameterMetadata().get().contains(metadata));
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(PROVIDER_WITH_PARAM);
    coverBeanEquals(PROVIDER_WITH_PARAM, PROVIDER);
  }

}
