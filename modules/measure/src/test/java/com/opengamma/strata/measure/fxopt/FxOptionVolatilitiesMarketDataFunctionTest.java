/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PCHIP;
import static com.opengamma.strata.measure.Measures.PRESENT_VALUE;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataFilter;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.PerturbationMapping;
import com.opengamma.strata.calc.marketdata.ScenarioDefinition;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ImmutableScenarioMarketData;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.data.scenario.ScenarioPerturbation;
import com.opengamma.strata.market.GenericDoubleShifts;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveDefinition;
import com.opengamma.strata.market.curve.RatesCurveGroup;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.RatesCurveGroupEntry;
import com.opengamma.strata.market.curve.RatesCurveGroupId;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.node.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.curve.node.FxSwapCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.option.Strike;
import com.opengamma.strata.market.param.ParameterizedData;
import com.opengamma.strata.market.param.PointShifts;
import com.opengamma.strata.market.param.PointShiftsBuilder;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.measure.ValuationZoneTimeDefinition;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.curve.RatesCurveCalibrator;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionSmileVolatilities;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionSurfaceVolatilities;
import com.opengamma.strata.pricer.fxopt.BlackFxVanillaOptionTradePricer;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesId;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesName;
import com.opengamma.strata.pricer.fxopt.InterpolatedStrikeSmileDeltaTermStructure;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.type.FxSwapConventions;
import com.opengamma.strata.product.fx.type.FxSwapTemplate;
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapConventions;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;

/**
 * Test {@code FxOptionVolatilitiesMarketDataFunction}.
 */
@Test
public class FxOptionVolatilitiesMarketDataFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VALUATION_DATE = LocalDate.of(2017, 2, 15);
  private static final LocalTime VALUATION_TIME = LocalTime.NOON;
  private static final LocalTime VALUATION_TIME_1 = LocalTime.MIDNIGHT;
  private static final ZoneId ZONE = ZoneId.of("Europe/London");
  private static final CurrencyPair GBP_USD = CurrencyPair.of(GBP, USD);
  private static final HolidayCalendarId NY_LO = USNY.combinedWith(GBLO);
  private static final DaysAdjustment SPOT_OFFSET = DaysAdjustment.NONE;
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(FOLLOWING, NY_LO);

  private static final List<Tenor> VOL_TENORS = ImmutableList.of(Tenor.TENOR_3M, Tenor.TENOR_6M);
  private static final List<Strike> STRIKES = ImmutableList.of(
      DeltaStrike.of(0.5), DeltaStrike.of(0.1), DeltaStrike.of(0.1), DeltaStrike.of(0.25), DeltaStrike.of(0.25));
  private static final List<ValueType> VALUE_TYPES = ImmutableList.of(
      ValueType.BLACK_VOLATILITY, ValueType.RISK_REVERSAL, ValueType.STRANGLE, ValueType.RISK_REVERSAL, ValueType.STRANGLE);
  private static final double[][] VOL_QUOTES = new double[][] {
      {0.18, -0.011, 0.031, -0.006, 0.011}, {0.14, -0.012, 0.032, -0.007, 0.012}};
  private static final double[][] VOL_QUOTES_1 = new double[][] {
      {0.24, -0.021, 0.043, -0.013, 0.021}, {0.11, -0.022, 0.042, -0.017, 0.022}};
  private static final List<Double> USD_QUOTES = ImmutableList.of(
      1.1E-5, 1.1E-5, 1.12E-5, 1.39E-5, 2.1E-5, 3.0E-5, 5.0E-5, 7.02E-5, 1.01E-4, 1.22E-4, 1.41E-4, 1.85E-4);
  private static final List<Double> USD_QUOTES_1 = ImmutableList.of(
      1.2E-5, 1.2E-5, 1.22E-5, 1.49E-5, 2.2E-5, 3.1E-5, 5.1E-5, 7.12E-5, 1.11E-4, 1.32E-4, 1.51E-4, 1.95E-4);
  private static final List<Double> GBP_QUOTES = ImmutableList.of(
      -3.53E-4, -7.02E-4, -0.00101, -0.00204, -0.0026, -0.00252, -8.0E-4);
  private static final List<Double> GBP_QUOTES_1 = ImmutableList.of(
      -2.53E-4, -6.02E-4, -0.00001, -0.00104, -0.0016, -0.00152, -7.0E-4);
  private static final List<Tenor> USD_TENORS = ImmutableList.of(
      Tenor.TENOR_1M, Tenor.TENOR_2M, Tenor.TENOR_3M, Tenor.TENOR_6M, Tenor.TENOR_9M, Tenor.TENOR_1Y, Tenor.TENOR_18M,
      Tenor.TENOR_2Y, Tenor.TENOR_3Y, Tenor.TENOR_4Y, Tenor.TENOR_5Y, Tenor.TENOR_10Y);
  private static final List<Period> GBP_PERIODS = ImmutableList.of(
      Period.ofMonths(1), Period.ofMonths(2), Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(9), Period.ofYears(1),
      Period.ofMonths(18));
  private static final FxRate FX = FxRate.of(GBP_USD, 1.53);
  private static final FxRate FX_1 = FxRate.of(GBP_USD, 1.43);
  private static final ImmutableMap<FxRateId, FxRate> MARKET_FX_QUOTES = ImmutableMap.of(FxRateId.of(GBP_USD), FX);
  private static final ImmutableMap<FxRateId, MarketDataBox<FxRate>> SCENARIO_MARKET_FX_QUOTES = ImmutableMap.of(
      FxRateId.of(GBP_USD), MarketDataBox.ofScenarioValues(FX, FX_1));

  private static final ImmutableList<FxOptionVolatilitiesNode> VOL_NODES;
  private static final ImmutableList<FxSwapCurveNode> GBP_NODES;
  private static final ImmutableList<FixedOvernightSwapCurveNode> USD_NODES;
  private static final ImmutableMap<QuoteId, Double> MARKET_QUOTES;
  private static final ImmutableMap<QuoteId, MarketDataBox<Double>> SCENARIO_MARKET_QUOTES;
  static {
    ImmutableList.Builder<FxOptionVolatilitiesNode> volNodeBuilder = ImmutableList.builder();
    ImmutableMap.Builder<QuoteId, Double> marketQuoteBuilder = ImmutableMap.builder();
    ImmutableMap.Builder<QuoteId, MarketDataBox<Double>> scenarioMarketQuoteBuilder = ImmutableMap.builder();
    ImmutableList.Builder<FixedOvernightSwapCurveNode> usdNodeBuilder = ImmutableList.builder();
    ImmutableList.Builder<FxSwapCurveNode> gbpNodeBuilder = ImmutableList.builder();
    for (int i = 0; i < VOL_TENORS.size(); ++i) {
      for (int j = 0; j < STRIKES.size(); ++j) {
        QuoteId quoteId = QuoteId.of(StandardId.of(
            "OG", VOL_TENORS.get(i).toString() + "_" + STRIKES.get(j).getLabel() + "_" + VALUE_TYPES.get(j).toString()));
        volNodeBuilder.add(FxOptionVolatilitiesNode.of(
            GBP_USD, SPOT_OFFSET, BDA, VALUE_TYPES.get(j), quoteId, VOL_TENORS.get(i), STRIKES.get(j)));
        marketQuoteBuilder.put(quoteId, VOL_QUOTES[i][j]);
        scenarioMarketQuoteBuilder.put(quoteId, MarketDataBox.ofScenarioValues(VOL_QUOTES[i][j], VOL_QUOTES_1[i][j]));
      }
    }
    for (int i = 0; i < USD_QUOTES.size(); ++i) {
      QuoteId quoteId = QuoteId.of(StandardId.of("OG", USD.toString() + "-OIS-" + USD_TENORS.get(i).toString()));
      usdNodeBuilder.add(FixedOvernightSwapCurveNode.of(
          FixedOvernightSwapTemplate.of(USD_TENORS.get(i), FixedOvernightSwapConventions.USD_FIXED_TERM_FED_FUND_OIS), quoteId));
      marketQuoteBuilder.put(quoteId, USD_QUOTES.get(i));
      scenarioMarketQuoteBuilder.put(quoteId, MarketDataBox.ofScenarioValues(USD_QUOTES.get(i), USD_QUOTES_1.get(i)));
    }
    for (int i = 0; i < GBP_QUOTES.size(); ++i) {
      QuoteId quoteId = QuoteId.of(StandardId.of("OG", GBP_USD.toString() + "-FX-" + GBP_PERIODS.get(i).toString()));
      gbpNodeBuilder.add(FxSwapCurveNode.of(FxSwapTemplate.of(GBP_PERIODS.get(i), FxSwapConventions.GBP_USD), quoteId));
      marketQuoteBuilder.put(quoteId, GBP_QUOTES.get(i));
      scenarioMarketQuoteBuilder.put(quoteId, MarketDataBox.ofScenarioValues(GBP_QUOTES.get(i), GBP_QUOTES_1.get(i)));
    }
    VOL_NODES = volNodeBuilder.build();
    USD_NODES = usdNodeBuilder.build();
    GBP_NODES = gbpNodeBuilder.build();
    MARKET_QUOTES = marketQuoteBuilder.build();
    SCENARIO_MARKET_QUOTES = scenarioMarketQuoteBuilder.build();
  }
  private static final ImmutableMarketData MARKET_DATA = ImmutableMarketData.builder(VALUATION_DATE)
      .addValueMap(MARKET_QUOTES)
      .addValueMap(MARKET_FX_QUOTES)
      .build();
  private static final LocalDate VALUATION_DATE_1 = VALUATION_DATE.plusDays(7);
  private static final MarketDataBox<LocalDate> VALUATION_DATES = MarketDataBox.ofScenarioValues(
      VALUATION_DATE, VALUATION_DATE_1);
  private static final ImmutableScenarioMarketData SCENARIO_MARKET_DATA = ImmutableScenarioMarketData.builder(VALUATION_DATES)
      .addBoxMap(SCENARIO_MARKET_QUOTES)
      .addBoxMap(SCENARIO_MARKET_FX_QUOTES)
      .build();

  private static final FxOptionVolatilitiesName VOL_NAME = FxOptionVolatilitiesName.of(GBP_USD.toString() + "_VOL");
  private static final FxOptionVolatilitiesId VOL_ID = FxOptionVolatilitiesId.of(VOL_NAME);
  private static final FxOptionVolatilitiesDefinition VOL_DEFINITION =
      FxOptionVolatilitiesDefinition.of(BlackFxOptionSmileVolatilitiesSpecification.builder()
          .name(VOL_NAME)
          .currencyPair(GBP_USD)
          .dayCount(ACT_365F)
          .nodes(VOL_NODES)
          .timeInterpolator(LINEAR)
          .strikeInterpolator(PCHIP)
          .build());
  private static final ValuationZoneTimeDefinition ZT_DEFINITION = ValuationZoneTimeDefinition.of(
      VALUATION_TIME, ZONE);
  private static final ValuationZoneTimeDefinition SCENARIO_ZT_DEFINITION = ValuationZoneTimeDefinition.of(
      VALUATION_TIME, ZONE, VALUATION_TIME, VALUATION_TIME_1);

  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("USD-DSCONOIS-GBP-DSCFX");
  private static final CurveName USD_CURVE_NAME = CurveName.of("USD-DSCON-OIS");
  private static final CurveDefinition USD_CURVE_DEFINITION = InterpolatedNodalCurveDefinition.builder()
      .dayCount(ACT_365F)
      .interpolator(LINEAR)
      .extrapolatorLeft(FLAT)
      .extrapolatorRight(FLAT)
      .name(USD_CURVE_NAME)
      .nodes(USD_NODES)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .build();
  private static final RatesCurveGroupEntry USD_ENTRY = RatesCurveGroupEntry.builder()
      .curveName(USD_CURVE_NAME)
      .discountCurrencies(USD)
      .indices(OvernightIndices.USD_FED_FUND)
      .build();
  private static final CurveName GBP_CURVE_NAME = CurveName.of("GBP-DSC-FX");
  private static final CurveDefinition GBP_CURVE_DEFINITION = InterpolatedNodalCurveDefinition.builder()
      .dayCount(ACT_365F)
      .interpolator(LINEAR)
      .extrapolatorLeft(FLAT)
      .extrapolatorRight(FLAT)
      .name(GBP_CURVE_NAME)
      .nodes(GBP_NODES)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .build();
  private static final RatesCurveGroupEntry GBP_ENTRY = RatesCurveGroupEntry.builder()
      .curveName(GBP_CURVE_NAME)
      .discountCurrencies(GBP)
      .build();
  private static final RatesCurveGroupDefinition CURVE_GROUP_DEFINITION = RatesCurveGroupDefinition.of(
      CURVE_GROUP_NAME, ImmutableList.of(GBP_ENTRY, USD_ENTRY), ImmutableList.of(GBP_CURVE_DEFINITION, USD_CURVE_DEFINITION));

  private static final MarketDataConfig CONFIG = MarketDataConfig.builder()
      .add(CURVE_GROUP_NAME, CURVE_GROUP_DEFINITION)
      .add(VOL_ID.getName().getName(), VOL_DEFINITION)
      .addDefault(ZT_DEFINITION)
      .build();
  private static final MarketDataConfig SCENARIO_CONFIG = MarketDataConfig.builder()
      .add(CURVE_GROUP_NAME, CURVE_GROUP_DEFINITION)
      .add(VOL_ID.getName().getName(), VOL_DEFINITION)
      .addDefault(SCENARIO_ZT_DEFINITION)
      .build();

  private static final ZonedDateTime EXPIRY = ZonedDateTime.of(2018, 5, 9, 13, 10, 0, 0, ZONE);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2018, 5, 13);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount GBP_AMOUNT = CurrencyAmount.of(GBP, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * 1.3d);
  private static final FxSingle FX_PRODUCT = FxSingle.of(GBP_AMOUNT, USD_AMOUNT, PAYMENT_DATE);
  private static final FxVanillaOption OPTION_PRODUCT = FxVanillaOption.builder()
      .longShort(SHORT)
      .expiryDate(EXPIRY.toLocalDate())
      .expiryTime(EXPIRY.toLocalTime())
      .expiryZone(ZONE)
      .underlying(FX_PRODUCT)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(VALUATION_DATE).build();
  private static final LocalDate CASH_SETTLE_DATE = VALUATION_DATE.plusDays(1);
  private static final AdjustablePayment PREMIUM = AdjustablePayment.of(GBP, NOTIONAL * 0.1, CASH_SETTLE_DATE);
  private static final FxVanillaOptionTrade OPTION_TRADE = FxVanillaOptionTrade.builder()
      .premium(PREMIUM)
      .product(OPTION_PRODUCT)
      .info(TRADE_INFO)
      .build();
  private static final List<CalculationTarget> TARGETS = ImmutableList.of(OPTION_TRADE);

  private static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(CURVE_GROUP_DEFINITION);
  private static final FxOptionMarketDataLookup VOL_LOOKUP = FxOptionMarketDataLookup.of(GBP_USD, VOL_ID);
  private static final CalculationRules RULES = CalculationRules.of(
      StandardComponents.calculationFunctions(), USD, RATES_LOOKUP, VOL_LOOKUP);
  private static final List<Column> COLUMN = ImmutableList.of(Column.of(PRESENT_VALUE));
  private static final MarketDataRequirements REQUIREMENTS = MarketDataRequirements.of(RULES, TARGETS, COLUMN, REF_DATA);
  private static final CalculationRunner CALC_RUNNER = CalculationRunner.ofMultiThreaded();

  private static final RatesCurveCalibrator CURVE_CALIBRATOR = RatesCurveCalibrator.standard();
  private static final RatesProvider EXP_RATES = CURVE_CALIBRATOR.calibrate(CURVE_GROUP_DEFINITION, MARKET_DATA, REF_DATA);
  private static final RatesProvider EXP_RATES_1 = CURVE_CALIBRATOR.calibrate(
      CURVE_GROUP_DEFINITION, SCENARIO_MARKET_DATA.scenario(1), REF_DATA);
  private static final BlackFxOptionSmileVolatilities EXP_VOLS;
  private static final BlackFxOptionSmileVolatilities EXP_VOLS_1;
  static {
    List<Double> expiry = VOL_TENORS.stream().map(t -> ACT_365F.relativeYearFraction(
        VALUATION_DATE, BDA.adjust(SPOT_OFFSET.adjust(VALUATION_DATE, REF_DATA).plus(t), REF_DATA))).collect(toList());
    int nSmiles = expiry.size();
    double[] atm = new double[nSmiles];
    double[][] rr = new double[nSmiles][2];
    double[][] str = new double[nSmiles][2];
    for (int i = 0; i < nSmiles; ++i) {
      atm[i] = VOL_QUOTES[i][0];
      rr[i][0] = VOL_QUOTES[i][1];
      rr[i][1] = VOL_QUOTES[i][3];
      str[i][0] = VOL_QUOTES[i][2];
      str[i][1] = VOL_QUOTES[i][4];
    }
    InterpolatedStrikeSmileDeltaTermStructure term = InterpolatedStrikeSmileDeltaTermStructure.of(
        DoubleArray.copyOf(expiry), DoubleArray.of(0.1, 0.25), DoubleArray.copyOf(atm), DoubleMatrix.copyOf(rr),
        DoubleMatrix.copyOf(str), ACT_365F, LINEAR, FLAT, FLAT, PCHIP, FLAT, FLAT);
    EXP_VOLS = BlackFxOptionSmileVolatilities.of(VOL_NAME, GBP_USD, VALUATION_DATE.atTime(VALUATION_TIME).atZone(ZONE), term);
    for (int i = 0; i < nSmiles; ++i) {
      atm[i] = VOL_QUOTES_1[i][0];
      rr[i][0] = VOL_QUOTES_1[i][1];
      rr[i][1] = VOL_QUOTES_1[i][3];
      str[i][0] = VOL_QUOTES_1[i][2];
      str[i][1] = VOL_QUOTES_1[i][4];
    }
    InterpolatedStrikeSmileDeltaTermStructure term1 = InterpolatedStrikeSmileDeltaTermStructure.of(
        DoubleArray.copyOf(expiry), DoubleArray.of(0.1, 0.25), DoubleArray.copyOf(atm), DoubleMatrix.copyOf(rr),
        DoubleMatrix.copyOf(str), ACT_365F, LINEAR, FLAT, FLAT, PCHIP, FLAT, FLAT);
    EXP_VOLS_1 = BlackFxOptionSmileVolatilities.of(
        VOL_NAME, GBP_USD, VALUATION_DATE_1.atTime(VALUATION_TIME_1).atZone(ZONE), term1);
  }
  private static final BlackFxVanillaOptionTradePricer PRICER = BlackFxVanillaOptionTradePricer.DEFAULT;

  public void test_singleMarketData() {
    MarketData marketDataCalibrated = StandardComponents.marketDataFactory().create(
        REQUIREMENTS, CONFIG, MARKET_DATA, REF_DATA);
    Results results = CALC_RUNNER.calculate(RULES, TARGETS, COLUMN, marketDataCalibrated, REF_DATA);
    CurrencyAmount computed = results.get(0, 0, CurrencyAmount.class).getValue();
    CurrencyAmount expected = PRICER.presentValue(OPTION_TRADE.resolve(REF_DATA), EXP_RATES, EXP_VOLS)
        .convertedTo(USD, EXP_RATES);
    assertEquals(computed, expected);
  }

  public void test_scenarioMarketData() {
    ScenarioMarketData marketDataCalibrated = StandardComponents.marketDataFactory().createMultiScenario(
        REQUIREMENTS, SCENARIO_CONFIG, SCENARIO_MARKET_DATA, REF_DATA, ScenarioDefinition.empty());
    Results results = CALC_RUNNER.calculateMultiScenario(RULES, TARGETS, COLUMN, marketDataCalibrated, REF_DATA);
    CurrencyScenarioArray pvs = results.get(0, 0, CurrencyScenarioArray.class).getValue();
    CurrencyAmount pv0 = PRICER.presentValue(OPTION_TRADE.resolve(REF_DATA), EXP_RATES, EXP_VOLS)
        .convertedTo(USD, EXP_RATES);
    CurrencyAmount pv1 = PRICER.presentValue(OPTION_TRADE.resolve(REF_DATA), EXP_RATES_1, EXP_VOLS_1)
        .convertedTo(USD, EXP_RATES_1);
    assertEquals(pvs.get(0), pv0);
    assertEquals(pvs.get(1), pv1);
  }

  public void test_quote_secenarioDefinition() {
    List<PerturbationMapping<?>> perturbationMapping = new ArrayList<>();
    int nScenarios = 3;
    for (Entry<QuoteId, Double> entry : MARKET_QUOTES.entrySet()) {
      DoubleArray shifts = DoubleArray.of(nScenarios, n -> Math.pow(0.9, n));
      ScenarioPerturbation<Double> perturb = GenericDoubleShifts.of(ShiftType.SCALED, shifts);
      perturbationMapping.add(PerturbationMapping.of(MarketDataFilter.ofId(entry.getKey()), perturb));
    }
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(perturbationMapping);
    ScenarioMarketData marketDataCalibrated = StandardComponents.marketDataFactory().createMultiScenario(
        REQUIREMENTS, SCENARIO_CONFIG, MARKET_DATA, REF_DATA, scenarioDefinition);
    Results results = CALC_RUNNER.calculateMultiScenario(RULES, TARGETS, COLUMN, marketDataCalibrated, REF_DATA);
    CurrencyScenarioArray pvs = results.get(0, 0, CurrencyScenarioArray.class).getValue();
    for (int i = 0; i < nScenarios; ++i) {
      ImmutableMap.Builder<QuoteId, Double> builder = ImmutableMap.builder();
      for (Entry<QuoteId, Double> entry : MARKET_QUOTES.entrySet()) {
        builder.put(entry.getKey(), entry.getValue() * Math.pow(0.9, i));
      }
      ImmutableMarketData shiftedMarketData = ImmutableMarketData.builder(VALUATION_DATE)
          .addValueMap(builder.build())
          .addValueMap(MARKET_FX_QUOTES)
          .build();
      MarketData shiftedMarketDataCalibrated = StandardComponents.marketDataFactory().create(
          REQUIREMENTS, CONFIG, shiftedMarketData, REF_DATA);
      Results shiftedResults = CALC_RUNNER.calculate(RULES, TARGETS, COLUMN, shiftedMarketDataCalibrated, REF_DATA);
      CurrencyAmount pv = shiftedResults.get(0, 0, CurrencyAmount.class).getValue();
      assertEquals(pvs.get(i), pv);
    }
  }

  public void test_parameter_secenarioDefinition() {
    List<PerturbationMapping<?>> perturbationMapping = new ArrayList<>();
    int nVolParams = EXP_VOLS.getParameterCount();
    int nScenarios = 3;
    PointShiftsBuilder builder = PointShifts.builder(ShiftType.SCALED);
    for (int i = 0; i < nVolParams; ++i) {
      Object id = EXP_VOLS.getParameterMetadata(i).getIdentifier();
      for (int j = 0; j < nScenarios; ++j) {
        builder.addShift(j, id, Math.pow(0.9, j));
      }
    }
    ScenarioPerturbation<ParameterizedData> perturb = builder.build();
    perturbationMapping.add(PerturbationMapping.of(MarketDataFilter.ofId(VOL_ID), perturb));
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(perturbationMapping);
    ScenarioMarketData marketDataCalibrated = StandardComponents.marketDataFactory().createMultiScenario(
        REQUIREMENTS, SCENARIO_CONFIG, MARKET_DATA, REF_DATA, scenarioDefinition);
    Results results = CALC_RUNNER.calculateMultiScenario(RULES, TARGETS, COLUMN, marketDataCalibrated, REF_DATA);
    CurrencyScenarioArray pvs = results.get(0, 0, CurrencyScenarioArray.class).getValue();
    for (int i = 0; i < nScenarios; ++i) {
      int index = i;
      BlackFxOptionSmileVolatilities shiftedSmile = EXP_VOLS.withPerturbation((j, v, m) -> Math.pow(0.9, index) * v);
      CurrencyAmount pv = PRICER.presentValue(OPTION_TRADE.resolve(REF_DATA), EXP_RATES, shiftedSmile)
          .convertedTo(USD, EXP_RATES);
      assertEquals(pvs.get(i), pv);
    }
  }

  public void test_builtData() {
    List<PerturbationMapping<?>> perturbationMapping = new ArrayList<>();
    int nScenarios = 3;
    for (Entry<QuoteId, Double> entry : MARKET_QUOTES.entrySet()) {
      DoubleArray shifts = DoubleArray.of(nScenarios, n -> Math.pow(0.9, n));
      ScenarioPerturbation<Double> perturb = GenericDoubleShifts.of(ShiftType.SCALED, shifts);
      perturbationMapping.add(PerturbationMapping.of(MarketDataFilter.ofId(entry.getKey()), perturb));
    }
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(perturbationMapping);
    ImmutableMarketData dataWithSurface = ImmutableMarketData.builder(VALUATION_DATE)
        .addValueMap(MARKET_QUOTES)
        .addValueMap(MARKET_FX_QUOTES)
        .addValue(VOL_ID, EXP_VOLS)
        .addValue(RatesCurveGroupId.of(CURVE_GROUP_NAME),
            RatesCurveGroup.ofCurves(CURVE_GROUP_DEFINITION, EXP_RATES.toImmutableRatesProvider().getDiscountCurves().values()))
        .build();
    ScenarioMarketData marketDataCalibrated = StandardComponents.marketDataFactory().createMultiScenario(
        REQUIREMENTS, SCENARIO_CONFIG, dataWithSurface, REF_DATA, scenarioDefinition);
    Results results = CALC_RUNNER.calculateMultiScenario(RULES, TARGETS, COLUMN, marketDataCalibrated, REF_DATA);
    CurrencyScenarioArray computed = results.get(0, 0, CurrencyScenarioArray.class).getValue();
    CurrencyAmount expected = PRICER.presentValue(OPTION_TRADE.resolve(REF_DATA), EXP_RATES, EXP_VOLS)
        .convertedTo(USD, EXP_RATES);
    // dependency graph is absent, thus scenarios are not created
    assertTrue(computed.getScenarioCount() == 1);
    assertEquals(computed.get(0), expected);
  }

  private static final List<Tenor> SURFACE_TENORS = ImmutableList.of(Tenor.TENOR_3M, Tenor.TENOR_6M, Tenor.TENOR_1Y);
  private static final List<Double> SURFACE_STRIKES = ImmutableList.of(1.35, 1.5, 1.65, 1.7);
  private static final double[][] SURFACE_VOL_QUOTES = new double[][] {
      {0.19, 0.15, 0.13, 0.14}, {0.14, 0.11, 0.09, 0.09}, {0.11, 0.09, 0.07, 0.07}};
  private static final ImmutableList<FxOptionVolatilitiesNode> SURFACE_NODES;
  private static final ImmutableMap<QuoteId, Double> SURFACE_QUOTES;
  static {
    ImmutableList.Builder<FxOptionVolatilitiesNode> nodeBuilder = ImmutableList.builder();
    ImmutableMap.Builder<QuoteId, Double> quoteBuilder = ImmutableMap.builder();
    for (int i = 0; i < SURFACE_TENORS.size(); ++i) {
      for (int j = 0; j < SURFACE_STRIKES.size(); ++j) {
        QuoteId quoteId = QuoteId.of(StandardId.of(
            "OG", GBP_USD.toString() + "_" + SURFACE_TENORS.get(i).toString() + "_" + SURFACE_STRIKES.get(j)));
        quoteBuilder.put(quoteId, SURFACE_VOL_QUOTES[i][j]);
        nodeBuilder.add(FxOptionVolatilitiesNode.of(
            GBP_USD, SPOT_OFFSET, BDA, ValueType.BLACK_VOLATILITY, quoteId, SURFACE_TENORS.get(i),
            SimpleStrike.of(SURFACE_STRIKES.get(j))));
      }
    }
    SURFACE_NODES = nodeBuilder.build();
    SURFACE_QUOTES = quoteBuilder.build();
  }
  private static final MarketData SURFACE_MARKET_DATA = MARKET_DATA.combinedWith(MarketData.of(VALUATION_DATE, SURFACE_QUOTES));
  private static final FxOptionVolatilitiesDefinition VOL_DEFINITION_SURFACE = FxOptionVolatilitiesDefinition.of(
      BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.builder()
          .name(VOL_NAME)
          .currencyPair(GBP_USD)
          .dayCount(ACT_365F)
          .nodes(SURFACE_NODES)
          .timeInterpolator(LINEAR)
          .timeExtrapolatorLeft(FLAT)
          .timeExtrapolatorRight(FLAT)
          .strikeInterpolator(PCHIP)
          .strikeExtrapolatorLeft(FLAT)
          .strikeExtrapolatorLeft(FLAT)
          .build());
  private static final MarketDataConfig SURFACE_CONFIG = MarketDataConfig.builder()
      .add(CURVE_GROUP_NAME, CURVE_GROUP_DEFINITION)
      .add(VOL_ID.getName().getName(), VOL_DEFINITION_SURFACE)
      .addDefault(ZT_DEFINITION)
      .build();
  private static final BlackFxOptionSurfaceVolatilities SURFACE_EXP_VOLS;
  static {
    List<Double> expiry = new ArrayList<>();
    List<Double> strike = new ArrayList<>();
    List<Double> vols = new ArrayList<>();
    for (int i = 0; i < SURFACE_TENORS.size(); ++i) {
      for (int j = 0; j < SURFACE_STRIKES.size(); ++j) {
        double yearFraction = ACT_365F.relativeYearFraction(
            VALUATION_DATE, BDA.adjust(SPOT_OFFSET.adjust(VALUATION_DATE, REF_DATA).plus(SURFACE_TENORS.get(i)), REF_DATA));
        expiry.add(yearFraction);
        strike.add(SURFACE_STRIKES.get(j));
        vols.add(SURFACE_VOL_QUOTES[i][j]);
      }
    }
    SurfaceInterpolator interp = GridSurfaceInterpolator.of(LINEAR, PCHIP);
    InterpolatedNodalSurface surface = InterpolatedNodalSurface.ofUnsorted(
        Surfaces.blackVolatilityByExpiryStrike(VOL_NAME.getName(), ACT_365F),
        DoubleArray.copyOf(expiry), DoubleArray.copyOf(strike), DoubleArray.copyOf(vols), interp);
    SURFACE_EXP_VOLS = BlackFxOptionSurfaceVolatilities.of(
        VOL_NAME, GBP_USD, VALUATION_DATE.atTime(VALUATION_TIME).atZone(ZONE), surface);
  }

  public void test_surface() {
    MarketData marketDataCalibrated = StandardComponents.marketDataFactory().create(
        REQUIREMENTS, SURFACE_CONFIG, SURFACE_MARKET_DATA, REF_DATA);
    Results results = CALC_RUNNER.calculate(RULES, TARGETS, COLUMN, marketDataCalibrated, REF_DATA);
    CurrencyAmount computed = results.get(0, 0, CurrencyAmount.class).getValue();
    CurrencyAmount expected = PRICER.presentValue(OPTION_TRADE.resolve(REF_DATA), EXP_RATES, SURFACE_EXP_VOLS)
        .convertedTo(USD, EXP_RATES);
    assertEquals(computed, expected);
  }

}
