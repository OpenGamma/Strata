/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.index.IborIndices;

/**
 * Market standard three leg basis swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
final class StandardThreeLegBasisSwapConventions {

  /**
   * EUR three leg basis swap of fixed, Euribor 3M and Euribor 6M.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final ThreeLegBasisSwapConvention EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M =
      ImmutableThreeLegBasisSwapConvention.of(
          "EUR-FIXED-1Y-EURIBOR-3M-EURIBOR-6M",
          FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          IborRateSwapLegConvention.of(IborIndices.EUR_EURIBOR_3M),
          IborRateSwapLegConvention.of(IborIndices.EUR_EURIBOR_6M));

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardThreeLegBasisSwapConventions() {
  }

}
