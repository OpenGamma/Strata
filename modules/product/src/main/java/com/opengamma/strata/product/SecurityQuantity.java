/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

/**
 * A quantity of a security.
 * <p>
 * This is used to represent the total quantity of a {@link Security}.
 * This is the base interface for {@link Position} and trades in securities, such as {@link SecurityTrade}.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface SecurityQuantity {

  /**
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * 
   * @return the security identifier
   */
  public abstract SecurityId getSecurityId();

  /**
   * Gets the quantity of the security.
   * <p>
   * This returns the <i>net</i> quantity of the underlying security.
   * It can be negative if the security is effectively being sold.
   * <p>
   * For a trade, the quantity is positive if the security is being bought and
   * negative if being sold. For a position, the quantity is positive if the net
   * position is <i>long</i> and negative if the net position is <i>short</i>.
   * 
   * @return the net quantity of the security that is held
   */
  public abstract double getQuantity();

}
