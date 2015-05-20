package com.opengamma.strata.examples.fpml;

import com.opengamma.strata.examples.fpml.generated.DataDocument;
import com.opengamma.strata.examples.fpml.generated.Trade;

import static com.opengamma.strata.examples.fpml.XmlTools.deserializeFromXml;

public class LoadACDSFromFpML {

  private static final String exampleProduct = "fpml/v5_7/pretrade/products/credit-derivatives/pretrade-ex-01-cdsn-long-form.xml";

  public static void main(String[] args) {

    DataDocument dataDocument = deserializeFromXml(DataDocument.class, exampleProduct);
    Trade cdsTrade = dataDocument.getTrade().get(0);
  }

}
