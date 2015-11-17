/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.PaymentPeriod;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapProduct;

/**
 * Pricer for for rate swap products.
 * <p>
 * This function provides the ability to price a {@link SwapProduct}.
 * The product is priced by pricing each leg.
 */
public class DiscountingSwapProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingSwapProductPricer DEFAULT = new DiscountingSwapProductPricer(
      DiscountingSwapLegPricer.DEFAULT);

  /**
   * Pricer for {@link SwapLeg}.
   */
  private final DiscountingSwapLegPricer legPricer;

  /**
   * Creates an instance.
   * 
   * @param legPricer  the pricer for {@link SwapLeg}
   */
  public DiscountingSwapProductPricer(
      DiscountingSwapLegPricer legPricer) {
    this.legPricer = ArgChecker.notNull(legPricer, "legPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer used to price the legs.
   * 
   * @return the pricer
   */
  public DiscountingSwapLegPricer getLegPricer() {
    return legPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swap product, converted to the specified currency.
   * <p>
   * The present value of the product is the value on the valuation date.
   * This is the discounted forecast value.
   * The result is converted to the specified currency.
   * 
   * @param product  the product to price
   * @param currency  the currency to convert to
   * @param provider  the rates provider
   * @return the present value of the swap product in the specified currency
   */
  public CurrencyAmount presentValue(SwapProduct product, Currency currency, RatesProvider provider) {
    double totalPv = 0;
    for (ExpandedSwapLeg leg : product.expand().getLegs()) {
      double pv = legPricer.presentValueInternal(leg, provider);
      totalPv += (pv * provider.fxRate(leg.getCurrency(), currency));
    }
    return CurrencyAmount.of(currency, totalPv);
  }

  /**
   * Calculates the present value of the swap product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * This is the discounted forecast value.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value of the swap product
   */
  public MultiCurrencyAmount presentValue(SwapProduct product, RatesProvider provider) {
    return swapValue(provider, product.expand(), legPricer::presentValueInternal);
  }

  /**
   * Calculates the forecast value of the swap product.
   * <p>
   * The forecast value of the product is the value on the valuation date without present value discounting.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the forecast value of the swap product
   */
  public MultiCurrencyAmount forecastValue(SwapProduct product, RatesProvider provider) {
    return swapValue(provider, product.expand(), legPricer::forecastValueInternal);
  }

  //-------------------------------------------------------------------------
  // calculate present or forecast value for the swap
  private static MultiCurrencyAmount swapValue(
      RatesProvider provider,
      ExpandedSwap swap,
      ToDoubleBiFunction<SwapLeg, RatesProvider> legFn) {

    if (swap.isCrossCurrency()) {
      return swap.getLegs().stream()
          .map(leg -> CurrencyAmount.of(leg.getCurrency(), legFn.applyAsDouble(leg, provider)))
          .collect(MultiCurrencyAmount.collector());
    } else {
      Currency currency = swap.getLegs().iterator().next().getCurrency();
      double total = 0d;
      for (ExpandedSwapLeg leg : swap.getLegs()) {
        total += legFn.applyAsDouble(leg, provider);
      }
      return MultiCurrencyAmount.of(currency, total);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the accrued interest since the last payment.
   * <p>
   * This determines the payment period applicable at the valuation date and calculates
   * the accrued interest since the last payment.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the accrued interest of the swap product
   */
  public MultiCurrencyAmount accruedInterest(SwapProduct product, RatesProvider provider) {
    ExpandedSwap swap = product.expand();
    MultiCurrencyAmount result = MultiCurrencyAmount.empty();
    for (ExpandedSwapLeg leg : swap.getLegs()) {
      result = result.plus(legPricer.accruedInterest(leg, provider));
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the par rate for swaps with a fixed leg. 
   * <p>
   * The par rate is the common rate on all payments of the fixed leg for which the total swap present value is 0.
   * <p>
   * At least one leg must be a fixed leg. The par rate will be computed with respect to the first fixed leg 
   * in which all the payments are fixed payments with a unique accrual period (no compounding) and no FX reset. 
   * If the fixed leg is compounding, the par rate is computed only when the number of fixed coupon payments is 1 and 
   * accrual factor of each sub-period is 1 
   * 
   * @param product  the swap product for which the par rate should be computed
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parRate(SwapProduct product, RatesProvider provider) {
    // find fixed leg
    ExpandedSwap swap = product.expand();
    ExpandedSwapLeg fixedLeg = fixedLeg(swap);
    Currency ccyFixedLeg = fixedLeg.getCurrency();
    // other payments (not fixed leg coupons) converted in fixed leg currency
    double otherLegsConvertedPv = 0.0;
    for (ExpandedSwapLeg leg : swap.getLegs()) {
      if (leg != fixedLeg) {
        double pvLocal = legPricer.presentValueInternal(leg, provider);
        otherLegsConvertedPv += (pvLocal * provider.fxRate(leg.getCurrency(), ccyFixedLeg));
      }
    }
    double fixedLegEventsPv = legPricer.presentValueEventsInternal(fixedLeg, provider);
    if (fixedLeg.getPaymentPeriods().size() > 1) { // try multiperiod par-rate
      // PVBP
      double pvbpFixedLeg = legPricer.pvbp(fixedLeg, provider);
      // Par rate
      return -(otherLegsConvertedPv + fixedLegEventsPv) / pvbpFixedLeg;
    } else {
      PaymentPeriod firstPeriod = fixedLeg.getPaymentPeriods().get(0);
      ArgChecker.isTrue(firstPeriod instanceof RatePaymentPeriod, "PaymentPeriod must be instance of RatePaymentPeriod");
      RatePaymentPeriod payment = (RatePaymentPeriod) firstPeriod;
      if (payment.getAccrualPeriods().size() == 1) { // no compounding
        // PVBP
        double pvbpFixedLeg = legPricer.pvbp(fixedLeg, provider);
        // Par rate
        return -(otherLegsConvertedPv + fixedLegEventsPv) / pvbpFixedLeg;
      }
      // try Compounding
      ImmutableList<RateAccrualPeriod> ap = payment.getAccrualPeriods();
      ArgChecker.isFalse(payment.getCompoundingMethod().equals(CompoundingMethod.NONE), "should be compounding");
      for (RateAccrualPeriod p : ap) {
        ArgChecker.isTrue(p.getYearFraction() == 1.0, "accrual factor should be 1");
        ArgChecker.isTrue(p.getSpread() == 0.0, "no spread");
      }
      double nbAp = ap.size();
      double notional = payment.getNotional();
      double df = provider.discountFactor(ccyFixedLeg, payment.getPaymentDate());
      return Math.pow(-(otherLegsConvertedPv + fixedLegEventsPv) / notional / df + 1d, 1 / nbAp) - 1d;
    }
  }

  /**
   * Computes the par spread for swaps. 
   * <p>
   * The par spread is the common spread on all payments of the first leg for which the total swap present value is 0.
   * <p>
   * The par spread will be computed with respect to the first leg. For that leg, all the payments have a unique 
   * accrual period or multiple accrual periods with Flat compounding and no FX reset.
   * 
   * @param product  the swap product for which the par rate should be computed
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parSpread(SwapProduct product, RatesProvider provider) {
    ExpandedSwap swap = product.expand();
    SwapLeg referenceLeg = swap.getLegs().get(0);
    Currency ccyReferenceLeg = referenceLeg.getCurrency();
    double convertedPv = presentValue(swap, ccyReferenceLeg, provider).getAmount();
    double pvbp = legPricer.pvbp(referenceLeg, provider);
    return -convertedPv / pvbp;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swap product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivity(SwapProduct product, RatesProvider provider) {
    return swapValueSensitivity(
        product.expand(),
        provider,
        legPricer::presentValueSensitivity);
  }

  /**
   * Calculates the present value sensitivity of the swap product converted in a given currency.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param currency  the currency to convert to
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the swap product converted in the given currency
   */
  public PointSensitivityBuilder presentValueSensitivity(SwapProduct product, Currency currency, RatesProvider provider) {
    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (ExpandedSwapLeg leg : product.expand().getLegs()) {
      PointSensitivityBuilder ls = legPricer.presentValueSensitivity(leg, provider);
      PointSensitivityBuilder lsConverted =
          ls.withCurrency(currency).multipliedBy(provider.fxRate(leg.getCurrency(), currency));
      builder = builder.combinedWith(lsConverted);
    }
    return builder;
  }

  /**
   * Calculates the forecast value sensitivity of the swap product.
   * <p>
   * The forecast value sensitivity of the product is the sensitivity of the forecast value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the forecast value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder forecastValueSensitivity(SwapProduct product, RatesProvider provider) {
    return swapValueSensitivity(
        product.expand(),
        provider,
        legPricer::forecastValueSensitivity);
  }

  // calculate present or forecast value sensitivity for the swap
  private static PointSensitivityBuilder swapValueSensitivity(
      ExpandedSwap swap,
      RatesProvider provider,
      BiFunction<SwapLeg, RatesProvider, PointSensitivityBuilder> legFn) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (ExpandedSwapLeg leg : swap.getLegs()) {
      builder = builder.combinedWith(legFn.apply(leg, provider));
    }
    return builder;
  }

  /**
   * Calculates the par rate curve sensitivity for a swap with a fixed leg. 
   * <p>
   * The par rate is the common rate on all payments of the fixed leg for which the total swap present value is 0.
   * <p>
   * At least one leg must be a fixed leg. The par rate will be computed with respect to the first fixed leg.
   * All the payments in that leg should be fixed payments with a unique accrual period (no compounding) and no FX reset.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par rate curve sensitivity of the swap product
   */
  public PointSensitivityBuilder parRateSensitivity(SwapProduct product, RatesProvider provider) {
    ExpandedSwap swap = product.expand();
    ExpandedSwapLeg fixedLeg = fixedLeg(swap);
    Currency ccyFixedLeg = fixedLeg.getCurrency();
    // other payments (not fixed leg coupons) converted in fixed leg currency
    double otherLegsConvertedPv = 0.0;
    for (ExpandedSwapLeg leg : swap.getLegs()) {
      if (leg != fixedLeg) {
        double pvLocal = legPricer.presentValueInternal(leg, provider);
        otherLegsConvertedPv += (pvLocal * provider.fxRate(leg.getCurrency(), ccyFixedLeg));
      }
    }
    double fixedLegEventsPv = legPricer.presentValueEventsInternal(fixedLeg, provider);
    double pvbpFixedLeg = legPricer.pvbp(fixedLeg, provider);
    // Backward sweep
    double otherLegsConvertedPvBar = -1.0d / pvbpFixedLeg;
    double fixedLegEventsPvBar = -1.0d / pvbpFixedLeg;
    double pvbpFixedLegBar = (otherLegsConvertedPv + fixedLegEventsPv) / (pvbpFixedLeg * pvbpFixedLeg);
    PointSensitivityBuilder pvbpFixedLegDr = legPricer.pvbpSensitivity(fixedLeg, provider);
    PointSensitivityBuilder fixedLegEventsPvDr = legPricer.presentValueSensitivityEventsInternal(fixedLeg, provider);
    PointSensitivityBuilder otherLegsConvertedPvDr = PointSensitivityBuilder.none();
    for (ExpandedSwapLeg leg : swap.getLegs()) {
      if (leg != fixedLeg) {
        PointSensitivityBuilder pvLegDr = legPricer.presentValueSensitivity(leg, provider)
            .multipliedBy(provider.fxRate(leg.getCurrency(), ccyFixedLeg));
        otherLegsConvertedPvDr = otherLegsConvertedPvDr.combinedWith(pvLegDr);
      }
    }
    otherLegsConvertedPvDr = otherLegsConvertedPvDr.withCurrency(ccyFixedLeg);
    return pvbpFixedLegDr.multipliedBy(pvbpFixedLegBar)
        .combinedWith(fixedLegEventsPvDr.multipliedBy(fixedLegEventsPvBar))
        .combinedWith(otherLegsConvertedPvDr.multipliedBy(otherLegsConvertedPvBar));
  }

  /**
   * Calculates the par spread curve sensitivity for a swap. 
   * <p>
   * The par spread is the common spread on all payments of the first leg for which the total swap present value is 0.
   * <p>
   * The par spread is computed with respect to the first leg. For that leg, all the payments have a unique 
   * accrual period (no compounding) and no FX reset.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par spread curve sensitivity of the swap product
   */
  public PointSensitivityBuilder parSpreadSensitivity(SwapProduct product, RatesProvider provider) {
    ExpandedSwap swap = product.expand();
    SwapLeg referenceLeg = swap.getLegs().get(0);
    Currency ccyReferenceLeg = referenceLeg.getCurrency();
    double convertedPv = presentValue(swap, ccyReferenceLeg, provider).getAmount();
    double pvbp = legPricer.pvbp(referenceLeg, provider);
    // Backward sweep
    double convertedPvBar = -1d / pvbp;
    double pvbpBar = convertedPv / (pvbp * pvbp);
    PointSensitivityBuilder pvbpDr = legPricer.pvbpSensitivity(referenceLeg, provider);
    PointSensitivityBuilder convertedPvDr = presentValueSensitivity(swap, ccyReferenceLeg, provider);
    return convertedPvDr.multipliedBy(convertedPvBar).combinedWith(pvbpDr.multipliedBy(pvbpBar));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future cash flows of the swap product.
   * <p>
   * Each expected cash flow is added to the result.
   * This is based on {@link #forecastValue(SwapProduct, RatesProvider)}.
   * 
   * @param product  the swap product for which the cash flows should be computed
   * @param provider  the rates provider
   * @return the cash flow
   */
  public CashFlows cashFlows(SwapProduct product, RatesProvider provider) {
    ExpandedSwap swap = product.expand();
    return swap.getLegs().stream()
        .map(leg -> legPricer.cashFlows(leg, provider))
        .reduce(CashFlows.NONE, CashFlows::combinedWith);
  }

  //-------------------------------------------------------------------------
  /**
   * Explains the present value of the swap product.
   * <p>
   * This returns explanatory information about the calculation.
   * 
   * @param product  the swap product for which present value should be computed
   * @param provider  the rates provider
   * @return the explanatory information
   */
  public ExplainMap explainPresentValue(SwapProduct product, RatesProvider provider) {
    ExpandedSwap swap = product.expand();

    ExplainMapBuilder builder = ExplainMap.builder();
    builder.put(ExplainKey.ENTRY_TYPE, "Swap");
    for (ExpandedSwapLeg leg : swap.getLegs()) {
      builder.addListEntryWithIndex(
          ExplainKey.LEGS, child -> legPricer.explainPresentValueInternal(leg, provider, child));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // checking that at least one leg is a fixed leg and returning the first one
  private ExpandedSwapLeg fixedLeg(ExpandedSwap swap) {
    List<ExpandedSwapLeg> fixedLegs = swap.getLegs(SwapLegType.FIXED);
    if (fixedLegs.isEmpty()) {
      throw new IllegalArgumentException("Swap must contain a fixed leg");
    }
    return fixedLegs.get(0);
  }

}
