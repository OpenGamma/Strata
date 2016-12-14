/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import org.joda.convert.FromString;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.TypedString;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * The type that provides meaning to additional curve information.
 * <p>
 * Additional curve information is stored in {@link CurveMetadata}.
 * It provides the ability to associate arbitrary information with a curve in a key-value map.
 * For example, it might be used to provide information about one of the axes.
 * <p>
 * Applications that wish to use curve information should declare a static
 * constant declaring the {@code CurveInfoType} instance, the type parameter
 * and an UpperCamelCase name. For example:
 * <pre>
 *  public static final CurveInfoType&lt;String&gt; OWNER = CurveInfoType.of("Owner");
 * </pre>
 * 
 * @param <T>  the type of the associated value
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
  /**
   * Key used to access information about the number of compounding per year, as an {@link Integer}.
   */
  public static final CurveInfoType<Integer> COMPOUNDING_PER_YEAR = CurveInfoType.of("CompoundingPerYear");
  /**
   * Key used to access information about the present value sensitivity to market quote, 
   * represented by a {@link DoubleArray}.
   */
  public static final CurveInfoType<DoubleArray> PV_SENSITIVITY_TO_MARKET_QUOTE =
      CurveInfoType.of("PVSensitivityToMarketQuote");
  /**
   * Key used to access information about the index factor.
   */
  public static final CurveInfoType<Double> CDS_INDEX_FACTOR = CurveInfoType.of("CdsIndexFactor");

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * The name may contain any character, but must not be empty.
   *
   * @param <T>  the type associated with the info
   * @param name  the name
   * @return a type instance with the specified name
   */
  @FromString
  public static <T> CurveInfoType<T> of(String name) {
    return new CurveInfoType<T>(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private CurveInfoType(String name) {
    super(name);
  }

}
