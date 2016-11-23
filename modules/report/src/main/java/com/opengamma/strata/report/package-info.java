/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

/**
 * <h2>Reporting Framework</h2>
 * This package and its sub-packages define a reporting framework used to format and report the results of calculations.
 * <p>
 * Reports are generated using a report template and a set of calculation results. A report contains a table of
 * data where the rows are the inputs to the calculations, for example trades, and the columns contain
 * data from the calculation results or the trades.
 * 
 * <h2>Report templates</h2>
 * Report templates specify the type of the report and the columns included in the report. A report template is
 * defined using an .ini file.
 * <p>
 * The first section of the .ini file defines the settings for the report. Currently this only contains the
 * type of the report:
 * <pre>
 *   [Settings]
 *   reportType = trade
 * </pre>
 * The report columns are defined by the remaining sections in the .ini file. The section header defines the
 * column name and the {@code value} attribute is an expression defining the value in the column. The following
 * snippet from a trade report for FRA trades defines three columns: Settlement Date, Index and Par Rate.
 * <pre>
 *   [Settlement Date]
 *   value = Trade.settlementDate
 *
 *   [Index]
 *   value = Product.index.name
 *
 *   [Par Rate]
 *   value = Measures.ParRate
 * </pre>
 * The {@code value} expression is consists of multiple sections separated by dots. The first section specifies
 * the object which is the source of the data in the column. The supported values are:
 * <ul>
 *   <li>Trade - the data is taken from the {@link com.opengamma.strata.product.Trade trade} or
 *   {@link com.opengamma.strata.product.TradeInfo trade info}</li>
 *   <li>Security - the data is taken from the {@link com.opengamma.strata.product.Security security} or
 *   {@link com.opengamma.strata.product.SecurityInfo security info}</li>
 *   <li>Position - the data is taken from the {@link com.opengamma.strata.product.Position position} or
 *   {@link com.opengamma.strata.product.PositionInfo position info}</li>
 *   <li>Target - the data is taken from the {@link com.opengamma.strata.basics.CalculationTarget calculation target}</li>
 *   <li>Measure - the data is taken from the results of the calculations</li>
 *   <li>Product - the data is taken from the {@link com.opengamma.strata.product.Product product} associated with
 *   the trade. This is only applicable if the trade implements
 *   {@link com.opengamma.strata.product.ProductTrade ProductTrade}</li>
 * </ul>
 * The remaining parts of the expression are evaluated against the source object to find the value to display
 * in the column. For example, if the expression is '{@code Product.index.name}' and the results contain
 * {@link com.opengamma.strata.product.fra.FraTrade FraTrade} instances
 * the following calls will be made for each trade in the results:
 * <ul>
 *   <li>{@code FraTrade.getProduct()} returning a {@link com.opengamma.strata.product.fra.Fra Fra}</li>
 *   <li>{@code Fra.getIndex()} returning an {@link com.opengamma.strata.basics.index.IborIndex IborIndex}</li>
 *   <li>{@code IborIndex.getName()} returning the index name</li>
 * </ul>
 * The cell in the report will contain name of the index referenced by the FRA.
 */
package com.opengamma.strata.report;
