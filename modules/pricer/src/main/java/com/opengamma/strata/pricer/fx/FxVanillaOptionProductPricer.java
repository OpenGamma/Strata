package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.Fx;
import com.opengamma.strata.finance.fx.FxVanillaOption;
import com.opengamma.strata.finance.fx.FxVanillaOptionProduct;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer for foreign exchange vanilla option transaction products.
 * <p>
 * This function provides the ability to price an {@link FxVanillaOptionProduct}.
 */
public abstract class FxVanillaOptionProductPricer {

  /**
   * Returns the pricer used to price the underlying FX product.
   * 
   * @return the pricer
   */
  public abstract DiscountingFxProductPricer getDiscountingFxProductPricer();

  /**
   * Calculates the price of the foreign exchange vanilla option product.
   * <p>
   * The price of the product is the value on the valuation date.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the price of the product
   */
  public double price(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    double undiscountedPrice = undiscountedPrice(option, ratesProvider, volatilityProvider, 0d);
    double discountFactor =
        ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    return discountFactor * undiscountedPrice;
  }

  /**
   * Calculates the present value of the foreign exchange vanilla option product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double price = price(option, ratesProvider, volatilityProvider);
    return CurrencyAmount.of(option.getPayoffCurrency(), signedNotional(option) * price);
  }

  abstract double undiscountedPrice(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider,
      double spread);

  //-------------------------------------------------------------------------
  /**
   * Calculates the delta of the foreign exchange vanilla option product.
   * <p>
   * The delta is the first derivative of the option price with respect to spot. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the delta of the product
   */
  public double delta(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    double undiscountedDelta = undiscountedDelta(option, ratesProvider, volatilityProvider, 0d);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    double fwdRateSpotSensitivity =
        getDiscountingFxProductPricer().forwardFxRateSpotSensitivity(underlying, ratesProvider);
    return undiscountedDelta * discountFactor * fwdRateSpotSensitivity;
  }

  /**
   * Calculates the present value delta of the foreign exchange vanilla option product.
   * <p>
   * The present value delta is the first derivative of the present value with respect to spot. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value delta of the product
   */
  public CurrencyAmount presentValueDelta(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double delta = delta(option, ratesProvider, volatilityProvider);
    return CurrencyAmount.of(option.getPayoffCurrency(), signedNotional(option) * delta);
  }

  /**
   * Calculates the present value sensitivity of the foreign exchange vanilla option product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The volatility is fixed in this sensitivity computation.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivities presentValueSensitivity(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    double undiscountedDelta = undiscountedDelta(option, ratesProvider, volatilityProvider, 0d);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    double notional = signedNotional(option);
    PointSensitivityBuilder fwdSensi = getDiscountingFxProductPricer().forwardFxRatePointSensitivity(
        underlying, ratesProvider).multipliedBy(notional * discountFactor * undiscountedDelta);
    double fwdPrice = undiscountedPrice(option, ratesProvider, volatilityProvider, 0d);
    PointSensitivityBuilder dscSensi = ratesProvider.discountFactors(option.getPayoffCurrency())
        .zeroRatePointSensitivity(underlying.getPaymentDate()).multipliedBy(notional * fwdPrice);
    return fwdSensi.combinedWith(dscSensi).build();
  }

  abstract double undiscountedDelta(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider,
      double spread);

  //-------------------------------------------------------------------------
  /**
   * Calculates the gamma of the foreign exchange vanilla option product.
   * <p>
   * The delta is the second derivative of the option price with respect to spot. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the gamma of the product
   */
  public double gamma(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    double undiscountedGamma = undiscountedGamma(option, ratesProvider, volatilityProvider, 0d);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    double fwdRateSpotSensitivity =
        getDiscountingFxProductPricer().forwardFxRateSpotSensitivity(underlying, ratesProvider);
    return undiscountedGamma * discountFactor * fwdRateSpotSensitivity * fwdRateSpotSensitivity;
  }

  /**
   * Calculates the present value delta of the foreign exchange vanilla option product.
   * <p>
   * The present value gamma is the second derivative of the present value with respect to spot. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value gamma of the product
   */
  public CurrencyAmount presentValueGamma(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double gamma = gamma(option, ratesProvider, volatilityProvider);
    return CurrencyAmount.of(option.getPayoffCurrency(), signedNotional(option) * gamma);
  }

  abstract double undiscountedGamma(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider,
      double spread);

  //-------------------------------------------------------------------------
  /**
   * Calculates the vega of the foreign exchange vanilla option product.
   * <p>
   * The vega is the first derivative of the option price with respect to volatility. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the vega of the product
   */
  public double vega(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    double undiscountedVega = undiscountedVega(option, ratesProvider, volatilityProvider, 0d);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    return discountFactor * undiscountedVega;
  }

  /**
   * Calculates the present value vega of the foreign exchange vanilla option product.
   * <p>
   * The present value vega is the first derivative of the present value with respect to volatility. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value vega of the product
   */
  public CurrencyAmount presentValueVega(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double vega = vega(option, ratesProvider, volatilityProvider);
    return CurrencyAmount.of(option.getPayoffCurrency(), signedNotional(option) * vega);
  }

  /**
   * Computes the present value sensitivity to the black volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value sensitivity
   */
  public FxOptionSensitivity presentValueSensitivityBlackVolatility(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    FxRate forward = getDiscountingFxProductPricer().forwardFxRate(underlying, ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    CurrencyAmount valueVega = presentValueVega(option, ratesProvider, volatilityProvider);
    return FxOptionSensitivity.of(strikePair, option.getExpiryDate(), strike.fxRate(strikePair),
        forward.fxRate(strikePair), valueVega.getCurrency(), valueVega.getAmount());
  }

  abstract double undiscountedVega(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider,
      double spread);

  //-------------------------------------------------------------------------
  /**
   * Calculates the Black theta of the foreign exchange vanilla option product.
   * <p>
   * The theta is the first derivative of the option price with respect to time parameter in Black formula, 
   * i.e., the discounted driftless theta. 
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the theta of the product
   */
  public double theta(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    Fx underlying = option.getUnderlying();
    double undiscountedTheta = undiscountedTheta(option, ratesProvider, volatilityProvider, 0d);
    double discountFactor = ratesProvider.discountFactor(option.getPayoffCurrency(), underlying.getPaymentDate());
    return discountFactor * undiscountedTheta;
  }

  /**
   * Calculates the present value theta of the foreign exchange vanilla option product.
   * <p>
   * The present value theta is the first derivative of the present value with time parameter in Black formula, 
   * i.e., the driftless theta of the present value.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the present value vega of the product
   */
  public CurrencyAmount presentValueTheta(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    double theta = theta(option, ratesProvider, volatilityProvider);
    return CurrencyAmount.of(option.getPayoffCurrency(), signedNotional(option) * theta);
  }

  abstract double undiscountedTheta(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider,
      double spread);

  //-------------------------------------------------------------------------
  /**
   * Calculates the implied Black volatility of the foreign exchange vanilla option product.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the implied volatility of the product
   */
  public double impliedVolatility(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    FxRate forward = getDiscountingFxProductPricer().forwardFxRate(option.getUnderlying(), ratesProvider);
    FxRate strike = option.getStrike();
    CurrencyPair strikePair = strike.getPair();
    return volatilityProvider.getVolatility(
        strikePair, option.getExpiryDate(), strike.fxRate(strikePair), forward.fxRate(strikePair));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the foreign exchange vanilla option product.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      FxVanillaOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {
    CurrencyPair strikePair = option.getStrike().getPair();
    double price = price(option, ratesProvider, volatilityProvider);
    double delta = delta(option, ratesProvider, volatilityProvider);
    double spot = ratesProvider.fxRate(strikePair);
    double signedNotional = signedNotional(option);
    CurrencyAmount domestic = CurrencyAmount.of(option.getPayoffCurrency(), (price - delta * spot) * signedNotional);
    CurrencyAmount foreign = CurrencyAmount.of(strikePair.getBase(), delta * signedNotional);
    return MultiCurrencyAmount.of(domestic, foreign);
  }

  //-------------------------------------------------------------------------
  // signed notional amount to computed present value and value Greeks
  private double signedNotional(FxVanillaOption option) {
    return (option.getLongShort().isLong() ? 1d : -1d) * option.getUnderlying().getReceiveCurrencyAmount().getAmount();
  }

}
