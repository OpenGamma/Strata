/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.ResolvableCalculationTarget;
import com.opengamma.strata.collect.Messages;

/**
 * A trade that has a security identifier that can be resolved using reference data.
 * <p>
 * This represents those trades that hold a security identifier. It allows the trade
 * to be resolved, returning an alternate representation of the same trade with complete
 * security information.
 */
public interface ResolvableSecurityTrade
    extends SecurityQuantityTrade, ResolvableCalculationTarget {

  /**
   * Resolves the security identifier using the specified reference data.
   * <p>
   * This takes the security identifier of this trade, looks it up in reference data,
   * and returns the equivalent trade with full security information.
   * If the security has underlying securities, they will also have been resolved in the result.
   * <p>
   * The resulting trade is bound to data from reference data.
   * If the data changes, the resulting trade form will not be updated.
   * Care must be taken when placing the resolved form in a cache or persistence layer.
   * 
   * @param refData  the reference data to use when resolving
   * @return the resolved trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if unable to resolve due to an invalid definition
   */
  @Override
  public default SecuritizedProductTrade<?> resolveTarget(ReferenceData refData) {
    SecurityId securityId = getSecurityId();
    Security security = refData.getValue(securityId);
    SecurityQuantityTrade trade = security.createTrade(getInfo(), getQuantity(), getPrice(), refData);
    if (trade instanceof SecuritizedProductTrade) {
      return (SecuritizedProductTrade<?>) trade;
    }
    throw new ClassCastException(Messages.format(
        "Reference data for security '{}' did not implement SecuritizedProductTrade: ", securityId, trade.getClass().getName()));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified info.
   * 
   * @param info  the new info
   * @return the instance with the specified info
   */
  @Override
  public abstract ResolvableSecurityTrade withInfo(TradeInfo info);

  /**
   * Returns an instance with the specified quantity.
   * 
   * @param quantity  the new quantity
   * @return the instance with the specified quantity
   */
  @Override
  public abstract ResolvableSecurityTrade withQuantity(double quantity);

  /**
   * Returns an instance with the specified price.
   * 
   * @param price  the new price
   * @return the instance with the specified price
   */
  @Override
  public abstract ResolvableSecurityTrade withPrice(double price);

}
