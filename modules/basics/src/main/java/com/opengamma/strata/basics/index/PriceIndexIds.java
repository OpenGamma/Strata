/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.basics.market.ReferenceData;

/**
 * Identifiers for common Price indices.
 * <p>
 * The constants defined here are identifiers, used to locate instances of
 * {@link PriceIndex} from {@link ReferenceData}.
 */
public final class PriceIndexIds {

  /**
   * An identifier for the harmonized consumer price index for the United Kingdom,
   * "Non-revised Harmonised Index of Consumer Prices".
   */
  public static final PriceIndexId GB_HICP = PriceIndexId.of("GB-HICP");
  /**
   * An identifier for the retail price index for the United Kingdom,
   * "Non-revised Retail Price Index All Items in the United Kingdom".
   */
  public static final PriceIndexId GB_RPI = PriceIndexId.of("GB-RPI");
  /**
   * An identifier for the retail price index for the United Kingdom excluding mortgage interest payments,
   * "Non-revised Retail Price Index Excluding Mortgage Interest Payments in the United Kingdom".
   */
  public static final PriceIndexId GB_RPIX = PriceIndexId.of("GB-RPIX");
  /**
   * An identifier for the consumer price index for Switzerland,
   * "Non-revised Consumer Price Index".
   */
  public static final PriceIndexId CH_CPI = PriceIndexId.of("CH-CPI");
  /**
   * An identifier for the consumer price index for Europe,
   * "Non-revised Harmonised Index of Consumer Prices All Items".
   */
  public static final PriceIndexId EU_AI_CPI = PriceIndexId.of("EU-AI-CPI");
  /**
   * An identifier for the consumer price index for Europe,
   * "Non-revised Harmonised Index of Consumer Prices Excluding Tobacco".
   */
  public static final PriceIndexId EU_EXT_CPI = PriceIndexId.of("EU-EXT-CPI");
  /**
   * An identifier for the consumer price index for Japan excluding fresh food,
   * "Non-revised Consumer Price Index Nationwide General Excluding Fresh Food".
   */
  public static final PriceIndexId JP_CPI_EXF = PriceIndexId.of("JP-CPI-EXF");
  /**
   * An identifier for the consumer price index for US Urban consumers,
   * "Non-revised index of Consumer Prices for All Urban Consumers (CPI-U) before seasonal adjustment".
   */
  public static final PriceIndexId US_CPI_U = PriceIndexId.of("US-CPI-U");
  /**
   * An identifier for the consumer price index for France,
   * "Non-revised Harmonised Index of Consumer Prices Excluding Tobacco".
   */
  public static final PriceIndexId FR_EXT_CPI = PriceIndexId.of("FR-EXT-CPI");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private PriceIndexIds() {
  }

}
