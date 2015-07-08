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
  public static final FixedIborSwapConvention USD_FIXED_6M_LIBOR_3M = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.USD,
          DayCounts.THIRTY_U_360,
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
  public static FixedIborSwapConvention USD_FIXED_1Y_LIBOR_3M = FixedIborSwapConvention.of(
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
  public static FixedIborSwapConvention EUR_FIXED_1Y_EURIBOR_3M = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.EUR,
          DayCounts.THIRTY_U_360,
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
  public static FixedIborSwapConvention EUR_FIXED_1Y_EURIBOR_6M = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.EUR,
          DayCounts.THIRTY_U_360,
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
  public static FixedIborSwapConvention GBP_FIXED_1Y_LIBOR_3M = FixedIborSwapConvention.of(
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
  public static FixedIborSwapConvention GBP_FIXED_6M_LIBOR_6M = FixedIborSwapConvention.of(
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
  public static FixedIborSwapConvention JPY_FIXED_6M_TIBORJ_3M = FixedIborSwapConvention.of(
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
  public static FixedIborSwapConvention JPY_FIXED_6M_LIBOR_6M = FixedIborSwapConvention.of(
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
  public static FixedIborSwapConvention CHF_FIXED_1Y_LIBOR_3M = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.CHF,
          DayCounts.THIRTY_U_360,
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
  public static FixedIborSwapConvention CHF_FIXED_1Y_LIBOR_6M = FixedIborSwapConvention.of(
      FixedRateSwapLegConvention.of(
          Currency.CHF,
          DayCounts.THIRTY_U_360,
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
