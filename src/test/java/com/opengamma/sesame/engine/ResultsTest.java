/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.util.result.ResultGenerator;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class ResultsTest {

  private static final Results RESULTS = new Results(ImmutableList.of("col1", "col2"),
                                                     ImmutableList.of(row("input1", "item11", "item12"),
                                                                      row("input2", "item21", "item22")),
                                                     Collections.<String, ResultItem>emptyMap());

  @Test
  public void getByIndex() {
    assertEquals("item11", RESULTS.get(0, 0).getResult().getValue());
    assertEquals("item22", RESULTS.get(1, 1).getResult().getValue());
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void getByInvalidRowIndex() {
    RESULTS.get(2, 0);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void getByInvalidColumnIndex() {
    RESULTS.get(0, 2);
  }

  @Test
  public void getRow() {
    assertEquals(row("input2", "item21", "item22"), RESULTS.get(1));
  }

  @Test
  public void getByColumnName() {
    assertEquals("item11", RESULTS.get(0, "col1").getResult().getValue());
    assertEquals("item22", RESULTS.get(1, "col2").getResult().getValue());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void getByUnknownColumnName() {
    RESULTS.get(0, "col3");
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void getByColumnNameAndInvalidRowIndex() {
    RESULTS.get(2, "col1");
  }

  private static ResultRow row(Object input, Object... results) {
    List<ResultItem> items = Lists.newArrayListWithCapacity(results.length);
    for (Object result : results) {
      items.add(new ResultItem(ResultGenerator.success(result), null));
    }
    return new ResultRow(input, items);
  }
}
