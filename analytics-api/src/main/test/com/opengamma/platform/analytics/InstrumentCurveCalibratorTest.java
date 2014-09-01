package com.opengamma.platform.analytics;

import static com.opengamma.basics.date.Tenor.TENOR_18M;
import static com.opengamma.basics.date.Tenor.TENOR_1D;
import static com.opengamma.basics.date.Tenor.TENOR_1M;
import static com.opengamma.basics.date.Tenor.TENOR_1W;
import static com.opengamma.basics.date.Tenor.TENOR_1Y;
import static com.opengamma.basics.date.Tenor.TENOR_2W;
import static com.opengamma.basics.date.Tenor.TENOR_2Y;
import static com.opengamma.basics.date.Tenor.TENOR_3M;
import static com.opengamma.basics.date.Tenor.TENOR_3Y;
import static com.opengamma.basics.date.Tenor.TENOR_6M;
import static com.opengamma.platform.analytics.CurveNodeInstrumentType.CASH;
import static com.opengamma.platform.analytics.CurveNodeInstrumentType.SWAP;
import static org.testng.Assert.assertNotNull;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.date.Tenor;

public class InstrumentCurveCalibratorTest {

  @Test
  public void cashOnly() {

    YieldCurve yieldCurve = new InstrumentCurveCalibrator().buildYieldCurve(
        ImmutableMap.of(
            TENOR_1D, CASH,
            TENOR_1W, CASH,
            TENOR_2W, CASH,
            TENOR_1M, CASH),
        ImmutableMap.of(
            TENOR_1D, 0.0012,
            TENOR_1W, 0.0015,
            TENOR_2W, 0.0025,
            TENOR_1M, 0.0043),
        Currency.USD,
        LocalDate.of(2014, 7, 1));

    assertNotNull(yieldCurve);
  }

  @Test
  public void cashAndSwaps() {

    ImmutableMap<Tenor, CurveNodeInstrumentType> instrumentTypes =
        ImmutableMap.<Tenor, CurveNodeInstrumentType>builder()
            .put(TENOR_1D, CASH)
            .put(TENOR_1W, CASH)
            .put(TENOR_2W, CASH)
            .put(TENOR_1M, CASH)
            .put(TENOR_3M, CASH)
            .put(TENOR_6M, CASH)
            .put(TENOR_1Y, CASH)
            .put(TENOR_18M, SWAP)
            .put(TENOR_2Y, SWAP)
            .put(TENOR_3Y, SWAP)
            .build();

    ImmutableMap<Tenor, Double> rates = ImmutableMap.<Tenor, Double>builder()
        .put(TENOR_1D, 0.0012)
        .put(TENOR_1W, 0.0015)
        .put(TENOR_2W, 0.0025)
        .put(TENOR_1M, 0.0043)
        .put(TENOR_3M, 0.006)
        .put(TENOR_6M, 0.0075)
        .put(TENOR_1Y, 0.009)
        .put(TENOR_18M, 0.016)
        .put(TENOR_2Y, 0.022)
        .put(TENOR_3Y, 0.029)
        .build();
    YieldCurve yieldCurve = new InstrumentCurveCalibrator().buildYieldCurve(
        instrumentTypes,
        rates,
        Currency.USD,
        LocalDate.of(2014, 7, 1));

    assertNotNull(yieldCurve);
  }
}
