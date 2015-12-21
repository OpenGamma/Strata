/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;

/**
 * A swap index.
 * <p>
 * Swap rates for CHF, EUR, GBP, JPY and USD are established by ISDA in co-operation with Reuters (now Thomson Reuters) 
 * and Intercapital Brokers (now ICAP plc). 
 * Ref: http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 * <p>
 * The most common implementations are provided in {@link SwapIndices}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface SwapIndex
    extends Index, Named {

  /**
   * Obtains a {@code SwapIndex} from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static SwapIndex of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of {@code SwapIndex} to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<SwapIndex> extendedEnum() {
    return SwapIndices.ENUM_LOOKUP;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets the template for creating Fixed-Ibor swap.
   * <p>
   * @return the template
   */
  public abstract FixedIborSwapTemplate getTemplate();

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this index.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
