/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * A named enum instance.
 * <p>
 * This extends {@link Named} for implementations of {@link Enum}.
 * The name is provided by the {@link Enum#toString()} method of the enum, typically
 * using the {@link EnumNames} helper class.
 * <p>
 * Implementations must provide a static method {@code of(String)} that allows the
 * instance to be created from the name, see {@link Named#of(Class, String)}.
 */
public interface NamedEnum extends Named {

  /**
   * Gets the unique name of the instance.
   * <p>
   * The name contains enough information to be able to recreate the instance.
   * 
   * @return the unique name
   */
  @Override
  public default String getName() {
    return toString();
  }

}
