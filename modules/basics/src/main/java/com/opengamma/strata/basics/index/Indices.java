/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.collect.named.CombinedExtendedEnum;

/**
 * Helper for {@code Index}
 */
final class Indices {

  /**
   * The extended enum lookup from name to instance.
   */
  static final CombinedExtendedEnum<Index> ENUM_LOOKUP = CombinedExtendedEnum.of(Index.class);

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private Indices() {
  }

}
