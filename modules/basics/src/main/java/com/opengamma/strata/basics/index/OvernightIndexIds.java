/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.basics.market.ReferenceData;

/**
 * Identifiers for common Overnight indices.
 * <p>
 * The constants defined here are identifiers, used to locate instances of
 * {@link OvernightIndex} from {@link ReferenceData}.
 */
public final class OvernightIndexIds {

  /**
   * An identifier for the SONIA index for GBP.
   * <p>
   * SONIA is an "Overnight" index.
   */
  public static final OvernightIndexId GBP_SONIA = OvernightIndexId.of("GBP-SONIA");
  /**
   * An identifier for the TOIS index for CHF.
   * <p>
   * TOIS is a "Tomorrow/Next" index.
   */
  public static final OvernightIndexId CHF_TOIS = OvernightIndexId.of("CHF-TOIS");
  /**
   * An identifier for the EONIA index for EUR.
   * <p>
   * EONIA is an "Overnight" index.
   */
  public static final OvernightIndexId EUR_EONIA = OvernightIndexId.of("EUR-EONIA");
  /**
   * An identifier for the TONAR index for JPY.
   * <p>
   * TONAR is an "Overnight" index.
   */
  public static final OvernightIndexId JPY_TONAR = OvernightIndexId.of("JPY-TONAR");
  /**
   * An identifier for the FED_FUND index for USD.
   * <p>
   * FED_FUND is an "Overnight" index.
   */
  public static final OvernightIndexId USD_FED_FUND = OvernightIndexId.of("USD-FED-FUND");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private OvernightIndexIds() {
  }

}
