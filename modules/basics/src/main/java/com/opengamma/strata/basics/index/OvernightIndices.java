/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard Overnight rate indices.
 * <p>
 * Each constant returns a standard definition of the specified index.
 */
public final class OvernightIndices {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<OvernightIndex> ENUM_LOOKUP = ExtendedEnum.of(OvernightIndex.class);

  //-------------------------------------------------------------------------
  /**
   * The SONIA index for GBP.
   * <p>
   * SONIA is an "Overnight" index.
   */
  public static final OvernightIndex GBP_SONIA = OvernightIndex.of("GBP-SONIA");
  /**
   * The TOIS index for CHF.
   * <p>
   * TOIS is a "Tomorrow/Next" index.
   */
  public static final OvernightIndex CHF_TOIS = OvernightIndex.of("CHF-TOIS");
  /**
   * The EONIA index for EUR.
   * <p>
   * EONIA is an "Overnight" index.
   */
  public static final OvernightIndex EUR_EONIA = OvernightIndex.of("EUR-EONIA");
  /**
   * The TONAR index for JPY.
   * <p>
   * TONAR is an "Overnight" index.
   */
  public static final OvernightIndex JPY_TONAR = OvernightIndex.of("JPY-TONAR");
  /**
   * The Fed Fund index for USD.
   * <p>
   * Fed Fund is an "Overnight" index.
   */
  public static final OvernightIndex USD_FED_FUND = OvernightIndex.of("USD-FED-FUND");
  /**
   * The AONIA index for AUD.
   * <p>
   * AONIA is an "Overnight" index.
   */
  public static final OvernightIndex AUD_AONIA = OvernightIndex.of("AUD-AONIA");
  /**
   * The CDI index for BRL.
   * <p>
   * The "Brazil Certificates of Interbank Deposit" index.
   */
  public static final OvernightIndex BRL_CDI = OvernightIndex.of("BRL-CDI");
  /**
   * The CORRA index for CAD.
   * <p>
   * The "Canadian Overnight Repo Rate Average" index.
   */
  public static final OvernightIndex CAD_CORRA = OvernightIndex.of("CAD-CORRA");
  /**
   * The TN index for DKK.
   * <p>
   * The "Tomorrow/Next-renten" index.
   */
  public static final OvernightIndex DKK_TNR = OvernightIndex.of("DKK-TNR");
  /**
   * The NOWA index for NOK.
   * <p>
   * The "Norwegian Overnight Weighted Average" index.
   */
  public static final OvernightIndex NOK_NOWA = OvernightIndex.of("NOK-NOWA");
  /**
   * The PLONIA index for PLN.
   * <p>
   * The "Polish Overnight" index.
   */
  public static final OvernightIndex PLN_POLONIA = OvernightIndex.of("PLN-POLONIA");
  /**
   * The SIOR index for SEK.
   * <p>
   * The "STIBOR T/N" index.
   */
  public static final OvernightIndex SEK_SIOR = OvernightIndex.of("SEK-SIOR");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private OvernightIndices() {
  }

}
