/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard Overnight future conventions.
 */
final class OvernightFutureConventions {
  // when some conventions are added this class should be made public

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<OvernightFutureConvention> ENUM_LOOKUP = ExtendedEnum.of(OvernightFutureConvention.class);

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private OvernightFutureConventions() {
  }

}
