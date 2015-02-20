package com.opengamma.platform.pricer.impl.future;

import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalFunctionData;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalPriceFunction;
import com.opengamma.analytics.financial.provider.description.interestrate.NormalSTIRFuturesProviderInterface;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.future.ExpandedIborFuture;
import com.opengamma.platform.finance.future.ExpandedIborFutureOption;
import com.opengamma.platform.finance.future.IborFutureOptionSecurityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.future.IborFutureOptionProductPricerFn;
import com.opengamma.platform.pricer.future.IborFutureProductPricerFn;

/**
 * Pricer implementation for ibor future option.
 * <p>
 * The ibor future option is priced based on normal model.
 */
public class NormalExpandedIborFutureOptionPricerFn
    implements IborFutureOptionProductPricerFn<ExpandedIborFutureOption> {

  /**
   * Default implementation.
   */
  public static final NormalExpandedIborFutureOptionPricerFn DEFAULT = new NormalExpandedIborFutureOptionPricerFn();

  private final IborFutureProductPricerFn<ExpandedIborFuture> expandedIborFuturePriceFn;

  private static final NormalPriceFunction NORMAL_FUNCTION = new NormalPriceFunction();
  
  /**
   * Creates an instance.
   */
  public NormalExpandedIborFutureOptionPricerFn() {
    expandedIborFuturePriceFn = DefaultExpandedIborFuturePricerFn.DEFAULT; //TODO add flexibility for future pricing
  }

  @Override
  public double price(PricingEnvironment env, ExpandedIborFutureOption iborFutureOptionProduct,
      NormalSTIRFuturesProviderInterface surface) {
    EuropeanVanillaOption option = createOption(env, iborFutureOptionProduct);
    NormalFunctionData normalPoint = createData(env, iborFutureOptionProduct, surface);
    return NORMAL_FUNCTION.getPriceFunction(option).evaluate(normalPoint);
  }

  @Override
  public CurrencyAmount presentValue(PricingEnvironment env, ExpandedIborFutureOption iborFutureOptionProduct,
      IborFutureOptionSecurityTrade trade, NormalSTIRFuturesProviderInterface surface) {
    double optionPrice = price(env, iborFutureOptionProduct, surface);
    double pv = (optionPrice - trade.getReferencePrice()) *
        iborFutureOptionProduct.getExpandedIborFuture().getNotional()
        * iborFutureOptionProduct.getExpandedIborFuture().getAccrualFactor() * trade.getMultiplier();
    return CurrencyAmount.of(iborFutureOptionProduct.getExpandedIborFuture().getCurrency(), pv);
  }

  @Override
  public double priceDelta(PricingEnvironment env, ExpandedIborFutureOption iborFutureOptionProduct,
      NormalSTIRFuturesProviderInterface surface) {
    EuropeanVanillaOption option = createOption(env, iborFutureOptionProduct);
    NormalFunctionData normalPoint = createData(env, iborFutureOptionProduct, surface);
    return NORMAL_FUNCTION.getDelta(option, normalPoint);
  }

  @Override
  public double priceGamma(PricingEnvironment env, ExpandedIborFutureOption iborFutureOptionProduct,
      NormalSTIRFuturesProviderInterface surface) {
    EuropeanVanillaOption option = createOption(env, iborFutureOptionProduct);
    NormalFunctionData normalPoint = createData(env, iborFutureOptionProduct, surface);
    return NORMAL_FUNCTION.getGamma(option, normalPoint);
  }

  @Override
  public double priceVega(PricingEnvironment env, ExpandedIborFutureOption iborFutureOptionProduct,
      NormalSTIRFuturesProviderInterface surface) {
    EuropeanVanillaOption option = createOption(env, iborFutureOptionProduct);
    NormalFunctionData normalPoint = createData(env, iborFutureOptionProduct, surface);
    return NORMAL_FUNCTION.getVega(option, normalPoint);
  }

  @Override
  public double priceTheta(PricingEnvironment env, ExpandedIborFutureOption iborFutureOptionProduct,
      NormalSTIRFuturesProviderInterface surface) {
    EuropeanVanillaOption option = createOption(env, iborFutureOptionProduct);
    NormalFunctionData normalPoint = createData(env, iborFutureOptionProduct, surface);
    return NORMAL_FUNCTION.getTheta(option, normalPoint);
  }

  private EuropeanVanillaOption createOption(PricingEnvironment env, ExpandedIborFutureOption iborFutureOptionProduct) {
    double strike = iborFutureOptionProduct.getStrike();
    double timeToExpiry = env.relativeTime(iborFutureOptionProduct.getExpirationDate());
    boolean isCall = iborFutureOptionProduct.isIsCall();
    return new EuropeanVanillaOption(strike, timeToExpiry, isCall);
  }

  // TODO add normal sensitivity
  // public SurfaceValue priceNormalSensitivity(PricingEnvironment env, ExpandedIborFutureOption iborFutureOptionProduct)
  // MultipleCurrencyMulticurveSensitivity presentValueCurveSensitivity

  private NormalFunctionData createData(PricingEnvironment env, ExpandedIborFutureOption iborFutureOptionProduct,
      NormalSTIRFuturesProviderInterface surface) {
    ExpandedIborFuture underlyingFuture = iborFutureOptionProduct.getExpandedIborFuture();
    double futurePrice = expandedIborFuturePriceFn.price(env, underlyingFuture);
    double timeToExpiry = env.relativeTime(iborFutureOptionProduct.getExpirationDate());
    double timeToLastTrade = env.relativeTime(iborFutureOptionProduct.getLastTradeDate());
    double delay = timeToLastTrade - timeToExpiry;
    double strike = iborFutureOptionProduct.getStrike();
    double volatility = surface.getVolatility(timeToExpiry, delay, strike, futurePrice);
    return new NormalFunctionData(futurePrice, 1.0, volatility);
  }
}
