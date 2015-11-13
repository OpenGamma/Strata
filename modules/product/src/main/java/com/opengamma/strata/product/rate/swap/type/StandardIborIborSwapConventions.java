/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swap.type;

import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.rate.swap.CompoundingMethod;

/**
 * Market standard Ibor-Ibor swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
final class StandardIborIborSwapConventions {

  /**
   * USD standard LIBOR 3M vs LIBOR 6M swap.
   * The LIBOR 3M leg pays semi-annually with 'Flat' compounding method.
   */
  public static final IborIborSwapConvention USD_LIBOR_3M_LIBOR_6M =
      ImmutableIborIborSwapConvention.of(
          "USD-LIBOR-3M-LIBOR-6M",
          IborRateSwapLegConvention.builder()
              .index(IborIndices.USD_LIBOR_3M)
              .paymentFrequency(Frequency.P6M)
              .compoundingMethod(CompoundingMethod.FLAT)
              .stubConvention(StubConvention.SHORT_INITIAL)
              .build(),
          IborRateSwapLegConvention.of(IborIndices.USD_LIBOR_6M));

  /**
   * USD standard LIBOR 1M vs LIBOR 3M swap.
   * The LIBOR 1M leg pays quarterly with 'Flat' compounding method.
   */
  public static final IborIborSwapConvention USD_LIBOR_1M_LIBOR_3M =
      ImmutableIborIborSwapConvention.of(
          "USD-LIBOR-1M-LIBOR-3M",
          IborRateSwapLegConvention.builder()
              .index(IborIndices.USD_LIBOR_1M)
              .paymentFrequency(Frequency.P3M)
              .compoundingMethod(CompoundingMethod.FLAT)
              .stubConvention(StubConvention.SHORT_INITIAL)
              .build(),
          IborRateSwapLegConvention.of(IborIndices.USD_LIBOR_3M));

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardIborIborSwapConventions() {
  }

}
