package com.opengamma.sesame.component;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

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
    checkResults(null, FunctionService.DEFAULT_SERVICES);
  }

  public void emptyMeansNoneAreUsed() {
    checkResults(ImmutableList.<String>of(), FunctionService.NONE);
  }

  public void unparseableElementIsIgnored() {
    checkResults(ImmutableList.of("CACHING", "MEEETRICS"), FunctionService.CACHING);
  }

  public void allUnparseableMeansDefaultsAreUsed() {
    checkResults(ImmutableList.of("CAAAACHING", "MEEETRICS"), FunctionService.DEFAULT_SERVICES);
  }

  public void successfulParsing() {
    checkResults(ImmutableList.of("CACHING", "METRICS"), FunctionService.CACHING, FunctionService.METRICS);
  }

  private void checkResults(List<String> requestedServices, FunctionService... expectedServices) {
    checkResults(requestedServices, EnumSet.copyOf(Arrays.asList(expectedServices)));
  }

  private void checkResults(List<String> requestedServices, EnumSet<FunctionService> expectedServices) {
    FunctionServiceParser parser = new FunctionServiceParser(requestedServices);
    assertThat(parser.determineFunctionServices(), is(expectedServices));
  }
}
