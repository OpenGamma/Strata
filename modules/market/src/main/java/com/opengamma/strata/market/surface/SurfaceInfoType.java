/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import org.joda.convert.FromString;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.TypedString;
import com.opengamma.strata.market.model.MoneynessType;

/**
 * The type that provides meaning to additional surface information.
 * <p>
 * Additional surface information is stored in {@link SurfaceMetadata}.
 * It provides the ability to associate arbitrary information with a surface in a key-value map.
 * For example, it might be used to provide information about one of the axes.
 * <p>
 * Applications that wish to use surface information should declare a static
 * constant declaring the {@code SurfaceInfoType} instance, the type parameter
 * and an UpperCamelCase name. For example:
 * <pre>
 *  public static final SurfaceInfoType&lt;String&gt; OWNER = SurfaceInfoType.of("Owner");
 * </pre>
 * 
 * @param <T>  the type of the associated value
 */
public final class SurfaceInfoType<T>
    extends TypedString<SurfaceInfoType<T>> {

  /**
   * Key used to access information about the {@link DayCount}.
   */
  public static final SurfaceInfoType<DayCount> DAY_COUNT = SurfaceInfoType.of("DayCount");
  /**
   * Key used to access information about the type of moneyness.
   */
  public static final SurfaceInfoType<MoneynessType> MONEYNESS_TYPE = SurfaceInfoType.of("MoneynessType");

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
  public static <T> SurfaceInfoType<T> of(String name) {
    return new SurfaceInfoType<T>(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private SurfaceInfoType(String name) {
    super(name);
  }

}
