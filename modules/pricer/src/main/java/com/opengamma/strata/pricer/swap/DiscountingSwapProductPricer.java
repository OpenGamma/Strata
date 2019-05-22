/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap;

import static com.opengamma.strata.basics.currency.MultiCurrencyAmount.toMultiCurrencyAmount;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Triple;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.FixedOvernightCompoundedAnnualRateComputation;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapPaymentPeriod;

/**
 * Pricer for for rate swap products.
 * <p>
 * This function provides the ability to price a {@link ResolvedSwap}.
 * The product is priced by pricing each leg.
 */
public class DiscountingSwapProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingSwapProductPricer DEFAULT = new DiscountingSwapProductPricer(
      DiscountingSwapLegPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedSwapLeg}.
   */
  private final DiscountingSwapLegPricer legPricer;

  /**
   * Creates an instance.
   * 
   * @param legPricer  the pricer for {@link ResolvedSwapLeg}
   */
  public DiscountingSwapProductPricer(
      DiscountingSwapLegPricer legPricer) {
    this.legPricer = ArgChecker.notNull(legPricer, "legPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying leg pricer.
   * 
   * @return the leg pricer
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
   * @param swap  the product
   * @param currency  the currency to convert to
   * @param provider  the rates provider
   * @return the present value of the swap product in the specified currency
   */
  public CurrencyAmount presentValue(ResolvedSwap swap, Currency currency, RatesProvider provider) {
    double totalPv = 0;
    for (ResolvedSwapLeg leg : swap.getLegs()) {
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
   * @param swap  the product
   * @param provider  the rates provider
   * @return the present value of the swap product
   */
  public MultiCurrencyAmount presentValue(ResolvedSwap swap, RatesProvider provider) {
    return swapValue(provider, swap, legPricer::presentValueInternal);
  }

  /**
   * Calculates the forecast value of the swap product.
   * <p>
   * The forecast value of the product is the value on the valuation date without present value discounting.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the forecast value of the swap product
   */
  public MultiCurrencyAmount forecastValue(ResolvedSwap swap, RatesProvider provider) {
    return swapValue(provider, swap, legPricer::forecastValueInternal);
  }

  //-------------------------------------------------------------------------
  // calculate present or forecast value for the swap
  private static MultiCurrencyAmount swapValue(
      RatesProvider provider,
      ResolvedSwap swap,
      ToDoubleBiFunction<ResolvedSwapLeg, RatesProvider> legFn) {

    if (swap.isCrossCurrency()) {
      return swap.getLegs().stream()
          .map(leg -> CurrencyAmount.of(leg.getCurrency(), legFn.applyAsDouble(leg, provider)))
          .collect(toMultiCurrencyAmount());
    } else {
      Currency currency = swap.getLegs().iterator().next().getCurrency();
      double total = 0d;
      for (ResolvedSwapLeg leg : swap.getLegs()) {
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
   * @param swap  the product
   * @param provider  the rates provider
   * @return the accrued interest of the swap product
   */
  public MultiCurrencyAmount accruedInterest(ResolvedSwap swap, RatesProvider provider) {
    MultiCurrencyAmount result = MultiCurrencyAmount.empty();
    for (ResolvedSwapLeg leg : swap.getLegs()) {
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
   * @param swap  the product
   * @param provider  the rates provider
   * @return the par rate
   * @throws IllegalArgumentException if there is no fixed leg
   */
  public double parRate(ResolvedSwap swap, RatesProvider provider) {
    // find fixed leg
    ResolvedSwapLeg fixedLeg = fixedLeg(swap);
    Currency ccyFixedLeg = fixedLeg.getCurrency();
    // other payments (not fixed leg coupons) converted in fixed leg currency
    double otherLegsConvertedPv = 0.0;
    for (ResolvedSwapLeg leg : swap.getLegs()) {
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
    }
    SwapPaymentPeriod firstPeriod = fixedLeg.getPaymentPeriods().get(0);
    ArgChecker.isTrue(firstPeriod instanceof RatePaymentPeriod, "PaymentPeriod must be instance of RatePaymentPeriod");
    RatePaymentPeriod payment = (RatePaymentPeriod) firstPeriod;
    if (payment.getAccrualPeriods().size() == 1) {
      RateAccrualPeriod firstAccrualPeriod = payment.getAccrualPeriods().get(0);
      if (firstAccrualPeriod.getRateComputation() instanceof FixedOvernightCompoundedAnnualRateComputation) { // check for future value notional
        double accrualFactor = payment.getAccrualPeriods().get(0).getYearFraction();
        double notional = payment.getNotional();
        double df = provider.discountFactor(ccyFixedLeg, payment.getPaymentDate());
        return Math.pow(-otherLegsConvertedPv  / (notional * df) + 1.0d,   1.0 / accrualFactor) - 1.0d;
      } else { // no compounding
        // PVBP
        double pvbpFixedLeg = legPricer.pvbp(fixedLeg, provider);
        // Par rate
        return -(otherLegsConvertedPv + fixedLegEventsPv) / pvbpFixedLeg;
      }
    }
    // try Compounding
    Triple<Boolean, Integer, Double> fixedCompounded = checkFixedCompounded(fixedLeg);
    ArgChecker.isTrue(fixedCompounded.getFirst(),
        "Swap should have a fixed leg and for one payment it should be based on compunding witout spread.");
    double notional = payment.getNotional();
    double df = provider.discountFactor(ccyFixedLeg, payment.getPaymentDate());
    return Math.pow(-(otherLegsConvertedPv + fixedLegEventsPv) / (notional * df) + 1.0d,
        1.0 / fixedCompounded.getSecond()) - 1.0d;
  }

  /**
   * Computes the market quote of swaps.
   * <p>
   * For swaps with a fixed leg, this is equal to the par rate. For swaps without a fixed leg, this is the 
   * constant spread on the first leg required to have a PV of 0.
   * <p>
   * All the payment periods of the first leg must be of the type {@link RatePaymentPeriod}.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the market quote
   * @throws IllegalArgumentException if there is no fixed leg and not all the payment periods of the first leg
   *   are of the type RatePaymentPeriod
   */
  public double marketQuote(ResolvedSwap swap, RatesProvider provider) {
    if (!swap.getLegs(SwapLegType.FIXED).isEmpty()) {
      return parRate(swap, provider);
    }
    // For non-fixed first legs: generate a new swap with spread to 0
    ResolvedSwapLeg referenceLeg = swap.getLegs().get(0);
    Currency ccyReferenceLeg = referenceLeg.getCurrency();
    List<SwapPaymentPeriod> paymentPeriods0 = new ArrayList<>();
    for (SwapPaymentPeriod period : referenceLeg.getPaymentPeriods()) {
      ArgChecker.isTrue(period instanceof RatePaymentPeriod, "Must be RatePaymentPeriod");
      RatePaymentPeriod ratePeriod = (RatePaymentPeriod) period;
      List<RateAccrualPeriod> accrualPeriods0 = new ArrayList<>();
      for (RateAccrualPeriod accrualPeriod : ratePeriod.getAccrualPeriods()) {
        accrualPeriods0.add(accrualPeriod.toBuilder().spread(0.0).build());
      }
      paymentPeriods0.add(ratePeriod.toBuilder().accrualPeriods(accrualPeriods0).build());
    }
    ResolvedSwapLeg referenceLeg0 = referenceLeg.toBuilder().paymentPeriods(paymentPeriods0).build();
    double convertedPvOtherLegs = 0.0;
    for (int i = 1; i < swap.getLegs().size(); i++) {
      convertedPvOtherLegs += legPricer.presentValue(swap.getLegs().get(i), ccyReferenceLeg, provider).getAmount();
    }
    double convertedPvLeg0 = legPricer.presentValue(referenceLeg0, provider).getAmount();
    double pvbp = legPricer.pvbp(referenceLeg, provider);
    return -(convertedPvOtherLegs + convertedPvLeg0) / pvbp;
  }

  /**
   * Computes the par spread for swaps.
   * <p>
   * The par spread is the common spread on all payments of the first leg for which the total swap present value is 0.
   * <p>
   * The par spread will be computed with respect to the first leg. For that leg, all the payments have a unique 
   * accrual period or multiple accrual periods with Flat compounding and no FX reset.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parSpread(ResolvedSwap swap, RatesProvider provider) {
    // does the fixed leg, if it exists, of the swap have a future value notional
    if (!swap.getLegs(SwapLegType.FIXED).isEmpty()) {
      ResolvedSwapLeg fixedLeg = fixedLeg(swap);
      Optional<FixedOvernightCompoundedAnnualRateComputation> annualRateCompOpt = findAnnualRateComputation(fixedLeg);
      if (annualRateCompOpt.isPresent()) {
        return parRate(swap, provider) - annualRateCompOpt.get().getRate();
      }
    }
    ResolvedSwapLeg referenceLeg = swap.getLegs().get(0);
    Currency ccyReferenceLeg = referenceLeg.getCurrency();
    // try one payment compounding, typically for inflation swaps
    Triple<Boolean, Integer, Double> fixedCompounded = checkFixedCompounded(referenceLeg);
    if (fixedCompounded.getFirst()) {
      double df = provider.discountFactor(ccyReferenceLeg, referenceLeg.getPaymentPeriods().get(0).getPaymentDate());
      double convertedPv = presentValue(swap, ccyReferenceLeg, provider).getAmount();
      double referenceConvertedPv = legPricer.presentValue(referenceLeg, provider).getAmount();
      double notional = ((RatePaymentPeriod) referenceLeg.getPaymentPeriods().get(0)).getNotional();
      double parSpread =
          Math.pow(-(convertedPv - referenceConvertedPv) / (df * notional) + 1.0d, 1.0d / fixedCompounded.getSecond()) -
              (1.0d + fixedCompounded.getThird());
      return parSpread;

    }
    // In other cases, try the standard multiperiod par spread
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
   * @param swap  the product
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivity(ResolvedSwap swap, RatesProvider provider) {
    return swapValueSensitivity(swap, provider, legPricer::presentValueSensitivity);
  }

  /**
   * Calculates the present value sensitivity of the swap product converted in a given currency.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param swap  the product
   * @param currency  the currency to convert to
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the swap product converted in the given currency
   */
  public PointSensitivityBuilder presentValueSensitivity(ResolvedSwap swap, Currency currency, RatesProvider provider) {
    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (ResolvedSwapLeg leg : swap.getLegs()) {
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
   * @param swap  the product
   * @param provider  the rates provider
   * @return the forecast value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder forecastValueSensitivity(ResolvedSwap swap, RatesProvider provider) {
    return swapValueSensitivity(swap, provider, legPricer::forecastValueSensitivity);
  }

  // calculate present or forecast value sensitivity for the swap
  private static PointSensitivityBuilder swapValueSensitivity(
      ResolvedSwap swap,
      RatesProvider provider,
      BiFunction<ResolvedSwapLeg, RatesProvider, PointSensitivityBuilder> legFn) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (ResolvedSwapLeg leg : swap.getLegs()) {
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
   * @param swap  the product
   * @param provider  the rates provider
   * @return the par rate curve sensitivity of the swap product
   * @throws IllegalArgumentException if there is no fixed leg
   */
  public PointSensitivityBuilder parRateSensitivity(ResolvedSwap swap, RatesProvider provider) {
    ResolvedSwapLeg fixedLeg = fixedLeg(swap);
    Currency ccyFixedLeg = fixedLeg.getCurrency();
    // other payments (not fixed leg coupons) converted in fixed leg currency
    double otherLegsConvertedPv = 0.0;
    for (ResolvedSwapLeg leg : swap.getLegs()) {
      if (leg != fixedLeg) {
        double pvLocal = legPricer.presentValueInternal(leg, provider);
        otherLegsConvertedPv += (pvLocal * provider.fxRate(leg.getCurrency(), ccyFixedLeg));
      }
    }
    // does the fixed leg have a future value notional
    Optional<FixedOvernightCompoundedAnnualRateComputation> annualRateCompOpt = findAnnualRateComputation(fixedLeg);
    if (annualRateCompOpt.isPresent()) {
      RatePaymentPeriod payment = (RatePaymentPeriod) fixedLeg.getPaymentPeriods().get(0);
      double af = annualRateCompOpt.get().getAccrualFactor();
      double notional = payment.getNotional();
      double df = provider.discountFactor(ccyFixedLeg, payment.getPaymentDate());
      double otherLegsConvertedPvBar = -Math.pow(-otherLegsConvertedPv  / (notional * df) + 1.0d, 1.0 / af - 1.0d) / (af * notional * df);
      double dfBar = Math.pow(-otherLegsConvertedPv  / (notional * df) + 1.0d, 1.0 / af - 1.0d) * otherLegsConvertedPv / (af * notional * df * df);
      PointSensitivityBuilder otherLegsConvertedPvDr = PointSensitivityBuilder.none();
      for (ResolvedSwapLeg leg : swap.getLegs()) {
        if (leg != fixedLeg) {
          PointSensitivityBuilder pvLegDr = getLegPricer().presentValueSensitivity(leg, provider)
              .multipliedBy(provider.fxRate(leg.getCurrency(), ccyFixedLeg));
          otherLegsConvertedPvDr = otherLegsConvertedPvDr.combinedWith(pvLegDr);
        }
      }
      otherLegsConvertedPvDr = otherLegsConvertedPvDr.withCurrency(ccyFixedLeg);
      PointSensitivityBuilder dfDr = provider.discountFactors(ccyFixedLeg).zeroRatePointSensitivity(fixedLeg.getPaymentPeriods().get(0).getPaymentDate());
      return dfDr.multipliedBy(dfBar).combinedWith(otherLegsConvertedPvDr.multipliedBy(otherLegsConvertedPvBar));
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
    for (ResolvedSwapLeg leg : swap.getLegs()) {
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
   * Computes the market quote curve sensitivity for swaps.
   * <p>
   * For swaps with a fixed leg, this is equal to the par rate. For swaps without a fixed leg, this is the 
   * constant spread on the first leg required to have a PV of 0.
   * <p>
   * All the payment periods of the first leg must be of the type {@link RatePaymentPeriod}.
   * <p>
   * The figures are reported in the currency of the first leg, even if in theory they should be dimensionless.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the par rate curve sensitivity of the swap product
   * @throws IllegalArgumentException if there is no fixed leg and not all the payment periods of the first leg
   *   are of the type RatePaymentPeriod
   */
  public PointSensitivityBuilder marketQuoteSensitivity(ResolvedSwap swap, RatesProvider provider) {
    if (!swap.getLegs(SwapLegType.FIXED).isEmpty()) {
      return parRateSensitivity(swap, provider);
    }
    // For non-fixed first legs: generate a new swap with spread to 0
    ResolvedSwapLeg referenceLeg = swap.getLegs().get(0);
    Currency ccyReferenceLeg = referenceLeg.getCurrency();
    List<SwapPaymentPeriod> paymentPeriods0 = new ArrayList<>();
    for (SwapPaymentPeriod period : referenceLeg.getPaymentPeriods()) {
      ArgChecker.isTrue(period instanceof RatePaymentPeriod, "Must be RatePaymentPeriod");
      RatePaymentPeriod ratePeriod = (RatePaymentPeriod) period;
      List<RateAccrualPeriod> accrualPeriods0 = new ArrayList<>();
      for (RateAccrualPeriod accrualPeriod : ratePeriod.getAccrualPeriods()) {
        accrualPeriods0.add(accrualPeriod.toBuilder().spread(0.0).build());
      }
      paymentPeriods0.add(ratePeriod.toBuilder().accrualPeriods(accrualPeriods0).build());
    }
    ResolvedSwapLeg referenceLeg0 = referenceLeg.toBuilder().paymentPeriods(paymentPeriods0).build();

    double convertedPvOtherLegs = 0.0;
    for (int i = 1; i < swap.getLegs().size(); i++) {
      convertedPvOtherLegs += legPricer.presentValue(swap.getLegs().get(i), ccyReferenceLeg, provider).getAmount();
    }
    double convertedPvLeg0 = legPricer.presentValue(referenceLeg0, provider).getAmount();
    double pvbp = legPricer.pvbp(referenceLeg, provider);
    // double marketQuote = -(convertedPvOtherLegs + convertedPvLeg0) / pvbp;
    // Backward sweep
    double marketQuoteBar = 1.0d;
    double convertedPvOtherLegsBar = -marketQuoteBar / pvbp;
    double convertedPvLeg0Bar = -marketQuoteBar / pvbp;
    double pvbpBar = (convertedPvOtherLegs + convertedPvLeg0) / (pvbp * pvbp) * marketQuoteBar;
    PointSensitivityBuilder dconvertedPvLeg0dr = legPricer.presentValueSensitivity(referenceLeg0, provider);
    PointSensitivityBuilder dconvertedPvOtherLegsdr = PointSensitivityBuilder.none();
    for (int i = 1; i < swap.getLegs().size(); i++) {
      ResolvedSwapLeg leg = swap.getLegs().get(i);
      dconvertedPvOtherLegsdr = dconvertedPvOtherLegsdr.combinedWith(
          legPricer.presentValueSensitivity(leg, provider)
              .multipliedBy(provider.fxRate(leg.getCurrency(), ccyReferenceLeg))
              .withCurrency(ccyReferenceLeg));
    }
    PointSensitivityBuilder dpvbpdr = legPricer.pvbpSensitivity(referenceLeg, provider);
    return dconvertedPvLeg0dr.multipliedBy(convertedPvLeg0Bar)
        .combinedWith(dconvertedPvOtherLegsdr.multipliedBy(convertedPvOtherLegsBar))
        .combinedWith(dpvbpdr.multipliedBy(pvbpBar));
  }

  /**
   * Calculates the par spread curve sensitivity for a swap.
   * <p>
   * The par spread is the common spread on all payments of the first leg for which the total swap present value is 0.
   * <p>
   * The par spread is computed with respect to the first leg. For that leg, all the payments have a unique 
   * accrual period (no compounding) and no FX reset.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the par spread curve sensitivity of the swap product
   */
  public PointSensitivityBuilder parSpreadSensitivity(ResolvedSwap swap, RatesProvider provider) {
    // does the fixed leg of the swap, if it exists, have a future value notional
    if (!swap.getLegs(SwapLegType.FIXED).isEmpty()) {
      ResolvedSwapLeg fixedLeg = fixedLeg(swap);
      Optional<FixedOvernightCompoundedAnnualRateComputation> annualRateCompOpt = findAnnualRateComputation(fixedLeg);
      if (annualRateCompOpt.isPresent()) {
        return parRateSensitivity(swap, provider);
      }
    }
    ResolvedSwapLeg referenceLeg = swap.getLegs().get(0);
    Currency ccyReferenceLeg = referenceLeg.getCurrency();
    double convertedPv = presentValue(swap, ccyReferenceLeg, provider).getAmount();
    PointSensitivityBuilder convertedPvDr = presentValueSensitivity(swap, ccyReferenceLeg, provider);
    // try one payment compounding, typically for inflation swaps
    Triple<Boolean, Integer, Double> fixedCompounded = checkFixedCompounded(referenceLeg);
    if (fixedCompounded.getFirst()) {
      double df = provider.discountFactor(ccyReferenceLeg, referenceLeg.getPaymentPeriods().get(0).getPaymentDate());
      PointSensitivityBuilder dfDr = provider.discountFactors(ccyReferenceLeg)
          .zeroRatePointSensitivity(referenceLeg.getPaymentPeriods().get(0).getPaymentDate());
      double referenceConvertedPv = legPricer.presentValue(referenceLeg, provider).getAmount();
      PointSensitivityBuilder referenceConvertedPvDr = legPricer.presentValueSensitivity(referenceLeg, provider);
      double notional = ((RatePaymentPeriod) referenceLeg.getPaymentPeriods().get(0)).getNotional();
      PointSensitivityBuilder dParSpreadDr =
          convertedPvDr.combinedWith(referenceConvertedPvDr.multipliedBy(-1)).multipliedBy(-1.0d / (df * notional))
              .combinedWith(dfDr.multipliedBy((convertedPv - referenceConvertedPv) / (df * df * notional)))
              .multipliedBy(1.0d / fixedCompounded.getSecond() *
                  Math.pow(-(convertedPv - referenceConvertedPv) / (df * notional) + 1.0d,
                      1.0d / fixedCompounded.getSecond() - 1.0d));
      return dParSpreadDr;
    }
    double pvbp = legPricer.pvbp(referenceLeg, provider);
    // Backward sweep
    double convertedPvBar = -1d / pvbp;
    double pvbpBar = convertedPv / (pvbp * pvbp);
    PointSensitivityBuilder pvbpDr = legPricer.pvbpSensitivity(referenceLeg, provider);
    return convertedPvDr.multipliedBy(convertedPvBar).combinedWith(pvbpDr.multipliedBy(pvbpBar));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future cash flows of the swap product.
   * <p>
   * Each expected cash flow is added to the result.
   * This is based on {@link #forecastValue(ResolvedSwap, RatesProvider)}.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the cash flow
   */
  public CashFlows cashFlows(ResolvedSwap swap, RatesProvider provider) {
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
   * @param swap  the product
   * @param provider  the rates provider
   * @return the explanatory information
   */
  public ExplainMap explainPresentValue(ResolvedSwap swap, RatesProvider provider) {
    ExplainMapBuilder builder = ExplainMap.builder();
    builder.put(ExplainKey.ENTRY_TYPE, "Swap");
    for (ResolvedSwapLeg leg : swap.getLegs()) {
      builder.addListEntryWithIndex(
          ExplainKey.LEGS, child -> legPricer.explainPresentValueInternal(leg, provider, child));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the swap product.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the currency exposure of the swap product
   */
  public MultiCurrencyAmount currencyExposure(ResolvedSwap swap, RatesProvider provider) {
    MultiCurrencyAmount ce = MultiCurrencyAmount.empty();
    for (ResolvedSwapLeg leg : swap.getLegs()) {
      ce = ce.plus(legPricer.currencyExposure(leg, provider));
    }
    return ce;
  }

  /**
   * Calculates the current cash of the swap product.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the current cash of the swap product
   */
  public MultiCurrencyAmount currentCash(ResolvedSwap swap, RatesProvider provider) {
    MultiCurrencyAmount ce = MultiCurrencyAmount.empty();
    for (ResolvedSwapLeg leg : swap.getLegs()) {
      ce = ce.plus(legPricer.currentCash(leg, provider));
    }
    return ce;
  }

  // finds a fixed overnight compounded annual rate computation if present
  private Optional<FixedOvernightCompoundedAnnualRateComputation> findAnnualRateComputation(ResolvedSwapLeg fixedLeg) {
    SwapPaymentPeriod firstPeriod = fixedLeg.getPaymentPeriods().get(0);
    if (firstPeriod instanceof RatePaymentPeriod) {
      RatePaymentPeriod payment = (RatePaymentPeriod) firstPeriod;
      RateAccrualPeriod firstAccrualPeriod = payment.getAccrualPeriods().get(0);
      if (firstAccrualPeriod.getRateComputation() instanceof FixedOvernightCompoundedAnnualRateComputation) {
        return Optional.of((FixedOvernightCompoundedAnnualRateComputation) firstAccrualPeriod.getRateComputation());
      }
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  // checking that at least one leg is a fixed leg and returning the first one
  private ResolvedSwapLeg fixedLeg(ResolvedSwap swap) {
    List<ResolvedSwapLeg> fixedLegs = swap.getLegs(SwapLegType.FIXED);
    if (fixedLegs.isEmpty()) {
      throw new IllegalArgumentException("Swap must contain a fixed leg");
    }
    return fixedLegs.get(0);
  }

  // Checks if the leg is a fixed leg with one payment and compounding
  // This type of leg is used in zero-coupon inflation swaps
  // When returning a 'true' for the first element, the second element is the number of periods which are used in 
  //   par rate/spread computation and the third element is the common fixed rate
  private Triple<Boolean, Integer, Double> checkFixedCompounded(ResolvedSwapLeg leg) {
    if (leg.getPaymentEvents().size() != 0) {
      return Triple.of(false, 0, 0.0d); // No event
    }
    RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) leg.getPaymentPeriods().get(0);
    if (ratePaymentPeriod.getCompoundingMethod() == CompoundingMethod.NONE) {
      return Triple.of(false, 0, 0.0d); // Should be compounded
    }
    ImmutableList<RateAccrualPeriod> accrualPeriods = ratePaymentPeriod.getAccrualPeriods();
    int nbAccrualPeriods = accrualPeriods.size();
    double fixedRate = 0;
    for (int i = 0; i < nbAccrualPeriods; i++) {
      if (!(accrualPeriods.get(i).getRateComputation() instanceof FixedRateComputation)) {
        return Triple.of(false, 0, 0.0d); // Should be fixed period
      }
      if ((i > 0) && (((FixedRateComputation) accrualPeriods.get(i).getRateComputation()).getRate() != fixedRate)) {
        return Triple.of(false, 0, 0.0d); // All fixed rates should be the same
      }
      fixedRate = ((FixedRateComputation) accrualPeriods.get(i).getRateComputation()).getRate();
      if (accrualPeriods.get(i).getSpread() != 0) {
        return Triple.of(false, 0, 0.0d); // Should have no spread
      }
      if (accrualPeriods.get(i).getGearing() != 1.0d) {
        return Triple.of(false, 0, 0.0d); // Should have a gearing of 1.
      }
      if (accrualPeriods.get(i).getYearFraction() != 1.0d) {
        return Triple.of(false, 0, 0.0d); // Should have a year fraction of 1.
      }
    }
    return Triple.of(true, nbAccrualPeriods, fixedRate);
  }

}
