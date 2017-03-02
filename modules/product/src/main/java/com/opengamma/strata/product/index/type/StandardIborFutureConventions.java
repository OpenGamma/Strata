/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import static com.opengamma.strata.basics.date.DateSequences.MONTHLY_IMM;
import static com.opengamma.strata.basics.date.DateSequences.QUARTERLY_IMM;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;

/**
 * Market standard Fixed-Ibor swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
final class StandardIborFutureConventions {

  /**
   * The 'GBP-LIBOR-3M-Quarterly-IMM' convention.
   * <p>
   * The 'GBP-LIBOR-3M' index based on quarterly IMM dates.
   */
  public static final IborFutureConvention GBP_LIBOR_3M_QUARTERLY_IMM =
      ImmutableIborFutureConvention.of(GBP_LIBOR_3M, QUARTERLY_IMM);

  /**
   * The 'GBP-LIBOR-3M-Monthly-IMM' convention.
   * <p>
   * The 'GBP-LIBOR-3M' index based on monthly IMM dates.
   */
  public static final IborFutureConvention GBP_LIBOR_3M_MONTHLY_IMM =
      ImmutableIborFutureConvention.of(GBP_LIBOR_3M, MONTHLY_IMM);

  //-------------------------------------------------------------------------
  /**
   * The 'EUR-EURIBOR-3M-Quarterly-IMM' convention.
   * <p>
   * The 'EUR-EURIBOR-3M' index based on quarterly IMM dates.
   */
  public static final IborFutureConvention EUR_EURIBOR_3M_QUARTERLY_IMM =
      ImmutableIborFutureConvention.of(EUR_EURIBOR_3M, QUARTERLY_IMM);

  /**
   * The 'EUR-EURIBOR-3M-Monthly-IMM' convention.
   * <p>
   * The 'EUR-EURIBOR-3M' index based on monthly IMM dates.
   */
  public static final IborFutureConvention EUR_EURIBOR_3M_MONTHLY_IMM =
      ImmutableIborFutureConvention.of(EUR_EURIBOR_3M, MONTHLY_IMM);

  //-------------------------------------------------------------------------
  /**
   * The 'USD-LIBOR-3M-Quarterly-IMM' convention.
   * <p>
   * The 'USD-LIBOR-3M' index based on quarterly IMM dates.
   */
  public static final IborFutureConvention USD_LIBOR_3M_QUARTERLY_IMM =
      ImmutableIborFutureConvention.of(USD_LIBOR_3M, QUARTERLY_IMM);

  /**
   * The 'USD-LIBOR-3M-Monthly-IMM' convention.
   * <p>
   * The 'USD-LIBOR-3M' index based on monthly IMM dates.
   */
  public static final IborFutureConvention USD_LIBOR_3M_MONTHLY_IMM =
      ImmutableIborFutureConvention.of(USD_LIBOR_3M, MONTHLY_IMM);

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardIborFutureConventions() {
  }

}
