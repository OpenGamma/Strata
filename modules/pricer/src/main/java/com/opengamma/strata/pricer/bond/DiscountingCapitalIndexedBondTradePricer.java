/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
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
import com.opengamma.strata.product.bond.ResolvedCapitalIndexedBondSettlement;
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
   * Gets the capital indexed bond product pricer.
   *
   * @return the product pricer
   */
  public DiscountingCapitalIndexedBondProductPricer getProductPricer() {
    return productPricer;
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
   * @param discountingProvider  the discount factors provider
   * @return the present value of the bond trade
   */
  public CurrencyAmount presentValue(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider) {

    validate(ratesProvider, discountingProvider);
    LocalDate settlementDate = settlementDate(trade, ratesProvider.getValuationDate());
    CurrencyAmount pvProduct = productPricer.presentValue(trade.getProduct(), ratesProvider,
        discountingProvider, settlementDate);
    return presentValueFromProductPresentValue(trade, ratesProvider, discountingProvider, pvProduct);
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
   * @param discountingProvider  the discount factors provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value of the bond trade
   */
  public CurrencyAmount presentValueWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, discountingProvider);
    LocalDate settlementDate = settlementDate(trade, ratesProvider.getValuationDate());
    CurrencyAmount pvProduct = productPricer.presentValueWithZSpread(
        trade.getProduct(), ratesProvider,
        discountingProvider,
        settlementDate,
        zSpread,
        compoundedRateType,
        periodsPerYear);
    return presentValueFromProductPresentValue(trade, ratesProvider, discountingProvider, pvProduct);
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
   * @param discountingProvider  the discount factors provider
   * @return the present value sensitivity of the bond trade
   */
  public PointSensitivities presentValueSensitivity(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider) {

    validate(ratesProvider, discountingProvider);
    LocalDate settlementDate = settlementDate(trade, ratesProvider.getValuationDate());
    PointSensitivityBuilder productSensi = productPricer.presentValueSensitivity(trade.getProduct(),
        ratesProvider, discountingProvider, settlementDate);
    return presentValueSensitivityFromProductPresentValueSensitivity(
        trade, ratesProvider, discountingProvider, productSensi).build();
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
   * @param discountingProvider  the discount factors provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the present value sensitivity of the bond trade
   */
  public PointSensitivities presentValueSensitivityWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, discountingProvider);
    LocalDate settlementDate = settlementDate(trade, ratesProvider.getValuationDate());
    PointSensitivityBuilder productSensi = productPricer.presentValueSensitivityWithZSpread(trade.getProduct(),
        ratesProvider, discountingProvider, settlementDate, zSpread, compoundedRateType, periodsPerYear);
    return presentValueSensitivityFromProductPresentValueSensitivity(
        trade, ratesProvider, discountingProvider, productSensi).build();
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
   * @param discountingProvider  the discount factors provider
   * @param cleanRealPrice  the clean real price
   * @return the present value of the settlement
   */
  public CurrencyAmount presentValueFromCleanPrice(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData,
      double cleanRealPrice) {

    validate(ratesProvider, discountingProvider);
    LocalDate valuationDate = ratesProvider.getValuationDate();
    ResolvedCapitalIndexedBond bond = trade.getProduct();
    LocalDate standardSettlementDate = bond.calculateSettlementDateFromValuation(valuationDate, refData);
    LocalDate tradeSettlementDate = settlementDate(trade, valuationDate);
    RepoCurveDiscountFactors repoDf = DiscountingCapitalIndexedBondProductPricer.repoCurveDf(bond, discountingProvider);
    double df = repoDf.discountFactor(standardSettlementDate);
    CurrencyAmount pvStandard = forecastValueStandardFromCleanPrice(
        bond, ratesProvider, standardSettlementDate, cleanRealPrice).multipliedBy(df);
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueFromProductPresentValue(trade, ratesProvider, discountingProvider, pvStandard);
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors issuerDf = DiscountingCapitalIndexedBondProductPricer.issuerCurveDf(bond, discountingProvider);
    double pvDiff = 0d;
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvDiff = -productPricer.presentValueCoupon(bond, ratesProvider, issuerDf, tradeSettlementDate, standardSettlementDate);
    } else {
      pvDiff = productPricer.presentValueCoupon(bond, ratesProvider, issuerDf, standardSettlementDate, tradeSettlementDate);
    }
    return presentValueFromProductPresentValue(
        trade, ratesProvider, discountingProvider, pvStandard.plus(pvDiff));
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
   * @param discountingProvider  the discount factors provider
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
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData,
      double cleanRealPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, discountingProvider);
    LocalDate valuationDate = ratesProvider.getValuationDate();
    ResolvedCapitalIndexedBond bond = trade.getProduct();
    LocalDate standardSettlementDate = bond.calculateSettlementDateFromValuation(valuationDate, refData);
    LocalDate tradeSettlementDate = settlementDate(trade, valuationDate);
    RepoCurveDiscountFactors repoDf = DiscountingCapitalIndexedBondProductPricer.repoCurveDf(bond, discountingProvider);
    double df = repoDf.discountFactor(standardSettlementDate);
    CurrencyAmount pvStandard = forecastValueStandardFromCleanPrice(
        bond, ratesProvider, standardSettlementDate, cleanRealPrice).multipliedBy(df);
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueFromProductPresentValue(trade, ratesProvider, discountingProvider, pvStandard);
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors issuerDf = DiscountingCapitalIndexedBondProductPricer.issuerCurveDf(bond, discountingProvider);
    double pvDiff = 0d;
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvDiff = -productPricer.presentValueCouponWithZSpread(
          bond,
          ratesProvider,
          issuerDf,
          tradeSettlementDate,
          standardSettlementDate,
          zSpread,
          compoundedRateType,
          periodsPerYear);
    } else {
      pvDiff = productPricer.presentValueCouponWithZSpread(
          bond,
          ratesProvider,
          issuerDf,
          standardSettlementDate,
          tradeSettlementDate,
          zSpread,
          compoundedRateType,
          periodsPerYear);
    }
    return presentValueFromProductPresentValue(
        trade, ratesProvider, discountingProvider, pvStandard.plus(pvDiff));
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
   * @param discountingProvider  the discount factors provider
   * @param cleanRealPrice  the clean real price
   * @return the present value sensitivity of the settlement
   */
  public PointSensitivities presentValueSensitivityFromCleanPrice(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData,
      double cleanRealPrice) {

    validate(ratesProvider, discountingProvider);
    LocalDate valuationDate = ratesProvider.getValuationDate();
    ResolvedCapitalIndexedBond bond = trade.getProduct();
    LocalDate standardSettlementDate = bond.calculateSettlementDateFromValuation(valuationDate, refData);
    LocalDate tradeSettlementDate = settlementDate(trade, valuationDate);
    RepoCurveDiscountFactors repoDf = DiscountingCapitalIndexedBondProductPricer.repoCurveDf(bond, discountingProvider);
    double df = repoDf.discountFactor(standardSettlementDate);
    PointSensitivityBuilder dfSensi = repoDf.zeroRatePointSensitivity(standardSettlementDate);
    PointSensitivityBuilder pvSensiStandard = forecastValueSensitivityStandardFromCleanPrice(bond, ratesProvider,
        standardSettlementDate, cleanRealPrice).multipliedBy(df).combinedWith(dfSensi.multipliedBy(
            forecastValueStandardFromCleanPrice(bond, ratesProvider, standardSettlementDate, cleanRealPrice)
                .getAmount()));
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueSensitivityFromProductPresentValueSensitivity(
          trade, ratesProvider, discountingProvider, pvSensiStandard).build();
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors issuerDf = DiscountingCapitalIndexedBondProductPricer.issuerCurveDf(bond, discountingProvider);
    PointSensitivityBuilder pvSensiDiff = PointSensitivityBuilder.none();
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvSensiDiff = pvSensiDiff.combinedWith(productPricer.presentValueSensitivityCoupon(bond, ratesProvider,
          issuerDf, tradeSettlementDate, standardSettlementDate).multipliedBy(-1d));
    } else {
      pvSensiDiff = pvSensiDiff.combinedWith(productPricer.presentValueSensitivityCoupon(bond, ratesProvider,
          issuerDf, standardSettlementDate, tradeSettlementDate));
    }
    return presentValueSensitivityFromProductPresentValueSensitivity(
        trade, ratesProvider, discountingProvider, pvSensiStandard.combinedWith(pvSensiDiff)).build();
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
   * @param discountingProvider  the discount factors provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @param cleanRealPrice  the clean real price
   * @return the present value sensitivity of the settlement
   */
  public PointSensitivities presentValueSensitivityFromCleanPriceWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData,
      double cleanRealPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    validate(ratesProvider, discountingProvider);
    LocalDate valuationDate = ratesProvider.getValuationDate();
    ResolvedCapitalIndexedBond bond = trade.getProduct();
    LocalDate standardSettlementDate = bond.calculateSettlementDateFromValuation(valuationDate, refData);
    LocalDate tradeSettlementDate = settlementDate(trade, valuationDate);
    RepoCurveDiscountFactors repoDf = DiscountingCapitalIndexedBondProductPricer.repoCurveDf(bond, discountingProvider);
    double df = repoDf.discountFactor(standardSettlementDate);
    PointSensitivityBuilder dfSensi = repoDf.zeroRatePointSensitivity(standardSettlementDate);
    PointSensitivityBuilder pvSensiStandard = forecastValueSensitivityStandardFromCleanPrice(bond, ratesProvider,
        standardSettlementDate, cleanRealPrice).multipliedBy(df).combinedWith(dfSensi.multipliedBy(
            forecastValueStandardFromCleanPrice(bond, ratesProvider, standardSettlementDate, cleanRealPrice)
                .getAmount()));
    if (standardSettlementDate.isEqual(tradeSettlementDate)) {
      return presentValueSensitivityFromProductPresentValueSensitivity(
          trade, ratesProvider, discountingProvider, pvSensiStandard).build();
    }
    // check coupon payment between two settlement dates
    IssuerCurveDiscountFactors issuerDf = DiscountingCapitalIndexedBondProductPricer.issuerCurveDf(bond, discountingProvider);
    PointSensitivityBuilder pvSensiDiff = PointSensitivityBuilder.none();
    if (standardSettlementDate.isAfter(tradeSettlementDate)) {
      pvSensiDiff = pvSensiDiff.combinedWith(productPricer.presentValueSensitivityCouponWithZSpread(
          bond,
          ratesProvider,
          issuerDf,
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
          issuerDf,
          standardSettlementDate,
          tradeSettlementDate,
          zSpread,
          compoundedRateType,
          periodsPerYear));
    }
    return presentValueSensitivityFromProductPresentValueSensitivity(
        trade, ratesProvider, discountingProvider, pvSensiStandard.combinedWith(pvSensiDiff)).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the bond trade.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @param refData  the reference data used to calculate the settlement date
   * @param cleanRealPrice  the clean real price
   * @return the currency exposure of the trade
   */
  public MultiCurrencyAmount currencyExposureFromCleanPrice(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData,
      double cleanRealPrice) {

    CurrencyAmount pv = presentValueFromCleanPrice(
        trade, ratesProvider, discountingProvider, refData, cleanRealPrice);
    return MultiCurrencyAmount.of(pv);
  }

  /**
   * Calculates the currency exposure of the bond trade.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
   * @return the currency exposure of the trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider) {

    CurrencyAmount pv = presentValue(trade, ratesProvider, discountingProvider);
    return MultiCurrencyAmount.of(pv);
  }

  /**
   * Calculates the currency exposure of the bond trade with z-spread.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @param discountingProvider  the discount factors provider
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
      LegalEntityDiscountingProvider discountingProvider,
      ReferenceData refData,
      double cleanRealPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    CurrencyAmount pv = presentValueFromCleanPriceWithZSpread(
        trade,
        ratesProvider,
        discountingProvider,
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
   * @param discountingProvider  the discount factors provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodsPerYear  the number of periods per year
   * @return the currency exposure of the trade
   */
  public MultiCurrencyAmount currencyExposureWithZSpread(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodsPerYear) {

    CurrencyAmount pv = presentValueWithZSpread(
        trade,
        ratesProvider,
        discountingProvider,
        zSpread,
        compoundedRateType,
        periodsPerYear);
    return MultiCurrencyAmount.of(pv);
  }

  /**
   * Calculates the current cash of the bond trade.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider, used to determine price index values
   * @return the current cash
   */
  public CurrencyAmount currentCash(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider) {

    LocalDate valuationDate = ratesProvider.getValuationDate();
    LocalDate settlementDate = settlementDate(trade, valuationDate);
    CurrencyAmount cashProduct = productPricer.currentCash(trade.getProduct(), ratesProvider, settlementDate);
    if (!trade.getSettlement().isPresent()) {
      return cashProduct;
    }
    BondPaymentPeriod settlePeriod = trade.getSettlement().get().getPayment();
    double cashSettle = settlePeriod.getPaymentDate().isEqual(valuationDate) ? netAmount(trade, ratesProvider).getAmount() : 0d;
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

    if (!trade.getSettlement().isPresent()) {
      // position has no settlement, thus it has no value
      return CurrencyAmount.zero(trade.getProduct().getCurrency());
    }
    BondPaymentPeriod settlePeriod = trade.getSettlement().get().getPayment();
    if (settlePeriod instanceof KnownAmountBondPaymentPeriod) {
      Payment payment = ((KnownAmountBondPaymentPeriod) settlePeriod).getPayment();
      return payment.getValue();
    } else if (settlePeriod instanceof CapitalIndexedBondPaymentPeriod) {
      CapitalIndexedBondPaymentPeriod casted = (CapitalIndexedBondPaymentPeriod) settlePeriod;
      double netAmount = productPricer.getPeriodPricer().forecastValue(casted, ratesProvider);
      return CurrencyAmount.of(casted.getCurrency(), netAmount);
    }
    throw new UnsupportedOperationException("unsupported settlement type");
  }

  //-------------------------------------------------------------------------
  private CurrencyAmount presentValueSettlement(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider) {

    if (!trade.getSettlement().isPresent()) {
      // position has no settlement, thus it has no value
      return CurrencyAmount.zero(trade.getProduct().getCurrency());
    }
    BondPaymentPeriod settlePeriod = trade.getSettlement().get().getPayment();
    ResolvedCapitalIndexedBond product = trade.getProduct();
    CurrencyAmount netAmount = netAmount(trade, ratesProvider);
    RepoCurveDiscountFactors repoDf = DiscountingCapitalIndexedBondProductPricer.repoCurveDf(product, discountingProvider);
    return netAmount.multipliedBy(repoDf.discountFactor(settlePeriod.getPaymentDate()));
  }

  private CurrencyAmount presentValueFromProductPresentValue(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      CurrencyAmount productPresentValue) {

    CurrencyAmount pvProduct = productPresentValue.multipliedBy(trade.getQuantity());
    CurrencyAmount pvPayment = presentValueSettlement(trade, ratesProvider, discountingProvider);
    return pvProduct.plus(pvPayment);
  }

  CurrencyAmount forecastValueStandardFromCleanPrice(
      ResolvedCapitalIndexedBond product,
      RatesProvider ratesProvider,
      LocalDate standardSettlementDate,
      double realCleanPrice) {

    double notional = product.getNotional();
    double netAmountReal = realCleanPrice * notional + product.accruedInterest(standardSettlementDate);
    double indexRatio = product.getYieldConvention().equals(CapitalIndexedBondYieldConvention.GB_IL_FLOAT) ?
        1d :
        productPricer.indexRatio(product, ratesProvider, standardSettlementDate);
    return CurrencyAmount.of(product.getCurrency(), indexRatio * netAmountReal);
  }

  //-------------------------------------------------------------------------
  // the sensitivity of the product plus settlement
  private PointSensitivityBuilder presentValueSensitivityFromProductPresentValueSensitivity(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider,
      PointSensitivityBuilder productPresnetValueSensitivity) {

    PointSensitivityBuilder sensiProduct = productPresnetValueSensitivity.multipliedBy(trade.getQuantity());
    PointSensitivityBuilder sensiPayment = presentValueSensitivitySettlement(trade, ratesProvider, discountingProvider);
    return sensiProduct.combinedWith(sensiPayment);
  }

  // the sensitivity of the present value of the settlement
  private PointSensitivityBuilder presentValueSensitivitySettlement(
      ResolvedCapitalIndexedBondTrade trade,
      RatesProvider ratesProvider,
      LegalEntityDiscountingProvider discountingProvider) {

    if (!trade.getSettlement().isPresent()) {
      // position has no settlement, thus it has no sensitivity
      return PointSensitivityBuilder.none();
    }
    ResolvedCapitalIndexedBondSettlement settlement = trade.getSettlement().get();
    BondPaymentPeriod settlePeriod = settlement.getPayment();
    ResolvedCapitalIndexedBond product = trade.getProduct();
    RepoCurveDiscountFactors repoDf = DiscountingCapitalIndexedBondProductPricer.repoCurveDf(product, discountingProvider);
    double df = repoDf.discountFactor(settlePeriod.getPaymentDate());
    double netAmount = netAmount(trade, ratesProvider).getAmount();
    PointSensitivityBuilder dfSensi = repoDf.zeroRatePointSensitivity(settlePeriod.getPaymentDate()).multipliedBy(netAmount);
    PointSensitivityBuilder naSensi = netAmountSensitivity(settlement, ratesProvider).multipliedBy(df);
    return dfSensi.combinedWith(naSensi);
  }

  // the sensitivity of the net amount of the settlement
  private PointSensitivityBuilder netAmountSensitivity(
      ResolvedCapitalIndexedBondSettlement settlement,
      RatesProvider ratesProvider) {

    BondPaymentPeriod settlePeriod = settlement.getPayment();
    if (settlePeriod instanceof KnownAmountBondPaymentPeriod) {
      return PointSensitivityBuilder.none();
    } else if (settlePeriod instanceof CapitalIndexedBondPaymentPeriod) {
      CapitalIndexedBondPaymentPeriod casted = (CapitalIndexedBondPaymentPeriod) settlePeriod;
      return productPricer.getPeriodPricer().forecastValueSensitivity(casted, ratesProvider);
    }
    throw new UnsupportedOperationException("unsupported settlement type");
  }

  // the sensitivity of the forecast value given the clean price
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

  /**
   * Calculates the settlement date.
   * <p>
   * The valuation date is returned if the settlement details are not stored.
   *
   * @param trade  the trade
   * @param valuationDate  the valuation date
   * @return the settlement date
   */
  public LocalDate settlementDate(ResolvedCapitalIndexedBondTrade trade, LocalDate valuationDate) {
    return trade.getSettlement()
        .map(settle -> settle.getSettlementDate())
        .orElse(valuationDate);
  }

  //-------------------------------------------------------------------------
  private void validate(RatesProvider ratesProvider, LegalEntityDiscountingProvider discountingProvider) {
    ArgChecker.isTrue(ratesProvider.getValuationDate().isEqual(discountingProvider.getValuationDate()),
        "the rates providers should be for the same date");
  }

}
