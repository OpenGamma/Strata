/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import com.opengamma.strata.product.common.SettlementType;

/**
 * Defines how the swaption will be settled.
 * <p>
 * Settlement can be physical, where an interest rate swap is created, or cash,
 * where a monetary amount is exchanged.
 * 
 * @see PhysicalSwaptionSettlement
 * @see CashSwaptionSettlement
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
