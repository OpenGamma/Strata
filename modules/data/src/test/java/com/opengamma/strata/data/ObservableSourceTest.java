/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link ObservableSource}.
 */
@Test
public class ObservableSourceTest {

  //-----------------------------------------------------------------------
  public void coverage() {
    ObservableSource test = ObservableSource.of("Foo");
    assertEquals(test.toString(), "Foo");
    assertSerialization(test);
    assertJodaConvert(ObservableSource.class, test);
  }

}
