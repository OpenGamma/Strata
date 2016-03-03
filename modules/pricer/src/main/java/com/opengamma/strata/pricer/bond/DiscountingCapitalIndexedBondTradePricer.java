/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.CompoundedRateType;
import com.opengamma.strata.market.view.IssuerCurveDiscountFactors;
import com.opengamma.strata.market.view.RepoCurveDiscountFactors;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.bond.CapitalIndexedBondPaymentPeriod;
import com.opengamma.strata.product.bond.ResolvedCapitalIndexedBond;
import com.opengamma.strata.product.bond.ResolvedCapitalIndexedBondTrade;

/**
 * Pricer for for capital index bond trades.
 * <p>
 * This function provides the ability to price a {@link ResolvedCapitalIndexedBondTrade}.
 */
public class DiscountingCapitalIndexedBondTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingCapitalIndexedBondTradePricer DEFAULT =
      new DiscountingCapitalIndexedBondTradePricer(DiscountingCapitalIndexedBondProductPricer.DEFAULT);
  /**
   * Pricer for {@link ResolvedCapitalIndexedBond}.
   */
  private final DiscountingCapitalIndexedBondProductPricer productPricer;

  /**
   * Creates an instance. 
   * 
   * @param productPricer  pricer for {@link ResolvedCapitalIndexedBond}
   */
  public DiscountingCapitalIndexedBondTradePricer(DiscountingCapitalIndexedBondProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the bond trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * Coupon payments of the underlying product are considered based on the settlement date of the trade. 
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param cleanRealPrice  the clean real price
   * @return the present value of the bond trade
   */
  public CurrencyAmount presentValue(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    LocalDate settlementDate = trade.getTradeInfo().getSettlementDate().get();
    CurrencyAmount pvProduct = productPricer.presentValue(trade.getProduct(), ratesProvider,
        issuerDiscountFactorsProvider, settlementDate);
    CurrencyAmount pvSettle = presentValueFromCleanPrice(
        trade, ratesProvider, issuerDiscountFactorsProvider, refData, cleanRealPrice);
    return pvProduct.multipliedBy(trade.getQuantity()).plus(pvSettle);
  }

  /**
   * Calculates the present value of the bond trade with z-spread.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * The result is expressed using the payment currency of the bond.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * <p>
   * Coupon payments of the underlying product are considered based on the settlement date of the trade. 
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @param cleanRealPrice  the clean real price
   * @return the present value of the bond trade
   */
  public CurrencyAmount presentValueWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    LocalDate settlementDate = trade.getTradeInfo().getSettlementDate().get();
    CurrencyAmount pvProduct = productPricer.presentValueWithZSpread(
        trade.getProduct(), ratesProvider,
        issuerDiscountFactorsProvider,
        settlementDate,
        zSpread,
        compoundedRateType,
        periodsPerYear);
    CurrencyAmount pvSettle = presentValueFromCleanPriceWithZSpread(
        trade,
        ratesProvider,
        issuerDiscountFactorsProvider,
        refData,
        cleanRealPrice,
        zSpread,
        compoundedRateType,
        periodsPerYear);
    return pvProduct.multipliedBy(trade.getQuantity()).plus(pvSettle);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the bond trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * Coupon payments of the underlying product are considered based on the settlement date of the trade. 
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param cleanRealPrice  the clean real price
   * @return the present value sensitivity of the bond trade
   */
  public PointSensitivityBuilder presentValueSensitivity(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    LocalDate settlementDate = trade.getTradeInfo().getSettlementDate().get();
    PointSensitivityBuilder productSensi = productPricer.presentValueSensitivity(trade.getProduct(),
        ratesProvider, issuerDiscountFactorsProvider, settlementDate).multipliedBy(trade.getQuantity());
    PointSensitivityBuilder settleSensi = presentValueSensitivityFromRealCleanPrice(
        trade, ratesProvider, issuerDiscountFactorsProvider, refData, cleanRealPrice);
    return productSensi.combinedWith(settleSensi);
  }

  /**
   * Calculates the present value sensitivity of the bond trade with z-spread.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * Coupon payments of the underlying product are considered based on the settlement date of the trade. 
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @param cleanRealPrice  the clean real price
   * @return the present value sensitivity of the bond trade
   */
  public PointSensitivityBuilder presentValueSensitivityWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    LocalDate settlementDate = trade.getTradeInfo().getSettlementDate().get();
    PointSensitivityBuilder productSensi = productPricer.presentValueSensitivityWithZSpread(trade.getProduct(),
        ratesProvider, issuerDiscountFactorsProvider, settlementDate, zSpread, compoundedRateType, periodsPerYear)
        .multipliedBy(trade.getQuantity());
    PointSensitivityBuilder settleSensi = presentValueSensitivityFromRealCleanPriceWithZSpread(
        trade,
        ratesProvider,
        issuerDiscountFactorsProvider,
        refData,
        cleanRealPrice,
        zSpread,
        compoundedRateType,
        periodsPerYear);
    return productSensi.combinedWith(settleSensi);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the settlement of the bond trade from the clean price.
   * <p>
   * Since the sign of the settlement notional is opposite to that of the product, negative amount will be returned 
   * for positive quantity of trade.  
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param refData  the reference data used to calculate the settlement date
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param cleanRealPrice  the clean real price
   * @return the present value of the settlement
   */
  public CurrencyAmount presentValueFromCleanPrice(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    ResolvedCapitalIndexedBond bond = trade.getProduct();
    LocalDate standardSettlementDate = bond.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData);
    LocalDate tradeSettlementDate = trade.getTradeInfo().getSettlementDate().get();
    StandardId securityId = trade.getSecurityStandardId();
    StandardId legalEntityId = bond.getLegalEntityId();
    Currency currency = bond.getCurrency();
    double df = issuerDiscountFactorsProvider
        .repoCurveDiscountFactors(securityId, legalEntityId, currency).discountFactor(standardSettlementDate);
    CurrencyAmount pvStandard =
        netAmountStandard(trade, ratesProvider, standardSettlementDate, cleanRealPrice).multipliedBy(df);
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return pvStandard;
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors discountFactors =
        issuerDiscountFactorsProvider.issuerCurveDiscountFactors(legalEntityId, currency);
    double pvDiff = 0d;
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvDiff = -productPricer.presentValueCoupon(
          bond, ratesProvider, discountFactors, tradeSettlementDate, standardSettlementDate);
    } else {
      pvDiff = productPricer.presentValueCoupon(
          bond, ratesProvider, discountFactors, standardSettlementDate, tradeSettlementDate);
    }
    return pvStandard.plus(pvDiff * trade.getQuantity());
  }

  /**
   * Calculates the present value of the settlement of the bond trade from the clean price with z-spread.
   * <p>
   * Since the sign of the settlement notional is opposite to that of the product, negative amount will be returned 
   * for positive quantity of trade.  
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic
   * compounded rates of the discounting curve.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @param cleanRealPrice  the clean real price
   * @return the present value of the settlement
   */
  public CurrencyAmount presentValueFromCleanPriceWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    ResolvedCapitalIndexedBond bond = trade.getProduct();
    LocalDate standardSettlementDate = bond.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData);
    LocalDate tradeSettlementDate = trade.getTradeInfo().getSettlementDate().get();
    StandardId securityId = trade.getSecurityStandardId();
    StandardId legalEntityId = bond.getLegalEntityId();
    Currency currency = bond.getCurrency();
    double df = issuerDiscountFactorsProvider
        .repoCurveDiscountFactors(securityId, legalEntityId, currency).discountFactor(standardSettlementDate);
    CurrencyAmount pvStandard =
        netAmountStandard(trade, ratesProvider, standardSettlementDate, cleanRealPrice).multipliedBy(df);
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return pvStandard;
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors discountFactors =
        issuerDiscountFactorsProvider.issuerCurveDiscountFactors(legalEntityId, currency);
    double pvDiff = 0d;
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvDiff = -productPricer.presentValueCouponWithZSpread(
          bond,
          ratesProvider,
          discountFactors,
          tradeSettlementDate,
          standardSettlementDate,
          zSpread,
          compoundedRateType,
          periodsPerYear);
    } else {
      pvDiff = productPricer.presentValueCouponWithZSpread(
          bond,
          ratesProvider,
          discountFactors,
          standardSettlementDate,
          tradeSettlementDate,
          zSpread,
          compoundedRateType,
          periodsPerYear);
    }
    return pvStandard.plus(pvDiff * trade.getQuantity());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the settlement of the bond trade from the real clean price.
   * <p>
   * The present value sensitivity of the settlement is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param refData  the reference data used to calculate the settlement date
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param cleanRealPrice  the clean real price
   * @return the present value sensitivity of the settlement
   */
  public PointSensitivityBuilder presentValueSensitivityFromRealCleanPrice(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    ResolvedCapitalIndexedBond bond = trade.getProduct();
    LocalDate standardSettlementDate = bond.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData);
    LocalDate tradeSettlementDate = trade.getTradeInfo().getSettlementDate().get();
    StandardId securityId = trade.getSecurityStandardId();
    StandardId legalEntityId = bond.getLegalEntityId();
    Currency currency = bond.getCurrency();
    RepoCurveDiscountFactors repoDiscountFactors =
        issuerDiscountFactorsProvider.repoCurveDiscountFactors(securityId, legalEntityId, currency);
    double df = repoDiscountFactors.discountFactor(standardSettlementDate);
    PointSensitivityBuilder dfSensi = repoDiscountFactors.zeroRatePointSensitivity(standardSettlementDate);
    PointSensitivityBuilder pvSensiStandard = netAmountStandardSensitivity(
        trade, ratesProvider, standardSettlementDate, cleanRealPrice).multipliedBy(df).combinedWith(
        dfSensi.multipliedBy(netAmountStandard(trade, ratesProvider, standardSettlementDate, cleanRealPrice).getAmount()));
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return pvSensiStandard;
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors issuerDiscountFactors =
        issuerDiscountFactorsProvider.issuerCurveDiscountFactors(legalEntityId, currency);
    PointSensitivityBuilder pvSensiDiff = PointSensitivityBuilder.none();
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvSensiDiff = pvSensiDiff.combinedWith(productPricer.presentValueSensitivityCoupon(bond, ratesProvider,
          issuerDiscountFactors, tradeSettlementDate, standardSettlementDate).multipliedBy(-1d));
    } else {
      pvSensiDiff = pvSensiDiff.combinedWith(productPricer.presentValueSensitivityCoupon(bond, ratesProvider,
          issuerDiscountFactors, standardSettlementDate, tradeSettlementDate));
    }
    return pvSensiStandard.combinedWith(pvSensiDiff.multipliedBy(trade.getQuantity()));
  }

  /**
   * Calculates the present value sensitivity of the settlement of the bond trade from the real clean price 
   * with z-spread.
   * <p>
   * The present value sensitivity of the settlement is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param refData  the reference data used to calculate the settlement date
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @param cleanRealPrice  the clean real price
   * @return the present value sensitivity of the settlement
   */
  public PointSensitivityBuilder presentValueSensitivityFromRealCleanPriceWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    ResolvedCapitalIndexedBond bond = trade.getProduct();
    LocalDate standardSettlementDate = bond.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData);
    LocalDate tradeSettlementDate = trade.getTradeInfo().getSettlementDate().get();
    StandardId securityId = trade.getSecurityStandardId();
    StandardId legalEntityId = bond.getLegalEntityId();
    Currency currency = bond.getCurrency();
    RepoCurveDiscountFactors repoDiscountFactors =
        issuerDiscountFactorsProvider.repoCurveDiscountFactors(securityId, legalEntityId, currency);
    double df = repoDiscountFactors.discountFactor(standardSettlementDate);
    PointSensitivityBuilder dfSensi = repoDiscountFactors.zeroRatePointSensitivity(standardSettlementDate);
    PointSensitivityBuilder pvSensiStandard = netAmountStandardSensitivity(
        trade, ratesProvider, standardSettlementDate, cleanRealPrice).multipliedBy(df).combinedWith(
        dfSensi.multipliedBy(netAmountStandard(trade, ratesProvider, standardSettlementDate, cleanRealPrice).getAmount()));
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return pvSensiStandard;
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors issuerDiscountFactors =
        issuerDiscountFactorsProvider.issuerCurveDiscountFactors(legalEntityId, currency);
    PointSensitivityBuilder pvSensiDiff = PointSensitivityBuilder.none();
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvSensiDiff = pvSensiDiff.combinedWith(productPricer.presentValueSensitivityCouponWithZSpread(
          bond,
          ratesProvider,
          issuerDiscountFactors,
          tradeSettlementDate,
          standardSettlementDate,
          zSpread,
          compoundedRateType,
          periodsPerYear)
          .multipliedBy(-1d));
    } else {
      pvSensiDiff = pvSensiDiff.combinedWith(productPricer.presentValueSensitivityCouponWithZSpread(
          bond,
          ratesProvider,
          issuerDiscountFactors,
          standardSettlementDate,
          tradeSettlementDate,
          zSpread,
          compoundedRateType,
          periodsPerYear));
    }
    return pvSensiStandard.combinedWith(pvSensiDiff.multipliedBy(trade.getQuantity()));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the bond trade.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param cleanRealPrice  the clean real price
   * @return the currency exposure of the trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice) {

    CurrencyAmount pv = presentValue(trade, ratesProvider, issuerDiscountFactorsProvider, refData, cleanRealPrice);
    return MultiCurrencyAmount.of(pv);
  }

  /**
   * Calculates the currency exposure of the bond trade with z-spread.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @param cleanRealPrice  the clean real price
   * @return the currency exposure of the trade
   */
  public MultiCurrencyAmount currencyExposureWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    CurrencyAmount pv = presentValueWithZSpread(
        trade,
        ratesProvider,
        issuerDiscountFactorsProvider,
        refData,
        cleanRealPrice,
        zSpread,
        compoundedRateType,
        periodsPerYear);
    return MultiCurrencyAmount.of(pv);
  }

  /**
   * Calculates the current of the bond trade.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param realCleanPrice  the real clean price
   * @return the current cash
   */
  public CurrencyAmount currentCash(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      double realCleanPrice) {

    LocalDate valuationDate = ratesProvider.getValuationDate();
    LocalDate settlementDate = trade.getTradeInfo().getSettlementDate().get();
    CapitalIndexedBondPaymentPeriod settle = trade.getSettlement();
    CurrencyAmount cashProduct = productPricer.currentCash(trade.getProduct(), ratesProvider, settlementDate);
    double cashSettle = settle.getPaymentDate().isEqual(valuationDate) ?
        netAmount(trade, ratesProvider, realCleanPrice).getAmount() : 0d;
    return cashProduct.plus(cashSettle);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the net amount of the settlement of the bond trade from real clean price. 
   * <p>
   * Since the sign of the settlement notional is opposite to that of the product, negative amount will be returned 
   * for positive quantity of trade.  
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param realCleanPrice  the real clean price
   * @return the net amount
   */
  public CurrencyAmount netAmount(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      double realCleanPrice) {

    CapitalIndexedBondPaymentPeriod settlement = trade.getSettlement();
    LocalDate paymentDate = settlement.getPaymentDate();
    double notional = trade.getProduct().getNotional();
    double netAmountRealByUnit =
        realCleanPrice + productPricer.accruedInterest(trade.getProduct(), paymentDate) / notional;
    double netAmount = productPricer.getPeriodPricer().forecastValue(settlement, ratesProvider);
    return CurrencyAmount.of(settlement.getCurrency(), netAmount * netAmountRealByUnit);
  }

  private CurrencyAmount netAmountStandard(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LocalDate standardSettlementDate,
      double realCleanPrice) {

    CapitalIndexedBondPaymentPeriod settlement = trade.getSettlement();
    double notional = trade.getProduct().getNotional();
    double netAmountRealByUnit =
        realCleanPrice + productPricer.accruedInterest(trade.getProduct(), standardSettlementDate) / notional;
    double indexRatio = productPricer.indexRatio(trade.getProduct(), ratesProvider, standardSettlementDate);
    return CurrencyAmount.of(settlement.getCurrency(), indexRatio * netAmountRealByUnit * settlement.getNotional());
  }

  /**
   * Calculates the net amount sensitivity of the settlement of the bond trade from real clean price. 
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param realCleanPrice  the real clean price
   * @return the net amount sensitivity
   */
  public PointSensitivityBuilder netAmountSensitivity(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      double realCleanPrice) {

    CapitalIndexedBondPaymentPeriod settlement = trade.getSettlement();
    LocalDate paymentDate = settlement.getPaymentDate();
    double notional = trade.getProduct().getNotional();
    double netAmountRealByUnit =
        realCleanPrice + productPricer.accruedInterest(trade.getProduct(), paymentDate) / notional;
    PointSensitivityBuilder netAmountSensi =
        productPricer.getPeriodPricer().forecastValueSensitivity(settlement, ratesProvider);
    return netAmountSensi.multipliedBy(netAmountRealByUnit);
  }

  private PointSensitivityBuilder netAmountStandardSensitivity(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LocalDate standardSettlementDate,
      double realCleanPrice) {

    CapitalIndexedBondPaymentPeriod settlement = trade.getSettlement();
    double notional = trade.getProduct().getNotional();
    double netAmountRealByUnit =
        realCleanPrice + productPricer.accruedInterest(trade.getProduct(), standardSettlementDate) / notional;
    PointSensitivityBuilder indexRatioSensi =
        productPricer.indexRatioSensitivity(trade.getProduct(), ratesProvider, standardSettlementDate);
    return indexRatioSensi.multipliedBy(netAmountRealByUnit * settlement.getNotional());
  }

  //-------------------------------------------------------------------------
  private void validate(RatesProvider ratesProvider, LegalEntityDiscountingProvider issuerDiscountFactorsProvider) {
    ArgChecker.isTrue(ratesProvider.getValuationDate().isEqual(issuerDiscountFactorsProvider.getValuationDate()),
        "the rates providers should be for the same date");
  }

}
