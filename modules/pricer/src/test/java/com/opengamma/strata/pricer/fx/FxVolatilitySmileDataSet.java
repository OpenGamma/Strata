package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;

import java.time.ZonedDateTime;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Sets of volatility data used in FX option tests.
 */
public class FxVolatilitySmileDataSet {

  private static final String NAME = "smileEurUsd";
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final DoubleArray DELTA = DoubleArray.of(0.10, 0.25);

  private static final DoubleArray TIME_5 =
      DoubleArray.of(0.25205479452054796, 0.5013698630136987, 1.0015120892282356, 2.0, 5.001512089228235);
  private static final DoubleArray ATM_5 = DoubleArray.of(0.185, 0.18, 0.17, 0.16, 0.16);
  private static final DoubleMatrix RISK_REVERSAL_5 = DoubleMatrix.ofUnsafe(
      new double[][] { {-0.011, -0.006 }, {-0.012, -0.007 }, {-0.013, -0.008 }, {-0.014, -0.009 }, {-0.014, -0.009 } });
  private static final DoubleMatrix STRANGLE_5 = DoubleMatrix.ofUnsafe(
      new double[][] { {0.0310, 0.0110 }, {0.0320, 0.0120 }, {0.0330, 0.0130 }, {0.0340, 0.0140 }, {0.0340, 0.0140 } });
  private static final InterpolatedSmileDeltaTermStructureStrikeInterpolation SMILE_TERM_5 =
      InterpolatedSmileDeltaTermStructureStrikeInterpolation.of(NAME, TIME_5, DELTA, ATM_5, RISK_REVERSAL_5, STRANGLE_5);

  private static final DoubleArray TIME_6 = DoubleArray.of(0.01, 0.252, 0.501, 1.0, 2.0, 5.0);
  private static final DoubleArray ATM_6 = DoubleArray.of(0.175, 0.185, 0.18, 0.17, 0.16, 0.16);
  private static final DoubleMatrix RISK_REVERSAL_6 = DoubleMatrix.ofUnsafe(new double[][] {
    {-0.010, -0.0050 }, {-0.011, -0.0060 }, {-0.012, -0.0070 },
    {-0.013, -0.0080 }, {-0.014, -0.0090 }, {-0.014, -0.0090 } });
  private static final DoubleMatrix STRANGLE_6 = DoubleMatrix.ofUnsafe(new double[][] {
    {0.0300, 0.0100 }, {0.0310, 0.0110 }, {0.0320, 0.0120 },
    {0.0330, 0.0130 }, {0.0340, 0.0140 }, {0.0340, 0.0140 } });
  private static final InterpolatedSmileDeltaTermStructureStrikeInterpolation SMILE_TERM_6 =
      InterpolatedSmileDeltaTermStructureStrikeInterpolation.of(NAME, TIME_6, DELTA, ATM_6, RISK_REVERSAL_6, STRANGLE_6);

  /**
   * Creates volatility provider with term structure of smile parameters. 
   * <p>
   * The number of time slices are 5, and the day count convention is ACT/ACT ISDA. 
   * 
   * @param dateTime  the valuation date time
   * @return  the volatility provider
   */
  public static BlackVolatilitySmileFxProvider createVolatilitySmileProvider5(ZonedDateTime dateTime) {
    return BlackVolatilitySmileFxProvider.of(SMILE_TERM_5, CURRENCY_PAIR, ACT_ACT_ISDA, dateTime);
  }

  /**
   * Creates volatility provider with term structure of smile parameters. 
   * <p>
   * The number of time slices are 6, and the day count convention is ACT/365F.
   * 
   * @param dateTime  the valuation date time
   * @return  the volatility provider
   */
  public static BlackVolatilitySmileFxProvider createVolatilitySmileProvider6(ZonedDateTime dateTime) {
    return BlackVolatilitySmileFxProvider.of(SMILE_TERM_6, CURRENCY_PAIR, ACT_365F, dateTime);
  }

  /**
   * Get the underlying smile term structure. 
   * <p>
   * The number of time slices are 5, and the day count convention is ACT/ACT ISDA. 
   * 
   * @return the smile term structure
   */
  public static InterpolatedSmileDeltaTermStructureStrikeInterpolation getSmileDeltaTermStructure5() {
    return SMILE_TERM_5;
  }

  /**
   * Get the underlying smile term structure. 
   * <p>
   * The number of time slices are 6, and the day count convention is ACT/365F.
   * 
   * @return the smile term structure
   */
  public static InterpolatedSmileDeltaTermStructureStrikeInterpolation getSmileDeltaTermStructure6() {
    return SMILE_TERM_6;
  }

}
