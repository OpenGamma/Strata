/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;


import static com.opengamma.financial.convention.initializer.PerCurrencyConventionHelper.DEPOSIT;
import static com.opengamma.financial.convention.initializer.PerCurrencyConventionHelper.getConventionLink;
import static com.opengamma.util.result.FailureStatus.ERROR;

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
import com.opengamma.core.convention.Convention;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.link.ConventionLink;
import com.opengamma.core.link.SecurityLink;
import com.opengamma.core.marketdatasnapshot.SnapshotDataBundle;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.financial.analytics.conversion.CalendarUtils;
import com.opengamma.financial.analytics.curve.AbstractCurveDefinition;
import com.opengamma.financial.analytics.curve.AbstractCurveSpecification;
import com.opengamma.financial.analytics.curve.CashNodeConverter;
import com.opengamma.financial.analytics.curve.ConverterUtils;
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
import com.opengamma.financial.convention.DepositConvention;
import com.opengamma.financial.convention.IborIndexConvention;
import com.opengamma.financial.convention.OvernightIndexConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.security.index.OvernightIndex;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.component.StringSet;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.Tenor;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Triple;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * Function implementation that provides a discounting multi-curve bundle.
 */
public class DefaultDiscountingMulticurveBundleFn implements DiscountingMulticurveBundleFn {

  private static final ParSpreadMarketQuoteDiscountingCalculator DISCOUNTING_CALCULATOR =
      ParSpreadMarketQuoteDiscountingCalculator.getInstance();

  private static final ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator CURVE_SENSITIVITY_CALCULATOR =
      ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator.getInstance();

  private final CurveDefinitionFn _curveDefinitionProvider;
  private final CurveSpecificationFn _curveSpecificationProvider;
  private final CurveSpecificationMarketDataFn _curveSpecificationMarketDataProvider;
  private final FXMatrixFn _fxMatrixProvider;

  private final SecuritySource _securitySource;
  private final ConventionSource _conventionSource;
  private final HolidaySource _holidaySource;
  private final RegionSource _regionSource;
  private final CurveNodeConverterFn _curveNodeConverter;

  private final RootFinderConfiguration _rootFinderConfiguration;

  /**
   * Map indicating which curves should be implied, and if so which curves they should
   * be implied from.
   */
  // todo - this is only a temporary solution to determine the implied deposit curves
  private final Set<String> _impliedCurveNames;

  public DefaultDiscountingMulticurveBundleFn(CurveDefinitionFn curveDefinitionProvider,
                                              CurveSpecificationFn curveSpecificationProvider,
                                              CurveSpecificationMarketDataFn curveSpecificationMarketDataProvider,
                                              FXMatrixFn fxMatrixProvider,
                                              SecuritySource securitySource,
                                              ConventionSource conventionSource,
                                              HolidaySource holidaySource,
                                              RegionSource regionSource,
                                              CurveNodeConverterFn curveNodeConverter,
                                              RootFinderConfiguration rootFinderConfiguration,
                                              StringSet impliedCurveNames) {

    _curveDefinitionProvider = curveDefinitionProvider;
    _curveSpecificationProvider = curveSpecificationProvider;
    _curveSpecificationMarketDataProvider = curveSpecificationMarketDataProvider;
    _fxMatrixProvider = fxMatrixProvider;
    _securitySource = securitySource;
    _conventionSource = conventionSource;
    _holidaySource = holidaySource;
    _regionSource = regionSource;
    _curveNodeConverter = curveNodeConverter;
    _rootFinderConfiguration = rootFinderConfiguration;
    _impliedCurveNames = impliedCurveNames.getStrings();
  }

  //-------------------------------------------------------------------------
  @Override
  public Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> generateBundle(
      Environment env, CurveConstructionConfiguration curveConfig) {

    // Each curve config may have one or more exogenous requirements which basically should
    // point to another curve config (which may point to one or more configs ...)
    // We need a depth-first evaluation of the tree formed by these configs as (direct) child
    // MulticurveProviderInterface instances are required to be passed into their parent's
    // evaluation via the known data parameter

    // If we can't build due to insufficient market data, then we keep going but don't call
    // the final build step. This way we ensure that market data requirements have been captured.

    // todo check for cycles in the config

    Result<FXMatrix> fxMatrixResult = _fxMatrixProvider.getFXMatrix(env, curveConfig);
    Result<MulticurveProviderDiscount> exogenousBundles = buildExogenousBundles(env, curveConfig, fxMatrixResult);
    return getCurves(env, curveConfig, exogenousBundles, fxMatrixResult);

  }

  @Override
  public Result<Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>>> extractImpliedDepositCurveData(
      Environment env, CurveConstructionConfiguration curveConfig) {

    // todo - this implementation is nowhere near complete
    Result<FXMatrix> fxMatrixResult = _fxMatrixProvider.getFXMatrix(env, curveConfig);

    Result<MulticurveProviderDiscount> exogenousBundles = buildExogenousBundles(env, curveConfig, fxMatrixResult);

    CurveGroupConfiguration group = curveConfig.getCurveGroups().get(0);
    Map.Entry<String, List<? extends CurveTypeConfiguration>> type = group.getTypesForCurves().entrySet().iterator().next();
    Result<CurveDefinition> curveDefinition = _curveDefinitionProvider.getCurveDefinition(type.getKey());

    // Any one of the above 3 could have failed, but we have attempted all to
    // try and report as many errors as possible as early as possible
    if (Result.allSuccessful(fxMatrixResult, exogenousBundles, curveDefinition)) {

      DiscountingCurveTypeConfiguration typeConfiguration = (DiscountingCurveTypeConfiguration) type.getValue().get(0);
      Currency currency = Currency.of(typeConfiguration.getReference());

      return Result.success(extractImpliedDepositCurveData(currency,
                                                           curveDefinition.getValue(),
                                                           exogenousBundles.getValue(),
                                                           env.getValuationTime())); // TODO can this be the valuation date?
    } else {
      return Result.failure(fxMatrixResult, exogenousBundles, curveDefinition);
    }
  }

  // REVIEW Chris 2014-03-05 - the return type needs to be a class
  private Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>>
  extractImpliedDepositCurveData(Currency currency,
                                 CurveDefinition impliedCurveDefinition,
                                 MulticurveProviderDiscount multicurves,
                                 ZonedDateTime valuationTime) {

    final ParRateDiscountingCalculator parRateDiscountingCalculator = ParRateDiscountingCalculator.getInstance();
    final Calendar calendar = CalendarUtils.getCalendar(_holidaySource, currency);
  
    final List<Tenor> tenors = new ArrayList<>();
    final List<Double> parRates = new ArrayList<>();
    final List<InstrumentDerivative> cashNodes = new ArrayList<>();
    
    ConventionLink<Convention> conventionLink = getConventionLink(currency, DEPOSIT);
    DepositConvention currencyDepositConvention = (DepositConvention) conventionLink.resolve();
    int spotLag = currencyDepositConvention.getSettlementDays();
    ZonedDateTime spotDate = ScheduleCalculator.getAdjustedDate(valuationTime, spotLag, calendar);
    
    DayCount dayCount = currencyDepositConvention.getDayCount();
    BusinessDayConvention businessDayConvention = currencyDepositConvention.getBusinessDayConvention();
    
    for (final CurveNode node : impliedCurveDefinition.getNodes()) {
  
      final Tenor tenor = node.getResolvedMaturity();
      final ZonedDateTime paymentDate =
          ScheduleCalculator.getAdjustedDate(spotDate, tenor.getPeriod(), businessDayConvention, calendar, currencyDepositConvention.isIsEOM());
      final double startTime = TimeCalculator.getTimeBetween(valuationTime, spotDate);
      final double endTime = TimeCalculator.getTimeBetween(valuationTime, paymentDate);
      final double accrualFactor = dayCount.getDayCountFraction(spotDate, paymentDate, calendar);
      final Cash cashDepositNode = new Cash(currency, startTime, endTime, 1, 0, accrualFactor);
      final double parRate = parRateDiscountingCalculator.visitCash(cashDepositNode, multicurves);
      tenors.add(tenor);
      cashNodes.add(new Cash(currency, startTime, endTime, 1, parRate, accrualFactor));
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

  // TODO sort this out [SSM-164]
  private Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> getCurves(
      Environment env,
      CurveConstructionConfiguration config,
      Result<MulticurveProviderDiscount> exogenousBundle,
      Result<FXMatrix> fxMatrixResult) {

    final int nGroups = config.getCurveGroups().size();

    @SuppressWarnings("unchecked")
    final MultiCurveBundle<GeneratorYDCurve>[] curveBundles = new MultiCurveBundle[nGroups];
    final LinkedHashMap<String, Currency> discountingMap = new LinkedHashMap<>();
    final LinkedHashMap<String, IborIndex[]> forwardIborMap = new LinkedHashMap<>();
    final LinkedHashMap<String, IndexON[]> forwardONMap = new LinkedHashMap<>();

    //TODO comparator to sort groups by order
    int i = 0; // Implementation Note: loop on the groups

    final Set<Currency> curvesToRemove = new HashSet<>();

    // Result to allow us to capture any failures in all these loops, the
    // actual value if a success is of no consequence
    Result<Boolean> curveBundleResult = Result.success(true);

    for (final CurveGroupConfiguration group : config.getCurveGroups()) { // Group - start

      final int nCurves = group.getTypesForCurves().size();

      @SuppressWarnings("unchecked")
      final SingleCurveBundle<GeneratorYDCurve>[] singleCurves = new SingleCurveBundle[nCurves];

      int j = 0;

      for (final Map.Entry<AbstractCurveDefinition, List<? extends CurveTypeConfiguration>> entry : group.resolveTypesForCurves().entrySet()) {

        AbstractCurveDefinition curve = entry.getKey();
        String curveName = curve.getName();

        if (_impliedCurveNames.contains(curveName)) {
          if (!(curve instanceof CurveDefinition)) {
            Result.failure(curveBundleResult, Result.failure(FailureStatus.ERROR, "Curve {} was configured in " +
                "function as an implied depo curve but is not a subclass of CurveDefinition in the db.", curveName));
          } else if (!exogenousBundle.isSuccess()) {
            curveBundleResult = Result.failure(curveBundleResult, exogenousBundle);
          } else {
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
            // TODO can this be valuationDate?
            singleCurves[j] = buildImpliedDepositCurve(currency, (CurveDefinition) curve, exogenousBundle.getValue(), env.getValuationTime());
            // todo note we do this below as well, refactor it to be common
            discountingMap.put(curveName, currency);

            // This curve needs to replace the existing discounting curve of the same currency
            curvesToRemove.add(currency);
          }
        } else {
          Result<AbstractCurveSpecification> curveSpecResult =
              _curveSpecificationProvider.getCurveSpecification(env, curve);

          if (curveSpecResult.isSuccess()) {

            final CurveSpecification specification = (CurveSpecification) curveSpecResult.getValue();

            Result<Map<ExternalIdBundle, Double>> marketDataResult =
                _curveSpecificationMarketDataProvider.requestData(env, specification);

            // Only proceed if we have all market data values available to us
            if (Result.allSuccessful(fxMatrixResult, marketDataResult)) {

              FXMatrix fxMatrix = fxMatrixResult.getValue();

              // todo this is temporary to allow us to get up and running fast
              final SnapshotDataBundle snapshot = createSnapshotDataBundle(marketDataResult.getValue());

              final int nNodes = specification.getNodes().size();
              final double[] parameterGuessForCurves = new double[nNodes];
              Arrays.fill(parameterGuessForCurves, 0.02);  // For FX forward, the FX rate is not a good initial guess. // TODO: change this // marketData
              final Result<InstrumentDerivative[]> derivativesForCurve =
                  extractInstrumentDerivatives(env, specification, snapshot, fxMatrix, env.getValuationTime());
              final List<IborIndex> iborIndex = new ArrayList<>();
              final List<IndexON> overnightIndex = new ArrayList<>();

              for (final CurveTypeConfiguration type : entry.getValue()) {
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
                  overnightIndex.add(createOvernightIndex((OvernightCurveTypeConfiguration) type));
                } else {
                  Result<?> typeFailure =
                      Result.failure(ERROR, "Cannot handle curveTypeConfiguration with type {} whilst building curve: {}",
                                     type.getClass(), curveName);

                  curveBundleResult = Result.failure(curveBundleResult, typeFailure);
                }
              }

              if (!iborIndex.isEmpty()) {
                forwardIborMap.put(curveName, iborIndex.toArray(new IborIndex[iborIndex.size()]));
              }
              if (!overnightIndex.isEmpty()) {
                forwardONMap.put(curveName, overnightIndex.toArray(new IndexON[overnightIndex.size()]));
              }
              if (derivativesForCurve.isSuccess()) {
                final GeneratorYDCurve generator = getGenerator(curve, env.getValuationDate());
                singleCurves[j] = new SingleCurveBundle<>(curveName, derivativesForCurve.getValue(), generator.initialGuess(parameterGuessForCurves), generator);
              } else {
                curveBundleResult = Result.failure(curveBundleResult, derivativesForCurve);
              }
            } else {
              curveBundleResult = Result.failure(curveBundleResult, fxMatrixResult, marketDataResult);
            }
          } else {
            curveBundleResult = Result.failure(curveBundleResult, curveSpecResult);
          }
        }
        j++;
      }
      if (curveBundleResult.isSuccess()) {
        curveBundles[i++] = new MultiCurveBundle<>(singleCurves);
      }
    } // Group - end

    if (Result.allSuccessful(exogenousBundle, curveBundleResult)) {

      MulticurveProviderDiscount exogenousCurves = adjustMulticurveBundle(curvesToRemove, exogenousBundle.getValue());
      return Result.success(
          createBuilder().makeCurvesFromDerivatives(curveBundles,
                                                    exogenousCurves,
                                                    discountingMap,
                                                    forwardIborMap,
                                                    forwardONMap,
                                                    DISCOUNTING_CALCULATOR,
                                                    CURVE_SENSITIVITY_CALCULATOR));
    } else {
      return Result.failure(exogenousBundle, curveBundleResult);
    }
  }

  private static SnapshotDataBundle createSnapshotDataBundle(Map<ExternalIdBundle, Double> marketData) {
    SnapshotDataBundle snapshotDataBundle = new SnapshotDataBundle();

    for (Map.Entry<ExternalIdBundle, Double> entry : marketData.entrySet()) {
      snapshotDataBundle.setDataPoint(entry.getKey(), entry.getValue());
    }
    return snapshotDataBundle;
  }

  private IndexON createOvernightIndex(OvernightCurveTypeConfiguration type) {
    OvernightIndex index  = SecurityLink.resolvable(type.getConvention().toBundle(), OvernightIndex.class).resolve();
    OvernightIndexConvention indexConvention =
        ConventionLink.resolvable(index.getConventionId(), OvernightIndexConvention.class).resolve();
    return ConverterUtils.indexON(index.getName(), indexConvention);
  }

  private IborIndex createIborIndex(IborCurveTypeConfiguration type) {

    com.opengamma.financial.security.index.IborIndex indexSecurity =
        SecurityLink.resolvable(type.getConvention(), com.opengamma.financial.security.index.IborIndex.class).resolve();

    IborIndexConvention indexConvention =
        ConventionLink.resolvable(indexSecurity.getConventionId(), IborIndexConvention.class).resolve();

    return ConverterUtils.indexIbor(indexSecurity.getName(), indexConvention, indexSecurity.getTenor());
  }

  private MulticurveProviderDiscount adjustMulticurveBundle(Set<Currency> curvesToRemove,
                                                            MulticurveProviderDiscount multicurves) {

    if (curvesToRemove.isEmpty()) {
      return multicurves;
    } else {
      MulticurveProviderDiscount copy = multicurves.copy();
      for (Currency currency : curvesToRemove) {
        copy.removeCurve(currency);
      }
      return copy;
    }
  }

  private SingleCurveBundle<GeneratorYDCurve> buildImpliedDepositCurve(Currency currency,
                                                                       CurveDefinition impliedCurveDefinition,
                                                                       MulticurveProviderDiscount multicurves,
                                                                       ZonedDateTime valuationTime) {

    Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>> data =
        extractImpliedDepositCurveData(currency, impliedCurveDefinition, multicurves, valuationTime);
    GeneratorYDCurve generator = getGenerator(impliedCurveDefinition, valuationTime.toLocalDate());
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

  private Result<InstrumentDerivative[]> extractInstrumentDerivatives(Environment env,
                                                                      CurveSpecification specification,
                                                                      SnapshotDataBundle snapshot,
                                                                      FXMatrix fxMatrix,
                                                                      ZonedDateTime valuationTime) {
    Set<CurveNodeWithIdentifier> nodes = specification.getNodes();
    List<InstrumentDerivative> derivativesForCurve = new ArrayList<>(nodes.size());
    List<Result<?>> failures = new ArrayList<>();

    for (CurveNodeWithIdentifier node : nodes) {
      CurveNodeVisitor<InstrumentDefinition<?>> nodeVisitor =
          createCurveNodeVisitor(node.getIdentifier(), snapshot, valuationTime, fxMatrix);
      InstrumentDefinition<?> definitionForNode = node.getCurveNode().accept(nodeVisitor);
      Result<InstrumentDerivative> derivativeResult =
          _curveNodeConverter.getDerivative(env, node, definitionForNode, valuationTime);
      if (derivativeResult.isSuccess()) {
        derivativesForCurve.add(derivativeResult.getValue());
      } else {
        failures.add(derivativeResult);
      }
    }
    if (failures.isEmpty()) {
      return Result.success(derivativesForCurve.toArray(new InstrumentDerivative[derivativesForCurve.size()]));
    } else {
      return Result.failure(failures);
    }
  }

  private GeneratorYDCurve getGenerator(final AbstractCurveDefinition definition, LocalDate valuationDate) {

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
  
  
  private Result<MulticurveProviderDiscount> buildExogenousBundles(Environment env,
                                                                   CurveConstructionConfiguration curveConfig,
                                                                   Result<FXMatrix> fxMatrixResult) {

    Result<Boolean> exogenousResult = Result.success(true);
    Set<MulticurveProviderDiscount> exogenousBundles = new HashSet<>();

    for (CurveConstructionConfiguration exogenousConfig : curveConfig.resolveCurveConfigurations()) {

      Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult = generateBundle(env, exogenousConfig);
      if (bundleResult.isSuccess()) {
        exogenousBundles.add(bundleResult.getValue().getFirst());
      } else {
        exogenousResult = Result.failure(exogenousResult, bundleResult);
      }
    }

    if (Result.allSuccessful(exogenousResult, fxMatrixResult)) {

      FXMatrix fxMatrix = fxMatrixResult.getValue();
      if (exogenousBundles.isEmpty()) {
        return Result.success(new MulticurveProviderDiscount(fxMatrix));
      } else {
        MulticurveProviderDiscount result = ProviderUtils.mergeDiscountingProviders(exogenousBundles);
        MulticurveProviderDiscount provider = ProviderUtils.mergeDiscountingProviders(result, fxMatrix);
        return Result.success(provider);
      }
    } else {
      return Result.failure(fxMatrixResult, exogenousResult);
    }
  }

  private CurveNodeVisitor<InstrumentDefinition<?>> createCurveNodeVisitor(ExternalId dataId,
                                                                           SnapshotDataBundle
                                                                               marketData,
                                                                           ZonedDateTime valuationTime,
                                                                           FXMatrix fxMatrix) {
    return CurveNodeVisitorAdapter.<InstrumentDefinition<?>>builder()
        .cashNodeVisitor(new CashNodeConverter(_securitySource, _conventionSource, _holidaySource, _regionSource,
                                               marketData, dataId, valuationTime))
        .fraNode(new FRANodeConverter(_securitySource,
                                      _conventionSource,
                                      _holidaySource,
                                      _regionSource,
                                      marketData,
                                      dataId,
                                      valuationTime))
        .fxForwardNode(new FXForwardNodeConverter(_conventionSource, _holidaySource, _regionSource,
                                                  marketData, dataId, valuationTime))
        .immFRANode(new RollDateFRANodeConverter(_securitySource,
                                                 _conventionSource,
                                                 _holidaySource,
                                                 _regionSource,
                                                 marketData,
                                                 dataId,
                                                 valuationTime))
        .immSwapNode(new RollDateSwapNodeConverter(_securitySource,
                                                   _conventionSource,
                                                   _holidaySource,
                                                   _regionSource,
                                                   marketData,
                                                   dataId,
                                                   valuationTime))
        .rateFutureNode(new RateFutureNodeConverter(_securitySource,
                                                    _conventionSource,
                                                    _holidaySource,
                                                    _regionSource,
                                                    marketData,
                                                    dataId,
                                                    valuationTime))
        .swapNode(new SwapNodeConverter(_securitySource,
                                        _conventionSource,
                                        _holidaySource,
                                        _regionSource,
                                        marketData,
                                        dataId,
                                        valuationTime,
                                        fxMatrix))
        .threeLegBasisSwapNode(new ThreeLegBasisSwapNodeConverter(_securitySource,
                                                                  _conventionSource,
                                                                  _holidaySource,
                                                                  _regionSource,
                                                                  marketData,
                                                                  dataId,
                                                                  valuationTime))
        .create();
  }
}
