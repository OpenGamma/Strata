/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swap.type;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Factory methods for market standard conventions
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
public class FixedIborSwapConventions {

  /**
   * USD(NY) vanilla fixed vs LIBOR 3M swap
   */
  public static final FixedIborSwapConvention VANILLA_USD_USNY = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.USD,
          DayCounts.THIRTY_360_ISDA,
          Frequency.P6M,
          BusinessDayAdjustment.of(
              BusinessDayConventions.MODIFIED_FOLLOWING,
              HolidayCalendars.GBLO.combineWith(HolidayCalendars.USNY)
          )
      ),
      IborRateSwapLegConvention.of(
          IborIndices.USD_LIBOR_3M
      )
  );

  /**
   * USD(London) vanilla fixed vs LIBOR 3M swap
   */
  public static FixedIborSwapConvention VANILLA_USD_GBLO = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.USD,
          DayCounts.ACT_360,
          Frequency.P12M,
          BusinessDayAdjustment.of(
              BusinessDayConventions.MODIFIED_FOLLOWING,
              HolidayCalendars.GBLO.combineWith(HolidayCalendars.USNY)
          )
      ),
      IborRateSwapLegConvention.of(
          IborIndices.USD_LIBOR_3M
      )
  );

  /**
   * EUR(1Y) vanilla fixed vs Euribor 3M swap
   */
  public static FixedIborSwapConvention VANILLA_EUR_3M = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.EUR,
          DayCounts.THIRTY_360_ISDA,
          Frequency.P12M,
          BusinessDayAdjustment.of(
              BusinessDayConventions.MODIFIED_FOLLOWING,
              HolidayCalendars.EUTA
          )
      ),
      IborRateSwapLegConvention.of(
          IborIndices.EUR_EURIBOR_3M
      )
  );

  /**
   * EUR(>1Y) vanilla fixed vs Euribor 6M swap
   */
  public static FixedIborSwapConvention VANILLA_EUR_6M = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.EUR,
          DayCounts.THIRTY_360_ISDA,
          Frequency.P12M,
          BusinessDayAdjustment.of(
              BusinessDayConventions.MODIFIED_FOLLOWING,
              HolidayCalendars.EUTA
          )
      ),
      IborRateSwapLegConvention.of(
          IborIndices.EUR_EURIBOR_6M
      )
  );

  /**
   * GBP(1Y) vanilla fixed vs LIBOR 3M swap
   */
  public static FixedIborSwapConvention VANILLA_GBP_3M = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.GBP,
          DayCounts.ACT_365F,
          Frequency.P12M,
          BusinessDayAdjustment.of(
              BusinessDayConventions.MODIFIED_FOLLOWING,
              HolidayCalendars.GBLO
          )
      ),
      IborRateSwapLegConvention.of(
          IborIndices.GBP_LIBOR_3M
      )
  );

  /**
   * GBP(>1Y) vanilla fixed vs LIBOR 6M swap
   */
  public static FixedIborSwapConvention VANILLA_GBP_6M = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.GBP,
          DayCounts.ACT_365F,
          Frequency.P6M,
          BusinessDayAdjustment.of(
              BusinessDayConventions.MODIFIED_FOLLOWING,
              HolidayCalendars.GBLO
          )
      ),
      IborRateSwapLegConvention.of(
          IborIndices.GBP_LIBOR_6M
      )
  );

  /**
   * JPY(Tibor) vanilla fixed vs Tibor 3M swap
   */
  public static FixedIborSwapConvention VANILLA_JPY_TIBOR = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.JPY,
          DayCounts.ACT_365F,
          Frequency.P6M,
          BusinessDayAdjustment.of(
              BusinessDayConventions.MODIFIED_FOLLOWING,
              HolidayCalendars.JPTO
          )
      ),
      IborRateSwapLegConvention.of(
          IborIndices.JPY_TIBOR_JAPAN_3M
      )
  );

  /**
   * JPY(LIBOR) vanilla fixed vs LIBOR 6M swap
   */
  public static FixedIborSwapConvention VANILLA_JPY_LIBOR = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.JPY,
          DayCounts.ACT_365F,
          Frequency.P6M,
          BusinessDayAdjustment.of(
              BusinessDayConventions.MODIFIED_FOLLOWING,
              HolidayCalendars.GBLO.combineWith(HolidayCalendars.JPTO)
          )
      ),
      IborRateSwapLegConvention.of(
          IborIndices.JPY_LIBOR_6M
      )
  );

  /**
   * CHF(1Y) vanilla fixed vs LIBOR 3M swap
   */
  public static FixedIborSwapConvention VANILLA_CHF_3M = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.CHF,
          DayCounts.THIRTY_360_ISDA,
          Frequency.P12M,
          BusinessDayAdjustment.of(
              BusinessDayConventions.MODIFIED_FOLLOWING,
              HolidayCalendars.GBLO.combineWith(HolidayCalendars.CHZU)
          )
      ),
      IborRateSwapLegConvention.of(
          IborIndices.CHF_LIBOR_3M
      )
  );

  /**
   * CHF(>1Y) vanilla fixed vs LIBOR 6M swap
   */
  public static FixedIborSwapConvention VANILLA_CHF_6M = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.CHF,
          DayCounts.THIRTY_360_ISDA,
          Frequency.P12M,
          BusinessDayAdjustment.of(
              BusinessDayConventions.MODIFIED_FOLLOWING,
              HolidayCalendars.GBLO.combineWith(HolidayCalendars.CHZU)
          )
      ),
      IborRateSwapLegConvention.of(
          IborIndices.CHF_LIBOR_6M
      )
  );

}
