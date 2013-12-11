/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;


import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.FunctionResultGenerator.failure;
import static com.opengamma.util.result.FunctionResultGenerator.propagateFailure;
import static com.opengamma.util.result.FunctionResultGenerator.success;

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
import com.opengamma.analytics.financial.provider.calculator.discounting.ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParSpreadMarketQuoteDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.generic.LastTimeCalculator;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.curve.MultiCurveBundle;
import com.opengamma.analytics.financial.provider.curve.SingleCurveBundle;
import com.opengamma.analytics.financial.provider.curve.multicurve.MulticurveDiscountBuildingRepository;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.ProviderUtils;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.marketdatasnapshot.SnapshotDataBundle;
import com.opengamma.core.region.RegionSource;
import com.opengamma.financial.analytics.conversion.CurveNodeConverter;
import com.opengamma.financial.analytics.curve.CashNodeConverter;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveConstructionConfigurationSource;
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
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeVisitor;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.convention.IborIndexConvention;
import com.opengamma.financial.convention.OvernightIndexConvention;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.marketdata.DefaultResettableMarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FunctionResult;
import com.opengamma.util.result.ResultStatus;
import com.opengamma.util.result.SuccessStatus;
import com.opengamma.util.tuple.Pair;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

public class DefaultDiscountingMulticurveBundleFn implements DiscountingMulticurveBundleFn {

  private final CurveDefinitionFn _curveDefinitionProvider;
  private final CurveSpecificationFn _curveSpecificationProvider;
  private final CurveSpecificationMarketDataFn _curveSpecificationMarketDataProvider;
  private final ValuationTimeFn _valuationTimeProvider;
  private final FXMatrixFn _fxMatrixProvider;
  private final HistoricalTimeSeriesFn _historicalTimeSeriesProvider;

  private final CurveConstructionConfigurationSource _curveConstructionConfigurationSource;
  private final ConventionSource _conventionSource;
  private final HolidaySource _holidaySource;
  private final RegionSource _regionSource;

  private final RootFinderConfiguration _rootFinderConfiguration;

  public DefaultDiscountingMulticurveBundleFn(CurveDefinitionFn curveDefinitionProvider,
                                              CurveSpecificationFn curveSpecificationProvider,
                                              CurveSpecificationMarketDataFn curveSpecificationMarketDataProvider,
                                              ValuationTimeFn valuationTimeProvider,
                                              FXMatrixFn fxMatrixProvider,
                                              HistoricalTimeSeriesFn historicalTimeSeriesProvider,
                                              CurveConstructionConfigurationSource curveConstructionConfigurationSource,
                                              ConventionSource conventionSource,
                                              HolidaySource holidaySource,
                                              RegionSource regionSource,
                                              RootFinderConfiguration rootFinderConfiguration) {

    _curveDefinitionProvider = curveDefinitionProvider;
    _curveSpecificationProvider = curveSpecificationProvider;
    _curveSpecificationMarketDataProvider = curveSpecificationMarketDataProvider;
    _valuationTimeProvider = valuationTimeProvider;
    _fxMatrixProvider = fxMatrixProvider;
    _historicalTimeSeriesProvider = historicalTimeSeriesProvider;
    _curveConstructionConfigurationSource = curveConstructionConfigurationSource;
    _conventionSource = conventionSource;
    _holidaySource = holidaySource;
    _regionSource = regionSource;
    _rootFinderConfiguration = rootFinderConfiguration;
  }

  @Override
  public FunctionResult<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> generateBundle(String curveConstructionConfigurationName) {

    final CurveConstructionConfiguration curveConstructionConfiguration =
        _curveConstructionConfigurationSource.getCurveConstructionConfiguration(curveConstructionConfigurationName);

    if (curveConstructionConfiguration != null) {


      // Each curve config may have one or more exogenous requirements which basically should
      // point to another curve config (which may point to one or more configs ...)
      // We need a depth-first evaluation of the tree formed by these configs as (direct) child
      // MulticurveProviderInterface instances are required to be passed into their parent's
      // evaluation via the known data parameter

      // If we can't build due to insufficient market data, then we keep going but don't call
      // the final build step. This way we ensure that market data requirements have been captured.

      // todo check for cycles in the config

      FunctionResult<FXMatrix> fxMatrixResult = _fxMatrixProvider.getFXMatrix(curveConstructionConfiguration);
      FunctionResult<MulticurveProviderDiscount> exogenousBundles = buildExogenousBundles(curveConstructionConfiguration, fxMatrixResult);
      return getCurves(curveConstructionConfiguration, exogenousBundles, fxMatrixResult);

    } else {
      return failure(MISSING_DATA, "CurveConstructionConfiguration named {} was not found",
                     curveConstructionConfigurationName);
    }
  }

  private MulticurveDiscountBuildingRepository createBuilder() {
    return new MulticurveDiscountBuildingRepository(
        _rootFinderConfiguration.getAbsoluteTolerance(),
        _rootFinderConfiguration.getRelativeTolerance(),
        _rootFinderConfiguration.getMaxIterations());
  }

  private FunctionResult<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> getCurves(
      CurveConstructionConfiguration config,
      FunctionResult<MulticurveProviderDiscount> exogenousBundle,
      FunctionResult<FXMatrix> fxMatrixResult) {

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

    for (final CurveGroupConfiguration group : config.getCurveGroups()) { // Group - start

      final int nCurves = group.getTypesForCurves().size();

      @SuppressWarnings("unchecked")
      final SingleCurveBundle<GeneratorYDCurve>[] singleCurves = new SingleCurveBundle[nCurves];

      int j = 0;

      for (final Map.Entry<String, List<CurveTypeConfiguration>> entry : group.getTypesForCurves().entrySet()) {

        final List<IborIndex> iborIndex = new ArrayList<>();
        final List<IndexON> overnightIndex = new ArrayList<>();
        final String curveName = entry.getKey();

        // TODO - curve def and spec are closely related and the curveSec provider should probably use the curveDef provider underneath
        FunctionResult<CurveDefinition> curveDefResult = _curveDefinitionProvider.getCurveDefinition(curveName);
        FunctionResult<CurveSpecification> curveSpecResult = _curveSpecificationProvider.getCurveSpecification(curveName);

        if (curveSpecResult.isResultAvailable()) {

          final CurveSpecification specification = curveSpecResult.getResult();

          // todo - this can now come from market data provider
          DefaultResettableMarketDataFn provider;
          final FunctionResult<HistoricalTimeSeriesBundle> htsResult = _historicalTimeSeriesProvider.getHtsForCurve(specification);
          FunctionResult<MarketDataValues> marketDataResult = _curveSpecificationMarketDataProvider.requestData(specification);

          // Only proceed if we have all market data values available to us
          if (curveDefResult.isResultAvailable() && htsResult.isResultAvailable() && fxMatrixResult.isResultAvailable() &&
              marketDataResult.getStatus() == SuccessStatus.SUCCESS) {

            CurveDefinition curveDefinition = curveDefResult.getResult();
            FXMatrix fxMatrix = fxMatrixResult.getResult();

            // todo this is temporary to allow us to get up and running fast
            final SnapshotDataBundle snapshot = marketDataResult.getResult().toSnapshot();

            final int nNodes = specification.getNodes().size();
            final double[] parameterGuessForCurves = new double[nNodes];
            Arrays.fill(parameterGuessForCurves, 0.02);  // For FX forward, the FX rate is not a good initial guess. // TODO: change this // marketData

            final InstrumentDerivative[] derivativesForCurve = extractInstrumentDerivatives(specification, snapshot, htsResult.getResult(), fxMatrix);

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
                final IborCurveTypeConfiguration ibor = (IborCurveTypeConfiguration) type;
                final IborIndexConvention iborIndexConvention = _conventionSource.getSingle(ibor.getConvention(),
                                                                                            IborIndexConvention.class);
                if (iborIndexConvention == null) {
                  throw new OpenGammaRuntimeException("Ibor index convention called " + ibor.getConvention() + " was null");
                }
                final int spotLag = iborIndexConvention.getSettlementDays();
                iborIndex.add(new IborIndex(iborIndexConvention.getCurrency(), ibor.getTenor().getPeriod(), spotLag, iborIndexConvention.getDayCount(),
                                            iborIndexConvention.getBusinessDayConvention(), iborIndexConvention.isIsEOM(), iborIndexConvention.getName()));
              } else if (type instanceof OvernightCurveTypeConfiguration) {
                final OvernightCurveTypeConfiguration overnight = (OvernightCurveTypeConfiguration) type;
                final OvernightIndexConvention overnightConvention = _conventionSource.getSingle(overnight.getConvention(),
                                                                                                 OvernightIndexConvention.class);
                if (overnightConvention == null) {
                  throw new OpenGammaRuntimeException("Overnight convention called " + overnight.getConvention() + " was null");
                }
                overnightIndex.add(new IndexON(overnightConvention.getName(), overnightConvention.getCurrency(), overnightConvention.getDayCount(), overnightConvention.getPublicationLag()));
              } else {
                throw new OpenGammaRuntimeException("Cannot handle " + type.getClass());
              }
            } // type - end


            if (!iborIndex.isEmpty()) {
              forwardIborMap.put(curveName, iborIndex.toArray(new IborIndex[iborIndex.size()]));
            }
            if (!overnightIndex.isEmpty()) {
              forwardONMap.put(curveName, overnightIndex.toArray(new IndexON[overnightIndex.size()]));
            }
            final GeneratorYDCurve generator = getGenerator(curveDefinition);
            singleCurves[j++] = new SingleCurveBundle<>(curveName, derivativesForCurve, generator.initialGuess(parameterGuessForCurves), generator);
          } else {
            curveBundlesComplete = false;
          }
        } else {
          curveBundlesComplete = false;
        }
      }
      if (curveBundlesComplete) {
        curveBundles[i++] = new MultiCurveBundle<>(singleCurves);
      }
    } // Group - end

    if (exogenousBundle.isResultAvailable() && curveBundlesComplete) {

      return success(createBuilder().makeCurvesFromDerivatives(curveBundles,
                                                               exogenousBundle.getResult(),
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

  private InstrumentDerivative[] extractInstrumentDerivatives(CurveSpecification specification,
                                                              SnapshotDataBundle snapshot,
                                                              HistoricalTimeSeriesBundle htsBundle,
                                                              FXMatrix fxMatrix) {

    ZonedDateTime valuationTime = _valuationTimeProvider.getTime();

    Set<CurveNodeWithIdentifier> nodes = specification.getNodes();
    final InstrumentDerivative[] derivativesForCurve = new InstrumentDerivative[nodes.size()];
    int i = 0;

    for (final CurveNodeWithIdentifier node : nodes) {

      final InstrumentDefinition<?> definitionForNode =
          node.getCurveNode().accept(createCurveNodeVisitor(node.getIdentifier(), snapshot, valuationTime, fxMatrix));

      // todo - we may need to allow the node converter implementation to be changed
      derivativesForCurve[i++] =
          (new CurveNodeConverter(_conventionSource)).getDerivative(node, definitionForNode, valuationTime, htsBundle);
    }

    return derivativesForCurve;
  }

  private GeneratorYDCurve getGenerator(final CurveDefinition definition) {

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
        LocalDate valuationDate = _valuationTimeProvider.getDate();
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

  private FunctionResult<MulticurveProviderDiscount> buildExogenousBundles(CurveConstructionConfiguration curveConstructionConfiguration,
                                                                           FunctionResult<FXMatrix> fxMatrixResult) {

    ResultStatus exogenousStatus = SuccessStatus.SUCCESS;
    Set<MulticurveProviderDiscount> exogenousBundles = new HashSet<>();

    List<String> exogenousConfigurations = curveConstructionConfiguration.getExogenousConfigurations();

    // THis null check should not be required but currently null can be returned - see PLAT-5047
    if (exogenousConfigurations != null) {
      for (String configName : exogenousConfigurations) {

        FunctionResult<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult =
            generateBundle(configName);

        if (bundleResult.getStatus().isResultAvailable()) {
          exogenousBundles.add(bundleResult.getResult().getFirst());
        } else {
          exogenousStatus = bundleResult.getStatus();
        }
      }
    }

    if (exogenousStatus.isResultAvailable() && fxMatrixResult.isResultAvailable()) {

      FXMatrix fxMatrix = fxMatrixResult.getResult();
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
