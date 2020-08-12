/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard FX swap conventions.
 */
public final class FxSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<FxSwapConvention> ENUM_LOOKUP = ExtendedEnum.of(FxSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The "EUR/USD" FX Swap convention.
   * <p>
   * EUR/USD convention with 2 days spot date.
   */
  public static final FxSwapConvention EUR_USD =
      FxSwapConvention.of(StandardFxSwapConventions.EUR_USD.getName());

  /**
   * The "EUR/GBP" FX Swap convention.
   * <p>
   * EUR/GBP convention with 2 days spot date.
   */
  public static final FxSwapConvention EUR_GBP =
      FxSwapConvention.of(StandardFxSwapConventions.EUR_GBP.getName());

  /**
   * The "EUR/JPY" FX Swap convention.
   * <p>
   * EUR/JPY convention with 2 days spot date.
   */
  public static final FxSwapConvention EUR_JPY =
      FxSwapConvention.of(StandardFxSwapConventions.EUR_JPY.getName());

  /**
   * The "GBP/USD" FX Swap convention.
   * <p>
   * GBP/USD convention with 2 days spot date.
   */
  public static final FxSwapConvention GBP_USD =
      FxSwapConvention.of(StandardFxSwapConventions.GBP_USD.getName());

  /**
   * The "GBP/JPY" FX Swap convention.
   * <p>
   * GBP/JPY convention with 2 days spot date.
   */
  public static final FxSwapConvention GBP_JPY =
      FxSwapConvention.of(StandardFxSwapConventions.GBP_JPY.getName());

  /**
   * The "USD/JPY" FX Swap convention.
   * <p>
   * USD/JPY convention with 2 days spot date.
   */
  public static final FxSwapConvention USD_JPY =
      FxSwapConvention.of(StandardFxSwapConventions.USD_JPY.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FxSwapConventions() {
  }

}
