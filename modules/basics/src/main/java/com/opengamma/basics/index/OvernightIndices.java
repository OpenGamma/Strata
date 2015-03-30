/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

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
  public static final OvernightIndex GBP_SONIA = OvernightIndex.of(StandardOvernightIndices.GBP_SONIA.getName());
  /**
   * The TOIS index for CHF.
   * <p>
   * TOIS is a "Tomorrow/Next" index.
   */
  public static final OvernightIndex CHF_TOIS = OvernightIndex.of(StandardOvernightIndices.CHF_TOIS.getName());
  /**
   * The EONIA index for EUR.
   * <p>
   * EONIA is an "Overnight" index.
   */
  public static final OvernightIndex EUR_EONIA = OvernightIndex.of(StandardOvernightIndices.EUR_EONIA.getName());
  /**
   * The TONAR index for JPY.
   * <p>
   * TONAR is an "Overnight" index.
   */
  public static final OvernightIndex JPY_TONAR = OvernightIndex.of(StandardOvernightIndices.JPY_TONAR.getName());
  /**
   * The FED_FUND index for USD.
   * <p>
   * FED_FUND is an "Overnight" index.
   */
  public static final OvernightIndex USD_FED_FUND = OvernightIndex.of(StandardOvernightIndices.USD_FED_FUND.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private OvernightIndices() {
  }

}
