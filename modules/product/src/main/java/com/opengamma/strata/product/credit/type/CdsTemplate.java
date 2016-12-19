/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.product.TradeTemplate;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.CdsTrade;

/**
 * A template for creating credit default swap trades.
 */
public interface CdsTemplate
    extends TradeTemplate {

  /**
   * Gets the market convention of the credit default swap.
   * 
   * @return the convention
   */
  public abstract CdsConvention getConvention();

  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified trade date.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the CDS, the protection is received from the counterparty on default, with the fixed coupon being paid.
   * If selling the CDS, the protection is paid to the counterparty on default, with the fixed coupon being received.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeDate  the date of the trade
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public abstract CdsTrade createTrade(
      StandardId legalEntityId,
      LocalDate tradeDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData);

  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified trade date and upfront fee.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the CDS, the protection is received from the counterparty on default, with the fixed coupon being paid.
   * If selling the CDS, the protection is paid to the counterparty on default, with the fixed coupon being received.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeDate  the date of the trade
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param upFrontFee  the reference data
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public abstract CdsTrade createTrade(
      StandardId legalEntityId,
      LocalDate tradeDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      AdjustablePayment upFrontFee,
      ReferenceData refData);

}