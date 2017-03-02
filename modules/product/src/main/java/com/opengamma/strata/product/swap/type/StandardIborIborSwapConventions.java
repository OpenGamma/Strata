/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.swap.CompoundingMethod;

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
   * JPY standard LIBOR 1M vs LIBOR 6M swap.
   * The LIBOR 1M leg pays monthly, the LIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_LIBOR_1M_LIBOR_6M =
      ImmutableIborIborSwapConvention.of(
          "JPY-LIBOR-1M-LIBOR-6M",
          IborRateSwapLegConvention.of(IborIndices.JPY_LIBOR_1M),
          IborRateSwapLegConvention.of(IborIndices.JPY_LIBOR_6M));

  /**
   * JPY standard LIBOR 3M vs LIBOR 6M swap.
   * The LIBOR 3M leg pays quarterly, the LIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_LIBOR_3M_LIBOR_6M =
      ImmutableIborIborSwapConvention.of(
          "JPY-LIBOR-3M-LIBOR-6M",
          IborRateSwapLegConvention.of(IborIndices.JPY_LIBOR_3M),
          IborRateSwapLegConvention.of(IborIndices.JPY_LIBOR_6M));

  //-------------------------------------------------------------------------
  /**
   * JPY standard LIBOR 6M vs TIBOR JAPAN 6M swap.
   * The two legs pay semi-annually.
   */
  public static final IborIborSwapConvention JPY_LIBOR_6M_TIBOR_JAPAN_6M =
      ImmutableIborIborSwapConvention.of(
          "JPY-LIBOR-6M-TIBOR-JAPAN-6M",
          IborRateSwapLegConvention.of(IborIndices.JPY_LIBOR_6M),
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_JAPAN_6M));

  /**
   * JPY standard LIBOR 6M vs TIBOR EUROYEN 6M swap.
   * The two legs pay semi-annually.
   */
  public static final IborIborSwapConvention JPY_LIBOR_6M_TIBOR_EUROYEN_6M =
      ImmutableIborIborSwapConvention.of(
          "JPY-LIBOR-6M-TIBOR-EUROYEN-6M",
          IborRateSwapLegConvention.of(IborIndices.JPY_LIBOR_6M),
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_EUROYEN_6M));

  //-------------------------------------------------------------------------
  /**
   * JPY standard TIBOR JAPAN 1M vs TIBOR JAPAN 6M swap.
   * The TIBOR 1M leg pays monthly, the TIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M =
      ImmutableIborIborSwapConvention.of(
          "JPY-TIBOR-JAPAN-1M-TIBOR-JAPAN-6M",
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_JAPAN_1M),
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_JAPAN_6M));

  /**
   * JPY standard TIBOR JAPAN 3M vs TIBOR JAPAN 6M swap.
   * The TIBOR 3M leg pays quarterly, the TIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M =
      ImmutableIborIborSwapConvention.of(
          "JPY-TIBOR-JAPAN-3M-TIBOR-JAPAN-6M",
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_JAPAN_3M),
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_JAPAN_6M));

  //-------------------------------------------------------------------------
  /**
   * JPY standard TIBOR EUROYEN 1M vs TIBOR EUROYEN 6M swap.
   * The TIBOR 1M leg pays monthly, the TIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M =
      ImmutableIborIborSwapConvention.of(
          "JPY-TIBOR-EUROYEN-1M-TIBOR-EUROYEN-6M",
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_EUROYEN_1M),
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_EUROYEN_6M));

  /**
   * JPY standard TIBOR EUROYEN 3M vs TIBOR EUROYEN 6M swap.
   * The TIBOR 3M leg pays quarterly, the TIBOR 6M leg pays semi-annually.
   */
  public static final IborIborSwapConvention JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M =
      ImmutableIborIborSwapConvention.of(
          "JPY-TIBOR-EUROYEN-3M-TIBOR-EUROYEN-6M",
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_EUROYEN_3M),
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_EUROYEN_6M));

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardIborIborSwapConventions() {
  }

}
