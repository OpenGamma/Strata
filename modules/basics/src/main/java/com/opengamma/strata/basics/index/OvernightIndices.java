/*
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
 * <p>
 * If a floating rate has a constant here, then it is fully supported by Strata
 * with example holiday calendar data.
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
   * The Sterling Overnight Index Average (SONIA) index.
   */
  public static final OvernightIndex GBP_SONIA = OvernightIndex.of("GBP-SONIA");
  /**
   * The SARON index for CHF.
   * <p>
   * The Swiss Average Overnight Rate (SARON) index.
   */
  public static final OvernightIndex CHF_SARON = OvernightIndex.of("CHF-SARON");
  /**
   * The TOIS index for CHF.
   * <p>
   * The Tomorrow/Next Overnight Indexed Swaps (TOIS) index, which is a "Tomorrow/Next" index.
   */
  public static final OvernightIndex CHF_TOIS = OvernightIndex.of("CHF-TOIS");
  /**
   * The EONIA index for EUR.
   * <p>
   * The Euro OverNight Index Average (EONIA) index.
   */
  public static final OvernightIndex EUR_EONIA = OvernightIndex.of("EUR-EONIA");
  /**
   * The TONAR index for JPY.
   * <p>
   * The Tokyo Overnight Average Rate (TONAR) index.
   */
  public static final OvernightIndex JPY_TONAR = OvernightIndex.of("JPY-TONAR");
  /**
   * The Fed Fund index for USD.
   * <p>
   * The Federal Funds Rate index.
   */
  public static final OvernightIndex USD_FED_FUND = OvernightIndex.of("USD-FED-FUND");
  /**
   * The SOFR index for USD.
   * <p>
   * The Secured Overnight Financing Rate (SOFR) index.
   */
  public static final OvernightIndex USD_SOFR = OvernightIndex.of("USD-SOFR");
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
   * The NZIONA index for NZD.
   * <p>
   * The "New Zealand Overnight" index.
   */
  public static final OvernightIndex NZD_NZIONA = OvernightIndex.of("NZD-NZIONA");
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
  /**
   * The SABOR index for ZAR.
   * <p>
   * The "South African Benchmark Overnight Rate" index.
   */
  public static final OvernightIndex ZAR_SABOR = OvernightIndex.of("ZAR-SABOR");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private OvernightIndices() {
  }

}
