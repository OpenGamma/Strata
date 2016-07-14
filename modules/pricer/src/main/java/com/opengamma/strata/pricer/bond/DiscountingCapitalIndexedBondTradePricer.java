/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.CompoundedRateType;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.bond.BondPaymentPeriod;
import com.opengamma.strata.product.bond.CapitalIndexedBondPaymentPeriod;
import com.opengamma.strata.product.bond.CapitalIndexedBondYieldConvention;
import com.opengamma.strata.product.bond.KnownAmountBondPaymentPeriod;
import com.opengamma.strata.product.bond.ResolvedCapitalIndexedBond;
import com.opengamma.strata.product.bond.ResolvedCapitalIndexedBondTrade;

/**
 * Pricer for for capital index bond trades.
 * <p>
 * This function provides the ability to price a {@link ResolvedCapitalIndexedBondTrade}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bonds in the trade model, pricers and market data.
 * For example, a price of 99.32% is represented in Strata by 0.9932.
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
   * @return the present value of the bond trade
   */
  public CurrencyAmount presentValue(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    LocalDate settlementDate = trade.getSettlementDate();
    CurrencyAmount pvProduct = productPricer.presentValue(trade.getProduct(), ratesProvider,
        issuerDiscountFactorsProvider, settlementDate);
    return presentValueFromProductPresentValue(trade, ratesProvider, issuerDiscountFactorsProvider, pvProduct);
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
   * @return the present value of the bond trade
   */
  public CurrencyAmount presentValueWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    LocalDate settlementDate = trade.getSettlementDate();
    CurrencyAmount pvProduct = productPricer.presentValueWithZSpread(
        trade.getProduct(), ratesProvider,
        issuerDiscountFactorsProvider,
        settlementDate,
        zSpread,
        compoundedRateType,
        periodsPerYear);
    return presentValueFromProductPresentValue(trade, ratesProvider, issuerDiscountFactorsProvider, pvProduct);
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
   * @return the present value sensitivity of the bond trade
   */
  public PointSensitivities presentValueSensitivity(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    LocalDate settlementDate = trade.getSettlementDate();
    PointSensitivityBuilder productSensi = productPricer.presentValueSensitivity(trade.getProduct(),
        ratesProvider, issuerDiscountFactorsProvider, settlementDate);
    return presentValueSensitivityFromProductPresentValueSensitivity(
        trade, ratesProvider, issuerDiscountFactorsProvider, productSensi).build();
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
   * @return the present value sensitivity of the bond trade
   */
  public PointSensitivities presentValueSensitivityWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    LocalDate settlementDate = trade.getSettlementDate();
    PointSensitivityBuilder productSensi = productPricer.presentValueSensitivityWithZSpread(trade.getProduct(),
        ratesProvider, issuerDiscountFactorsProvider, settlementDate, zSpread, compoundedRateType, periodsPerYear);
    return presentValueSensitivityFromProductPresentValueSensitivity(
        trade, ratesProvider, issuerDiscountFactorsProvider, productSensi).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the bond trade from the clean price.
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
    LocalDate standardSettlementDate = bond.calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData);
    LocalDate tradeSettlementDate = trade.getSettlementDate();
    StandardId legalEntityId = bond.getLegalEntityId();
    Currency currency = bond.getCurrency();
    double df = issuerDiscountFactorsProvider
        .repoCurveDiscountFactors(bond.getSecurityId(), legalEntityId, currency).discountFactor(standardSettlementDate);
    CurrencyAmount pvStandard = forecastValueStandardFromCleanPrice(
        bond, ratesProvider, standardSettlementDate, cleanRealPrice).multipliedBy(df);
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueFromProductPresentValue(trade, ratesProvider, issuerDiscountFactorsProvider, pvStandard);
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
    return presentValueFromProductPresentValue(
        trade, ratesProvider, issuerDiscountFactorsProvider, pvStandard.plus(pvDiff));
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
    LocalDate standardSettlementDate = bond.calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData);
    LocalDate tradeSettlementDate = trade.getSettlementDate();
    StandardId legalEntityId = bond.getLegalEntityId();
    Currency currency = bond.getCurrency();
    double df = issuerDiscountFactorsProvider
        .repoCurveDiscountFactors(bond.getSecurityId(), legalEntityId, currency).discountFactor(standardSettlementDate);
    CurrencyAmount pvStandard = forecastValueStandardFromCleanPrice(
        bond, ratesProvider, standardSettlementDate, cleanRealPrice).multipliedBy(df);
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueFromProductPresentValue(trade, ratesProvider, issuerDiscountFactorsProvider, pvStandard);
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
    return presentValueFromProductPresentValue(
        trade, ratesProvider, issuerDiscountFactorsProvider, pvStandard.plus(pvDiff));
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
  public PointSensitivities presentValueSensitivityFromCleanPrice(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice) {

    validate(ratesProvider, issuerDiscountFactorsProvider);
    ResolvedCapitalIndexedBond bond = trade.getProduct();
    LocalDate standardSettlementDate = bond.calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData);
    LocalDate tradeSettlementDate = trade.getSettlementDate();
    StandardId legalEntityId = bond.getLegalEntityId();
    Currency currency = bond.getCurrency();
    RepoCurveDiscountFactors repoDiscountFactors =
        issuerDiscountFactorsProvider.repoCurveDiscountFactors(bond.getSecurityId(), legalEntityId, currency);
    double df = repoDiscountFactors.discountFactor(standardSettlementDate);
    PointSensitivityBuilder dfSensi = repoDiscountFactors.zeroRatePointSensitivity(standardSettlementDate);
    PointSensitivityBuilder pvSensiStandard = forecastValueSensitivityStandardFromCleanPrice(bond, ratesProvider,
        standardSettlementDate, cleanRealPrice).multipliedBy(df).combinedWith(dfSensi.multipliedBy(
        forecastValueStandardFromCleanPrice(bond, ratesProvider, standardSettlementDate, cleanRealPrice)
                .getAmount()));
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueSensitivityFromProductPresentValueSensitivity(
          trade, ratesProvider, issuerDiscountFactorsProvider, pvSensiStandard).build();
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
    return presentValueSensitivityFromProductPresentValueSensitivity(
        trade, ratesProvider, issuerDiscountFactorsProvider, pvSensiStandard.combinedWith(pvSensiDiff)).build();
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
  public PointSensitivities presentValueSensitivityFromCleanPriceWithZSpread(
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
    LocalDate standardSettlementDate = bond.calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData);
    LocalDate tradeSettlementDate = trade.getSettlementDate();
    StandardId legalEntityId = bond.getLegalEntityId();
    Currency currency = bond.getCurrency();
    RepoCurveDiscountFactors repoDiscountFactors =
        issuerDiscountFactorsProvider.repoCurveDiscountFactors(bond.getSecurityId(), legalEntityId, currency);
    double df = repoDiscountFactors.discountFactor(standardSettlementDate);
    PointSensitivityBuilder dfSensi = repoDiscountFactors.zeroRatePointSensitivity(standardSettlementDate);
    PointSensitivityBuilder pvSensiStandard = forecastValueSensitivityStandardFromCleanPrice(bond, ratesProvider,
        standardSettlementDate, cleanRealPrice).multipliedBy(df).combinedWith(dfSensi.multipliedBy(
        forecastValueStandardFromCleanPrice(bond, ratesProvider, standardSettlementDate, cleanRealPrice)
                .getAmount()));
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueSensitivityFromProductPresentValueSensitivity(
          trade, ratesProvider, issuerDiscountFactorsProvider, pvSensiStandard).build();
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
    return presentValueSensitivityFromProductPresentValueSensitivity(
        trade, ratesProvider, issuerDiscountFactorsProvider, pvSensiStandard.combinedWith(pvSensiDiff)).build();
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
  public MultiCurrencyAmount currencyExposureFromCleanPrice(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice) {

    CurrencyAmount pv = presentValueFromCleanPrice(
        trade, ratesProvider, issuerDiscountFactorsProvider, refData, cleanRealPrice);
    return MultiCurrencyAmount.of(pv);
  }

  /**
   * Calculates the currency exposure of the bond trade.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @return the currency exposure of the trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData) {

    CurrencyAmount pv = presentValue(trade, ratesProvider, issuerDiscountFactorsProvider, refData);
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
  public MultiCurrencyAmount currencyExposureFromCleanPriceWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double cleanRealPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    CurrencyAmount pv = presentValueFromCleanPriceWithZSpread(
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
   * Calculates the currency exposure of the bond trade with z-spread.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param issuerDiscountFactorsProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the currency exposure of the trade
   */
  public MultiCurrencyAmount currencyExposureWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      ReferenceData refData,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    CurrencyAmount pv = presentValueWithZSpread(
        trade,
        ratesProvider,
        issuerDiscountFactorsProvider,
        refData,
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
   * @return the current cash
   */
  public CurrencyAmount currentCash(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider) {

    LocalDate valuationDate = ratesProvider.getValuationDate();
    LocalDate settlementDate = trade.getSettlementDate();
    BondPaymentPeriod settle = trade.getSettlement();
    CurrencyAmount cashProduct = productPricer.currentCash(trade.getProduct(), ratesProvider, settlementDate);
    double cashSettle =
        settle.getPaymentDate().isEqual(valuationDate) ? netAmount(trade, ratesProvider).getAmount() : 0d;
    return cashProduct.plus(cashSettle);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the net amount of the settlement of the bond trade.
   * <p>
   * Since the sign of the settlement notional is opposite to that of the product, negative amount will be returned 
   * for positive quantity of trade.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @return the net amount
   */
  public CurrencyAmount netAmount(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider) {

    BondPaymentPeriod settlement = trade.getSettlement();
    if (settlement instanceof KnownAmountBondPaymentPeriod) {
      Payment payment = ((KnownAmountBondPaymentPeriod) settlement).getPayment();
      return payment.getValue();
    } else if (settlement instanceof CapitalIndexedBondPaymentPeriod) {
      CapitalIndexedBondPaymentPeriod casted = (CapitalIndexedBondPaymentPeriod) settlement;
      double netAmount = productPricer.getPeriodPricer().forecastValue(casted, ratesProvider);
      return CurrencyAmount.of(casted.getCurrency(), netAmount);
    }
    throw new UnsupportedOperationException("unsupported settlement type");
  }

  //-------------------------------------------------------------------------
  private CurrencyAmount presentValueSettlement(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider) {

    BondPaymentPeriod settlement = trade.getSettlement();
    ResolvedCapitalIndexedBond product = trade.getProduct();
    RepoCurveDiscountFactors discountFactors = issuerDiscountFactorsProvider.repoCurveDiscountFactors(
        product.getSecurityId(), product.getLegalEntityId(), product.getCurrency());
    return netAmount(trade, ratesProvider).multipliedBy(discountFactors.discountFactor(settlement.getPaymentDate()));
  }
  
  private CurrencyAmount presentValueFromProductPresentValue(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider, 
      CurrencyAmount productPresentValue) {

    CurrencyAmount pvProduct = productPresentValue.multipliedBy(trade.getQuantity());
    CurrencyAmount pvPayment = presentValueSettlement(trade, ratesProvider, issuerDiscountFactorsProvider);
    return pvProduct.plus(pvPayment);
  }

  CurrencyAmount forecastValueStandardFromCleanPrice(
      ResolvedCapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate standardSettlementDate,
      double realCleanPrice) {

    double notional = product.getNotional();
    double netAmountReal = realCleanPrice * notional + product.accruedInterest(standardSettlementDate);
    double indexRatio = product.getYieldConvention().equals(CapitalIndexedBondYieldConvention.GB_IL_FLOAT) ? 1d :
        productPricer.indexRatio(product, ratesProvider, standardSettlementDate);
    return CurrencyAmount.of(product.getCurrency(), indexRatio * netAmountReal);
  }

  //-------------------------------------------------------------------------
  private PointSensitivityBuilder netAmountSensitivity(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider) {

    BondPaymentPeriod settlement = trade.getSettlement();
    if (settlement instanceof KnownAmountBondPaymentPeriod) {
      return PointSensitivityBuilder.none();
    } else if (settlement instanceof CapitalIndexedBondPaymentPeriod) {
      CapitalIndexedBondPaymentPeriod casted = (CapitalIndexedBondPaymentPeriod) settlement;
      return productPricer.getPeriodPricer().forecastValueSensitivity(casted, ratesProvider);
    }
    throw new UnsupportedOperationException("unsupported settlement type");
  }

  private PointSensitivityBuilder presentValueSensitivitySettlement(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider) {

    BondPaymentPeriod settlement = trade.getSettlement();
    ResolvedCapitalIndexedBond product = trade.getProduct();
    RepoCurveDiscountFactors discountFactors = issuerDiscountFactorsProvider.repoCurveDiscountFactors(
        product.getSecurityId(), product.getLegalEntityId(), product.getCurrency());
    double df = discountFactors.discountFactor(settlement.getPaymentDate());
    double netAmount = netAmount(trade, ratesProvider).getAmount();
    PointSensitivityBuilder dfSensi =
        discountFactors.zeroRatePointSensitivity(settlement.getPaymentDate()).multipliedBy(netAmount);
    PointSensitivityBuilder naSensi = netAmountSensitivity(trade, ratesProvider).multipliedBy(df);
    return dfSensi.combinedWith(naSensi);
  }

  private PointSensitivityBuilder presentValueSensitivityFromProductPresentValueSensitivity(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider issuerDiscountFactorsProvider,
      PointSensitivityBuilder productPresnetValueSensitivity) {

    PointSensitivityBuilder sensiProduct = productPresnetValueSensitivity.multipliedBy(trade.getQuantity());
    PointSensitivityBuilder sensiPayment =
        presentValueSensitivitySettlement(trade, ratesProvider, issuerDiscountFactorsProvider);
    return sensiProduct.combinedWith(sensiPayment);
  }

  PointSensitivityBuilder forecastValueSensitivityStandardFromCleanPrice(
      ResolvedCapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate standardSettlementDate,
      double realCleanPrice) {

    if (product.getYieldConvention().equals(CapitalIndexedBondYieldConvention.GB_IL_FLOAT)) {
      return PointSensitivityBuilder.none();
    }
    double notional = product.getNotional();
    double netAmountReal = realCleanPrice * notional + product.accruedInterest(standardSettlementDate);
    PointSensitivityBuilder indexRatioSensi =
        productPricer.indexRatioSensitivity(product, ratesProvider, standardSettlementDate);
    return indexRatioSensi.multipliedBy(netAmountReal);
  }

  //-------------------------------------------------------------------------
  private void validate(RatesProvider ratesProvider, LegalEntityDiscountingProvider issuerDiscountFactorsProvider) {
    ArgChecker.isTrue(ratesProvider.getValuationDate().isEqual(issuerDiscountFactorsProvider.getValuationDate()),
        "the rates providers should be for the same date");
  }

}
