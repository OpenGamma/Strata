/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Helper for conventions.
 */
final class IborFixingDepositConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<IborFixingDepositConvention> ENUM_LOOKUP = ExtendedEnum.of(IborFixingDepositConvention.class);

  /**
   * Restricted constructor.
   */
  private IborFixingDepositConventions() {
  }

}
