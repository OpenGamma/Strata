package com.opengamma.sesame.irfutureoption;

import org.threeten.bp.Period;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.BlackSTIRFuturesProviderInterface;
import com.opengamma.analytics.financial.provider.description.interestrate.BlackSTIRFuturesSmileProvider;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.GridInterpolator2D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.analytics.math.surface.InterpolatedDoublesSurface;
import com.opengamma.analytics.math.surface.Surface;
import com.opengamma.financial.convention.businessday.ModifiedFollowingBusinessDayConvention;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.financial.security.option.IRFutureOptionSecurity;
import com.opengamma.sesame.DiscountingMulticurveCombinerFn;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Internal test function to use the multicurve construction to build a MulticurveDiscountProvider and a static list 
 * of vols.
 */
public final class TestBlackSTIRFuturesProviderFn implements BlackSTIRFuturesProviderFn {
  
  private final DiscountingMulticurveCombinerFn _discountingMulticurveCombinerFn;
  
  public TestBlackSTIRFuturesProviderFn(DiscountingMulticurveCombinerFn discountingMulticurveCombinerFn) {
    ArgumentChecker.notNull(discountingMulticurveCombinerFn, "discountingMulticurveCombinerFn");
    _discountingMulticurveCombinerFn = discountingMulticurveCombinerFn;
  }

  @Override
  public Result<BlackSTIRFuturesProviderInterface> getBlackSTIRFuturesProvider(Environment env, IRFutureOptionSecurity irFutureOption) {
    
    Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> bundleResult =
        _discountingMulticurveCombinerFn.createMergedMulticurveBundle(env, irFutureOption, Result.success(new FXMatrix()));
    
    if (bundleResult.isSuccess()) {

      MulticurveProviderDiscount multicurve = bundleResult.getValue().getFirst();
      
      IborIndex index = new IborIndex(Currency.USD, Period.ofMonths(3), 2, DayCounts.ACT_360, new ModifiedFollowingBusinessDayConvention(), true, "USD");
      
      Surface<Double, Double, Double> blackParameters = testSurface;
      
      BlackSTIRFuturesProviderInterface black = new BlackSTIRFuturesSmileProvider(multicurve, blackParameters, index);
      
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