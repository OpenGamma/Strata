/**
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
  public static final FxIndex ECB_EUR_CHF = FxIndex.of(StandardFxIndices.ECB_EUR_CHF.getName());
  /**
   * The FX index for conversion from EUR to GBP, as defined by the European Central Bank
   * "Euro foreign exchange reference rates".
   */
  public static final FxIndex ECB_EUR_GBP = FxIndex.of(StandardFxIndices.ECB_EUR_GBP.getName());
  /**
   * The FX index for conversion from EUR to JPY, as defined by the European Central Bank
   * "Euro foreign exchange reference rates".
   */
  public static final FxIndex ECB_EUR_JPY = FxIndex.of(StandardFxIndices.ECB_EUR_JPY.getName());
  /**
   * The FX index for conversion from EUR to USD, as defined by the European Central Bank
   * "Euro foreign exchange reference rates".
   */
  public static final FxIndex ECB_EUR_USD = FxIndex.of(StandardFxIndices.ECB_EUR_USD.getName());

  /**
   * The FX index for conversion from USD to CHF, as defined by the WM company
   * "Closing Spot rates".
   */
  public static final FxIndex WM_USD_CHF = FxIndex.of(StandardFxIndices.WM_USD_CHF.getName());
  /**
   * The FX index for conversion from GBP to USD, as defined by the WM company
   * "Closing Spot rates".
   */
  public static final FxIndex WM_GBP_USD = FxIndex.of(StandardFxIndices.WM_GBP_USD.getName());
  /**
   * The FX index for conversion from EUR to GBP, as defined by the WM company
   * "Closing Spot rates".
   */
  public static final FxIndex WM_EUR_USD = FxIndex.of(StandardFxIndices.WM_EUR_USD.getName());
  /**
   * The FX index for conversion from USD to JPY, as defined by the WM company
   * "Closing Spot rates".
   */
  public static final FxIndex WM_USD_JPY = FxIndex.of(StandardFxIndices.WM_USD_JPY.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FxIndices() {
  }

}
