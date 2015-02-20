package com.opengamma.platform.pricer.future;

import com.opengamma.analytics.financial.provider.description.interestrate.NormalSTIRFuturesProviderInterface;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.future.IborFutureOptionProduct;
import com.opengamma.platform.finance.future.IborFutureOptionSecurityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for Ibor future option products.
 * <p>
 * This function provides the ability to price a {@link IborFutureOptionProduct}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of product
 */
public interface IborFutureOptionProductPricerFn<T extends IborFutureOptionProduct> {

  //TODO decide implementation of model parameters including volatility surface
  //TODO curve sensitivity
  //  public abstract MulticurveSensitivity priceCurveSensitivity(PricingEnvironment env, T iborFutureOptionProduct);

  /**
   * Calculates price of the Ibor future option.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param env The pricing environment
   * @param iborFutureOptionProduct The pricing option
   * @param surface The volatility surface
   * @return option price
   */
  public abstract double price(PricingEnvironment env, T iborFutureOptionProduct,
      NormalSTIRFuturesProviderInterface surface);

  public abstract CurrencyAmount presentValue(PricingEnvironment env, T iborFutureOptionProduct,
      IborFutureOptionSecurityTrade trade, NormalSTIRFuturesProviderInterface surface);

  /**
   * Calculates price delta of the Ibor future option.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param env The pricing environment
   * @param iborFutureOptionProduct The pricing option
   * @param surface The volatility surface
   * @return option price
   */
  public abstract double priceDelta(PricingEnvironment env, T iborFutureOptionProduct,
      NormalSTIRFuturesProviderInterface surface);

  /**
   * Calculates price gamma of the Ibor future option.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param env The pricing environment
   * @param iborFutureOptionProduct The pricing option
   * @param surface The volatility surface
   * @return option price
   */
  public abstract double priceGamma(PricingEnvironment env, T iborFutureOptionProduct,
      NormalSTIRFuturesProviderInterface surface);

  /**
   * Calculates price vega of the Ibor future option.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param env The pricing environment
   * @param iborFutureOptionProduct The pricing option
   * @param surface The volatility surface
   * @return option price
   */
  public abstract double priceVega(PricingEnvironment env, T iborFutureOptionProduct,
      NormalSTIRFuturesProviderInterface surface);

  /**
   * Calculates price theta of the Ibor future option.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param env The pricing environment
   * @param iborFutureOptionProduct The pricing option
   * @param surface The volatility surface
   * @return option price
   */
  public abstract double priceTheta(PricingEnvironment env, T iborFutureOptionProduct,
      NormalSTIRFuturesProviderInterface surface);
}
