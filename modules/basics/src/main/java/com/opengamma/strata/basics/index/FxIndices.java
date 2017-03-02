/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard foreign exchange indices.
 * <p>
 * Each constant returns a standard definition of the specified index.
 */
public final class FxIndices {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<FxIndex> ENUM_LOOKUP = ExtendedEnum.of(FxIndex.class);

  /**
   * The FX index for conversion from EUR to CHF, as defined by the European Central Bank
   * "Euro foreign exchange reference rates".
   */
  public static final FxIndex EUR_CHF_ECB = FxIndex.of("EUR/CHF-ECB");
  /**
   * The FX index for conversion from EUR to GBP, as defined by the European Central Bank
   * "Euro foreign exchange reference rates".
   */
  public static final FxIndex EUR_GBP_ECB = FxIndex.of("EUR/GBP-ECB");
  /**
   * The FX index for conversion from EUR to JPY, as defined by the European Central Bank
   * "Euro foreign exchange reference rates".
   */
  public static final FxIndex EUR_JPY_ECB = FxIndex.of("EUR/JPY-ECB");
  /**
   * The FX index for conversion from EUR to USD, as defined by the European Central Bank
   * "Euro foreign exchange reference rates".
   */
  public static final FxIndex EUR_USD_ECB = FxIndex.of("EUR/USD-ECB");

  /**
   * The FX index for conversion from USD to CHF, as defined by the WM company
   * "Closing Spot rates".
   */
  public static final FxIndex USD_CHF_WM = FxIndex.of("USD/CHF-WM");
  /**
   * The FX index for conversion from GBP to USD, as defined by the WM company
   * "Closing Spot rates".
   */
  public static final FxIndex GBP_USD_WM = FxIndex.of("GBP/USD-WM");
  /**
   * The FX index for conversion from EUR to GBP, as defined by the WM company
   * "Closing Spot rates".
   */
  public static final FxIndex EUR_USD_WM = FxIndex.of("EUR/USD-WM");
  /**
   * The FX index for conversion from USD to JPY, as defined by the WM company
   * "Closing Spot rates".
   */
  public static final FxIndex USD_JPY_WM = FxIndex.of("USD/JPY-WM");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FxIndices() {
  }

}
