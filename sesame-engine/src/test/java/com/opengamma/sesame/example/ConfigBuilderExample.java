/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.columns;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutput;
import static com.opengamma.sesame.config.ConfigBuilder.nonPortfolioOutputs;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static com.opengamma.sesame.config.ConfigBuilder.viewDef;

import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.config.ViewDef;

/**
 * Mini DSL for building instances of {@link ViewDef} and related classes in code. See the
 * {@link #main} method for an example.
 */
public final class ConfigBuilderExample {

  public void main(String[] args) {
    ViewDef example1 =
        viewDef("columns only",
                column(OutputNames.DESCRIPTION),
                column(OutputNames.DESCRIPTION,
                       config(
                           implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                           arguments(
                               function(DefaultIdSchemeFn.class,
                                        argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))),
                column("Bloomberg Ticker", OutputNames.DESCRIPTION,
                       output(EquitySecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class))),
                       output(CashFlowSecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class)))),
                column("ACTIV Symbol", OutputNames.DESCRIPTION,
                       output(EquitySecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                                  arguments(
                                      function(DefaultIdSchemeFn.class,
                                               argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))),
                       output(CashFlowSecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                                  arguments(
                                      function(DefaultIdSchemeFn.class,
                                               argument("scheme", ExternalSchemes.ACTIVFEED_TICKER)))))));
    System.out.println(example1);

    ViewDef example2 =
        viewDef("columns and other outputs",
                columns(
                    column(OutputNames.DESCRIPTION),
                    column(OutputNames.DESCRIPTION,
                           config(
                               implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                               arguments(
                                   function(DefaultIdSchemeFn.class,
                                            argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))),
                    column("Bloomberg Ticker", OutputNames.DESCRIPTION,
                           output(EquitySecurity.class,
                                  config(
                                      implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class))),
                           output(CashFlowSecurity.class,
                                  config(
                                      implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class)))),
                    column("ACTIV Symbol", OutputNames.DESCRIPTION,
                           output(EquitySecurity.class,
                                  config(
                                      implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                                      arguments(
                                          function(DefaultIdSchemeFn.class,
                                                   argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))),
                           output(CashFlowSecurity.class,
                                  config(
                                      implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                                      arguments(
                                          function(DefaultIdSchemeFn.class,
                                                   argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))))),
                nonPortfolioOutputs(
                    nonPortfolioOutput("USD Discounting Curve",
                                       output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE))));
    System.out.println(example2);

    ViewDef example3 =
        viewDef("other outputs only",
                nonPortfolioOutput("USD Discounting Curve",
                                   output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE))); // TODO some function config
    System.out.println(example3);
  }

}
