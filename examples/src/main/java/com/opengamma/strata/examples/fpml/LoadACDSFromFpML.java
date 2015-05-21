package com.opengamma.strata.examples.fpml;

import com.google.common.base.Preconditions;
import com.opengamma.strata.examples.fpml.generated.CreditDefaultSwap;
import com.opengamma.strata.examples.fpml.generated.DataDocument;
import com.opengamma.strata.examples.fpml.generated.Product;
import com.opengamma.strata.examples.fpml.generated.Trade;

import javax.xml.bind.JAXBElement;

import static com.opengamma.strata.examples.fpml.XmlTools.deserializeFromXml;

public class LoadACDSFromFpML {

  private static final String exampleCds = "fpml/v5_7/pretrade/products/credit-derivatives/pretrade-ex-01-cdsn-long-form.xml";
  private static final String exampleIndex = "fpml/v5_7/pretrade/products/credit-derivatives/pretrade-ex-06-cdx-short-form.xml";

  public static void main(String[] args) {
    go(exampleCds);
    go(exampleIndex);
  }

  private static void go(String xmlFile) {
    DataDocument dataDocument = deserializeFromXml(DataDocument.class, xmlFile);
    Trade cdsTrade = dataDocument.getTrade().get(0);
    cdsTrade.getId();
    JAXBElement<? extends Product> product = cdsTrade.getProduct();
    Preconditions.checkArgument(product.getDeclaredType().equals(CreditDefaultSwap.class));
    Preconditions.checkArgument(product.getValue() instanceof  CreditDefaultSwap);
    CreditDefaultSwap cdsProduct = (CreditDefaultSwap)product.getValue();
    cdsProduct.getFeeLeg();
  }

}
