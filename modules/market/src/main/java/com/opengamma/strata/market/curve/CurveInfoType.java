/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import org.joda.convert.FromString;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.type.TypedString;

/**
 * The type of additional curve information.
 * 
 * @param <T>  the type associated with the info
 */
public final class CurveInfoType<T>
    extends TypedString<CurveInfoType<T>> {

  /**
   * Key used to access information about the {@link DayCount}.
   */
  public static final CurveInfoType<DayCount> DAY_COUNT = CurveInfoType.of("DayCount");
  /**
   * Key used to access information about the {@link JacobianCalibrationMatrix}.
   */
  public static final CurveInfoType<JacobianCalibrationMatrix> JACOBIAN = CurveInfoType.of("Jacobian");

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code CurveInfoType} by name.
   * <p>
   * Curve names may contain any character, but must not be empty.
   *
   * @param name  the name of the curve
   * @return a curve with the specified name
   */
  @FromString
  public static <T> CurveInfoType<T> of(String name) {
    return new CurveInfoType<T>(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the curve
   */
  private CurveInfoType(String name) {
    super(name);
  }

}
