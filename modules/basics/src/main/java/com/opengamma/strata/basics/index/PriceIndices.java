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
  public static final PriceIndex GB_HICP = PriceIndex.of("GB-HICP");
  /**
   * The retail price index for the United Kingdom,
   * "Non-revised Retail Price Index All Items in the United Kingdom".
   */
  public static final PriceIndex GB_RPI = PriceIndex.of("GB-RPI");
  /**
   * The retail price index for the United Kingdom excluding mortgage interest payments,
   * "Non-revised Retail Price Index Excluding Mortgage Interest Payments in the United Kingdom".
   */
  public static final PriceIndex GB_RPIX = PriceIndex.of("GB-RPIX");
  /**
   * The consumer price index for Switzerland,
   * "Non-revised Consumer Price Index".
   */
  public static final PriceIndex CH_CPI = PriceIndex.of("CH-CPI");
  /**
   * The consumer price index for Europe,
   * "Non-revised Harmonised Index of Consumer Prices All Items".
   */
  public static final PriceIndex EU_AI_CPI = PriceIndex.of("EU-AI-CPI");
  /**
   * The consumer price index for Europe,
   * "Non-revised Harmonised Index of Consumer Prices Excluding Tobacco".
   */
  public static final PriceIndex EU_EXT_CPI = PriceIndex.of("EU-EXT-CPI");
  /**
   * The consumer price index for Japan excluding fresh food,
   * "Non-revised Consumer Price Index Nationwide General Excluding Fresh Food".
   */
  public static final PriceIndex JP_CPI_EXF = PriceIndex.of("JP-CPI-EXF");
  /**
   * The consumer price index for US Urban consumers,
   * "Non-revised index of Consumer Prices for All Urban Consumers (CPI-U) before seasonal adjustment".
   */
  public static final PriceIndex US_CPI_U = PriceIndex.of("US-CPI-U");
  /**
   * The consumer price index for France,
   * "Non-revised Harmonised Index of Consumer Prices Excluding Tobacco".
   */
  public static final PriceIndex FR_EXT_CPI = PriceIndex.of("FR-EXT-CPI");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private PriceIndices() {
  }

}
