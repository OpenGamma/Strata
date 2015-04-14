/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import java.util.regex.Pattern;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.type.TypedString;

/**
 * Identifies a measure that can be produced by the system, for example present value, or par rate.
 */
public final class Measure extends TypedString<Measure> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /** Pattern for checking the measure name. It must only contains the characters A-Z, a-z, 0-9 and -. */
  private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9-]+");

  // Standard measures ---------------------------------------------------------------------

  /** Measure representing the ID of the calculation target. */
  public static final Measure ID = Measure.of("ID");

  /** Measure representing the counterparty of the calculation target, presumably a trade. */
  public static final Measure COUNTERPARTY = Measure.of("Counterparty");

  /** Measure representing the settlement date of the calculation target. */
  public static final Measure SETTLE_DATE = Measure.of("SettleDate");

  /** Measure representing the maturity date of the calculation target. */
  public static final Measure MATURITY_DATE = Measure.of("MaturityDate");

  /** Measure representing the notional amount of the calculation target. */
  public static final Measure NOTIONAL = Measure.of("Notional");

  /** Measure representing the present value of the calculation target. */
  public static final Measure PRESENT_VALUE = Measure.of("PresentValue");

  /** Measure representing the net present value of the calculation target. */
  public static final Measure NPV = Measure.of("NPV");

  // ---------------------------------------------------------------------------------------

  /**
   * Obtains a {@code Measure} by name.
   * <p>
   * Measure names must only contains the characters A-Z, a-z, 0-9 and -.
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
    validateName(name);
  }

  /**
   * Checks the name matches {@link #NAME_PATTERN}.
   *
   * @param name  the name
   * @throws IllegalArgumentException if the name doesn't match the pattern
   */
  private static void validateName(String name) {
    if (!NAME_PATTERN.matcher(name).matches()) {
      throw new IllegalArgumentException("Measure names must only contains the characters A-Z, a-z, 0-9 and -");
    }
  }
}
