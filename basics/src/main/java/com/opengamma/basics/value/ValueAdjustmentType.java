/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.value;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.collect.ArgChecker;

/**
 * The type of value adjustment.
 * <p>
 * A {@code double} value can be transformed into another value in various different ways.
 * Each type is a function of two values, the base value and the modifying value.
 * <p>
 * Each type represents a different way to express the same concept.
 * For example, here is how an increase from 200 to 220 could be represented:
 * <p>
 * <table class="border 1px solid black;border-collapse:collapse">
 * <tr>
 * <th>Type</th><th>baseValue</th><th>modifyingValue</th><th>Calculation</th>
 * </tr><tr>
 * <td>Absolute</td><td>200</td><td>220</td><td>{@code result = modifyingValue = 220}</td>
 * </tr><tr>
 * <td>DeltaAmount</td><td>200</td><td>20</td><td>{@code result = baseValue + modifyingValue = (200 + 20) = 220}</td>
 * </tr><tr>
 * <td>DeltaMultiplier</td><td>200</td><td>0.1</td>
 * <td>{@code result = baseValue + baseValue * modifyingValue = (200 + 200 * 0.1) = 220}</td>
 * </tr><tr>
 * <td>Multiplier</td><td>200</td><td>1.1</td><td>{@code result = baseValue * modifyingValue = (200 * 1.1) = 220}</td>
 * </tr>
 * </table>
 */
public enum ValueAdjustmentType {

  /**
   * The modifying value represents the result absolutely.
   * The input base value is ignored.
   * <p>
   * The result is {@code modifyingValue}.
   */
  ABSOLUTE {
    @Override
    public double adjust(double baseValue, double modifyingValue) {
      return modifyingValue;
    }
  },
  /**
   * Calculates the result by treating the modifying value as a delta, adding it to the base value.
   * <p>
   * The result is {@code (baseValue + modifyingValue)}.
   */
  DELTA_AMOUNT {
    @Override
    public double adjust(double baseValue, double modifyingValue) {
      return (baseValue + modifyingValue);
    }
  },
  /**
   * Calculates the result by treating the modifying value as a multiplication factor, adding it to the base value.
   * <p>
   * The result is {@code (baseValue + baseValue * modifyingValue)}.
   */
  DELTA_MULTIPLIER {
    @Override
    public double adjust(double baseValue, double modifyingValue) {
      return (baseValue + baseValue * modifyingValue);
    }
  },
  /**
   * Calculates the result by treating the modifying value as a multiplication factor to apply to the base value.
   * <p>
   * The result is {@code (baseValue * modifyingValue)}.
   */
  MULTIPLIER {
    @Override
    public double adjust(double baseValue, double modifyingValue) {
      return (baseValue * modifyingValue);
    }
  };

  //-----------------------------------------------------------------------
  /**
   * Adjusts the base value based on the type and the modifying value.
   * 
   * @param baseValue  the base, or previous, value to be adjusted
   * @param modifyingValue  the value that the type uses to modify the base value
   * @return the calculated result
   */
  public abstract double adjust(double baseValue, double modifyingValue);

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static ValueAdjustmentType of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  /**
   * Returns the formatted unique name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

}
