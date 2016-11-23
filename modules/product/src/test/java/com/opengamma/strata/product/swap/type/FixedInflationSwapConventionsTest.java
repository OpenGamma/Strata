/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.PriceIndices;

/**
 * Test {@link FixedInflationSwapConventions}.
 */
@Test
public class FixedInflationSwapConventionsTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "floatLeg")
  static Object[][] data_float_leg() {
    return new Object[][] {
        {FixedInflationSwapConventions.CHF_FIXED_ZC_CH_CPI, PriceIndices.CH_CPI},
        {FixedInflationSwapConventions.EUR_FIXED_ZC_EU_AI_CPI, PriceIndices.EU_AI_CPI},
        {FixedInflationSwapConventions.EUR_FIXED_ZC_EU_EXT_CPI, PriceIndices.EU_EXT_CPI},
        {FixedInflationSwapConventions.EUR_FIXED_ZC_FR_CPI, PriceIndices.FR_EXT_CPI},
        {FixedInflationSwapConventions.GBP_FIXED_ZC_GB_HCIP, PriceIndices.GB_HICP},
        {FixedInflationSwapConventions.GBP_FIXED_ZC_GB_RPI, PriceIndices.GB_RPI},
        {FixedInflationSwapConventions.GBP_FIXED_ZC_GB_RPIX, PriceIndices.GB_RPIX},
        {FixedInflationSwapConventions.JPY_FIXED_ZC_JP_CPI, PriceIndices.JP_CPI_EXF},
        {FixedInflationSwapConventions.USD_FIXED_ZC_US_CPI, PriceIndices.US_CPI_U},
    };
  }

  @Test(dataProvider = "floatLeg")
  public void test_float_leg(FixedInflationSwapConvention convention, PriceIndex floatLeg) {
    assertEquals(convention.getFloatingLeg().getIndex(), floatLeg);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FixedIborSwapConventions.class);
    coverPrivateConstructor(StandardFixedIborSwapConventions.class);
  }

}
