/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;


import static com.opengamma.financial.convention.businessday.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.propagateFailure;
import static com.opengamma.util.result.ResultGenerator.success;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.curve.interestrate.generator.GeneratorCurveYieldInterpolated;
import com.opengamma.analytics.financial.curve.interestrate.generator.GeneratorCurveYieldInterpolatedAnchorNode;
import com.opengamma.analytics.financial.curve.interestrate.generator.GeneratorYDCurve;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.instrument.index.IndexON;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.cash.derivative.Cash;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParRateDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParSpreadMarketQuoteDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.generic.LastTimeCalculator;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.curve.MultiCurveBundle;
import com.opengamma.analytics.financial.provider.curve.SingleCurveBundle;
import com.opengamma.analytics.financial.provider.curve.multicurve.MulticurveDiscountBuildingRepository;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.ProviderUtils;
import com.opengamma.analytics.financial.schedule.ScheduleCalculator;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.marketdatasnapshot.SnapshotDataBundle;
import com.opengamma.core.region.RegionSource;
import com.opengamma.financial.analytics.conversion.CalendarUtils;
import com.opengamma.financial.analytics.conversion.CurveNodeConverter;
import com.opengamma.financial.analytics.curve.CashNodeConverter;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.CurveGroupConfiguration;
import com.opengamma.financial.analytics.curve.CurveNodeVisitorAdapter;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.curve.CurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.DiscountingCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.FRANodeConverter;
import com.opengamma.financial.analytics.curve.FXForwardNodeConverter;
import com.opengamma.financial.analytics.curve.FixedDateInterpolatedCurveDefinition;
import com.opengamma.financial.analytics.curve.IborCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.InterpolatedCurveDefinition;
import com.opengamma.financial.analytics.curve.OvernightCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.RateFutureNodeConverter;
import com.opengamma.financial.analytics.curve.RollDateFRANodeConverter;
import com.opengamma.financial.analytics.curve.RollDateSwapNodeConverter;
import com.opengamma.financial.analytics.curve.SwapNodeConverter;
import com.opengamma.financial.analytics.curve.ThreeLegBasisSwapNodeConverter;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeVisitor;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.convention.IborIndexConvention;
import com.opengamma.financial.convention.OvernightIndexConvention;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.result.SuccessStatus;
import com.opengamma.util.time.Tenor;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Triple;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

public class DefaultDiscountingMulticurveBundleFn implements DiscountingMulticurveBundleFn {

  private final CurveDefinitionFn _curveDefinitionProvider;
  private final CurveSpecificationFn _curveSpecificationProvider;
  private final CurveSpecificationMarketDataFn _curveSpecificationMarketDataProvider;
  private final ValuationTimeFn _valuationTimeProvider;
  private final FXMatrixFn _fxMatrixProvider;
  private final HistoricalTimeSeriesFn _historicalTimeSeriesProvider;

  private final ConfigSource _configSource;
  private final ConventionSource _conventionSource;
  private final HolidaySource _holidaySource;
  private final RegionSource _regionSource;

  private final RootFinderConfiguration _rootFinderConfiguration;


  /**
   * Map indicating which curves should be implied, and if so which curves they should
   * be implied from.
   */
  private final Set<String> _impliedCurveNames;

  public DefaultDiscountingMulticurveBundleFn(CurveDefinitionFn curveDefinitionProvider,
                                              CurveSpecificationFn curveSpecificationProvider,
                                              CurveSpecificationMarketDataFn curveSpecificationMarketDataProvider,
                                              ValuationTimeFn valuationTimeProvider,
                                              FXMatrixFn fxMatrixProvider,
                                              HistoricalTimeSeriesFn historicalTimeSeriesProvider,
                                              ConfigSource configSource,
                                              ConventionSource conventionSource,
                                              HolidaySource holidaySource,
                                              RegionSource regionSource,
                                              RootFinderConfiguration rootFinderConfiguration,
                                              Set<String> impliedCurveNames) {

    _curveDefinitionProvider = curveDefinitionProvider;
    _curveSpecificationProvider = curveSpecificationProvider;
    _curveSpecificationMarketDataProvider = curveSpecificationMarketDataProvider;
    _valuationTimeProvider = valuationTimeProvider;
    _fxMatrixProvider = fxMatrixProvider;
    _historicalTimeSeriesProvider = historicalTimeSeriesProvider;
    _configSource = configSource;
    _conventionSource = conventionSource;
    _holidaySource = holidaySource;
    _regionSource = regionSource;
    _rootFinderConfiguration = rootFinderConfiguration;
    _impliedCurveNames = impliedCurveNames;
  }

  @Override
  public Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> generateBundle(
      CurveConstructionConfiguration curveConfig) {

    return generateBundle(curveConfig, _valuationTimeProvider.getTime());
  }

  private Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> generateBundle(
      CurveConstructionConfiguration curveConfig, ZonedDateTime valuationTime) {

    // Each curve config may have one or more exogenous requirements which basically should
    // point to another curve config (which may point to one or more configs ...)
    // We need a depth-first evaluation of the tree formed by these configs as (direct) child
    // MulticurveProviderInterface instances are required to be passed into their parent's
    // evaluation via the known data parameter

    // If we can't build due to insufficient market data, then we keep going but don't call
    // the final build step. This way we ensure that market data requirements have been captured.

    // todo check for cycles in the config

    Result<FXMatrix> fxMatrixResult = _fxMatrixProvider.getFXMatrix(curveConfig, valuationTime);
    Result<MulticurveProviderDiscount> exogenousBundles = buildExogenousBundles(curveConfig, fxMatrixResult, valuationTime);
    return getCurves(curveConfig, exogenousBundles, fxMatrixResult, valuationTime);

  }

  @Override
  public Result<Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>>> extractImpliedDepositCurveData(
      CurveConstructionConfiguration curveConfig, ZonedDateTime valuationTime) {

    // todo - this implementation is nowhere near complete
    Result<FXMatrix> fxMatrixResult = _fxMatrixProvider.getFXMatrix(curveConfig, valuationTime);
    Result<MulticurveProviderDiscount> exogenousBundles = buildExogenousBundles(curveConfig, fxMatrixResult, valuationTime);

    CurveGroupConfiguration group = curveConfig.getCurveGroups().get(0);
    Map.Entry<String, List<CurveTypeConfiguration>> type = group.getTypesForCurves().entrySet().iterator().next();
    Result<CurveDefinition> curveDefinition = _curveDefinitionProvider.getCurveDefinition(type.getKey());
    DiscountingCurveTypeConfiguration typeConfiguration = (DiscountingCurveTypeConfiguration) type.getValue().get(0);
    Currency currency = Currency.of(typeConfiguration.getReference());

    return success(extractImpliedDepositCurveData(currency,
                                                  curveDefinition.getValue(),
                                                  exogenousBundles.getValue(),
                                                  valuationTime));
  }

  private Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>> extractImpliedDepositCurveData(Currency currency,
                                                                                        CurveDefinition impliedCurveDefinition,
                                                                                        MulticurveProviderDiscount multicurves,
                                                                                        ZonedDateTime valuationTime) {

    final DayCount dayCount = DayCounts.ACT_365; //TODO

    final ParRateDiscountingCalculator parRateDiscountingCalculator = ParRateDiscountingCalculator.getInstance();
    final Calendar calendar = CalendarUtils.getCalendar(_holidaySource, currency);

    final List<Tenor> tenors = new ArrayList<>();
    final List<Double> parRates = new ArrayList<>();
    final List<InstrumentDerivative> cashNodes = new ArrayList<>();

    for (final CurveNode node : impliedCurveDefinition.getNodes()) {
      final Tenor tenor = node.getResolvedMaturity();
      final ZonedDateTime paymentDate =
          ScheduleCalculator.getAdjustedDate(valuationTime, tenor.getPeriod(), MODIFIED_FOLLOWING, calendar, true);
      final double endTime = TimeCalculator.getTimeBetween(valuationTime, paymentDate);
      final double accrualFactor = dayCount.getDayCountFraction(valuationTime, valuationTime.plus(tenor.getPeriod()), calendar);
      final Cash cashDepositNode = new Cash(currency, 0, endTime, 1, 0, accrualFactor);
      final double parRate = parRateDiscountingCalculator.visitCash(cashDepositNode, multicurves);
      tenors.add(tenor);
      cashNodes.add(new Cash(currency, 0, endTime, 1, parRate, accrualFactor));
      parRates.add(parRate);
    }

    return Triple.of(tenors, parRates, cashNodes);
  }

  private MulticurveDiscountBuildingRepository createBuilder() {
    return new MulticurveDiscountBuildingRepository(
        _rootFinderConfiguration.getAbsoluteTolerance(),
        _rootFinderConfiguration.getRelativeTolerance(),
        _rootFinderConfiguration.getMaxIterations());
  }

  private Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> getCurves(
      CurveConstructionConfiguration config,
      Result<MulticurveProviderDiscount> exogenousBundle,
      Result<FXMatrix> fxMatrixResult, ZonedDateTime valuationTime) {

    final int nGroups = config.getCurveGroups().size();

    @SuppressWarnings("unchecked")
    final MultiCurveBundle<GeneratorYDCurve>[] curveBundles = new MultiCurveBundle[nGroups];
    final LinkedHashMap<String, Currency> discountingMap = new LinkedHashMap<>();
    final LinkedHashMap<String, IborIndex[]> forwardIborMap = new LinkedHashMap<>();
    final LinkedHashMap<String, IndexON[]> forwardONMap = new LinkedHashMap<>();

    //TODO comparator to sort groups by order
    int i = 0; // Implementation Note: loop on the groups

    // TODO - this is not fine-grained enough to provide useful detail back to the user
    boolean curveBundlesComplete = true;

    final Set<Currency> curvesToRemove = new HashSet<>();

    for (final CurveGroupConfiguration group : config.getCurveGroups()) { // Group - start

      final int nCurves = group.getTypesForCurves().size();

      @SuppressWarnings("unchecked")
      final SingleCurveBundle<GeneratorYDCurve>[] singleCurves = new SingleCurveBundle[nCurves];

      int j = 0;

      for (final Map.Entry<String, List<CurveTypeConfiguration>> entry : group.getTypesForCurves().entrySet()) {

        final String curveName = entry.getKey();
        Result<CurveDefinition> curveDefResult = _curveDefinitionProvider.getCurveDefinition(curveName);

        if (_impliedCurveNames.contains(curveName)) {
          if (curveDefResult.isValueAvailable()) {

            if (exogenousBundle.isValueAvailable()) {
              // todo error handling if curve is not in bundle

              Currency currency = null;
              for (CurveTypeConfiguration type : entry.getValue()) {
                if (type instanceof DiscountingCurveTypeConfiguration) {
                  final String reference = ((DiscountingCurveTypeConfiguration) type).getReference();
                  try {
                    currency = Currency.of(reference);
                  } catch (final IllegalArgumentException e) {
                    throw new OpenGammaRuntimeException("Cannot handle reference type " + reference + " for discounting curves");
                  }
                }
              }

              singleCurves[j] = buildImpliedDepositCurve(currency, curveDefResult.getValue(), exogenousBundle.getValue(),
                                                           valuationTime);
              // todo note we do this below as well, refactor it to be common
              discountingMap.put(curveName, currency);

              // This curve needs to replace the existing discounting curve of the same currency
              curvesToRemove.add(currency);
            }
          }
        } else {

          // TODO - curve def and spec are closely related and the curveSec provider should probably use the curveDef provider underneath
          Result<CurveSpecification> curveSpecResult = _curveSpecificationProvider.getCurveSpecification(curveName, valuationTime);

          if (curveSpecResult.isValueAvailable()) {

            final CurveSpecification specification = curveSpecResult.getValue();

            // todo - this lookup is not needed for all curves but we get it for all, can we restrict so we only get it when we need it?
            final Result<HistoricalTimeSeriesBundle> htsResult = _historicalTimeSeriesProvider.getHtsForCurve(
                specification);
            Result<MarketDataValues> marketDataResult = _curveSpecificationMarketDataProvider.requestData(specification);

            // Only proceed if we have all market data values available to us
            if (curveDefResult.isValueAvailable() && htsResult.isValueAvailable() && fxMatrixResult.isValueAvailable() &&
                marketDataResult.getStatus() == SuccessStatus.SUCCESS) {

              CurveDefinition curveDefinition = curveDefResult.getValue();
              FXMatrix fxMatrix = fxMatrixResult.getValue();

              // todo this is temporary to allow us to get up and running fast
              final SnapshotDataBundle snapshot = marketDataResult.getValue().toSnapshot();

              final int nNodes = specification.getNodes().size();
              final double[] parameterGuessForCurves = new double[nNodes];
              Arrays.fill(parameterGuessForCurves, 0.02);  // For FX forward, the FX rate is not a good initial guess. // TODO: change this // marketData

              final InstrumentDerivative[] derivativesForCurve =
                  extractInstrumentDerivatives(specification, snapshot, htsResult.getValue(), fxMatrix, valuationTime);

              final List<IborIndex> iborIndex = new ArrayList<>();
              final List<IndexON> overnightIndex = new ArrayList<>();

              for (final CurveTypeConfiguration type : entry.getValue()) { // Type - start
                if (type instanceof DiscountingCurveTypeConfiguration) {
                  final String reference = ((DiscountingCurveTypeConfiguration) type).getReference();
                  try {
                    final Currency currency = Currency.of(reference);
                    //should this map check that the curve name has not already been entered?
                    discountingMap.put(curveName, currency);
                  } catch (final IllegalArgumentException e) {
                    throw new OpenGammaRuntimeException("Cannot handle reference type " + reference + " for discounting curves");
                  }
                } else if (type instanceof IborCurveTypeConfiguration) {
                  iborIndex.add(createIborIndex((IborCurveTypeConfiguration) type));
                } else if (type instanceof OvernightCurveTypeConfiguration) {
                  overnightIndex.add(createIndexON((OvernightCurveTypeConfiguration) type));
                } else {
                  // todo - don't throw exception
                  throw new OpenGammaRuntimeException("Cannot handle " + type.getClass());
                }
              } // type - end


              if (!iborIndex.isEmpty()) {
                forwardIborMap.put(curveName, iborIndex.toArray(new IborIndex[iborIndex.size()]));
              }
              if (!overnightIndex.isEmpty()) {
                forwardONMap.put(curveName, overnightIndex.toArray(new IndexON[overnightIndex.size()]));
              }

              final GeneratorYDCurve generator = getGenerator(curveDefinition, _valuationTimeProvider.getDate());
              singleCurves[j] = new SingleCurveBundle<>(curveName, derivativesForCurve, generator.initialGuess(parameterGuessForCurves), generator);
            } else {
              curveBundlesComplete = false;
            }
          } else {
            curveBundlesComplete = false;
          }
        }
        j++;
      }
      if (curveBundlesComplete) {
        curveBundles[i++] = new MultiCurveBundle<>(singleCurves);
      }
    } // Group - end

    if (exogenousBundle.isValueAvailable() && curveBundlesComplete) {

      MulticurveProviderDiscount exogenouscurves = adjustMulticurveBundle(curvesToRemove, exogenousBundle.getValue());
      return success(createBuilder().makeCurvesFromDerivatives(curveBundles,
                                                               exogenouscurves,
                                                               discountingMap,
                                                               forwardIborMap,
                                                               forwardONMap,
                                                               ParSpreadMarketQuoteDiscountingCalculator.getInstance(),
                                                               ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator.getInstance()));
    } else {
      // todo - supply some useful information in the failure message!
      return failure(MISSING_DATA, "Unable to get intermediate data");
    }
  }

  private IndexON createIndexON(OvernightCurveTypeConfiguration type) {

    final OvernightIndexConvention overnightConvention =
        _conventionSource.getSingle(type.getConvention(), OvernightIndexConvention.class);

    return new IndexON(overnightConvention.getName(), overnightConvention.getCurrency(),
                              overnightConvention.getDayCount(), overnightConvention.getPublicationLag());
  }

  private IborIndex createIborIndex(IborCurveTypeConfiguration type) {

    final IborIndexConvention iborIndexConvention =
        _conventionSource.getSingle(type.getConvention(), IborIndexConvention.class);

    return new IborIndex(iborIndexConvention.getCurrency(),
                                type.getTenor().getPeriod(),
                                iborIndexConvention.getSettlementDays(),
                                iborIndexConvention.getDayCount(),
                                iborIndexConvention.getBusinessDayConvention(),
                                iborIndexConvention.isIsEOM(),
                                iborIndexConvention.getName());
  }

  private MulticurveProviderDiscount adjustMulticurveBundle(Set<Currency> curvesToRemove,
                                                            MulticurveProviderDiscount multicurves) {

    if (!curvesToRemove.isEmpty()) {
      MulticurveProviderDiscount copy = multicurves.copy();
      for (Currency currency : curvesToRemove) {
        copy.removeCurve(currency);
      }
      return copy;
    } else {
      return multicurves;
    }
  }

  private SingleCurveBundle<GeneratorYDCurve> buildImpliedDepositCurve(Currency currency,
                                                                       CurveDefinition impliedCurveDefinition,
                                                                       MulticurveProviderDiscount multicurves,
                                                                       ZonedDateTime valuationDate) {

    Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>> data =
        extractImpliedDepositCurveData(currency, impliedCurveDefinition, multicurves, valuationDate);
    GeneratorYDCurve generator = getGenerator(impliedCurveDefinition, valuationDate.toLocalDate());
    List<InstrumentDerivative> instrumentDerivatives = data.getThird();
    return new SingleCurveBundle<>(impliedCurveDefinition.getName(),
                                   instrumentDerivatives.toArray(new InstrumentDerivative[instrumentDerivatives.size()]),
                                   convertToArray(data.getSecond()), generator);
  }

  private double[] convertToArray(List<Double> first) {
    double[] result = new double[first.size()];
    for (int i = 0; i < first.size(); i++) {
      result[i] = first.get(i);
    }
    return result;
  }

  private InstrumentDerivative[] extractInstrumentDerivatives(CurveSpecification specification,
                                                              SnapshotDataBundle snapshot,
                                                              HistoricalTimeSeriesBundle htsBundle,
                                                              FXMatrix fxMatrix, ZonedDateTime valuationDate) {

    Set<CurveNodeWithIdentifier> nodes = specification.getNodes();
    final InstrumentDerivative[] derivativesForCurve = new InstrumentDerivative[nodes.size()];
    int i = 0;

    for (final CurveNodeWithIdentifier node : nodes) {

      final InstrumentDefinition<?> definitionForNode =
          node.getCurveNode().accept(createCurveNodeVisitor(node.getIdentifier(), snapshot, valuationDate, fxMatrix));

      // todo - we may need to allow the node converter implementation to be changed
      derivativesForCurve[i] =
          (new CurveNodeConverter(_conventionSource)).getDerivative(node, definitionForNode, valuationDate, htsBundle);
      i++;
    }

    return derivativesForCurve;
  }

  private GeneratorYDCurve getGenerator(final CurveDefinition definition, LocalDate valuationDate) {

    if (definition instanceof InterpolatedCurveDefinition) {
      final InterpolatedCurveDefinition interpolatedDefinition = (InterpolatedCurveDefinition) definition;
      final String interpolatorName = interpolatedDefinition.getInterpolatorName();
      final String leftExtrapolatorName = interpolatedDefinition.getLeftExtrapolatorName();
      final String rightExtrapolatorName = interpolatedDefinition.getRightExtrapolatorName();
      final Interpolator1D interpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(interpolatorName,
                                                                                                  leftExtrapolatorName,
                                                                                                  rightExtrapolatorName);
      if (definition instanceof FixedDateInterpolatedCurveDefinition) {
        final FixedDateInterpolatedCurveDefinition fixedDateDefinition = (FixedDateInterpolatedCurveDefinition) definition;
        final List<LocalDate> fixedDates = fixedDateDefinition.getFixedDates();
        final DoubleArrayList nodePoints = new DoubleArrayList(fixedDates.size()); //TODO what about equal node points?
        for (final LocalDate fixedDate : fixedDates) {
          nodePoints.add(TimeCalculator.getTimeBetween(valuationDate, fixedDate)); //TODO what to do if the fixed date is before the valuation date?
        }
        final double anchor = nodePoints.get(0); //TODO should the anchor go into the definition?
        return new GeneratorCurveYieldInterpolatedAnchorNode(nodePoints.toDoubleArray(), anchor, interpolator);
      }
      return new GeneratorCurveYieldInterpolated(LastTimeCalculator.getInstance(), interpolator);
    }

    throw new OpenGammaRuntimeException("Cannot handle curves of type " + definition.getClass());
  }

  private Result<MulticurveProviderDiscount> buildExogenousBundles(CurveConstructionConfiguration curveConfig,
                                                                           Result<FXMatrix> fxMatrixResult,
                                                                           ZonedDateTime valuationTime) {

    ResultStatus exogenousStatus = SuccessStatus.SUCCESS;
    Set<MulticurveProviderDiscount> exogenousBundles = new HashSet<>();

    for (CurveConstructionConfiguration exogenousConfig : curveConfig.getResolvedCurveConfigurations()) {

      Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult =
          generateBundle(exogenousConfig, valuationTime);

      if (bundleResult.getStatus().isResultAvailable()) {
        exogenousBundles.add(bundleResult.getValue().getFirst());
      } else {
        exogenousStatus = bundleResult.getStatus();
      }
    }

    if (exogenousStatus.isResultAvailable() && fxMatrixResult.isValueAvailable()) {

      FXMatrix fxMatrix = fxMatrixResult.getValue();
      if (exogenousBundles.isEmpty()) {
        return success(new MulticurveProviderDiscount(fxMatrix));
      } else {
        MulticurveProviderDiscount result = ProviderUtils.mergeDiscountingProviders(exogenousBundles);
        MulticurveProviderDiscount provider = ProviderUtils.mergeDiscountingProviders(result, fxMatrix);
        return success(provider);
      }
    } else if (!exogenousStatus.isResultAvailable()) {

      return failure(MISSING_DATA, "Unable to build exogenous curve bundles");
    } else {

      return propagateFailure(fxMatrixResult);
    }
  }

  private CurveNodeVisitor<InstrumentDefinition<?>> createCurveNodeVisitor(ExternalId dataId,
                                                                           SnapshotDataBundle
                                                                               marketData,
                                                                           ZonedDateTime valuationTime,
                                                                           FXMatrix fxMatrix) {
    return CurveNodeVisitorAdapter.<InstrumentDefinition<?>>builder()
        .cashNodeVisitor(new CashNodeConverter(_conventionSource, _holidaySource, _regionSource,
                                               marketData, dataId, valuationTime))
        .fraNode(new FRANodeConverter(_conventionSource,
                                      _holidaySource,
                                      _regionSource,
                                      marketData,
                                      dataId,
                                      valuationTime))
        .fxForwardNode(new FXForwardNodeConverter(_conventionSource, _holidaySource, _regionSource,
                                                  marketData, dataId, valuationTime))
        .immFRANode(new RollDateFRANodeConverter(_conventionSource,
                                                 _holidaySource,
                                                 _regionSource,
                                                 marketData,
                                                 dataId,
                                                 valuationTime))
        .immSwapNode(new RollDateSwapNodeConverter(_conventionSource,
                                                   _holidaySource,
                                                   _regionSource,
                                                   marketData,
                                                   dataId,
                                                   valuationTime))
        .rateFutureNode(new RateFutureNodeConverter(_conventionSource,
                                                    _holidaySource,
                                                    _regionSource,
                                                    marketData,
                                                    dataId,
                                                    valuationTime))
        .swapNode(new SwapNodeConverter(_conventionSource,
                                        _holidaySource,
                                        _regionSource,
                                        marketData,
                                        dataId,
                                        valuationTime,
                                        fxMatrix))
        .threeLegBasisSwapNode(new ThreeLegBasisSwapNodeConverter(_conventionSource,
                                                                          _holidaySource,
                                                                          _regionSource,
                                                                          marketData,
                                                                          dataId,
                                                                          valuationTime))
        .create();
  }
}
