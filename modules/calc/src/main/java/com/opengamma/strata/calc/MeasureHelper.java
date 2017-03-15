/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Helper for measures.
 */
final class MeasureHelper {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<Measure> ENUM_LOOKUP = ExtendedEnum.of(Measure.class);

  //-------------------------------------------------------------------------
  private MeasureHelper() {
  }

}
