/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard price indices.
 * <p>
 * Each constant returns a standard definition of the specified index.
 */
public final class PriceIndices {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<PriceIndex> ENUM_LOOKUP = ExtendedEnum.of(PriceIndex.class);

  /**
   * The harmonized consumer price index for the United Kingdom,
   * "Non-revised Harmonised Index of Consumer Prices".
   */
  public static final PriceIndex UK_HICP = PriceIndex.of("UK-HICP");
  /**
   * The retail price index for the United Kingdom,
   * "Non-revised Retail Price Index All Items in the United Kingdom".
   */
  public static final PriceIndex UK_RPI = PriceIndex.of("UK-RPI");
  /**
   * The retail price index for the United Kingdom excluding mortgage interest payments,
   * "Non-revised Retail Price Index Excluding Mortgage Interest Payments in the United Kingdom".
   */
  public static final PriceIndex UK_RPIX = PriceIndex.of("UK-RPIX");
  /**
   * The consumer price index for Switzerland,
   * "Non-revised Consumer Price Index".
   */
  public static final PriceIndex SWF_CPI = PriceIndex.of("SWF-CPI");
  /**
   * The consumer price index for Europe,
   * "Non-revised Harmonised Index of Consumer Prices All Items".
   */
  public static final PriceIndex EUR_AI_CPI = PriceIndex.of("EUR-AI-CPI");
  /**
   * The consumer price index for Japan excluding fresh food,
   * "Non-revised Consumer Price Index Nationwide General Excluding Fresh Food".
   */
  public static final PriceIndex JPY_CPI_EXF = PriceIndex.of("JPY-CPI-EXF");
  /**
   * The consumer price index for US Urban consumers,
   * "Non-revised index of Consumer Prices for All Urban Consumers (CPI-U) before seasonal adjustment".
   */
  public static final PriceIndex USA_CPI_U = PriceIndex.of("USA-CPI-U");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private PriceIndices() {
  }

}
