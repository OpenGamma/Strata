/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard Overnight-Overnight swap conventions.
 */
public final class OvernightOvernightSwapConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<OvernightOvernightSwapConvention> ENUM_LOOKUP =
      ExtendedEnum.of(OvernightOvernightSwapConvention.class);

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private OvernightOvernightSwapConventions() {
  }

}
