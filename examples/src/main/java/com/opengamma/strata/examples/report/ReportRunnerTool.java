/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.measure.StandardComponents.marketDataFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Strings;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.calc.runner.CalculationTasks;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.report.Report;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.ReportRequirements;
import com.opengamma.strata.report.ReportRunner;
import com.opengamma.strata.report.ReportTemplate;
import com.opengamma.strata.report.cashflow.CashFlowReportRunner;
import com.opengamma.strata.report.cashflow.CashFlowReportTemplate;
import com.opengamma.strata.report.framework.format.ReportOutputFormat;
import com.opengamma.strata.report.trade.TradeReportRunner;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Tool for running a report from the command line.
 */
public final class ReportRunnerTool implements AutoCloseable {

  /**
   * The calculation runner.
   */
  private final CalculationRunner runner;

  @Parameter(
      names = {"-t", "--template"},
      description = "Report template input file",
      required = true,
      converter = ReportTemplateParameterConverter.class)
  private ReportTemplate template;

  @Parameter(
      names = {"-m", "--marketdata"},
      description = "Market data root directory",
      validateValueWith = MarketDataRootValidator.class)
  private File marketDataRoot;

  @Parameter(
      names = {"-p", "--portfolio"},
      description = "Portfolio input file",
      required = true,
      converter = TradeListParameterConverter.class)
  private TradeList tradeList;

  @Parameter(
      names = {"-d", "--date"},
      description = "Valuation date, YYYY-MM-DD",
      required = true,
      converter = LocalDateParameterConverter.class)
  private LocalDate valuationDate;

  @Parameter(
      names = {"-f", "--format"},
      description = "Report output format, ascii or csv",
      converter = ReportOutputFormatParameterConverter.class)
  private ReportOutputFormat format = ReportOutputFormat.ASCII_TABLE;

  @Parameter(
      names = {"-i", "--id"},
      description = "An ID by which to select a single trade")
  private String idSearch;

  @Parameter(
      names = {"-h", "--help"},
      description = "Displays this message",
      help = true)
  private boolean help;

  @Parameter(
      names = {"-v", "--version"},
      description = "Prints the version of this tool",
      help = true)
  private boolean version;

  /**
   * Runs the tool.
   * 
   * @param args  the command-line arguments
   */
  public static void main(String[] args) {
    try (ReportRunnerTool reportRunner = new ReportRunnerTool(CalculationRunner.ofMultiThreaded())) {
      JCommander commander = new JCommander(reportRunner);
      commander.setProgramName(ReportRunnerTool.class.getSimpleName());
      try {
        commander.parse(args);
      } catch (ParameterException e) {
        System.err.println("Error: " + e.getMessage());
        System.err.println();
        commander.usage();
        return;
      }
      if (reportRunner.help) {
        commander.usage();
      } else if (reportRunner.version) {
        String versionName = ReportRunnerTool.class.getPackage().getImplementationVersion();
        if (versionName == null) {
          versionName = "unknown";
        }
        System.out.println("Strata Report Runner Tool, version " + versionName);
      } else {
        try {
          reportRunner.run();
        } catch (Exception e) {
          System.err.println(Messages.format("Error: {}\n", e.getMessage()));
          commander.usage();
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  // creates an instance
  private ReportRunnerTool(CalculationRunner runner) {
    this.runner = ArgChecker.notNull(runner, "runner");
  }

  //-------------------------------------------------------------------------
  private void run() {
    ReportRunner<ReportTemplate> reportRunner = getReportRunner(template);
    ReportRequirements requirements = reportRunner.requirements(template);
    ReportCalculationResults calculationResults = runCalculationRequirements(requirements);

    Report report = reportRunner.runReport(calculationResults, template);

    switch (format) {
      case ASCII_TABLE:
        report.writeAsciiTable(System.out);
        break;
      case CSV:
        report.writeCsv(System.out);
        break;
    }
  }

  private ReportCalculationResults runCalculationRequirements(ReportRequirements requirements) {
    List<Column> columns = requirements.getTradeMeasureRequirements();

    ExampleMarketDataBuilder marketDataBuilder =
        marketDataRoot == null ? ExampleMarketData.builder() : ExampleMarketDataBuilder.ofPath(marketDataRoot.toPath());

    CalculationFunctions functions = StandardComponents.calculationFunctions();
    RatesMarketDataLookup ratesLookup = marketDataBuilder.ratesLookup(valuationDate);
    CalculationRules rules = CalculationRules.of(functions, ratesLookup);

    MarketData marketData = marketDataBuilder.buildSnapshot(valuationDate);

    List<Trade> trades;

    if (Strings.nullToEmpty(idSearch).trim().isEmpty()) {
      trades = tradeList.getTrades();
    } else {
      trades = tradeList.getTrades().stream()
          .filter(t -> t.getInfo().getId().isPresent())
          .filter(t -> t.getInfo().getId().get().getValue().equals(idSearch))
          .collect(toImmutableList());
      if (trades.size() > 1) {
        throw new IllegalArgumentException(
            Messages.format("More than one trade found matching ID: '{}'", idSearch));
      }
    }
    if (trades.isEmpty()) {
      throw new IllegalArgumentException("No trades found. Please check the input portfolio or trade ID filter.");
    }

    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // calculate the results
    CalculationTasks tasks = CalculationTasks.of(rules, trades, columns);
    MarketDataRequirements reqs = tasks.requirements(refData);
    MarketData calibratedMarketData = marketDataFactory().create(reqs, MarketDataConfig.empty(), marketData, refData);
    Results results = runner.getTaskRunner().calculate(tasks, calibratedMarketData, refData);

    return ReportCalculationResults.of(
        valuationDate, trades, requirements.getTradeMeasureRequirements(), results, functions, refData);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ReportRunner<ReportTemplate> getReportRunner(ReportTemplate reportTemplate) {
    // double-casts to achieve result type, allowing report runner to be used without external knowledge of template type
    if (reportTemplate instanceof TradeReportTemplate) {
      return (ReportRunner) TradeReportRunner.INSTANCE;
    } else if (reportTemplate instanceof CashFlowReportTemplate) {
      return (ReportRunner) CashFlowReportRunner.INSTANCE;
    }
    throw new IllegalArgumentException(Messages.format("Unsupported report type: {}", reportTemplate.getClass().getSimpleName()));
  }

  @Override
  public void close() {
    runner.close();
  }

}
