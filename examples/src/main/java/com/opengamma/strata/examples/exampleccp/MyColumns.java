package com.opengamma.strata.examples.exampleccp;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.config.Measure;

import java.util.List;

public class MyColumns {

  public static List<Column> create() {
    return ImmutableList.of(
        Column.of(Measure.ID),
        Column.of(Measure.COUNTERPARTY),
        Column.of(Measure.SETTLEMENT_DATE),
        Column.of(Measure.MATURITY_DATE),
        Column.of(Measure.NOTIONAL),
        Column.of(Measure.PRESENT_VALUE),
        Column.of(Measure.PRESENT_VALUE_PAY_LEG),
        Column.of(Measure.PRESENT_VALUE_RECEIVE_LEG),
        Column.of(Measure.ACCRUED_INTEREST));
  }

}
