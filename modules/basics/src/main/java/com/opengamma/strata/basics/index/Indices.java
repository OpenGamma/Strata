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
  static final CombinedExtendedEnum<Index> INDEX_LOOKUP = CombinedExtendedEnum.of(Index.class);
  /**
   * The extended enum lookup from name to instance.
   */
  static final CombinedExtendedEnum<RateIndex> RATE_INDEX_LOOKUP = CombinedExtendedEnum.of(RateIndex.class);
  /**
   * The extended enum lookup from name to instance.
   */
  static final CombinedExtendedEnum<FloatingRateIndex> FLOATING_RATE_INDEX_LOOKUP =
      CombinedExtendedEnum.of(FloatingRateIndex.class);

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private Indices() {
  }

}
