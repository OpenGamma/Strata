/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import static com.opengamma.strata.basics.date.DateSequences.MONTHLY_IMM;
import static com.opengamma.strata.basics.date.DateSequences.QUARTERLY_IMM;
import static com.opengamma.strata.basics.date.DateSequences.QUARTERLY_IMM_3_SERIAL;
import static com.opengamma.strata.basics.date.DateSequences.QUARTERLY_IMM_6_SERIAL;
import static com.opengamma.strata.basics.index.IborIndices.CHF_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;

/**
 * Commonly traded Ibor future contract specifications.
 */
final class StandardIborFutureContractSpecs {

  /**
   * The 'GBP-LIBOR-3M-IMM-ICE' contract.
   * <p>
   * The ICE "L" contract based on quarterly IMM dates.
   */
  public static final IborFutureContractSpec GBP_LIBOR_3M_IMM_ICE =
      ImmutableIborFutureContractSpec.builder()
          .name("GBP-LIBOR-3M-IMM-ICE")
          .index(GBP_LIBOR_3M)
          .dateSequence(QUARTERLY_IMM_3_SERIAL)
          .notional(500_000d)
          .build();

  //-------------------------------------------------------------------------
  /**
   * The 'CHF-LIBOR-3M-IMM-ICE' contract.
   * <p>
   * The ICE "S" contract based on quarterly IMM dates, also known as "FES".
   */
  public static final IborFutureContractSpec CHF_LIBOR_3M_IMM_ICE =
      ImmutableIborFutureContractSpec.builder()
          .name("CHF-LIBOR-3M-IMM-ICE")
          .index(CHF_LIBOR_3M)
          .dateSequence(QUARTERLY_IMM)
          .notional(1_000_000d)
          .build();

  //-------------------------------------------------------------------------
  /**
   * The 'EUR-EURIBOR-3M-IMM-ICE' contract.
   * <p>
   * The ICE "I" contract based on quarterly IMM dates, also known as "FEI".
   */
  public static final IborFutureContractSpec EUR_EURIBOR_3M_IMM_ICE =
      ImmutableIborFutureContractSpec.builder()
          .name("EUR-EURIBOR-3M-IMM-ICE")
          .index(EUR_EURIBOR_3M)
          .dateSequence(QUARTERLY_IMM_6_SERIAL)
          .notional(1_000_000d)
          .build();

  //-------------------------------------------------------------------------
  /**
   * The 'EUR-EURIBOR-3M-IMM-EUREX' contract.
   * <p>
   * The EUREX "FEU3" contract based on quarterly IMM dates, also known as "FEU3".
   * https://www.eurex.com/ex-en/markets/int/mon/packs-bundles/euribor/Three-Month-EURIBOR-Futures-137458
   */
  public static final IborFutureContractSpec EUR_EURIBOR_3M_IMM_EUREX =
      ImmutableIborFutureContractSpec.builder()
          .name("EUR-EURIBOR-3M-IMM-ICE")
          .index(EUR_EURIBOR_3M)
          .dateSequence(QUARTERLY_IMM_6_SERIAL)
          .notional(1_000_000d)
          .build();
  //-------------------------------------------------------------------------
  /**
   * The 'USD-LIBOR-1M-IMM-CME' contract.
   * <p>
   * The CME "GLB" contract based on monthly IMM dates.
   * https://www.cmegroup.com/markets/interest-rates/stirs/1-month-libor.contractSpecs.html
   *
   */
  public static final IborFutureContractSpec USD_LIBOR_1M_IMM_CME =
      ImmutableIborFutureContractSpec.builder()
          .name("USD-LIBOR-1M-IMM-CME")
          .index(USD_LIBOR_1M)
          .dateSequence(MONTHLY_IMM)
          .notional(1_000_000d)
          .build();

  //-------------------------------------------------------------------------
  /**
   * The 'USD-LIBOR-3M-IMM-CME' contract.
   * <p>
   * The CME "ED" contract based on quarterly IMM dates.
   * https://www.cmegroup.com/markets/interest-rates/stirs/eurodollar.contractSpecs.html
   */
  public static final IborFutureContractSpec USD_LIBOR_3M_IMM_CME =
      ImmutableIborFutureContractSpec.builder()
          .name("USD-LIBOR-3M-IMM-CME")
          .index(USD_LIBOR_3M)
          .dateSequence(QUARTERLY_IMM_6_SERIAL)
          .notional(1_000_000d)
          .build();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardIborFutureContractSpecs() {
  }

}
