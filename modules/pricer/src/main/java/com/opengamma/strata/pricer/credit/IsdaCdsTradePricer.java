/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;

/**
 * Pricer for single-name credit default swaps (CDS) trade based on ISDA standard model. 
 * <p>
 * The implementation is based on the ISDA model versions 1.8.2.
 */
public class IsdaCdsTradePricer {

  /**
   * Default implementation.
   */
  public static final IsdaCdsTradePricer DEFAULT = new IsdaCdsTradePricer();

  /**
   * The product pricer.
   */
  private final IsdaCdsProductPricer productPricer;
  /**
   * The upfront fee pricer.
   */
  private final DiscountingPaymentPricer upfrontPricer;

  //-------------------------------------------------------------------------
  /**
   * The default constructor.
   * <p>
   * The default pricers are used.
   */
  public IsdaCdsTradePricer() {
    this.productPricer = IsdaCdsProductPricer.DEFAULT;
    this.upfrontPricer = DiscountingPaymentPricer.DEFAULT;
  }

  /**
   * The constructor with the accrual-on-default formula specified.
   * 
   * @param formula  the accrual-on-default formula
   */
  public IsdaCdsTradePricer(AccrualOnDefaultFormula formula) {
    this.productPricer = new IsdaCdsProductPricer(formula);
    this.upfrontPricer = DiscountingPaymentPricer.DEFAULT;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the accrual-on-default formula used in this pricer. 
   * 
   * @return the formula
   */
  public AccrualOnDefaultFormula getAccrualOnDefaultFormula() {
    return productPricer.getAccrualOnDefaultFormula();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the underlying product, which is the present value per unit notional. 
   * <p>
   * This method can calculate the clean or dirty price, see {@link PriceType}. 
   * If calculating the clean price, the accrued interest is calculated based on the step-in date.
   * <p>
   * This is coherent to {@link #presentValueOnSettle(ResolvedCdsTrade, CreditRatesProvider, PriceType, ReferenceData)}.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param priceType  the price type
   * @param refData  the reference data
   * @return the price
   */
  public double price(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      PriceType priceType,
      ReferenceData refData) {

    return price(trade, ratesProvider, trade.getProduct().getFixedRate(), priceType, refData);
  }

  // internal price computation with specified coupon rate
  double price(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      double fractionalSpread,
      PriceType priceType,
      ReferenceData refData) {

    LocalDate settlementDate = calculateSettlementDate(trade, ratesProvider, refData);
    return productPricer.price(trade.getProduct(), ratesProvider, fractionalSpread, settlementDate, priceType, refData);
  }

  /**
   * Calculates the price sensitivity of the underlying product. 
   * <p>
   * The price sensitivity of the product is the sensitivity of price to the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the present value sensitivity
   */
  public PointSensitivities priceSensitivity(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    LocalDate settlementDate = calculateSettlementDate(trade, ratesProvider, refData);
    return productPricer.priceSensitivity(trade.getProduct(), ratesProvider, settlementDate, refData).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread of the underlying product.
   * <p>
   * The par spread is a coupon rate such that the clean price is 0. 
   * The result is represented in decimal form. 
   * <p>
   * This is coherent to {@link #price(ResolvedCdsTrade, CreditRatesProvider, PriceType, ReferenceData)}.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the par spread
   */
  public double parSpread(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    LocalDate settlementDate = calculateSettlementDate(trade, ratesProvider, refData);
    return productPricer.parSpread(trade.getProduct(), ratesProvider, settlementDate, refData);
  }

  /**
   * Calculates the par spread sensitivity of the underling product.
   * <p>
   * The par spread sensitivity of the product is the sensitivity of par spread to the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the present value sensitivity
   */
  public PointSensitivities parSpreadSensitivity(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    LocalDate settlementDate = calculateSettlementDate(trade, ratesProvider, refData);
    return productPricer.parSpreadSensitivity(trade.getProduct(), ratesProvider, settlementDate, refData).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the trade.
   * <p>
   * The present value of the product is based on the valuation date.
   * <p>
   * This method can calculate the clean or dirty present value, see {@link PriceType}. 
   * If calculating the clean value, the accrued interest is calculated based on the step-in date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param priceType  the price type
   * @param refData  the reference data
   * @return the price
   */
  public CurrencyAmount presentValue(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      PriceType priceType,
      ReferenceData refData) {

    CurrencyAmount pvProduct = productPricer.presentValue(
        trade.getProduct(), ratesProvider, ratesProvider.getValuationDate(), priceType, refData);
    if (!trade.getUpfrontFee().isPresent()) {
      return pvProduct;
    }
    Payment upfront = trade.getUpfrontFee().get();
    CurrencyAmount pvUpfront =
        upfrontPricer.presentValue(upfront, ratesProvider.discountFactors(upfront.getCurrency()).toDiscountFactors());
    return pvProduct.plus(pvUpfront);
  }

  /**
   * Calculates the present value sensitivity of the trade. 
   * <p>
   * The present value sensitivity of the trade is the sensitivity of present value to the underlying curves.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    PointSensitivityBuilder pvSensiProduct = productPricer.presentValueSensitivity(
        trade.getProduct(), ratesProvider, ratesProvider.getValuationDate(), refData);
    if (!trade.getUpfrontFee().isPresent()) {
      return pvSensiProduct.build();
    }
    Payment upfront = trade.getUpfrontFee().get();
    PointSensitivityBuilder pvUpfront = upfrontPricer.presentValueSensitivity(
        upfront, ratesProvider.discountFactors(upfront.getCurrency()).toDiscountFactors());
    return pvSensiProduct.combinedWith(pvUpfront).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the underlying product. 
   * <p>
   * The present value is computed based on the settlement date rather than the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param priceType  the price type
   * @param refData  the reference data
   * @return the price
   */
  public CurrencyAmount presentValueOnSettle(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      PriceType priceType,
      ReferenceData refData) {

    LocalDate settlementDate = calculateSettlementDate(trade, ratesProvider, refData);
    return productPricer.presentValue(trade.getProduct(), ratesProvider, settlementDate, priceType, refData);
  }

  /**
   * Calculates the present value sensitivity of the underlying product. 
   * <p>
   * The present value sensitivity of the product is the sensitivity of present value to the underlying curves.
   * The present value sensitivity is computed based on the settlement date rather than the valuation date.
   * <p>
   * This is coherent to {@link #presentValueOnSettle(ResolvedCdsTrade, CreditRatesProvider, PriceType, ReferenceData)}.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueOnSettleSensitivity(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    LocalDate settlementDate = calculateSettlementDate(trade, ratesProvider, refData);
    return productPricer.presentValueSensitivity(trade.getProduct(), ratesProvider, settlementDate, refData).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the risky PV01 of the underlying product. 
   * <p>
   * RPV01 is defined as minus of the present value sensitivity to coupon rate.
   * <p>
   * This is computed based on the settlement date rather than the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param priceType  the price type
   * @param refData  the reference date
   * @return the RPV01
   */
  public CurrencyAmount rpv01OnSettle(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      PriceType priceType,
      ReferenceData refData) {

    LocalDate settlementDate = calculateSettlementDate(trade, ratesProvider, refData);
    return productPricer.rpv01(trade.getProduct(), ratesProvider, settlementDate, priceType, refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the recovery01 of the underlying product.
   * <p>
   * The recovery01 is defined as the present value sensitivity to the recovery rate.
   * Since the ISDA standard model requires the recovery rate to be constant throughout the lifetime of the CDS,  
   * one currency amount is returned by this method.
   * <p>
   * This is computed based on the settlement date rather than the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the recovery01
   */
  public CurrencyAmount recovery01OnSettle(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    LocalDate settlementDate = calculateSettlementDate(trade, ratesProvider, refData);
    return productPricer.recovery01(trade.getProduct(), ratesProvider, settlementDate, refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the jump-to-default of the underlying product.
   * <p>
   * The jump-to-default is the value of the product in case of immediate default.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the jump-to-default
   */
  public JumpToDefault jumpToDefault(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    LocalDate settlementDate = calculateSettlementDate(trade, ratesProvider, refData);
    return productPricer.jumpToDefault(trade.getProduct(), ratesProvider, settlementDate, refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the expected loss of the underlying product.
   * <p>
   * The expected loss is the (undiscounted) expected default settlement value paid by the protection seller. 
   * The resulting value is always positive.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the expected loss
   */
  public CurrencyAmount expectedLoss(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider) {

    return productPricer.expectedLoss(trade.getProduct(), ratesProvider);
  }

  //-------------------------------------------------------------------------
  private LocalDate calculateSettlementDate(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    return trade.getInfo().getSettlementDate()
        .orElse(trade.getProduct().calculateSettlementDateFromValuation(ratesProvider.getValuationDate(), refData));
  }

}
