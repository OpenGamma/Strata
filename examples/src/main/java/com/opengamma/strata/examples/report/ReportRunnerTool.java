/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import java.time.LocalDate;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.config.pricing.PricingRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.examples.engine.ExampleEngine;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.function.OpenGammaPricingRules;
import com.opengamma.strata.report.Report;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.ReportRequirements;
import com.opengamma.strata.report.ReportRunner;
import com.opengamma.strata.report.ReportTemplate;
import com.opengamma.strata.report.trade.TradeReportRunner;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Tool for running a report from the command line.
 */
public class ReportRunnerTool {

  @Parameter(names = { "--template", "-t" }, description = "Report template input file", required = true, converter = ReportTemplateParameterConverter.class)
  private ReportTemplate template;
 
  @Parameter(names = { "--portfolio", "-p" }, description = "Portfolio input file", required = true, converter = PortfolioParameterConverter.class)
  private TradePortfolio portfolio;

  @Parameter(names = { "--date", "-d" }, description = "Valuation date, YYYY-MM-DD", required = true, converter = LocalDateParameterConverter.class)
  private LocalDate valuationDate;
  
  @Parameter(names = { "--output", "-o" }, description = "Output type, ascii or csv", converter = ReportOutputTypeParameterConverter.class)
  private ReportOutputType outputType = ReportOutputType.ASCII_TABLE;
  
  @Parameter(names = { "--help", "-h" }, description = "Displays this message", help = true)
  private boolean help;
  
  /**
   * Runs the tool.
   * 
   * @param args  the command-line arguments
   */
  public static void main(String[] args) {
    ReportRunnerTool reportRunner = new ReportRunnerTool();
    JCommander commander = new JCommander(reportRunner);
    commander.setProgramName(ReportRunnerTool.class.getSimpleName());
    try {
      commander.parse(args);
    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      System.err.println();
      commander.usage();
      return;
    }
    reportRunner.run();
  }
  
  private void run() {
    ReportRunner<ReportTemplate> reportRunner = getReportRunner(template);

    ReportRequirements requirements = reportRunner.requirements(template);
    ReportCalculationResults calculationResults = runCalculationRequirements(requirements);
    
    Report report = reportRunner.runReport(calculationResults, template);
    
    switch (outputType) {
      case ASCII_TABLE:
        report.writeAsciiTable(System.out);
        break;
      case CSV:
        report.writeCsv(System.out);
        break;
    }
  }
  
  private ReportCalculationResults runCalculationRequirements(ReportRequirements requirements) {
    CalculationEngine calculationEngine = ExampleEngine.create();
    MarketDataRules marketDataRules = ExampleMarketData.rules();
    PricingRules pricingRules = OpenGammaPricingRules.standard();

    List<Column> columns = requirements.getTradeMeasureRequirements();
    
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(pricingRules)
        .marketDataRules(marketDataRules)
        .reportingRules(ReportingRules.fixedCurrency(Currency.USD))
        .build();
    
    BaseMarketData snapshot = BaseMarketData.builder(valuationDate)
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), 1.61)
        .build();
    
    Results results = calculationEngine.calculate(portfolio.getTrades(), columns, rules, snapshot);
    return ReportCalculationResults.builder()
        .valuationDate(valuationDate)
        .columns(requirements.getTradeMeasureRequirements())
        .calculationResults(results)
        .build();
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  private ReportRunner<ReportTemplate> getReportRunner(ReportTemplate reportTemplate) {
    if (reportTemplate instanceof TradeReportTemplate) {
      // double-cast to achieve result type, allowing report runner to be used without external knowledge of template type
      return (ReportRunner) new TradeReportRunner();
    }
    throw new IllegalArgumentException(Messages.format("Unsupported report type: {}", reportTemplate.getClass().getSimpleName()));
  }
  
}
