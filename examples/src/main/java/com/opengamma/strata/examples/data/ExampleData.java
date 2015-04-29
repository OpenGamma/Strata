package com.opengamma.strata.examples.data;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.joda.beans.ser.JodaBeanSer;

import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.examples.finance.SwapPricingExample;

/**
 * Contains utilities for working with data in the examples environment.
 */
public final class ExampleData {

  /**
   * Restricted constructor.
   */
  private ExampleData() {
  }
  
  //-------------------------------------------------------------------------
  /**
   * Loads a golden copy of expected results from a JSON resource.
   * 
   * @param name  the name of the results
   * @return the loaded results
   */
  public static Results loadExpectedResults(String name) {
    return loadFromJson(String.format("/goldencopy/%s.json", name), Results.class);
  }
  
  // loads a resource from JSON
  public static <T> T loadFromJson(String resourceName, Class<T> clazz) {
    InputStream tsResource = SwapPricingExample.class.getResourceAsStream(resourceName);
    if (tsResource == null) {
      throw new MissingExampleDataException(resourceName);
    }
    Reader tsReader = new InputStreamReader(tsResource);
    return JodaBeanSer.COMPACT.jsonReader().read(tsReader, clazz);
  }
    
}
