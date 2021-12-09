/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A market convention for Fixed-Float swap trades, covering Ibor and Overnight indices.
 * <p>
 * This is a marker interface, see {@link FixedIborSwapConvention} 
 * and {@link FixedOvernightSwapConvention} for more information.
 */
public interface FixedFloatSwapConvention
    extends SingleCurrencySwapConvention {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FixedFloatSwapConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return FixedFloatSwapConventions.CONVENTIONS_LOOKUP.lookup(uniqueName);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this convention.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the fixed leg.
   * 
   * @return the fixed leg convention
   */
  public abstract FixedRateSwapLegConvention getFixedLeg();

  /**
   * Gets the market convention of the floating leg.
   * 
   * @return the floating leg convention
   */
  public abstract FloatRateSwapLegConvention getFloatingLeg();

  //-------------------------------------------------------------------------
  /**
   * Obtains a template based on the specified tenor.
   * <p>
   * The swap will start on the spot date.
   * 
   * @param tenor  the tenor of the swap
   * @return the template
   */
  public abstract FixedFloatSwapTemplate toTemplate(Tenor tenor);

}
