/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * Identifies a measure that can be produced by the system.
 * <p>
 * A measure identifies the calculation result that is required.
 * For example present value, par rate or spread.
 * <p>
 * Some measures represent aspects of the calculation target rather than a calculation.
 * For example, the target identifier, counterparty and trade date.
 * <p>
 * Note that not all measures will be available for all targets.
 */
public interface Measure extends Named {

  /**
   * Obtains an instance from the specified unique name.
   *
   * @param uniqueName  the unique name
   * @return the measure
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static Measure of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this measure.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

  //-------------------------------------------------------------------------
  /**
   * Flag indicating whether measure values should be automatically converted to the reporting currency.
   *
   * @return true if measure values should be automatically converted to the reporting currency
   */
  public abstract boolean isCurrencyConvertible();

  //-------------------------------------------------------------------------
  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the measure to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  static ExtendedEnum<Measure> extendedEnum() {
    return MeasureHelper.ENUM_LOOKUP;
  }

}
