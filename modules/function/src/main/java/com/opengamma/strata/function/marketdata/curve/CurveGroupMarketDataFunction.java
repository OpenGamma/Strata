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
import com.opengamma.strata.basics.market.ObservableValues;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.function.MarketDataFunction;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.ParRates;
import com.opengamma.strata.market.curve.definition.CurveGroupDefinition;
import com.opengamma.strata.market.curve.definition.CurveGroupEntry;
import com.opengamma.strata.market.curve.definition.CurveNode;
import com.opengamma.strata.market.curve.definition.InterpolatedNodalCurveDefinition;
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
    Optional<CurveGroupDefinition> optionalGroup = marketDataConfig.get(CurveGroupDefinition.class, id.getName());

    if (!optionalGroup.isPresent()) {
      return MarketDataRequirements.empty();
    }
    CurveGroupDefinition groupDefn = optionalGroup.get();

    // request par rates for any curves that need market data
    // no par rates are requested if the curve definition contains all the market data needed to build the curve
    List<ParRatesId> parRatesIds = groupDefn.getEntries().stream()
        .filter(entry -> entry.getCurveDefinition() instanceof InterpolatedNodalCurveDefinition)
        .filter(entry -> requiresMarketData((InterpolatedNodalCurveDefinition) entry.getCurveDefinition()))
        .map(entry -> entry.getCurveDefinition().getName())
        .map(curveName -> ParRatesId.of(groupDefn.getName(), curveName, id.getMarketDataFeed()))
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
   * @param curveDefn  the curve definition
   * @return true if the curve requires market data for calibration
   */
  private boolean requiresMarketData(InterpolatedNodalCurveDefinition curveDefn) {
    return curveDefn.getNodes().stream().anyMatch(node -> !node.requirements().isEmpty());
  }

  @Override
  public Result<CurveGroup> build(CurveGroupId id, MarketDataLookup marketData, MarketDataConfig marketDataConfig) {
    CurveGroupName groupName = id.getName();
    Optional<CurveGroupDefinition> optionalGroup = marketDataConfig.get(CurveGroupDefinition.class, groupName);

    if (!optionalGroup.isPresent()) {
      return Result.failure(FailureReason.MISSING_DATA, "No configuration found for curve group '{}'", groupName);
    }
    CurveGroupDefinition groupDefn = optionalGroup.get();
    return buildCurveGroup(groupDefn, marketData, id.getMarketDataFeed());
  }

  @Override
  public Class<CurveGroupId> getMarketDataIdType() {
    return CurveGroupId.class;
  }

  /**
   * Builds a curve group given the configuration for the group and a set of market data.
   *
   * @param groupDefn  the definition of the curve group
   * @param marketData  the market data containing any values required to build the curve group
   * @param feed  the market data feed that is the source of the observable data
   * @return a result containing the curve group or details of why it couldn't be built
   */
  Result<CurveGroup> buildCurveGroup(
      CurveGroupDefinition groupDefn,
      MarketDataLookup marketData,
      MarketDataFeed feed) {

    List<SingleCurveBundle<GeneratorYDCurve>> singleCurveBundles = new ArrayList<>();
    Multimap<String, IborIndex> iborIndicesByCurveName = ArrayListMultimap.create();
    Multimap<String, IndexON> onIndicesByCurveName = ArrayListMultimap.create();
    Map<String, Currency> discountingCurrenciesByCurveName = new HashMap<>();
    Map<String, CurveMetadata> curveMetadata = new HashMap<>();
    CurveGroupName groupName = groupDefn.getName();
    LocalDate valuationDate = marketData.getValuationDate();

    for (CurveGroupEntry curveEntry : groupDefn.getEntries()) {
      // We can only handle InterpolatedNodalCurveDefinition for now
      if (!(curveEntry.getCurveDefinition() instanceof InterpolatedNodalCurveDefinition)) {
        return Result.failure(
            FailureReason.NOT_APPLICABLE,
            "Only InterpolatedNodalCurveDefinition is supported, cannot build curve '{}', group '{}' from configuration type {}",
            curveEntry.getCurveDefinition().getName(),
            groupName,
            curveEntry.getCurveDefinition().getClass().getName());
      }
      InterpolatedNodalCurveDefinition curveDefn = (InterpolatedNodalCurveDefinition) curveEntry.getCurveDefinition();
      CurveName curveName = curveDefn.getName();
      CurveMetadata metadata = curveDefn.metadata(valuationDate);
      curveMetadata.put(curveName.toString(), metadata);
      Result<ParRates> parRatesResult = parRates(curveDefn, marketData, groupName, feed);

      if (!parRatesResult.isSuccess()) {
        return Result.failure(parRatesResult);
      }
      ParRates parRates = parRatesResult.getValue();
      List<CurveNode> curveNodes = curveDefn.getNodes();
      List<InstrumentDerivative> derivatives = createDerivatives(curveNodes, parRates, valuationDate);

      Set<com.opengamma.strata.basics.index.IborIndex> iborIndices = curveEntry.getIborIndices();
      Set<OvernightIndex> overnightIndices = curveEntry.getOvernightIndices();
      Set<Currency> discountingCurrencies = curveEntry.getDiscountCurrencies();

      iborIndices.stream().forEach(idx -> iborIndicesByCurveName.put(curveName.toString(), Legacy.iborIndex(idx)));
      iborIndices.stream().forEach(idx -> iborIndicesByCurveName.put(curveName.toString(), Legacy.iborIndex(idx)));
      overnightIndices.stream().forEach(idx -> onIndicesByCurveName.put(curveName.toString(), Legacy.overnightIndex(idx)));
      discountingCurrencies.stream().forEach(ccy -> discountingCurrenciesByCurveName.put(curveName.toString(), ccy));
      singleCurveBundles.add(createSingleCurveBundle(curveDefn, derivatives));
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

    Map<Currency, Curve> discountCurves = legacyDiscountCurves.entrySet().stream()
        .collect(toImmutableMap(tp -> tp.getKey(), tp -> createCurve(tp.getValue(), curveMetadata)));

    Map<Index, Curve> iborCurves = legacyIborCurves.entrySet().stream()
        .collect(toImmutableMap(tp -> Legacy.iborIndex(tp.getKey()), tp -> createCurve(tp.getValue(), curveMetadata)));

    Map<Index, Curve> overnightCurves = legacyOvernightCurves.entrySet().stream()
        .collect(toImmutableMap(tp -> Legacy.overnightIndex(tp.getKey()), tp -> createCurve(tp.getValue(), curveMetadata)));

    Map<Index, Curve> forwardCurves = ImmutableMap.<Index, Curve>builder()
        .putAll(iborCurves)
        .putAll(overnightCurves)
        .build();

    CurveGroup curveGroup = CurveGroup.of(groupDefn.getName(), discountCurves, forwardCurves);
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
   * @param curveDefn  the curve definition
   * @param marketData  a set of market data
   * @param groupName  the name of the curve group being built
   * @param feed  the market data feed that is the source of the underlying market data
   * @return the par rates required for the curve if available.
   */
  private Result<ParRates> parRates(
      InterpolatedNodalCurveDefinition curveDefn,
      MarketDataLookup marketData,
      CurveGroupName groupName,
      MarketDataFeed feed) {

    // Only try to get par rates from the market data if the curve needs market data
    if (requiresMarketData(curveDefn)) {
      ParRatesId parRatesId = ParRatesId.of(groupName, curveDefn.getName(), feed);

      if (!marketData.containsValue(parRatesId)) {
        return Result.failure(FailureReason.MISSING_DATA, "No par rates for {}", parRatesId);
      }
      return Result.success(marketData.getValue(parRatesId));
    } else {
      return Result.success(ParRates.builder().build());
    }
  }

  private SingleCurveBundle<GeneratorYDCurve> createSingleCurveBundle(
      InterpolatedNodalCurveDefinition curveDefn,
      List<InstrumentDerivative> derivatives) {

    double[] parameterGuessForCurves = new double[derivatives.size()];
    Arrays.fill(parameterGuessForCurves, 0.02);
    GeneratorYDCurve curveGenerator = createCurveGenerator(curveDefn);
    double[] startingPoint = curveGenerator.initialGuess(parameterGuessForCurves);
    InstrumentDerivative[] derivativeArray = derivatives.toArray(new InstrumentDerivative[derivatives.size()]);
    return new SingleCurveBundle<>(curveDefn.getName().toString(), derivativeArray, startingPoint, curveGenerator);
  }

  private List<InstrumentDerivative> createDerivatives(
      List<CurveNode> nodes,
      ParRates parRates,
      LocalDate valuationDate) {

    return nodes.stream()
        .map(node -> node.trade(valuationDate, ObservableValues.ofIdMap(parRates.getRates())))
        .map(trade -> TradeToDerivativeConverter.convert(trade, valuationDate))
        .collect(toImmutableList());
  }

  /**
   * Creates a curve generator for a curve.
   *
   * @param curveDefn  the curve definition
   * @return a generator capable of generating the curve
   */
  private GeneratorYDCurve createCurveGenerator(InterpolatedNodalCurveDefinition curveDefn) {
    CombinedInterpolatorExtrapolator interpolatorExtrapolator = CombinedInterpolatorExtrapolator.of(
        curveDefn.getInterpolator(),
        curveDefn.getExtrapolatorLeft(),
        curveDefn.getExtrapolatorRight());

    return new GeneratorCurveYieldInterpolated(LastTimeCalculator.getInstance(), interpolatorExtrapolator);
  }

}
