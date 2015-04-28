/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.GridInterpolator2D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.analytics.math.surface.InterpolatedDoublesSurface;

/**
 * Tests {@link NormalVolatilityExpSimpleMoneynessIborFutureProvider}
 */
@Test
public class NormalVolatilityExpSimpleMoneynessIborFutureProviderTest {

  private static final Interpolator1D LINEAR_FLAT =
      CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR,
          Interpolator1DFactory.FLAT_EXTRAPOLATOR, Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final double[] TIMES =
      new double[] {0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00};
  private static final double[] MONEYNESS_PRICES =
      new double[] {-0.02, -0.02, -0.02, -0.01, -0.01, -0.01, 0.00, 0.00, 0.00, 0.01, 0.01, 0.01};
  private static final double[] MONEYNESS_RATES =
      new double[] {0.02, 0.02, 0.02, 0.01, 0.01, 0.01, 0.00, 0.00, 0.00, -0.01, -0.01, -0.01};
  private static final double[] NORMAL_VOL =
      new double[] {0.01, 0.011, 0.012, 0.011, 0.012, 0.013, 0.012, 0.013, 0.014, 0.010, 0.012, 0.014};
  private static final InterpolatedDoublesSurface PARAMETERS_PRICE =
      new InterpolatedDoublesSurface(TIMES, MONEYNESS_PRICES, NORMAL_VOL, INTERPOLATOR_2D);
  private static final InterpolatedDoublesSurface PARAMETERS_RATE =
      new InterpolatedDoublesSurface(TIMES, MONEYNESS_RATES, NORMAL_VOL, INTERPOLATOR_2D);

  private static final LocalDate VALUATION_DATE = date(2015, 2, 17);
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(LONDON_ZONE);

  private static final NormalVolatilityExpSimpleMoneynessIborFutureProvider VOL_SIMPLE_MONEY_PRICE =
      NormalVolatilityExpSimpleMoneynessIborFutureProvider.of(
          PARAMETERS_PRICE, true, EUR_EURIBOR_3M, ACT_365F, VALUATION_DATE_TIME);

  private static final NormalVolatilityExpSimpleMoneynessIborFutureProvider VOL_SIMPLE_MONEY_RATE =
      NormalVolatilityExpSimpleMoneynessIborFutureProvider.of(
          PARAMETERS_RATE, false, EUR_EURIBOR_3M, ACT_365F, VALUATION_DATE_TIME);

  private static final LocalDate[] TEST_EXPIRY =
      new LocalDate[] {date(2015, 2, 17), date(2015, 5, 17), date(2015, 6, 17), date(2017, 2, 17)};
  private static final int NB_TEST = TEST_EXPIRY.length;
  private static final LocalDate[] TEST_FIXING =
      new LocalDate[] {date(2015, 2, 17), date(2015, 5, 17), date(2015, 5, 17), date(2015, 5, 17)};
  private static final double[] TEST_STRIKE_PRICE = new double[] {0.985, 0.985, 0.985, 0.985};
  private static final double[] TEST_FUTURE_PRICE = new double[] {0.98, 0.985, 1.00, 1.01};

  private static final double TOLERANCE_VOL = 1.0E-10;

  //-------------------------------------------------------------------------
  public void test_valuationDate() {
    assertEquals(VOL_SIMPLE_MONEY_PRICE.getValuationDateTime(), VALUATION_DATE_TIME);
  }

  public void test_futureIndex() {
    assertEquals(VOL_SIMPLE_MONEY_PRICE.getFutureIndex(), EUR_EURIBOR_3M);
  }

  public void volatility_price() {
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = VOL_SIMPLE_MONEY_RATE.relativeTime(TEST_EXPIRY[i], LocalTime.MIDNIGHT, LONDON_ZONE);
      double volExpected = PARAMETERS_PRICE.getZValue(expiryTime, TEST_STRIKE_PRICE[i] - TEST_FUTURE_PRICE[i]);
      double volComputed = VOL_SIMPLE_MONEY_PRICE.getVolatility(TEST_EXPIRY[i], TEST_FIXING[i],
          TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
      assertEquals(volComputed, volExpected, TOLERANCE_VOL);
    }
  }

  public void volatility_rate() {
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = VOL_SIMPLE_MONEY_RATE.relativeTime(TEST_EXPIRY[i], LocalTime.MIDNIGHT, LONDON_ZONE);
      double volExpected = PARAMETERS_RATE.getZValue(expiryTime, TEST_FUTURE_PRICE[i] - TEST_STRIKE_PRICE[i]);
      double volComputed = VOL_SIMPLE_MONEY_RATE.getVolatility(TEST_EXPIRY[i], TEST_FIXING[i],
          TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
      assertEquals(volComputed, volExpected, TOLERANCE_VOL);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    NormalVolatilityExpSimpleMoneynessIborFutureProvider test =
        NormalVolatilityExpSimpleMoneynessIborFutureProvider.of(
            PARAMETERS_RATE, false, EUR_EURIBOR_3M, ACT_365F, VALUATION_DATE_TIME);
    coverImmutableBean(test);

    NormalVolatilityExpSimpleMoneynessIborFutureProvider test2 =
        NormalVolatilityExpSimpleMoneynessIborFutureProvider.of(
            PARAMETERS_RATE, true, EUR_EURIBOR_6M, ACT_360, VALUATION_DATE_TIME.plusDays(1));
    coverBeanEquals(test, test2);
  }

}
