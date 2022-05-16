/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBond;
import com.opengamma.strata.product.bond.ResolvedFixedCouponBondOption;

/**
 * Pricer for fixed coupon bond options based on Black formula for the (dirty) bond price.
 * <p>
 * The volatilities are stored in a (normalized) BondYieldVolatilities. They are stored as 
 * bond yield equivalent volatilities and are converted to bond price volatilities through the formula 
 *     "price volatility = duration * yield volatility".
 */
public class BlackFixedCouponBondOptionPricer {

  /**
   * Pricer for {@link ResolvedFixedCouponBond}.
   */
  private final DiscountingFixedCouponBondProductPricer bondPricer;

  /**
   * Creates an instance.
   * 
   * @param bondPricer  the pricer for the underlying {@link ResolvedFixedCouponBond}.
   */
  public BlackFixedCouponBondOptionPricer(DiscountingFixedCouponBondProductPricer bondPricer) {
    this.bondPricer = ArgChecker.notNull(bondPricer, "bondPricer");
  }

  /**
   * Default implementation.
   */
  public static final BlackFixedCouponBondOptionPricer DEFAULT = new BlackFixedCouponBondOptionPricer(
      DiscountingFixedCouponBondProductPricer.DEFAULT);

  //-------------------------------------------------------------------------
  /**
   * Gets the bond pricer.
   * 
   * @return the bond pricer
   */
  protected DiscountingFixedCouponBondProductPricer getBondPricer() {
    return bondPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the bond option.
   * <p>
   * The volatilities are stored as yield volatilities. They are converted to bond volatilities using the 
   * approximated formula "price volatility = duration * yield volatility".
   * 
   * @param bondOption  the bond option
   * @param legalEntityProvider  the provider to value the bond
   * @param volatilities  the volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedFixedCouponBondOption bondOption,
      LegalEntityDiscountingProvider legalEntityProvider,
      BondYieldVolatilities volatilities) {

    Currency ccy = bondOption.getCurrency();
    double expiry = volatilities.relativeTime(bondOption.getExpiry());
    ResolvedFixedCouponBond underlying = bondOption.getUnderlying();
    LocalDate settlementDate = bondOption.getSettlement().getSettlementDate();
    double cleanPriceStrike = bondOption.getSettlement().getPrice();
    double dirtyPriceStrike = bondPricer.dirtyPriceFromCleanPrice(underlying, settlementDate, cleanPriceStrike);
    double yieldStrike = bondPricer.yieldFromDirtyPrice(underlying, settlementDate, dirtyPriceStrike);
    double dirtyPriceSettle = bondPricer.dirtyPriceFromCurves(underlying, legalEntityProvider, settlementDate);
    double yieldSettle = bondPricer.yieldFromDirtyPrice(underlying, settlementDate, dirtyPriceSettle);
    double modifiedDurationSettle = bondPricer.modifiedDurationFromYield(underlying, settlementDate, yieldSettle);
    double priceVolatility = volatilities
        .priceVolatilityEquivalent(expiry, modifiedDurationSettle, yieldStrike, yieldSettle);
    boolean isCall = bondOption.getQuantity() > 0.0d; // Call = right to buy
    double dfSettle = legalEntityProvider
        .repoCurveDiscountFactors(underlying.getSecurityId(), underlying.getLegalEntityId(), ccy)
        .discountFactor(settlementDate);
    double price = dfSettle *
        BlackFormulaRepository.price(dirtyPriceSettle, dirtyPriceStrike, expiry, priceVolatility, isCall);
    return CurrencyAmount.of(ccy, price * Math.abs(bondOption.getQuantity()) * bondOption.getLongShort().sign());
  }

  /**
   * Returns the present value sensitivity to the underlying curves.
   * <p>
   * The sensitivity is computed with "sticky strike" volatility, i.e. the volatility used in the Black formula
   * is not impacted by the curve-implied change in moneyness.
   * <p>
   * The volatilities are stored as yield volatilities. They are converted to bond volatilities using the 
   * approximated formula "price volatility = duration * yield volatility".
   * 
   * @param bondOption  the bond option
   * @param legalEntityProvider  the provider to value the bond
   * @param volatilities  the volatilities
   * @return the present value sensitivity to rates
   */
  public PointSensitivities presentValueSensitivityRatesStickyStrike(
      ResolvedFixedCouponBondOption bondOption,
      LegalEntityDiscountingProvider legalEntityProvider,
      BondYieldVolatilities volatilities) {

    Currency ccy = bondOption.getCurrency();
    double expiry = volatilities.relativeTime(bondOption.getExpiry());
    ResolvedFixedCouponBond underlying = bondOption.getUnderlying();
    LocalDate settlementDate = bondOption.getSettlement().getSettlementDate();
    double cleanPriceStrike = bondOption.getSettlement().getPrice();
    double dirtyPriceStrike = bondPricer.dirtyPriceFromCleanPrice(underlying, settlementDate, cleanPriceStrike);
    double yieldStrike = bondPricer.yieldFromDirtyPrice(underlying, settlementDate, dirtyPriceStrike);
    double dirtyPriceSettle = bondPricer.dirtyPriceFromCurves(underlying, legalEntityProvider, settlementDate);
    ValueDerivatives yieldSettle = bondPricer.yieldFromDirtyPriceAd(underlying, settlementDate, dirtyPriceSettle);
    ValueDerivatives modifiedDurationSettle =
        bondPricer.modifiedDurationFromYieldAd(underlying, settlementDate, yieldSettle.getValue());
    ValueDerivatives priceVolatility = volatilities
        .priceVolatilityEquivalentAd(expiry, modifiedDurationSettle.getValue(), yieldStrike, yieldSettle.getValue());
    boolean isCall = bondOption.getQuantity() > 0.0d; // Call = right to buy
    double dfSettle = legalEntityProvider
        .repoCurveDiscountFactors(underlying.getSecurityId(), underlying.getLegalEntityId(), ccy)
        .discountFactor(settlementDate);
    ValueDerivatives black = BlackFormulaRepository
        .priceAdjoint(dirtyPriceSettle, dirtyPriceStrike, expiry, priceVolatility.getValue(), isCall);
    // Backward sweep
    double pvBar = 1.0;
    double priceBar = Math.abs(bondOption.getQuantity()) * bondOption.getLongShort().sign() * pvBar;
    double dfSettleBar = black.getValue() * priceBar;
    double blackBar = dfSettle * priceBar;
    double dirtyPriceSettleBar = black.getDerivative(0) * blackBar;
    double priceVolatilityBar = black.getDerivative(3) * blackBar;
    double modifiedDurationSettleBar = priceVolatility.getDerivative(0) * priceVolatilityBar;
    double yieldSettleBar = modifiedDurationSettle.getDerivative(0) * modifiedDurationSettleBar;
    dirtyPriceSettleBar += yieldSettle.getDerivative(0) * yieldSettleBar;
    PointSensitivityBuilder sensitivity = PointSensitivityBuilder.none();
    RepoCurveZeroRateSensitivity dfSettleDr = legalEntityProvider
        .repoCurveDiscountFactors(underlying.getSecurityId(), underlying.getLegalEntityId(), ccy)
        .zeroRatePointSensitivity(settlementDate);
    sensitivity = sensitivity.combinedWith(dfSettleDr.multipliedBy(dfSettleBar));
    PointSensitivityBuilder dirtyPriceSettleDr =
        bondPricer.dirtyPriceSensitivity(underlying, legalEntityProvider, settlementDate);
    sensitivity = sensitivity.combinedWith(dirtyPriceSettleDr.multipliedBy(dirtyPriceSettleBar));
    return sensitivity.build();
  }

  /**
   * Returns the present value sensitivity to the underlying yield volatilities.
   * <p>
   * The sensitivity is to the underlying yield volatilities, before they are transformed to bond price 
   * volatilities as described below.
   * <p>
   * The volatilities are stored as yield volatilities. They are converted to bond volatilities using the 
   * approximated formula "price volatility = duration * yield volatility".
   * 
   * @param bondOption  the bond option
   * @param legalEntityProvider  the provider to value the bond
   * @param volatilities  the volatilities
   * @return the present value sensitivity to the yield volatility parameters
   */
  public BondYieldSensitivity presentValueSensitivityModelParamsVolatility(
      ResolvedFixedCouponBondOption bondOption,
      LegalEntityDiscountingProvider legalEntityProvider,
      BondYieldVolatilities volatilities) {

    Currency ccy = bondOption.getCurrency();
    double expiry = volatilities.relativeTime(bondOption.getExpiry());
    ResolvedFixedCouponBond underlying = bondOption.getUnderlying();
    LocalDate settlementDate = bondOption.getSettlement().getSettlementDate();
    double cleanPriceStrike = bondOption.getSettlement().getPrice();
    double dirtyPriceStrike = bondPricer.dirtyPriceFromCleanPrice(underlying, settlementDate, cleanPriceStrike);
    double yieldStrike = bondPricer.yieldFromDirtyPrice(underlying, settlementDate, dirtyPriceStrike);
    double dirtyPriceSettle = bondPricer.dirtyPriceFromCurves(underlying, legalEntityProvider, settlementDate);
    double yieldSettle = bondPricer.yieldFromDirtyPrice(underlying, settlementDate, dirtyPriceSettle);
    double modifiedDurationSettle = bondPricer.modifiedDurationFromYield(underlying, settlementDate, yieldSettle);
    ValueDerivatives priceVolatility = volatilities
        .priceVolatilityEquivalentAd(expiry, modifiedDurationSettle, yieldStrike, yieldSettle);
    boolean isCall = bondOption.getQuantity() > 0.0d; // Call = right to buy
    double dfSettle = legalEntityProvider
        .repoCurveDiscountFactors(underlying.getSecurityId(), underlying.getLegalEntityId(), ccy)
        .discountFactor(settlementDate);
    ValueDerivatives black = BlackFormulaRepository
        .priceAdjoint(dirtyPriceSettle, dirtyPriceStrike, expiry, priceVolatility.getValue(), isCall);
    // Backward sweep
    double pvBar = 1.0;
    double priceBar = pvBar * Math.abs(bondOption.getQuantity()) * bondOption.getLongShort().sign();
    double blackBar = dfSettle * priceBar;
    double priceVolatilityBar = black.getDerivative(3) * blackBar;
    double yieldVolatilityBar = priceVolatility.getDerivative(1) * priceVolatilityBar;
    return BondYieldSensitivity
        .of(volatilities.getName(), expiry, modifiedDurationSettle, yieldStrike, yieldSettle, ccy, yieldVolatilityBar);
  }

}
