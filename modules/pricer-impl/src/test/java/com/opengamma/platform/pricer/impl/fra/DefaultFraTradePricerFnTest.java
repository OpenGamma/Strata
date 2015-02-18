package com.opengamma.platform.pricer.impl.fra;

import static com.opengamma.basics.currency.Currency.GBP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.fra.Fra;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.fra.FraProductPricerFn;

/**
 * Test DefaultFraTradePricerFn.
 */
@Test
public class DefaultFraTradePricerFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);

  /**
   * Test against mock data
   */
  public void test_presentValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 500d);
    FraProductPricerFn<Fra> mockFraFn = mock(FraProductPricerFn.class);
    when(mockFraFn.presentValue(mockEnv, FraDummyData.FRA)).thenReturn(expected);
    DefaultFraTradePricerFn test = new DefaultFraTradePricerFn(mockFraFn);
    assertEquals(test.presentValue(mockEnv, FraDummyData.FRA_TRADE), expected);
  }
}
