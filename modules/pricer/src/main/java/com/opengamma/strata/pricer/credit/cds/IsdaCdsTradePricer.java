/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.product.credit.ResolvedCds;
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
   * Calculates the price of the underlying product, which is the present value per unit notional. 
   * <p>
   * This is coherent to {@link #presentValueOnSettle(ResolvedCds, CreditRatesProvider, PriceType, ReferenceData)}.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param referenceDate  the reference date
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
      double flactionalSpread,
      PriceType priceType,
      ReferenceData refData) {

    ResolvedCds product = trade.getProduct();
    LocalDate settlementDate = trade.getInfo().getSettlementDate()
        .orElse(product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData));
    return productPricer.price(product, ratesProvider, flactionalSpread, settlementDate, priceType, refData);
  }

  /**
   * Calculates the par spread of the underlying product.
   * <p>
   * The par spread is a coupon rate such that the clean price is 0. 
   * The result is represented in decimal form. 
   * <p>
   * This is coherent to {@link #price(ResolvedCds, CreditRatesProvider, PriceType, ReferenceData)}.
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

    ResolvedCds product = trade.getProduct();
    LocalDate settlementDate = trade.getInfo().getSettlementDate()
        .orElse(product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData));
    return productPricer.parSpread(product, ratesProvider, settlementDate, refData);
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
  public PointSensitivityBuilder priceSensitivity(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    ResolvedCds product = trade.getProduct();
    LocalDate settlementDate = trade.getInfo().getSettlementDate()
        .orElse(product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData));
    PointSensitivityBuilder priceSensiProduct =
        productPricer.priceSensitivity(product, ratesProvider, settlementDate, refData);
    return priceSensiProduct;
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
  public PointSensitivityBuilder parSpreadSensitivity(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    ResolvedCds product = trade.getProduct();
    LocalDate settlementDate = trade.getInfo().getSettlementDate()
        .orElse(product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData));
    PointSensitivityBuilder spreadSensiProduct =
        productPricer.parSpreadSensitivity(product, ratesProvider, settlementDate, refData);
    return spreadSensiProduct;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the trade.
   * <p>
   * The present value of the product is based on the valuation date.
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

    ResolvedCds product = trade.getProduct();
    CurrencyAmount pvProduct = productPricer.presentValue(
        product, ratesProvider, ratesProvider.getValuationDate(), priceType, refData);
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
  public PointSensitivityBuilder presentValueSensitivity(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    ResolvedCds product = trade.getProduct();
    PointSensitivityBuilder pvSensiProduct =
        productPricer.presentValueSensitivity(product, ratesProvider, ratesProvider.getValuationDate(), refData);
    if (!trade.getUpfrontFee().isPresent()) {
      return pvSensiProduct;
    }
    Payment upfront = trade.getUpfrontFee().get();
    PointSensitivityBuilder pvUpfront = upfrontPricer.presentValueSensitivity(
        upfront, ratesProvider.discountFactors(upfront.getCurrency()).toDiscountFactors());
    return pvSensiProduct.combinedWith(pvUpfront);
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

    ResolvedCds product = trade.getProduct();
    LocalDate settlementDate = trade.getInfo().getSettlementDate()
        .orElse(product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData));
    CurrencyAmount pvProduct = productPricer.presentValue(product, ratesProvider, settlementDate, priceType, refData);
    return pvProduct;
  }

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

    ResolvedCds product = trade.getProduct();
    LocalDate settlementDate = trade.getInfo().getSettlementDate()
        .orElse(product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData));
    return productPricer.rpv01(product, ratesProvider, settlementDate, priceType, refData);
  }

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

    ResolvedCds product = trade.getProduct();
    LocalDate settlementDate = trade.getInfo().getSettlementDate()
        .orElse(product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData));
    return productPricer.recovery01(product, ratesProvider, settlementDate, refData);
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
  public PointSensitivityBuilder presentValueOnSettleSensitivity(
      ResolvedCdsTrade trade,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    ResolvedCds product = trade.getProduct();
    LocalDate settlementDate = trade.getInfo().getSettlementDate()
        .orElse(product.getSettlementDateOffset().adjust(ratesProvider.getValuationDate(), refData));
    PointSensitivityBuilder pvSensiProduct =
        productPricer.presentValueSensitivity(product, ratesProvider, settlementDate, refData);
    return pvSensiProduct;
  }

}
