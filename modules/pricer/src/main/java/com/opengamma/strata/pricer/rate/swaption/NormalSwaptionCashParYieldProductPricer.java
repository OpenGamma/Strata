package com.opengamma.strata.pricer.rate.swaption;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.FixedRateObservation;
import com.opengamma.strata.finance.rate.RateObservation;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLegType;
import com.opengamma.strata.finance.rate.swap.SwapProduct;
import com.opengamma.strata.finance.rate.swaption.CashSettlement;
import com.opengamma.strata.finance.rate.swaption.CashSettlementMethod;
import com.opengamma.strata.finance.rate.swaption.ExpandedSwaption;
import com.opengamma.strata.finance.rate.swaption.SettlementType;
import com.opengamma.strata.finance.rate.swaption.SwaptionProduct;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;
import com.opengamma.strata.pricer.impl.option.NormalFunctionData;
import com.opengamma.strata.pricer.impl.option.NormalPriceFunction;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapProductPricer;

/**
 * Pricer for swaption with par yield curve method of cash settlement in a normal model on the swap rate.
 * <p>
 * The swap underlying the swaption should have a fixed leg on which the forward rate is computed. The underlying swap
 * should be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap conventions. The volatilities from the provider
 * are taken as such.
 * <p>
 * The value of the swaption after expiry is 0. For a swaption which already expired, negative number is returned by 
 * the method, {@link NormalVolatilitySwaptionProvider#relativeTime(ZonedDateTime)}.
 */
public class NormalSwaptionCashParYieldProductPricer {

  /**
   * Default implementation.
   */
  public static final NormalSwaptionCashParYieldProductPricer DEFAULT =
      new NormalSwaptionCashParYieldProductPricer(DiscountingSwapProductPricer.DEFAULT);

  /** 
   * Pricer for {@link SwapProduct}. 
   */
  private final DiscountingSwapProductPricer swapPricer;
  /** 
   * Normal model pricing function. 
   */
  public static final NormalPriceFunction NORMAL = new NormalPriceFunction();

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public NormalSwaptionCashParYieldProductPricer(DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swap pricer");
  }

  /**
   * Calculates the present value of the swaption product.
   * <p>
   * The result is expressed using the currency of the swapion.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the present value of the swaption product
   */
  public CurrencyAmount presentValue(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {
    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, volatilityProvider);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = volatilityProvider.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0.0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0.0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double strike = getStrike(fixedLeg);
    double tenor = volatilityProvider.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilityProvider.getVolatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(annuityCash * discountSettle), volatility);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    Function1D<NormalFunctionData, Double> func = NORMAL.getPriceFunction(option);
    double pv = func.evaluate(normalData) * ((expanded.getLongShort() == LongShort.LONG) ? 1.0 : -1.0);
    return CurrencyAmount.of(fixedLeg.getCurrency(), pv);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption product.
   * 
   * @param swaption  the swaption to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the present value of the swaption product
   */
  public MultiCurrencyAmount currencyExposure(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {
    return MultiCurrencyAmount.of(presentValue(swaption, ratesProvider, volatilityProvider));
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied normal volatility of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the normal implied volatility associated to the swaption
   */
  public double impliedVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {
    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, volatilityProvider);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = volatilityProvider.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    ArgChecker.isTrue(expiry >= 0.0d, "option should be before expiry to compute an implied volatility");
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double strike = getStrike(fixedLeg);
    double tenor = volatilityProvider.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    return volatilityProvider.getVolatility(expiryDateTime, tenor, strike, forward);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param swaption  the swaption product
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivityStickyStrike(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {
    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, volatilityProvider);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = volatilityProvider.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0.0d) { // Option has expired already
      return PointSensitivityBuilder.none();
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double annuityCashDr = swapPricer.getLegPricer().annuityCashDerivative(fixedLeg, forward);
    LocalDate settlementDate = ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate();
    double discountSettle = ratesProvider.discountFactor(fixedLeg.getCurrency(), settlementDate);
    double strike = getStrike(fixedLeg);
    double tenor = volatilityProvider.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilityProvider.getVolatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    NormalFunctionData normalData = NormalFunctionData.of(forward, 1d, volatility);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    ValueDerivatives price = NORMAL.getPriceAdjoint(option, normalData);
    PointSensitivityBuilder forwardSensi = swapPricer.parRateSensitivity(underlying, ratesProvider);
    PointSensitivityBuilder discountSettleSensi =
        ratesProvider.discountFactors(fixedLeg.getCurrency()).zeroRatePointSensitivity(settlementDate);
    double sign = (expanded.getLongShort() == LongShort.LONG) ? 1.0 : -1.0;
    return forwardSensi.multipliedBy(sign * discountSettle * 
        (annuityCash * price.getDerivative(0) + annuityCashDr * price.getValue()))
        .combinedWith(discountSettleSensi.multipliedBy(sign * annuityCash * price.getValue()));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption product.
   * <p>
   * The sensitivity to the normal volatility is also called normal vega.
   * 
   * @param swaption  the swaption product
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the point sensitivity to the normal volatility
   */
  public SwaptionSensitivity presentValueSensitivityNormalVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {
    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, volatilityProvider);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = volatilityProvider.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    double tenor = volatilityProvider.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double strike = getStrike(fixedLeg);
    if (expiry < 0.0d) { // Option has expired already
      return SwaptionSensitivity.of(volatilityProvider.getConvention(), expiryDateTime, tenor, strike, 0.0d,
          fixedLeg.getCurrency(), 0.0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double volatility = volatilityProvider.getVolatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(annuityCash * discountSettle), volatility);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    double vega = NORMAL.getVega(option, normalData) * ((expanded.getLongShort() == LongShort.LONG) ? 1.0 : -1.0);
    return SwaptionSensitivity.of(
        volatilityProvider.getConvention(), expiryDateTime, tenor, strike, forward, fixedLeg.getCurrency(), vega);
  }

  //-------------------------------------------------------------------------
  // check that one leg is fixed and return it
  private ExpandedSwapLeg fixedLeg(ExpandedSwap swap) {
    ArgChecker.isFalse(swap.isCrossCurrency(), "swap should be single currency");
    // find fixed leg
    List<ExpandedSwapLeg> fixedLegs = swap.getLegs(SwapLegType.FIXED);
    if (fixedLegs.isEmpty()) {
      throw new IllegalArgumentException("Swap must contain a fixed leg");
    }
    return fixedLegs.get(0);
  }

  // get fixed rate 
  private double getStrike(ExpandedSwapLeg fixedLeg) {
    PaymentPeriod paymentPeriod = fixedLeg.getPaymentPeriods().get(0);
    ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "payment period should be RatePaymentPeriod");
    RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
    // compounding is caught when par rate is computed
    RateObservation rateObservation = ratePaymentPeriod.getAccrualPeriods().get(0).getRateObservation();
    ArgChecker.isTrue(rateObservation instanceof FixedRateObservation, "swap leg should be fixed leg");
    return ((FixedRateObservation) rateObservation).getRate();
  }

  // validate that the rates and volatilities providers are coherent
  private void validate(RatesProvider ratesProvider, ExpandedSwaption swaption,
      NormalVolatilitySwaptionProvider volatilityProvider) {
    ArgChecker.isTrue(volatilityProvider.getValuationDateTime().toLocalDate().equals(ratesProvider.getValuationDate()),
        "volatility and rate data should be for the same date");
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "underlying swap should be single currency");
    ArgChecker.isTrue(swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH),
        "swaption should be cash settlement");
    CashSettlement cashSettle = (CashSettlement) swaption.getSwaptionSettlement();
    ArgChecker.isTrue(cashSettle.getCashSettlementMethod().equals(CashSettlementMethod.PAR_YIELD),
        "cash settlement method should be par yield");
  }

}
