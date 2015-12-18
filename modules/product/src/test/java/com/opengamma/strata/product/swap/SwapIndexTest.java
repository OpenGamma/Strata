/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;

/**
 * Test {@link SwapIndex}.
 */
@Test
public class SwapIndexTest {
  private static final Tenor TENOR = Tenor.TENOR_2Y;
  private static final FixedIborSwapTemplate TEMPLATE =
      FixedIborSwapTemplate.of(TENOR, FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M);
  private static final SwapIndex CMS_INDEX_1 = SwapIndex.of(TEMPLATE);
  private static final String NAME = new String("test");
  private static final SwapIndex CMS_INDEX_2 = SwapIndex.of(NAME, TEMPLATE);

  public void test_getter() {
    assertEquals(CMS_INDEX_1.getName(), TEMPLATE.toString());
    assertEquals(CMS_INDEX_1.getTemplate(), TEMPLATE);
    assertEquals(CMS_INDEX_2.getName(), NAME);
    assertEquals(CMS_INDEX_2.getTemplate(), TEMPLATE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(CMS_INDEX_1);
    SwapIndex other =
        SwapIndex.of("Other", FixedIborSwapTemplate.of(Tenor.TENOR_5Y, FixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M));
    coverBeanEquals(CMS_INDEX_1, other);
  }

  public void test_serialization() {
    assertSerialization(CMS_INDEX_1);
  }
}
