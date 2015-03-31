/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.type.TypedString;

/**
 * Identifies a measure that can be produced by the system, for example present value, or par rate.
 */
public final class Measure extends TypedString<Measure> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Obtains a {@code Measure} by name.
   *
   * @param name  the name of the measure
   * @return the measure matching the name
   */
  @FromString
  public static Measure of(String name) {
    return new Measure(name);
  }

  /**
   * @param name the name of the measure
   */
  private Measure(String name) {
    super(name);
  }

}
