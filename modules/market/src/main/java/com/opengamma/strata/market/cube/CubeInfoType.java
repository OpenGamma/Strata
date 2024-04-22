/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import org.joda.convert.FromString;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.TypedString;
import com.opengamma.strata.market.model.MoneynessType;

/**
 * The type that provides meaning to additional cube information.
 * <p>
 * Additional cube information is stored in {@link CubeMetadata}.
 * It provides the ability to associate arbitrary information with a cube in a key-value map.
 * For example, it might be used to provide information about one of the axes.
 * <p>
 * Applications that wish to use cube information should declare a static
 * constant declaring the {@code CubeInfoType} instance, the type parameter
 * and an UpperCamelCase name. For example:
 * <pre>
 *  public static final CubeInfoType&lt;String&gt; OWNER = CubeInfoType.of("Owner");
 * </pre>
 *
 * @param <T>  the type of the associated value
 */
public final class CubeInfoType<T>
    extends TypedString<CubeInfoType<T>> {

  /**
   * Key used to access information about the {@link DayCount}.
   */
  public static final CubeInfoType<DayCount> DAY_COUNT = CubeInfoType.of("DayCount");

  /**
   * Key used to access information about the type of moneyness.
   */
  public static final CubeInfoType<MoneynessType> MONEYNESS_TYPE = CubeInfoType.of("MoneynessType");

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
  public static <T> CubeInfoType<T> of(String name) {
    return new CubeInfoType<T>(name);
  }

  /**
   * Creates an instance.
   *
   * @param name  the name
   */
  private CubeInfoType(String name) {
    super(name);
  }

}
