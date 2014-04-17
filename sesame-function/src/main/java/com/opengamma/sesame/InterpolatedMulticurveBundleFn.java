/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.FailureStatus.MISSING_DATA;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.instrument.index.IndexON;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.model.interestrate.curve.DiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldPeriodicCurve;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlock;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.matrix.DoubleMatrix2D;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.core.link.ConventionLink;
import com.opengamma.core.marketdatasnapshot.SnapshotDataBundle;
import com.opengamma.financial.analytics.curve.ConverterUtils;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.curve.CurveGroupConfiguration;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.curve.CurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.DiscountingCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.IborCurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.InterpolatedCurveSpecification;
import com.opengamma.financial.analytics.curve.OvernightCurveTypeConfiguration;
import com.opengamma.financial.analytics.ircurve.strips.ContinuouslyCompoundedRateNode;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.DiscountFactorNode;
import com.opengamma.financial.analytics.ircurve.strips.PeriodicallyCompoundedRateNode;
import com.opengamma.financial.convention.IborIndexConvention;
import com.opengamma.financial.convention.OvernightIndexConvention;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.SuccessStatus;
import com.opengamma.util.time.Tenor;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;
import com.opengamma.util.tuple.Triple;

/**
 * Multicurve bundle function using interpolated (pre-fitted) curves.
 */
public class InterpolatedMulticurveBundleFn implements DiscountingMulticurveBundleFn {
  
  private final CurveSpecificationFn _curveSpecificationProvider;
  private final CurveSpecificationMarketDataFn _curveSpecificationMarketDataProvider;
  
  public InterpolatedMulticurveBundleFn(CurveSpecificationFn curveSpecificationProvider,
                                        CurveSpecificationMarketDataFn curveSpecificationMarketDataProvider) {
    // TODO argument checker
    _curveSpecificationProvider = curveSpecificationProvider;
    _curveSpecificationMarketDataProvider = curveSpecificationMarketDataProvider;
  }

  @Override
  public Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> generateBundle(Environment env,
                                                                                           CurveConstructionConfiguration curveConfig) {
    
    boolean valid = true;
    
    ZonedDateTime now = env.getValuationTime();
    
    final MulticurveProviderDiscount curveBundle = new MulticurveProviderDiscount(new FXMatrix());
    final LinkedHashMap<String, Pair<Integer, Integer>> unitMap = new LinkedHashMap<>();
    final LinkedHashMap<String, Pair<CurveBuildingBlock, DoubleMatrix2D>> unitBundles = new LinkedHashMap<>();
    int totalNodes = 0;
    
    for (final CurveGroupConfiguration group: curveConfig.getCurveGroups()) {
      
      for (final Map.Entry<CurveDefinition, List<? extends CurveTypeConfiguration>> entry: group.resolveTypesForCurves().entrySet()) {
        
        CurveDefinition curve = entry.getKey();
        
        Result<CurveSpecification> curveSpecResult = _curveSpecificationProvider.getCurveSpecification(env, curve);
        
        if (curveSpecResult.isSuccess()) {

          InterpolatedCurveSpecification specification = (InterpolatedCurveSpecification) curveSpecResult.getValue();
          
          Result<Map<ExternalIdBundle, Double>> marketDataResult = _curveSpecificationMarketDataProvider.requestData(env, specification);
          
          if (marketDataResult.getStatus() == SuccessStatus.SUCCESS) {
            
            // todo this is temporary to allow us to get up and running fast
            final SnapshotDataBundle snapshot = createSnapshotDataBundle(marketDataResult.getValue());
            
            Set<CurveNodeWithIdentifier> nodes = specification.getNodes();
            final int n = nodes.size();
            final double[] ttm = new double[n];
            final double[] yields = new double[n];
            final double[][] jacobian = new double[n][n];
            
            int i = 0;
            int compoundPeriodsPerYear = 0;
            final int nNodesForCurve = specification.getNodes().size();
            boolean isYield = false;
            for (CurveNodeWithIdentifier node: nodes) {
              CurveNode curveNode = node.getCurveNode();
              if (curveNode instanceof ContinuouslyCompoundedRateNode) {
                if (i == 0) {
                  // First node - set expecation that all nodes are ContinuouslyCompoundedRateNodes
                  isYield = true;
                } else {
                  if (!isYield) {
                    throw new OpenGammaRuntimeException("Expected only continuously-compounded rate nodes, found " + curveNode);
                  }
                }
              } else if (curveNode instanceof DiscountFactorNode) {
                if (i == 0) {
                  // First node - set expectation that all nodes are DiscountFactorNodes
                  isYield = false;
                } else {
                  if (isYield) {
                    throw new OpenGammaRuntimeException("Expected only discount factor nodes, found " + curveNode);
                  }
                }
              } else if (curveNode instanceof PeriodicallyCompoundedRateNode) {
                if (i == 0) {
                  compoundPeriodsPerYear = ((PeriodicallyCompoundedRateNode) curveNode).getCompoundingPeriodsPerYear();
                  isYield = true;
                } else {
                  if (!isYield) {
                    throw new OpenGammaRuntimeException("Was expecting only periodically compounded nodes; have " + curveNode);
                  }
                }
              } else {
                throw new OpenGammaRuntimeException("Can only handle discount factor or continuously-compounded rate nodes; have " + curveNode);
              }
              
              // ttm
              final Tenor maturity = curveNode.getResolvedMaturity();
              ttm[i] = TimeCalculator.getTimeBetween(now, now.plus(maturity.getPeriod()));
              
              // yield
              final Double yield = snapshot.getDataPoint(node.getIdentifier());
              if (yield == null) {
                throw new OpenGammaRuntimeException("Could not get market data value for " + node);
              }
              yields[i] = yield;
              jacobian[i][i] = 1;
              i++;
            }

            final String interpolatorName = specification.getInterpolatorName();
            final String rightExtrapolatorName = specification.getRightExtrapolatorName();
            final String leftExtrapolatorName = specification.getLeftExtrapolatorName();
            final Interpolator1D interpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(interpolatorName, leftExtrapolatorName, rightExtrapolatorName);
            final String curveName = curve.getName();
            final InterpolatedDoublesCurve rawCurve = InterpolatedDoublesCurve.from(ttm, yields, interpolator, curveName);
            final YieldAndDiscountCurve discountCurve;
            if (compoundPeriodsPerYear != 0 && isYield) {
              discountCurve = YieldPeriodicCurve.from(compoundPeriodsPerYear, rawCurve);
            } else if (isYield) {
              discountCurve = new YieldCurve(curveName, rawCurve);
            } else {
              discountCurve = new DiscountCurve(curveName, rawCurve);
            }
                        
            for (final CurveTypeConfiguration type: entry.getValue()) {
              if (type instanceof DiscountingCurveTypeConfiguration) {
                final Currency currency = Currency.parse(((DiscountingCurveTypeConfiguration) type).getReference());
                curveBundle.setCurve(currency, discountCurve);
              } else if (type instanceof IborCurveTypeConfiguration) {
                curveBundle.setCurve(createIborIndex((IborCurveTypeConfiguration) type), discountCurve);
              } else if (type instanceof OvernightCurveTypeConfiguration) {
                curveBundle.setCurve(createIndexON((OvernightCurveTypeConfiguration) type), discountCurve);
              } else {
                throw new OpenGammaRuntimeException("Unsupported curve type configuration " + type);
              }
            }

            unitMap.put(curveName, Pairs.of(totalNodes + nNodesForCurve, nNodesForCurve));
            unitBundles.put(curveName, Pairs.of(new CurveBuildingBlock(unitMap), new DoubleMatrix2D(jacobian)));
            totalNodes += nNodesForCurve;
          } else {
            valid = false;
          }
        } else {
          valid = false;
        }
        
      }
    }
    
    if (valid) {
      return Result.success(Pairs.of(curveBundle, new CurveBuildingBlockBundle(unitBundles)));
    } else {
      // todo - supply some useful information in the failure message!
      return Result.failure(MISSING_DATA, "Unable to get intermediate data");
    }
  }

  private static SnapshotDataBundle createSnapshotDataBundle(Map<ExternalIdBundle, Double> marketData) {
    SnapshotDataBundle snapshotDataBundle = new SnapshotDataBundle();

    for (Map.Entry<ExternalIdBundle, Double> entry : marketData.entrySet()) {
      snapshotDataBundle.setDataPoint(entry.getKey(), entry.getValue());
    }
    return snapshotDataBundle;
  }

  private IndexON createIndexON(OvernightCurveTypeConfiguration type) {
    OvernightIndexConvention indexConvention = ConventionLink.of(OvernightIndexConvention.class, type.getConvention()).resolve();
    return ConverterUtils.indexON(indexConvention.getName(), indexConvention);
  }

  private IborIndex createIborIndex(IborCurveTypeConfiguration type) {
    IborIndexConvention indexConvention = ConventionLink.of(IborIndexConvention.class, type.getConvention()).resolve();
    return ConverterUtils.indexIbor(indexConvention.getName(), indexConvention, type.getTenor());
  }
  
  @Override
  public Result<Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>>> extractImpliedDepositCurveData(Environment env, CurveConstructionConfiguration curveConfig) {
    throw new UnsupportedOperationException();
  }

}
