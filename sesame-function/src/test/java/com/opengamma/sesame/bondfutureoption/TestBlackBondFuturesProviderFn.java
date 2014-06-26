package com.opengamma.sesame.bondfutureoption;

import org.testng.internal.annotations.Sets;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.legalentity.CreditRating;
import com.opengamma.analytics.financial.legalentity.LegalEntity;
import com.opengamma.analytics.financial.legalentity.Region;
import com.opengamma.analytics.financial.legalentity.Sector;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.BlackBondFuturesFlatProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.BlackBondFuturesProviderInterface;
import com.opengamma.analytics.financial.provider.description.interestrate.IssuerProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.ParameterIssuerProviderInterface;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.GridInterpolator2D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.analytics.math.surface.InterpolatedDoublesSurface;
import com.opengamma.analytics.math.surface.Surface;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.IssuerProviderFn;
import com.opengamma.sesame.trade.BondFutureOptionTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Internal test function to use the multicurve construction to build a MulticurveDiscountProvider and a static list 
 * of vols.
 */
public final class TestBlackBondFuturesProviderFn implements BlackBondFuturesProviderFn {
  
  private final IssuerProviderFn _discountingMulticurveCombinerFn;
  
  public TestBlackBondFuturesProviderFn(IssuerProviderFn discountingMulticurveCombinerFn) {
    ArgumentChecker.notNull(discountingMulticurveCombinerFn, "discountingMulticurveCombinerFn");
    _discountingMulticurveCombinerFn = discountingMulticurveCombinerFn;
  }

  @Override
  public Result<BlackBondFuturesProviderInterface> getBlackBondFuturesProvider(Environment env, BondFutureOptionTrade trade) {
    
    Result<Pair<ParameterIssuerProviderInterface, CurveBuildingBlockBundle>> bundleResult =
        _discountingMulticurveCombinerFn.createBundle(env, trade, Result.success(new FXMatrix()));
    
    if (bundleResult.isSuccess()) {

      IssuerProviderDiscount multicurve = (IssuerProviderDiscount) bundleResult.getValue().getFirst();
      
      Surface<Double, Double, Double> blackParameters = testSurface;
            
      LegalEntity simpleLegalEntity = new LegalEntity("", "", Sets.<CreditRating>newHashSet(), Sector.of(""), Region.of(""));
      BlackBondFuturesProviderInterface black = new BlackBondFuturesFlatProviderDiscount(multicurve, blackParameters, simpleLegalEntity);
      
      return Result.success(black);
    }
    
    return Result.failure(bundleResult);
  }

  private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
                                                                                                            Interpolator1DFactory.LINEAR,
                                                                                                            Interpolator1DFactory.FLAT_EXTRAPOLATOR,
                                                                                                            Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  
  private final InterpolatedDoublesSurface testSurface = InterpolatedDoublesSurface.from(
      new double[] { 10, 20, 30 },                                                                                   
      new double[] { 10, 20, 30 },
      new double[] { 10, 20, 30 },
      INTERPOLATOR_2D
      );
}