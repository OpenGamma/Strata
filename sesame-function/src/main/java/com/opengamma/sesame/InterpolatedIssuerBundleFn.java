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
import java.util.Map.Entry;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.legalentity.LegalEntity;
import com.opengamma.analytics.financial.legalentity.LegalEntityFilter;
import com.opengamma.analytics.financial.model.interestrate.curve.DiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldPeriodicCurve;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlock;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.IssuerProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.ParameterIssuerProviderInterface;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.matrix.DoubleMatrix2D;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.core.marketdatasnapshot.SnapshotDataBundle;
import com.opengamma.financial.analytics.curve.AbstractCurveDefinition;
import com.opengamma.financial.analytics.curve.AbstractCurveSpecification;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.CurveGroupConfiguration;
import com.opengamma.financial.analytics.curve.CurveTypeConfiguration;
import com.opengamma.financial.analytics.curve.InterpolatedCurveSpecification;
import com.opengamma.financial.analytics.curve.IssuerCurveTypeConfiguration;
import com.opengamma.financial.analytics.ircurve.strips.ContinuouslyCompoundedRateNode;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.DiscountFactorNode;
import com.opengamma.financial.analytics.ircurve.strips.PeriodicallyCompoundedRateNode;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.result.SuccessStatus;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * Issuer provider function using interpolated (pre-fitted) curves.
 */
public class InterpolatedIssuerBundleFn implements IssuerProviderBundleFn {
  
  private final CurveSpecificationFn _curveSpecificationProvider;
  
  private final CurveSpecificationMarketDataFn _curveSpecificationMarketDataProvider;

  public InterpolatedIssuerBundleFn(CurveSpecificationFn curveSpecificationProvider,
                                    CurveSpecificationMarketDataFn curveSpecificationMarketDataProvider) {
    _curveSpecificationProvider = ArgumentChecker.notNull(curveSpecificationProvider, "curveSpecificationProvider");
    _curveSpecificationMarketDataProvider =
        ArgumentChecker.notNull(curveSpecificationMarketDataProvider, "curveSpecificationMarketDataProvider");
  }

  @Override
  public Result<Pair<ParameterIssuerProviderInterface, CurveBuildingBlockBundle>> generateBundle(Environment env,
                                                                                                 CurveConstructionConfiguration curveConfig) {
    boolean valid = true;
    ZonedDateTime now = env.getValuationTime();
    IssuerProviderDiscount curveBundle = new IssuerProviderDiscount(new FXMatrix());
    LinkedHashMap<String, Pair<CurveBuildingBlock, DoubleMatrix2D>> unitBundles = new LinkedHashMap<>();

    /* For each group of curves */
    for (CurveGroupConfiguration group: curveConfig.getCurveGroups()) {

      /* For each curve definition */
      for (Entry<AbstractCurveDefinition, List<? extends CurveTypeConfiguration>> entry: group.resolveTypesForCurves().entrySet()) {

        LinkedHashMap<String, Pair<Integer, Integer>> unitMap = new LinkedHashMap<>();
        int totalNodes = 0;
        AbstractCurveDefinition curve = entry.getKey();
        Result<AbstractCurveSpecification> curveSpecResult = _curveSpecificationProvider.getCurveSpecification(env, curve);
        
        if (curveSpecResult.isSuccess()) {
  
          InterpolatedCurveSpecification specification = (InterpolatedCurveSpecification) curveSpecResult.getValue();
          
          Result<Map<ExternalIdBundle, Double>> marketDataResult =
              _curveSpecificationMarketDataProvider.requestData(env, specification);
          
          if (marketDataResult.getStatus() == SuccessStatus.SUCCESS) {
            
            // todo this is temporary to allow us to get up and running fast
            SnapshotDataBundle snapshot = createSnapshotDataBundle(marketDataResult.getValue());
  
            int n = specification.getNodes().size();
            
            double[] times = new double[n];
            double[] yields = new double[n];
            double[][] jacobian = new double[n][n];
            boolean isYield = false;
            int i = 0;
            int compoundPeriodsPerYear = 0;
            int nNodesForCurve = specification.getNodes().size();
            for (CurveNodeWithIdentifier node: specification.getNodes()) {
              CurveNode curveNode = node.getCurveNode();
              if (curveNode instanceof ContinuouslyCompoundedRateNode) {
                if (i == 0) {
                  isYield = true;
                } else {
                  if (!isYield) {
                    throw new OpenGammaRuntimeException("Was expecting only continuously-compounded rate nodes; have " + curveNode);
                  }
                }
              } else if (curveNode instanceof DiscountFactorNode) {
                if (i == 0) {
                  isYield = false;
                } else {
                  if (isYield) {
                    throw new OpenGammaRuntimeException("Was expecting only discount factor nodes; have " + curveNode);
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
              Double marketValue = snapshot.getDataPoint(node.getIdentifier());
              if (marketValue == null) {
                throw new OpenGammaRuntimeException("Could not get market value for " + node);
              }
              times[i] = TimeCalculator.getTimeBetween(now, now.plus(curveNode.getResolvedMaturity().getPeriod()));
              yields[i] = marketValue;
              jacobian[i][i] = 1;
              i++;
            }

            Interpolator1D interpolator =
                CombinedInterpolatorExtrapolatorFactory.getInterpolator(specification.getInterpolatorName(),
                                                                        specification.getLeftExtrapolatorName(),
                                                                        specification.getRightExtrapolatorName());
            String curveName = curve.getName();
            InterpolatedDoublesCurve rawCurve = InterpolatedDoublesCurve.from(times, yields, interpolator, curveName);
            YieldAndDiscountCurve discountCurve;
            if (compoundPeriodsPerYear != 0 && isYield) {
              discountCurve = YieldPeriodicCurve.from(compoundPeriodsPerYear, rawCurve);
            } else if (isYield) {
              discountCurve = new YieldCurve(curveName, rawCurve);
            } else {
              discountCurve = new DiscountCurve(curveName, rawCurve);
            }
    
            for (CurveTypeConfiguration type: entry.getValue()) {
              if (type instanceof IssuerCurveTypeConfiguration) {
                IssuerCurveTypeConfiguration issuer = (IssuerCurveTypeConfiguration) type;
                curveBundle.setCurve(Pairs.<Object, LegalEntityFilter<LegalEntity>>of(issuer.getKeys(), issuer.getFilters()), discountCurve);
                curveBundle.getMulticurveProvider().setCurve(Currency.of(curveName.substring(0, 3)), discountCurve);
              }
            }
            unitMap.put(curveName, Pairs.of(totalNodes, nNodesForCurve));
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
      return Result.success(Pairs.of((ParameterIssuerProviderInterface) curveBundle, new CurveBuildingBlockBundle(unitBundles)));
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

}
