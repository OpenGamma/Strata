/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swap.type;


import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.finance.rate.swap.CompoundingMethod;

public class IborIborSwapConventions {
  
  /**
   * USD standard LIBOR 3M vs LIBOR 6M swap.
   * <p>
   * The LIBOR 3M leg pays semi-annually with FLAT compounding method.
   */
  public static final IborIborSwapConvention USD_LIBOR_3M_LIBOR_6M =
      IborIborSwapConvention.of(
          IborRateSwapLegConvention.builder()
          .index(IborIndices.USD_LIBOR_3M)
          .paymentFrequency(Frequency.P6M)
          .compoundingMethod(CompoundingMethod.FLAT).build(),
          IborRateSwapLegConvention.of(IborIndices.USD_LIBOR_6M));
  
  /**
   * USD standard LIBOR 1M vs LIBOR 3M swap.
   * <p>
   * The LIBOR 1M leg pays quarterly with FLAT compounding method.
   */
  public static final IborIborSwapConvention USD_LIBOR_1M_LIBOR_3M =
      IborIborSwapConvention.of(
          IborRateSwapLegConvention.builder()
          .index(IborIndices.USD_LIBOR_1M)
          .paymentFrequency(Frequency.P3M)
          .compoundingMethod(CompoundingMethod.FLAT).build(),
          IborRateSwapLegConvention.of(IborIndices.USD_LIBOR_3M));

}
