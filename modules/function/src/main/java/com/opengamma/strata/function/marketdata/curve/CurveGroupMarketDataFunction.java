/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jooq.lambda.Seq;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.opengamma.analytics.financial.curve.interestrate.generator.GeneratorCurveYieldInterpolated;
import com.opengamma.analytics.financial.curve.interestrate.generator.GeneratorYDCurve;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.instrument.index.IndexON;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParSpreadMarketQuoteDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.generic.LastTimeCalculator;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.curve.MultiCurveBundle;
import com.opengamma.analytics.financial.provider.curve.SingleCurveBundle;
import com.opengamma.analytics.financial.provider.curve.multicurve.MulticurveDiscountBuildingRepository;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.ParRates;
import com.opengamma.strata.market.curve.config.CurveGroupConfig;
import com.opengamma.strata.market.curve.config.CurveGroupEntry;
import com.opengamma.strata.market.curve.config.CurveNode;
import com.opengamma.strata.market.curve.config.InterpolatedCurveConfig;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.ParRatesId;
import com.opengamma.strata.pricer.impl.Legacy;

/**
 * Market data function that builds a {@link CurveGroup}.
 */
public class CurveGroupMarketDataFunction implements MarketDataFunction<CurveGroup, CurveGroupId> {

  /** The analytics object that performs the curve calibration. */
  private final MulticurveDiscountBuildingRepository curveBuilder;

  // TODO Where should the root finder config come from?
  //   Should it be possible to override it for each call? Put it in MarketDataConfig?
  //   Is it a system-wide setting?
  /**
   * Creates a new function for building curve groups that delegates to {@code curveBuilder} to perform calibration.
   *
   * @param rootFinderConfig  configuration for the root finder used when calibrating curves
   */
  public CurveGroupMarketDataFunction(RootFinderConfig rootFinderConfig) {
    this.curveBuilder = new MulticurveDiscountBuildingRepository(
        rootFinderConfig.getAbsoluteTolerance(),
        rootFinderConfig.getRelativeTolerance(),
        rootFinderConfig.getMaximumSteps());
  }

  @Override
  public MarketDataRequirements requirements(CurveGroupId id, MarketDataConfig marketDataConfig) {
    Optional<CurveGroupConfig> optionalConfig = marketDataConfig.get(CurveGroupConfig.class, id.getName());

    if (!optionalConfig.isPresent()) {
      return MarketDataRequirements.empty();
    }
    CurveGroupConfig groupConfig = optionalConfig.get();

    // Request par rates for any curves that need market data.
    // No par rates are requested if the curve config contains all the market data needed to build the curve.
    List<ParRatesId> parRatesIds = groupConfig.getEntries().stream()
        .filter(entry -> entry.getCurveConfig() instanceof InterpolatedCurveConfig)
        .filter(entry -> requiresMarketData((InterpolatedCurveConfig) entry.getCurveConfig()))
        .map(entry -> entry.getCurveConfig().getName())
        .map(curveName -> ParRatesId.of(groupConfig.getName(), curveName, id.getMarketDataFeed()))
        .collect(toImmutableList());

    return MarketDataRequirements.builder().addValues(parRatesIds).build();
  }

  /**
   * Checks if the curve configuration requires market data.
   * <p>
   * If the curve configuration contains all the data required to build the curve it is not necessary to
   * request par rates for the curve points. However if market data is required for any point on the
   * curve this function must add {@link ParRates} to its market data requirements.
   *
   * @param curveConfig  configuration for a curve
   * @return true if the curve requires market data for calibration
   */
  private boolean requiresMarketData(InterpolatedCurveConfig curveConfig) {
    return curveConfig.getNodes().stream().anyMatch(node -> !node.requirements().isEmpty());
  }

  @Override
  public Result<CurveGroup> build(CurveGroupId id, MarketDataLookup marketData, MarketDataConfig marketDataConfig) {
    CurveGroupName groupName = id.getName();
    Optional<CurveGroupConfig> optionalGroup = marketDataConfig.get(CurveGroupConfig.class, groupName);

    if (!optionalGroup.isPresent()) {
      return Result.failure(FailureReason.MISSING_DATA, "No configuration found for curve group '{}'", groupName);
    }
    CurveGroupConfig groupConfig = optionalGroup.get();
    return buildCurveGroup(groupConfig, marketData, id.getMarketDataFeed());
  }

  @Override
  public Class<CurveGroupId> getMarketDataIdType() {
    return CurveGroupId.class;
  }

  /**
   * Builds a curve group given the configuration for the group and a set of market data.
   *
   * @param groupConfig  configuration for a curve group
   * @param marketData  market data containing any values required to build the curve group
   * @param feed  the market data feed that is the source of the observable data
   * @return a result containing the curve group or details of why it couldn't be built
   */
  public Result<CurveGroup> buildCurveGroup(
      CurveGroupConfig groupConfig,
      MarketDataLookup marketData,
      MarketDataFeed feed) {

    List<SingleCurveBundle<GeneratorYDCurve>> singleCurveBundles = new ArrayList<>();
    Multimap<String, IborIndex> iborIndicesByCurveName = ArrayListMultimap.create();
    Multimap<String, IndexON> onIndicesByCurveName = ArrayListMultimap.create();
    Map<String, Currency> discountingCurrenciesByCurveName = new HashMap<>();
    Map<String, CurveMetadata> curveMetadata = new HashMap<>();
    CurveGroupName groupName = groupConfig.getName();
    LocalDate valuationDate = marketData.getValuationDate();

    for (CurveGroupEntry curveEntry : groupConfig.getEntries()) {
      // We can only handle InterpolatedCurveConfig for now
      if (!(curveEntry.getCurveConfig() instanceof InterpolatedCurveConfig)) {
        return Result.failure(
            FailureReason.NOT_APPLICABLE,
            "Only InterpolatedCurveConfig is supported, cannot build curve '{}', group '{}' from configuration type {}",
            curveEntry.getCurveConfig().getName(),
            groupName,
            curveEntry.getCurveConfig().getClass().getName());
      }
      InterpolatedCurveConfig curveConfig = (InterpolatedCurveConfig) curveEntry.getCurveConfig();
      CurveName curveName = curveConfig.getName();
      CurveMetadata metadata = curveConfig.metadata(valuationDate);
      curveMetadata.put(curveName.toString(), metadata);
      Result<ParRates> parRatesResult = parRates(curveConfig, marketData, groupName, feed);

      if (!parRatesResult.isSuccess()) {
        return Result.failure(parRatesResult);
      }
      ParRates parRates = parRatesResult.getValue();
      List<CurveNode> curveNodes = curveConfig.getNodes();
      List<InstrumentDerivative> derivatives = createDerivatives(curveNodes, parRates, valuationDate);

      Set<com.opengamma.strata.basics.index.IborIndex> iborIndices = curveEntry.getIborIndices();
      Set<OvernightIndex> overnightIndices = curveEntry.getOvernightIndices();
      Optional<Currency> discountingCurrency = curveEntry.getDiscountingCurrency();

      iborIndices.stream().forEach(idx -> iborIndicesByCurveName.put(curveName.toString(), Legacy.iborIndex(idx)));
      overnightIndices.stream().forEach(idx -> onIndicesByCurveName.put(curveName.toString(), Legacy.overnightIndex(idx)));
      discountingCurrency.ifPresent(currency -> discountingCurrenciesByCurveName.put(curveName.toString(), currency));
      singleCurveBundles.add(createSingleCurveBundle(curveConfig, derivatives));
    }
    @SuppressWarnings("rawtypes")
    SingleCurveBundle[] singleBundleArray = singleCurveBundles.toArray(new SingleCurveBundle[singleCurveBundles.size()]);
    @SuppressWarnings("unchecked")
    List<MultiCurveBundle<GeneratorYDCurve>> curveBundles = ImmutableList.of(new MultiCurveBundle<>(singleBundleArray));

    Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> calibratedCurves =
        curveBuilder.makeCurvesFromDerivatives(
            curveBundles,
            new MulticurveProviderDiscount(),
            discountingCurrenciesByCurveName,
            iborIndicesByCurveName,
            onIndicesByCurveName,
            ParSpreadMarketQuoteDiscountingCalculator.getInstance(),
            ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator.getInstance());

    MulticurveProviderDiscount multicurve = calibratedCurves.getFirst();
    Map<Currency, YieldAndDiscountCurve> legacyDiscountCurves = multicurve.getDiscountingCurves();
    Map<IborIndex, YieldAndDiscountCurve> legacyIborCurves = multicurve.getForwardIborCurves();
    Map<IndexON, YieldAndDiscountCurve> legacyOvernightCurves = multicurve.getForwardONCurves();

    Map<Currency, Curve> discountCurves =
        Seq.seq(legacyDiscountCurves).toMap(tp -> tp.v1, tp -> createCurve(tp.v2, curveMetadata));

    Map<Index, Curve> iborCurves =
        Seq.seq(legacyIborCurves).toMap(tp -> Legacy.iborIndex(tp.v1), tp -> createCurve(tp.v2, curveMetadata));

    Map<Index, Curve> overnightCurves =
        Seq.seq(legacyOvernightCurves).toMap(tp -> Legacy.overnightIndex(tp.v1), tp -> createCurve(tp.v2, curveMetadata));

    Map<Index, Curve> forwardCurves = ImmutableMap.<Index, Curve>builder()
        .putAll(iborCurves)
        .putAll(overnightCurves)
        .build();

    CurveGroup curveGroup = CurveGroup.of(groupConfig.getName(), discountCurves, forwardCurves);
    return Result.success(curveGroup);
  }

  /**
   * Creates a new-style curve from a legacy curve and a set of curve metadata.
   */
  private static Curve createCurve(YieldAndDiscountCurve curve, Map<String, CurveMetadata> curveMetadata) {
    return Legacy.curve(curve, curveMetadata.get(curve.getName()));
  }

  /**
   * Returns the par rates required for the curve if available.
   * <p>
   * If no market data is required to build the curve an empty set of par rates is returned.
   * <p>
   * If the curve requires par rates which are available in {@code marketData} they are returned.
   * <p>
   * If the curve requires par rates which are not available in {@code marketData} a failure is returned.
   *
   * @param curveConfig  configuration for a curve
   * @param marketData  a set of market data
   * @param groupName  the name of the curve group being built
   * @param feed  the market data feed that is the source of the underlying market data
   * @return the par rates required for the curve if available.
   */
  private Result<ParRates> parRates(
      InterpolatedCurveConfig curveConfig,
      MarketDataLookup marketData,
      CurveGroupName groupName,
      MarketDataFeed feed) {

    // Only try to get par rates from the market data if the curve needs market data
    if (requiresMarketData(curveConfig)) {
      ParRatesId parRatesId = ParRatesId.of(groupName, curveConfig.getName(), feed);

      if (!marketData.containsValue(parRatesId)) {
        return Result.failure(FailureReason.MISSING_DATA, "No par rates for {}", parRatesId);
      }
      return Result.success(marketData.getValue(parRatesId));
    } else {
      return Result.success(ParRates.builder().build());
    }
  }

  private SingleCurveBundle<GeneratorYDCurve> createSingleCurveBundle(
      InterpolatedCurveConfig curveConfig,
      List<InstrumentDerivative> derivatives) {

    double[] parameterGuessForCurves = new double[derivatives.size()];
    Arrays.fill(parameterGuessForCurves, 0.02);
    GeneratorYDCurve curveGenerator = createCurveGenerator(curveConfig);
    double[] startingPoint = curveGenerator.initialGuess(parameterGuessForCurves);
    InstrumentDerivative[] derivativeArray = derivatives.toArray(new InstrumentDerivative[derivatives.size()]);
    return new SingleCurveBundle<>(curveConfig.getName().toString(), derivativeArray, startingPoint, curveGenerator);
  }

  private List<InstrumentDerivative> createDerivatives(
      List<CurveNode> nodes,
      ParRates parRates,
      LocalDate valuationDate) {

    Map<ObservableKey, Double> parRateValues = parRates.getRates().entrySet().stream()
        .collect(toImmutableMap(entry -> entry.getKey().toObservableKey(), Map.Entry::getValue));

    return nodes.stream()
        .map(node -> node.trade(valuationDate, parRateValues))
        .map(trade -> TradeToDerivativeConverter.convert(trade, valuationDate))
        .collect(toImmutableList());
  }

  /**
   * Creates a curve generator for a curve.
   *
   * @param curveConfig  configuration defining the curve
   * @return a generator capable of generating the curve
   */
  private GeneratorYDCurve createCurveGenerator(InterpolatedCurveConfig curveConfig) {
    CombinedInterpolatorExtrapolator interpolatorExtrapolator = CombinedInterpolatorExtrapolator.of(
        curveConfig.getInterpolator(),
        curveConfig.getLeftExtrapolator(),
        curveConfig.getRightExtrapolator());

    return new GeneratorCurveYieldInterpolated(LastTimeCalculator.getInstance(), interpolatorExtrapolator);
  }
}
