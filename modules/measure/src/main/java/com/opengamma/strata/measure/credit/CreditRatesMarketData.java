package com.opengamma.strata.measure.credit;

import java.time.LocalDate;

import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;

public interface CreditRatesMarketData {

  /**
   * Gets the valuation date.
   *
   * @return the valuation date
   */
  public default LocalDate getValuationDate() {
    return getMarketData().getValuationDate();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the lookup that provides access to repo and issuer curves.
   * 
   * @return the discounting lookup
   */
  public abstract CreditRatesMarketDataLookup getLookup();

  /**
   * Gets the market data.
   * 
   * @return the market data
   */
  public abstract MarketData getMarketData();

  /**
   * Returns a copy of this instance with the specified market data.
   * 
   * @param marketData  the market data to use
   * @return a market view based on the specified data
   */
  public abstract CreditRatesMarketData withMarketData(MarketData marketData);

  //-------------------------------------------------------------------------
  /**
   * Gets the discounting provider.
   * <p>
   * This provides access to repo and issuer curves.
   * 
   * @return the discounting provider
   */
  public abstract CreditRatesProvider creditRatesProvider();

}
