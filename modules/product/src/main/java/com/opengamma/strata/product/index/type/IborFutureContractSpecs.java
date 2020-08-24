/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard Ibor future conventions.
 */
public final class IborFutureContractSpecs {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<IborFutureContractSpec> ENUM_LOOKUP = ExtendedEnum.of(IborFutureContractSpec.class);

  //-------------------------------------------------------------------------
  /**
   * The 'GBP-LIBOR-3M-IMM-ICE' contract.
   * <p>
   * The ICE "L" contract based on quarterly IMM dates, also known as "FSS".
   */
  public static final IborFutureContractSpec GBP_LIBOR_3M_IMM_ICE =
      IborFutureContractSpec.of(StandardIborFutureContractSpecs.GBP_LIBOR_3M_IMM_ICE.getName());

  /**
   * The 'GBP-LIBOR-3M-IMM-ICE-SERIAL' contract.
   * <p>
   * The ICE "L" contract based on serial monthly IMM dates, also known as "FSS".
   * <p>
   * This exists solely to allow code to refer to the nth monthly contract for the initial serial months.
   * In most cases, {@link #GBP_LIBOR_3M_IMM_ICE} should be used with a specific year-month.
   */
  public static final IborFutureContractSpec GBP_LIBOR_3M_IMM_ICE_SERIAL =
      IborFutureContractSpec.of(StandardIborFutureContractSpecs.GBP_LIBOR_3M_IMM_ICE_SERIAL.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'EUR-EURIBOR-3M-IMM-ICE' contract.
   * <p>
   * The ICE "I" contract based on quarterly IMM dates, also known as "FEI".
   */
  public static final IborFutureContractSpec EUR_EURIBOR_3M_IMM_ICE =
      IborFutureContractSpec.of(StandardIborFutureContractSpecs.EUR_EURIBOR_3M_IMM_ICE.getName());

  /**
   * The 'EUR-EURIBOR-3M-IMM-ICE-SERIAL' contract.
   * <p>
   * The ICE "I" contract based on serial monthly IMM dates, also known as "FEI".
   * <p>
   * This exists solely to allow code to refer to the nth monthly contract for the initial serial months.
   * In most cases, {@link #EUR_EURIBOR_3M_IMM_ICE} should be used with a specific year-month.
   */
  public static final IborFutureContractSpec EUR_EURIBOR_3M_IMM_ICE_SERIAL =
      IborFutureContractSpec.of(StandardIborFutureContractSpecs.EUR_EURIBOR_3M_IMM_ICE_SERIAL.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'USD-LIBOR-3M-IMM-CME' contract.
   * <p>
   * The CME "ED" contract based on quarterly IMM dates.
   */
  public static final IborFutureContractSpec USD_LIBOR_3M_IMM_CME =
      IborFutureContractSpec.of(StandardIborFutureContractSpecs.USD_LIBOR_3M_IMM_CME.getName());

  /**
   * The 'USD-LIBOR-3M-IMM-CME-SERIAL' contract.
   * <p>
   * The CME "ED" contract based on serial monthly IMM dates.
   * <p>
   * This exists solely to allow code to refer to the nth monthly contract for the initial serial months.
   * In most cases, {@link #USD_LIBOR_3M_IMM_CME} should be used with a specific year-month.
   */
  public static final IborFutureContractSpec USD_LIBOR_3M_IMM_CME_SERIAL =
      IborFutureContractSpec.of(StandardIborFutureContractSpecs.USD_LIBOR_3M_IMM_CME_SERIAL.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private IborFutureContractSpecs() {
  }

}
