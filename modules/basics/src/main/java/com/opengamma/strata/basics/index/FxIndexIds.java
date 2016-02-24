/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.basics.market.ReferenceData;

/**
 * Identifiers for common FX indices.
 * <p>
 * The constants defined here are identifiers, used to locate instances of
 * {@link FxIndex} from {@link ReferenceData}.
 */
public final class FxIndexIds {

  /**
   * An identifier for the FX index for conversion from EUR to CHF, as defined by
   * the European Central Bank "Euro foreign exchange reference rates".
   */
  public static final FxIndexId EUR_CHF_ECB = FxIndexId.of("EUR/CHF-ECB");
  /**
   * An identifier for the FX index for conversion from EUR to GBP, as defined by
   * the European Central Bank "Euro foreign exchange reference rates".
   */
  public static final FxIndexId EUR_GBP_ECB = FxIndexId.of("EUR/GBP-ECB");
  /**
   * An identifier for the FX index for conversion from EUR to JPY, as defined by
   * the European Central Bank "Euro foreign exchange reference rates".
   */
  public static final FxIndexId EUR_JPY_ECB = FxIndexId.of("EUR/JPY-ECB");
  /**
   * An identifier for the FX index for conversion from EUR to USD, as defined by
   * the European Central Bank "Euro foreign exchange reference rates".
   */
  public static final FxIndexId EUR_USD_ECB = FxIndexId.of("EUR/USD-ECB");

  /**
   * An identifier for the FX index for conversion from USD to CHF, as defined by
   * the WM company "Closing Spot rates".
   */
  public static final FxIndexId USD_CHF_WM = FxIndexId.of("USD/CHF-WM");
  /**
   * An identifier for the FX index for conversion from GBP to USD, as defined by
   * the WM company "Closing Spot rates".
   */
  public static final FxIndexId GBP_USD_WM = FxIndexId.of("GBP/USD-WM");
  /**
   * An identifier for the FX index for conversion from EUR to GBP, as defined by
   * the WM company "Closing Spot rates".
   */
  public static final FxIndexId EUR_USD_WM = FxIndexId.of("EUR/USD-WM");
  /**
   * An identifier for the FX index for conversion from USD to JPY, as defined by
   * the WM company "Closing Spot rates".
   */
  public static final FxIndexId USD_JPY_WM = FxIndexId.of("USD/JPY-WM");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FxIndexIds() {
  }

}
