/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.Arrays;

import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.financial.security.equity.EquitySecurity;

/**
 *
 */
public final class ViewDefBuilder {

  private static final String DESCRIPTION = "Description";

  private ViewDefBuilder() {
  }

  public static ViewDef viewDef(String name, ViewColumn... columns) {
    return new ViewDef(name, Arrays.asList(columns));
  }

  public static ViewColumn column(String name, ColumnOutput... requirements) {
    return new ViewColumn(name, Arrays.asList(requirements));
  }

  public static ColumnOutput output(String outputName, Class<?> targetType) {
    return new ColumnOutput(outputName, targetType);
  }

  //static FunctionConfig config

  public static void main(String[] args) {
    ViewDef viewDef =
        viewDef("name",
                column("Description",
                       output(DESCRIPTION, EquitySecurity.class),
                       output(DESCRIPTION, CashFlowSecurity.class)),
                column("Bloomberg Ticker",
                       output(DESCRIPTION, EquitySecurity.class),
                       output(DESCRIPTION, CashFlowSecurity.class)),
                column("ACTIV Symbol",
                       output(DESCRIPTION, EquitySecurity.class),
                       output(DESCRIPTION, CashFlowSecurity.class)));
    System.out.println(viewDef);
  }
}
