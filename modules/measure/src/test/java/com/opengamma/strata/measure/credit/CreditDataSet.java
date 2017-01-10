/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.credit;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.product.common.BuySell.BUY;

import java.time.LocalDate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.IsdaCreditCurveDefinition;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.node.CdsIsdaCreditCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.ResolvedTradeParameterMetadata;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.credit.ConstantRecoveryRates;
import com.opengamma.strata.pricer.credit.FastCreditCurveCalibrator;
import com.opengamma.strata.pricer.credit.ImmutableCreditRatesProvider;
import com.opengamma.strata.pricer.credit.IsdaCompliantCreditCurveCalibrator;
import com.opengamma.strata.pricer.credit.IsdaCreditDiscountFactors;
import com.opengamma.strata.pricer.credit.LegalEntitySurvivalProbabilities;
import com.opengamma.strata.pricer.credit.RecoveryRates;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.ResolvedCdsIndexTrade;
import com.opengamma.strata.product.credit.ResolvedCdsTrade;
import com.opengamma.strata.product.credit.type.CdsConvention;
import com.opengamma.strata.product.credit.type.DatesCdsTemplate;
import com.opengamma.strata.product.credit.type.ImmutableCdsConvention;

/**
 * Testing data.
 */
public class CreditDataSet {

  private static final IsdaCompliantCreditCurveCalibrator BUILDER = FastCreditCurveCalibrator.standard();

  static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double ONE_BP = 1.0e-4;
  private static final LocalDate VALUATION_DATE = LocalDate.of(2013, 1, 3);
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");
  private static final StandardId INDEX_ID = StandardId.of("OG", "ABCXX");
  private static final ImmutableList<StandardId> LEGAL_ENTITIES;
  static {
    Builder<StandardId> builder = ImmutableList.builder();
    for (int i = 0; i < 97; ++i) {
      builder.add(StandardId.of("OG", String.valueOf(i)));
    }
    LEGAL_ENTITIES = builder.build();
  }
  private static final double NOTIONAL = 1.0e7;
  // CDS trade
  private static final Cds CDS_PRODUCT = Cds.of(
      BUY, LEGAL_ENTITY, USD, NOTIONAL, LocalDate.of(2012, 12, 20), LocalDate.of(2020, 10, 20), P3M, CALENDAR, 0.015);
  private static final LocalDate SETTLEMENT_DATE = CDS_PRODUCT.getSettlementDateOffset().adjust(VALUATION_DATE, REF_DATA);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(VALUATION_DATE)
      .settlementDate(SETTLEMENT_DATE)
      .build();
  private static final Payment CDS_UPFRONT = Payment.of(USD, -NOTIONAL * 0.2, SETTLEMENT_DATE);
  static final CdsTrade CDS_TRADE = CdsTrade.builder()
      .product(CDS_PRODUCT)
      .info(TRADE_INFO)
      .upfrontFee(AdjustablePayment.of(CDS_UPFRONT))
      .build();
  static final ResolvedCdsTrade RESOLVED_CDS_TRADE = CDS_TRADE.resolve(REF_DATA);
  // CDS index trade
  private static final double INDEX_FACTOR = 93d / 97d;
  private static final CdsIndex INDEX_PRODUCT = CdsIndex.of(
      BUY, INDEX_ID, LEGAL_ENTITIES, USD, NOTIONAL, LocalDate.of(2012, 12, 20), LocalDate.of(2020, 10, 20), P3M, CALENDAR, 0.015);
  private static final Payment INDEX_UPFRONT = Payment.of(USD, -NOTIONAL * 0.15, SETTLEMENT_DATE);
  static final CdsIndexTrade INDEX_TRADE = CdsIndexTrade.builder()
      .product(INDEX_PRODUCT)
      .info(TRADE_INFO)
      .upfrontFee(AdjustablePayment.of(INDEX_UPFRONT))
      .build();
  static final ResolvedCdsIndexTrade RESOLVED_INDEX_TRADE = INDEX_TRADE.resolve(REF_DATA);
  // CDS lookup
  static final CurveId CDS_CREDIT_CURVE_ID = CurveId.of("Default", "Credit-ABC");
  static final CurveId USD_DSC_CURVE_ID = CurveId.of("Default", "Dsc-USD");
  static final CurveId CDS_RECOVERY_CURVE_ID = CurveId.of("Default", "Recovery-ABC");
  static final CreditRatesMarketDataLookup CDS_LOOKUP = CreditRatesMarketDataLookup.of(
      ImmutableMap.of(Pair.of(LEGAL_ENTITY, USD), CDS_CREDIT_CURVE_ID),
      ImmutableMap.of(USD, USD_DSC_CURVE_ID),
      ImmutableMap.of(LEGAL_ENTITY, CDS_RECOVERY_CURVE_ID));
  static final CalculationParameters CDS_PARAMS = CalculationParameters.of(CDS_LOOKUP);
  // CDS index lookup
  static final CurveId INDEX_CREDIT_CURVE_ID = CurveId.of("Default", "Credit-ABCXX");
  static final CurveId INDEX_RECOVERY_CURVE_ID = CurveId.of("Default", "Recovery-ABCXX");
  static final CreditRatesMarketDataLookup INDEX_LOOKUP = CreditRatesMarketDataLookup.of(
      ImmutableMap.of(Pair.of(INDEX_ID, USD), INDEX_CREDIT_CURVE_ID),
      ImmutableMap.of(USD, USD_DSC_CURVE_ID),
      ImmutableMap.of(INDEX_ID, INDEX_RECOVERY_CURVE_ID));
  static final CalculationParameters INDEX_PARAMS = CalculationParameters.of(INDEX_LOOKUP);

  // curve
  private static final LocalDate[] PAR_SPD_DATES =
      new LocalDate[] {LocalDate.of(2013, 6, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2015, 3, 20),
          LocalDate.of(2016, 3, 20), LocalDate.of(2018, 3, 20), LocalDate.of(2023, 3, 20)};
  private static final double[] PAR_SPREADS = new double[] {50, 70, 80, 95, 100, 95, 80};
  private static final int NUM_MARKET_CDS = PAR_SPD_DATES.length;
  private static final ResolvedCdsTrade[] MARKET_CDS = new ResolvedCdsTrade[NUM_MARKET_CDS];
  private static final ResolvedCdsIndexTrade[] MARKET_CDS_INDEX = new ResolvedCdsIndexTrade[NUM_MARKET_CDS];
  private static final double RECOVERY_RATE = 0.4;
//  private static final RecoveryRates RECOVERY_CURVE = ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, RECOVERY_RATE);
//  private static final RecoveryRates RECOVERY_CURVE_INDEX = ConstantRecoveryRates.of(INDEX_ID, VALUATION_DATE, RECOVERY_RATE);
//  private static final IsdaCompliantZeroRateDiscountFactors YIELD_CURVE;
  private static final NodalCurve CDS_CREDIT_CURVE;
  private static final NodalCurve INCDEX_CREDIT_CURVE;
  private static final ConstantCurve CDS_RECOVERY_RATE;
  private static final ConstantCurve INDEX_RECOVERY_RATE;
  private static final NodalCurve DISCOUNT_CUVE;
  private static final CurveName CREDIT_CURVE_NAME = CurveName.of("credit");
  private static final CdsConvention CDS_CONV = ImmutableCdsConvention.builder()
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, SAT_SUN))
      .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
      .currency(USD)
      .dayCount(ACT_360)
      .name("sat_sun_conv")
      .paymentFrequency(Frequency.P3M)
      .settlementDateOffset(DaysAdjustment.ofBusinessDays(3, SAT_SUN))
      .build();
  static final ImmutableList<ResolvedTradeParameterMetadata> CDS_METADATA;
  private static final ImmutableList<ResolvedTradeParameterMetadata> CDS_INDEX_METADATA;
  static {
    double flatRate = 0.05;
    double t = 20.0;
    IsdaCreditDiscountFactors yieldCurve = IsdaCreditDiscountFactors.of(
        USD, VALUATION_DATE, CurveName.of("discount"), DoubleArray.of(t), DoubleArray.of(flatRate), ACT_365F);
    DISCOUNT_CUVE = yieldCurve.getCurve();
    RecoveryRates recoveryRate = ConstantRecoveryRates.of(LEGAL_ENTITY, VALUATION_DATE, RECOVERY_RATE);
    ImmutableMarketDataBuilder dataBuilder = ImmutableMarketData.builder(VALUATION_DATE);
    Builder<CdsIsdaCreditCurveNode> nodesBuilder = ImmutableList.builder();
    Builder<ResolvedTradeParameterMetadata> cdsMetadataBuilder = ImmutableList.builder();
    Builder<ResolvedTradeParameterMetadata> cdsIndexMetadataBuilder = ImmutableList.builder();
    for (int i = 0; i < NUM_MARKET_CDS; i++) {
      QuoteId quoteId = QuoteId.of(StandardId.of("OG", PAR_SPD_DATES[i].toString()));
      CdsIsdaCreditCurveNode node = CdsIsdaCreditCurveNode.ofParSpread(
          DatesCdsTemplate.of(VALUATION_DATE, PAR_SPD_DATES[i], CDS_CONV), quoteId, LEGAL_ENTITY);
      MARKET_CDS[i] = CdsTrade.builder()
          .product(Cds.of(
              BUY, LEGAL_ENTITY, USD, NOTIONAL, VALUATION_DATE, PAR_SPD_DATES[i], P3M, SAT_SUN, PAR_SPREADS[i] * ONE_BP))
          .info(TradeInfo.of(VALUATION_DATE))
          .build()
          .resolve(REF_DATA);
      MARKET_CDS_INDEX[i] = CdsIndexTrade.builder()
          .product(CdsIndex.of(
              BuySell.BUY, INDEX_ID, LEGAL_ENTITIES, USD, NOTIONAL, VALUATION_DATE, PAR_SPD_DATES[i], P3M, SAT_SUN,
              PAR_SPREADS[i] * ONE_BP))
          .info(TradeInfo.of(VALUATION_DATE))
          .build()
          .resolve(REF_DATA);
      dataBuilder.addValue(quoteId, PAR_SPREADS[i] * ONE_BP);
      nodesBuilder.add(node);
      cdsMetadataBuilder.add(ResolvedTradeParameterMetadata.of(
          MARKET_CDS[i],
          MARKET_CDS[i].getProduct().getProtectionEndDate().toString()));
      cdsIndexMetadataBuilder.add(ResolvedTradeParameterMetadata.of(
          MARKET_CDS_INDEX[i],
          MARKET_CDS_INDEX[i].getProduct().getProtectionEndDate().toString()));
    }
    ImmutableMarketData marketData = dataBuilder.build();
    ImmutableList<CdsIsdaCreditCurveNode> nodes = nodesBuilder.build();
    CDS_METADATA = cdsMetadataBuilder.build();
    CDS_INDEX_METADATA = cdsIndexMetadataBuilder.build();
    ImmutableCreditRatesProvider rates = ImmutableCreditRatesProvider.builder()
        .valuationDate(VALUATION_DATE)
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, recoveryRate))
        .discountCurves(ImmutableMap.of(USD, yieldCurve))
        .build();
    IsdaCreditCurveDefinition definition = IsdaCreditCurveDefinition.of(
        CREDIT_CURVE_NAME, USD, VALUATION_DATE, ACT_365F, nodes, true, true);
    LegalEntitySurvivalProbabilities calibrated = BUILDER.calibrate(definition, marketData, rates, REF_DATA);
    NodalCurve underlyingCurve = ((IsdaCreditDiscountFactors) calibrated.getSurvivalProbabilities()).getCurve();
    CDS_CREDIT_CURVE = underlyingCurve;
    INCDEX_CREDIT_CURVE = underlyingCurve.withMetadata(
        underlyingCurve.getMetadata()
            .withInfo(CurveInfoType.CDS_INDEX_FACTOR, INDEX_FACTOR)
            .withParameterMetadata(CDS_INDEX_METADATA)); // replace parameter metadata
    CDS_RECOVERY_RATE = ConstantCurve.of(Curves.recoveryRates("CDS recovery rate", ACT_365F), RECOVERY_RATE);
    INDEX_RECOVERY_RATE = ConstantCurve.of(Curves.recoveryRates("Index recovery rate", ACT_365F), RECOVERY_RATE);
  }
  static final ScenarioMarketData MARKET_DATA = new TestMarketDataMap(
      VALUATION_DATE,
      ImmutableMap.of(
          CDS_CREDIT_CURVE_ID, CDS_CREDIT_CURVE, INDEX_CREDIT_CURVE_ID, INCDEX_CREDIT_CURVE,
          USD_DSC_CURVE_ID, DISCOUNT_CUVE,
          CDS_RECOVERY_CURVE_ID, CDS_RECOVERY_RATE, INDEX_RECOVERY_CURVE_ID, INDEX_RECOVERY_RATE),
      ImmutableMap.of());

}
