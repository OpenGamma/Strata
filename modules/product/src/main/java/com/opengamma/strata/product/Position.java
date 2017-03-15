/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.CalculationTarget;

/**
 * A position in a security.
 * <p>
 * This is used to represent the total quantity of a {@link Security}.
 * A position is effectively the sum of one or more trades.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface Position
    extends SecurityQuantity, CalculationTarget {

  /**
   * Gets the standard position information.
   * <p>
   * All positions contain this standard set of information.
   * It includes the identifier and an extensible data map.
   * 
   * @return the position information
   */
  public abstract PositionInfo getInfo();

  /**
   * Gets the identifier of the underlying security.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * 
   * @return the security identifier
   */
  @Override
  public abstract SecurityId getSecurityId();

  /**
   * Gets the net quantity of the security.
   * <p>
   * This returns the <i>net</i> quantity of the underlying security.
   * The result is positive if the net position is <i>long</i> and negative
   * if the net position is <i>short</i>.
   * 
   * @return the net quantity of the underlying security
   */
  @Override
  public abstract double getQuantity();

}
