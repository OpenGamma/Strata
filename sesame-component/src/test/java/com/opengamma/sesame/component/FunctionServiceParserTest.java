package com.opengamma.sesame.component;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.EnumSet;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.sesame.engine.FunctionService;
import com.opengamma.util.test.TestGroup;

/**
 * Tests the FunctionServiceParser handles user supplied
 * lists correctly.
 */
@Test(groups = TestGroup.UNIT)
public class FunctionServiceParserTest {

  public void nullMeansDefaultsAreUsed() {
    FunctionServiceParser parser = new FunctionServiceParser(null);
    assertThat(parser.determineFunctionServices(), is(FunctionService.DEFAULT_SERVICES));
  }

  public void emptyMeansNoneAreUsed() {
    FunctionServiceParser parser = new FunctionServiceParser(ImmutableList.<String>of());
    assertThat(parser.determineFunctionServices().isEmpty(), is(true));
  }

  public void unparseableElementIsIgnored() {
    FunctionServiceParser parser = new FunctionServiceParser(ImmutableList.of("CACHING", "MEEETRICS"));
    assertThat(parser.determineFunctionServices(), is(EnumSet.of(FunctionService.CACHING)));
  }

  public void allUnparseableMeansDefaultsAreUsed() {
    FunctionServiceParser parser = new FunctionServiceParser(ImmutableList.of("CAAAACHING", "MEEETRICS"));
    assertThat(parser.determineFunctionServices(), is(FunctionService.DEFAULT_SERVICES));
  }

  public void successfulParsing() {
    FunctionServiceParser parser = new FunctionServiceParser(ImmutableList.of("CACHING", "METRICS"));
    assertThat(parser.determineFunctionServices(), is(EnumSet.of(FunctionService.CACHING, FunctionService.METRICS)));
  }
}
