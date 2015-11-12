/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swaption;

/**
 * An interface that can return the settlement type and settlement method of swaptions.
 */
public interface SwaptionSettlement {

  /**
   * Gets the settlement type of swaption. 
   * <p>
   * The settlement type is cash settlement or physical settlement, defined in {@link SettlementType}.
   * 
   * @return the settlement type
   */
  public SettlementType getSettlementType();

}
