/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Helper for conventions.
 */
final class FraConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<FraConvention> ENUM_LOOKUP = ExtendedEnum.of(FraConvention.class);

  /**
   * Restricted constructor.
   */
  private FraConventions() {
  }

}
