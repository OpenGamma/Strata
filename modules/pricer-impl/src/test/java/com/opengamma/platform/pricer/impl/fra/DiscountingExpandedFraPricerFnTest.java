package com.opengamma.platform.pricer.impl.fra;

import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.platform.pricer.impl.fra.FraDummyData.FRA;
import static com.opengamma.platform.pricer.impl.fra.FraDummyData.FRA_AFMA;
import static com.opengamma.platform.pricer.impl.fra.FraDummyData.FRA_NONE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.temporal.ChronoUnit;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.fra.ExpandedFra;
import com.opengamma.platform.finance.observation.RateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.observation.DispatchingRateObservationFn;
import com.opengamma.platform.pricer.observation.RateObservationFn;

/**
 * Test DiscountingExpandedFraPricerFn
 */
@Test
public class DiscountingExpandedFraPricerFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);
  private final DispatchingRateObservationFn mockObs = mock(DispatchingRateObservationFn.class);
  private static final double TOLERANCE = 1E-12;

  /**
   * Consistency between present value and future value for ISDA, and present value for no discounting. 
   */
  public void testConsistencyISDA() {
    double discountFactor = 0.98d;
    RateObservationFn<RateObservation> observation = DispatchingRateObservationFn.DEFAULT;
    when(mockEnv.discountFactor(FRA.getCurrency(), FRA.expand().getPaymentDate())).thenReturn(discountFactor);
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(observation);
    assertEquals(test.presentValue(mockEnv, FRA.expand()).getAmount(FRA.getCurrency()).getAmount(),
        test.futureValue(mockEnv, FRA.expand()).multipliedBy(discountFactor).getAmount(FRA.getCurrency()).getAmount(),
        TOLERANCE);
    assertEquals(test.presentValue(mockEnv, FRA_NONE.expand()), test.futureValue(mockEnv, FRA.expand()));
  }

  /**
   * 
   */
  public void TestFutureValueAFMA() {
    double discountFactor = 0.965d;
    RateObservationFn<RateObservation> observation = DispatchingRateObservationFn.DEFAULT;
    when(mockEnv.discountFactor(FRA_AFMA.getCurrency(), FRA_AFMA.expand().getPaymentDate())).thenReturn(discountFactor);
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(observation);
    assertThrowsIllegalArg(() -> test.futureValue(mockEnv, FRA_AFMA.expand()));
  }

  /**
   * 
   */
  public void TestFutureValueNoDsc() {
    double discountFactor = 0.965d;
    RateObservationFn<RateObservation> observation = DispatchingRateObservationFn.DEFAULT;
    when(mockEnv.discountFactor(FRA_NONE.getCurrency(), FRA_NONE.expand().getPaymentDate())).thenReturn(discountFactor);
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(observation);
    assertThrowsIllegalArg(() -> test.futureValue(mockEnv, FRA_NONE.expand()));
  }

  /**
   * Test future value for ISDA. 
   */
  public void testFutureISDA() {
    double forwardRate = 0.02;
    ExpandedFra fraExp = FRA.expand();
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate()))
        .thenReturn(forwardRate);
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    MultiCurrencyAmount computed = test.futureValue(mockEnv, fraExp);
    double fixedRate = FRA.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    double expected = notional * (forwardRate - fixedRate) * yearFraction / (1 + forwardRate * yearFraction);
    assertEquals(computed.getAmount(FRA.getCurrency()).getAmount(), expected, TOLERANCE);
  }

  /**
   * Test present value for AFMA. 
   */
  public void testPresentAFMA() {
    double forwardRate = 0.018;
    ExpandedFra fraExp = FRA_AFMA.expand();
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate()))
        .thenReturn(forwardRate);
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    MultiCurrencyAmount computed = test.presentValue(mockEnv, fraExp);
    double fixedRate = FRA_AFMA.getFixedRate();
    double yearFraction = ChronoUnit.DAYS.between(fraExp.getStartDate(), fraExp.getEndDate()) / 365.0;
    double notional = fraExp.getNotional();
    double expected = notional * (1.0 / (1 + fixedRate * yearFraction) - 1.0 / (1 + forwardRate * yearFraction));
    assertEquals(computed.getAmount(FRA.getCurrency()).getAmount(), expected, TOLERANCE);
  }
}
