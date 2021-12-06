/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.collect.named.CombinedExtendedEnum;

/**
 * Helper for {@code FixedFloatSwapConvention}
 */
public class FixedFloatSwapConventions {
  
  /**
   * The extended enum lookup from name to instance.
   */
  static final CombinedExtendedEnum<FixedFloatSwapConvention> CONVENTIONS_LOOKUP = 
      CombinedExtendedEnum.of(FixedFloatSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FixedFloatSwapConventions() {
  }

}
