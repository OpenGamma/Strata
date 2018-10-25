/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

public class Test {
  
  public static void main(String[] args) {
  
    ResolvedSwapTrade resolve = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M.toTrade(
        LocalDate.of(2017, 10, 13),
        LocalDate.of(2017, 12, 29),
        LocalDate.of(2023, 12, 31),
        BuySell.BUY,
        11300000d,
        0.0208d
    ).resolve(ReferenceData.standard());
  }
}
