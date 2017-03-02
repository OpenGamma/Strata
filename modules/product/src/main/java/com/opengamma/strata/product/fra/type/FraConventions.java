/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra.type;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard FRA conventions.
 * <p>
 * FRA conventions are based on the details held within the {@link IborIndex}.
 * As such, there is a factory method rather than constants for the conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
public final class FraConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<FraConvention> ENUM_LOOKUP = ExtendedEnum.of(FraConvention.class);

  /**
   * Obtains a convention based on the specified index.
   * <p>
   * This uses the index name to find the matching convention.
   * By default, this will always return a convention, however configuration may be added
   * to restrict the conventions that are registered.
   * 
   * @param index  the index, from which the index name is used to find the matching convention
   * @return the convention
   * @throws IllegalArgumentException if no convention is registered for the index
   */
  public static FraConvention of(IborIndex index) {
    return FraConvention.of(index);
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FraConventions() {
  }

}
