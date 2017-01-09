/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.pricer.common.PriceType;
import com.opengamma.strata.product.credit.CdsQuote;
import com.opengamma.strata.product.credit.ResolvedCds;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.type.CdsQuoteConvention;

/**
 * The market quote converter for credit default swaps.
 */
public class CdsMarketQuoteConverter {

  /**
   * The default implementation.
   */
  public static final CdsMarketQuoteConverter DEFAULT = new CdsMarketQuoteConverter();

  /**
   * The credit curve calibrator.
   */
  private final IsdaCompliantCreditCurveCalibrator calibrator;
  /**
   * The trade pricer.
   */
  private final IsdaCdsTradePricer pricer;

  //-------------------------------------------------------------------------
  /**
   * The default constructor.
   * <p>
   * The original ISDA accrual-on-default formula (version 1.8.2 and lower) is used.
   */
  public CdsMarketQuoteConverter() {
    this.calibrator = FastCreditCurveCalibrator.DEFAULT;
    this.pricer = IsdaCdsTradePricer.DEFAULT;
  }

  /**
   * The constructor with the accrual-on-default formula specified.
   * 
   * @param formula  the accrual-on-default formula
   */
  public CdsMarketQuoteConverter(AccrualOnDefaultFormula formula) {
    this.calibrator = new FastCreditCurveCalibrator(formula);
    this.pricer = new IsdaCdsTradePricer(formula);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes market clean price from points upfront.
   * <p>
   * The points upfront and resultant price are represented as a fraction. 
   * 
   * @param pointsUpfront  the points upfront
   * @return the clean price
   */
  public double cleanPriceFromPointsUpfront(double pointsUpfront) {
    return 1d - pointsUpfront;
  }

  /**
   * Computes the market clean price. 
   * <p>
   * The market clean price is usually expressed in percentage. 
   * Here a fraction of notional is returned, e.g., 0.98 is 98(%) clean price.
   * <p>
   * A relevant credit curve must be pre-calibrated and stored in {@code ratesProvider}.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the clean price
   */
  public double cleanPrice(ResolvedCdsTrade trade, CreditRatesProvider ratesProvider, ReferenceData refData) {
    double puf = pointsUpfront(trade, ratesProvider, refData);
    return 1d - puf;
  }

  /**
   * Computes the points upfront. 
   * <p>
   * The points upfront quote is usually expressed in percentage. 
   * Here a fraction of notional is returned, e.g., 0.01 is 1(%) points up-front
   * <p>
   * The relevant credit curve must be pre-calibrated and stored in {@code ratesProvider}.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the points upfront
   */
  public double pointsUpfront(ResolvedCdsTrade trade, CreditRatesProvider ratesProvider, ReferenceData refData) {
    return pricer.price(trade, ratesProvider, PriceType.CLEAN, refData);
  }

  /**
   * Converts quoted spread to points upfront.
   * <p>
   * Thus {@code quote} must be {@code CdsQuoteConvention.QUOTED_SPREAD}.
   * <p>
   * The relevant discount curve and recovery rate curve must be stored in {@code ratesProvider}.
   * The credit curve is internally calibrated to convert one quote type to the other quote type.
   * 
   * @param trade  the trade
   * @param quote  the quote
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the quote
   */
  public CdsQuote pointsUpFrontFromQuotedSpread(
      ResolvedCdsTrade trade,
      CdsQuote quote,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    ArgChecker.notNull(trade, "trade");
    ArgChecker.notNull(quote, "quote");
    ArgChecker.notNull(ratesProvider, "ratesProvider");
    ArgChecker.notNull(refData, "refData");
    ArgChecker.isTrue(quote.getQuoteConvention().equals(CdsQuoteConvention.QUOTED_SPREAD), "quote must be quoted spread");

    ResolvedCds product = trade.getProduct();
    Currency currency = product.getCurrency();
    StandardId legalEntityId = product.getLegalEntityId();
    LocalDate valuationDate = ratesProvider.getValuationDate();
    NodalCurve creditCurve = calibrator.calibrate(
        ImmutableList.of(trade),
        DoubleArray.of(quote.getQuotedValue()),
        DoubleArray.of(0d),
        CurveName.of("temp"),
        valuationDate,
        ratesProvider.discountFactors(currency),
        ratesProvider.recoveryRates(legalEntityId),
        refData);
    CreditRatesProvider ratesProviderNew = ratesProvider.toImmutableCreditRatesProvider().toBuilder()
        .creditCurves(ImmutableMap.of(
            Pair.of(legalEntityId, currency),
            LegalEntitySurvivalProbabilities.of(
                legalEntityId, IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, creditCurve))))
        .build();
    double puf = pointsUpfront(trade, ratesProviderNew, refData);
    return CdsQuote.of(CdsQuoteConvention.POINTS_UPFRONT, puf);
  }

  /**
   * Converts points upfront to quoted spread.
   * <p>
   * Thus {@code quote} must be {@code CdsQuoteConvention.POINTS_UPFRONT}.
   * <p>
   * The relevant discount curve and recovery rate curve must be stored in {@code ratesProvider}.
   * The credit curve is internally calibrated to convert one quote type to the other quote type.
   * 
   * @param trade  the trade
   * @param quote  the quote
   * @param ratesProvider  the rates provider
   * @param refData  the reference data
   * @return the quote
   */
  public CdsQuote quotedSpreadFromPointsUpfront(
      ResolvedCdsTrade trade,
      CdsQuote quote,
      CreditRatesProvider ratesProvider,
      ReferenceData refData) {

    ArgChecker.notNull(trade, "trade");
    ArgChecker.notNull(quote, "quote");
    ArgChecker.notNull(ratesProvider, "ratesProvider");
    ArgChecker.notNull(refData, "refData");
    ArgChecker.isTrue(quote.getQuoteConvention().equals(CdsQuoteConvention.POINTS_UPFRONT), "quote must be points upfront");

    ResolvedCds product = trade.getProduct();
    Currency currency = product.getCurrency();
    StandardId legalEntityId = product.getLegalEntityId();
    LocalDate valuationDate = ratesProvider.getValuationDate();
    NodalCurve creditCurve = calibrator.calibrate(
        ImmutableList.of(trade),
        DoubleArray.of(product.getFixedRate()),
        DoubleArray.of(quote.getQuotedValue()),
        CurveName.of("temp"),
        valuationDate,
        ratesProvider.discountFactors(currency),
        ratesProvider.recoveryRates(legalEntityId),
        refData);
    CreditRatesProvider ratesProviderNew = ratesProvider.toImmutableCreditRatesProvider().toBuilder()
        .creditCurves(ImmutableMap.of(
            Pair.of(legalEntityId, currency),
            LegalEntitySurvivalProbabilities.of(
                legalEntityId, IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, creditCurve))))
        .build();
    double sp = pricer.parSpread(trade, ratesProviderNew, refData);
    return CdsQuote.of(CdsQuoteConvention.QUOTED_SPREAD, sp);
  }

  /**
   * The par spread quotes are converted to points upfronts or quoted spreads. 
   * <p>
   * The relevant discount curve and recovery rate curve must be stored in {@code ratesProvider}.
   * The credit curve is internally calibrated to par spread values.
   * <p>
   * {@code trades} must be sorted in ascending order in maturity and coherent to {@code quotes}. 
   * <p> 
   * The resultant quote is specified by {@code targetConvention}.
   * 
   * @param trades  the trades
   * @param quotes  the quotes
   * @param ratesProvider  the rates provider
   * @param targetConvention  the target convention
   * @param refData  the reference data
   * @return the quotes
   */
  public List<CdsQuote> quotesFromParSpread(
      List<ResolvedCdsTrade> trades,
      List<CdsQuote> quotes,
      CreditRatesProvider ratesProvider,
      CdsQuoteConvention targetConvention,
      ReferenceData refData) {

    ArgChecker.noNulls(trades, "trades");
    ArgChecker.noNulls(quotes, "quotes");
    ArgChecker.notNull(ratesProvider, "ratesProvider");
    ArgChecker.notNull(targetConvention, "targetConvention");
    ArgChecker.notNull(refData, "refData");

    int nNodes = trades.size();
    ArgChecker.isTrue(quotes.size() == nNodes, "trades and quotes must be the same size");
    quotes.forEach(
        q -> ArgChecker.isTrue(q.getQuoteConvention().equals(CdsQuoteConvention.PAR_SPREAD), "quote must be par spread"));
    Iterator<StandardId> legalEntities =
        trades.stream().map(t -> t.getProduct().getLegalEntityId()).collect(Collectors.toSet()).iterator();
    StandardId legalEntityId = legalEntities.next();
    ArgChecker.isFalse(legalEntities.hasNext(), "legal entity must be common to trades");
    Iterator<Currency> currencies = trades.stream().map(t -> t.getProduct().getCurrency()).collect(Collectors.toSet()).iterator();
    Currency currency = currencies.next();
    ArgChecker.isFalse(currencies.hasNext(), "currency must be common to trades");

    LocalDate valuationDate = ratesProvider.getValuationDate();
    CreditDiscountFactors discountFactors = ratesProvider.discountFactors(currency);
    RecoveryRates recoveryRates = ratesProvider.recoveryRates(legalEntityId);
    NodalCurve creditCurve = calibrator.calibrate(
        trades,
        DoubleArray.of(nNodes, q -> quotes.get(q).getQuotedValue()),
        DoubleArray.filled(nNodes),
        CurveName.of("temp"),
        valuationDate,
        discountFactors,
        recoveryRates,
        refData);
    CreditRatesProvider ratesProviderNew = ratesProvider.toImmutableCreditRatesProvider().toBuilder()
        .creditCurves(ImmutableMap.of(
            Pair.of(legalEntityId, currency),
            LegalEntitySurvivalProbabilities.of(
                legalEntityId, IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, creditCurve))))
        .build();

    Function<ResolvedCdsTrade, CdsQuote> quoteValueFunction =
        createQuoteValueFunction(ratesProviderNew, targetConvention, refData);
    ImmutableList<CdsQuote> result = trades.stream().map(c -> quoteValueFunction.apply(c))
        .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    return result;
  }

  //-------------------------------------------------------------------------
  private Function<ResolvedCdsTrade, CdsQuote> createQuoteValueFunction(
      CreditRatesProvider ratesProviderNew,
      CdsQuoteConvention targetConvention,
      ReferenceData refData) {

    Function<ResolvedCdsTrade, CdsQuote> quoteValueFunction;
    if (targetConvention.equals(CdsQuoteConvention.POINTS_UPFRONT)) {
      quoteValueFunction = new Function<ResolvedCdsTrade, CdsQuote>() {
        @Override
        public CdsQuote apply(ResolvedCdsTrade x) {
          double puf = pointsUpfront(x, ratesProviderNew, refData);
          return CdsQuote.of(targetConvention, puf);
        }
      };
    } else if (targetConvention.equals(CdsQuoteConvention.QUOTED_SPREAD)) {
      quoteValueFunction = new Function<ResolvedCdsTrade, CdsQuote>() {
        @Override
        public CdsQuote apply(ResolvedCdsTrade x) {
          double puf = pointsUpfront(x, ratesProviderNew, refData);
          return quotedSpreadFromPointsUpfront(x, CdsQuote.of(CdsQuoteConvention.POINTS_UPFRONT, puf), ratesProviderNew, refData);
        }
      };
    } else {
      throw new IllegalArgumentException("unsuported CDS quote convention");
    }
    return quoteValueFunction;
  }

}
