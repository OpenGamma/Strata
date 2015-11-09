package com.opengamma.strata.pricer.rate.future;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.finance.rate.future.DeliverableSwapFuture;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.HullWhiteOneFactorPiecewiseConstantProvider;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

@Test
public class HullWhiteDeliverableSwapFutureProductPricerTest {

  private static final ImmutableRatesProvider PROVIDER = DeliverableSwapFuturePricerTestDataSets.RATES_PROVIDER;
  private static final HullWhiteOneFactorPiecewiseConstantProvider HW_PROVIDER =
      DeliverableSwapFuturePricerTestDataSets.HULL_WHITE_PROVIDER;
  private static final Swap SWAP = DeliverableSwapFuturePricerTestDataSets.SWAP;
  private static final DeliverableSwapFuture FUTURE = DeliverableSwapFuturePricerTestDataSets.SWAP_FUTURE;
  private static final double TOL = 1.0e-13;
  private static final double EPS = 1.0e-6;
  private static final HullWhiteDeliverableSwapFutureProductPricer PRICER =
      HullWhiteDeliverableSwapFutureProductPricer.DEFAULT;

  //-------------------------------------------------------------------------
  public void regression_price() {
    assertEquals(PRICER.price(FUTURE, PROVIDER, HW_PROVIDER), 1.0222181740116423, TOL);
  }

  public void regression_priceSensitivity() {
    PointSensitivities point = PRICER.priceSensitivity(FUTURE, PROVIDER, HW_PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    double[] expDsc = new double[] {0.002797409919438432, 0.002292166577292524, 0.0010759097605893947,
      -0.010487876273859748, -0.0680638930854084, -0.07328738324456216 };
    double[] expFwd = new double[] {0.12490237822152674, 0.11910282001466879, -0.012521352359753726,
      -0.07708487469158797, -0.2900222129646318, -9.209849498229216 };
    assertTrue(DoubleArrayMath.fuzzyEquals(computed
        .getSensitivity(PROVIDER.getDiscountCurves().get(USD).getName(), USD)
        .getSensitivity().toArray(), expDsc, TOL));
    assertTrue(DoubleArrayMath.fuzzyEquals(computed
        .getSensitivity(PROVIDER.getIndexCurves().get(USD_LIBOR_3M).getName(), USD)
        .getSensitivity().toArray(), expFwd, TOL));
  }
}
