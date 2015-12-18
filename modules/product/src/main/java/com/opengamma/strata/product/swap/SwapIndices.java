package com.opengamma.strata.product.swap;

import com.opengamma.strata.collect.named.ExtendedEnum;

public final class SwapIndices {

  /** 
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<SwapIndex> ENUM_LOOKUP = ExtendedEnum.of(SwapIndex.class);

  //-------------------------------------------------------------------------
  public static final SwapIndex USD_1100_1Y = SwapIndex.of("USD-1100-1Y");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private SwapIndices() {
  }
}
