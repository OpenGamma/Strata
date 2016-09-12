/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.Map;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.ReportingCurrency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioFxConvertible;
import com.opengamma.strata.data.scenario.ScenarioFxRateProvider;

/**
 * A single cell within a calculation task.
 * <p>
 * Each {@link CalculationTask} calculates a result for one or more cells.
 * This class capture details of each cell.
 */
@BeanDefinition(style = "light")
public final class CalculationTaskCell implements ImmutableBean {

  /**
   * The row index of the cell in the results grid.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final int rowIndex;
  /**
   * The column index of the cell in the results grid.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final int columnIndex;
  /**
   * The measure to be calculated.
   */
  @PropertyDefinition(validate = "notNull")
  private final Measure measure;
  /**
   * The reporting currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ReportingCurrency reportingCurrency;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance, specifying the cell indices, measure and reporting currency.
   * <p>
   * The result will contain no calculation parameters.
   * 
   * @param rowIndex  the row index
   * @param columnIndex  the column index
   * @param measure  the measure to calculate
   * @param reportingCurrency  the reporting currency
   * @return the cell
   */
  public static CalculationTaskCell of(
      int rowIndex,
      int columnIndex,
      Measure measure,
      ReportingCurrency reportingCurrency) {

    return new CalculationTaskCell(rowIndex, columnIndex, measure, reportingCurrency);
  }

  //-------------------------------------------------------------------------
  /**
   * Determines the reporting currency.
   * <p>
   * The reporting currency is specified using {@link ReportingCurrency}.
   * If the currency is defined to be the "natural" currency, then the function
   * is used to determine the natural currency.
   * 
   * @param task  the calculation task
   * @param refData  the reference data
   * @return the reporting currency
   */
  Currency reportingCurrency(CalculationTask task, ReferenceData refData) {
    if (reportingCurrency.isSpecific()) {
      return reportingCurrency.getCurrency();
    }
    // this should never throw an exception, because it is only called if the measure is currency-convertible
    return task.naturalCurrency(refData);
  }

  /**
   * Creates the result from the map of calculated measures.
   * <p>
   * This extracts the calculated measure and performs currency conversion if necessary.
   * 
   * @param task  the calculation task
   * @param target  the target of the calculation
   * @param results  the map of result by measure
   * @param fxProvider  the market data
   * @param refData  the reference data
   * @return the calculation result
   */
  CalculationResult createResult(
      CalculationTask task,
      CalculationTarget target,
      Map<Measure, Result<?>> results,
      ScenarioFxRateProvider fxProvider,
      ReferenceData refData) {

    // caller expects that this method does not throw an exception
    Result<?> calculated = results.get(measure);
    if (calculated == null) {
      calculated = Result.failure(
          FailureReason.CALCULATION_FAILED,
          "Measure '{}' was not calculated by the function for target type '{}'",
          measure, target.getClass().getName());
    }
    Result<?> result = convertCurrencyIfNecessary(task, calculated, fxProvider, refData);
    return CalculationResult.of(rowIndex, columnIndex, result);
  }

  // converts the value, if appropriate
  private Result<?> convertCurrencyIfNecessary(
      CalculationTask task,
      Result<?> result,
      ScenarioFxRateProvider fxProvider,
      ReferenceData refData) {

    // the result is only converted if it is a success and both the measure and value are convertible
    if (measure.isCurrencyConvertible() &&
        !reportingCurrency.isNone() &&
        result.isSuccess() &&
        result.getValue() instanceof ScenarioFxConvertible) {

      ScenarioFxConvertible<?> convertible = (ScenarioFxConvertible<?>) result.getValue();
      return convertCurrency(task, convertible, fxProvider, refData);
    }
    return result;
  }

  // converts the value
  private Result<?> convertCurrency(
      CalculationTask task,
      ScenarioFxConvertible<?> value,
      ScenarioFxRateProvider fxProvider,
      ReferenceData refData) {

    Currency resolvedReportingCurrency = reportingCurrency(task, refData);
    try {
      return Result.success(value.convertedTo(resolvedReportingCurrency, fxProvider));
    } catch (RuntimeException ex) {
      return Result.failure(
          FailureReason.CURRENCY_CONVERSION,
          ex,
          "Failed to convert value '{}' to currency '{}'",
          value,
          resolvedReportingCurrency);
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return Messages.format(
        "CalculationTaskCell[({}, {}), measure={}, currency={}]",
        rowIndex, columnIndex, measure, reportingCurrency);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CalculationTaskCell}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(CalculationTaskCell.class);

  /**
   * The meta-bean for {@code CalculationTaskCell}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  private CalculationTaskCell(
      int rowIndex,
      int columnIndex,
      Measure measure,
      ReportingCurrency reportingCurrency) {
    ArgChecker.notNegative(rowIndex, "rowIndex");
    ArgChecker.notNegative(columnIndex, "columnIndex");
    JodaBeanUtils.notNull(measure, "measure");
    JodaBeanUtils.notNull(reportingCurrency, "reportingCurrency");
    this.rowIndex = rowIndex;
    this.columnIndex = columnIndex;
    this.measure = measure;
    this.reportingCurrency = reportingCurrency;
  }

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the row index of the cell in the results grid.
   * @return the value of the property
   */
  public int getRowIndex() {
    return rowIndex;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the column index of the cell in the results grid.
   * @return the value of the property
   */
  public int getColumnIndex() {
    return columnIndex;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the measure to be calculated.
   * @return the value of the property, not null
   */
  public Measure getMeasure() {
    return measure;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reporting currency.
   * @return the value of the property, not null
   */
  public ReportingCurrency getReportingCurrency() {
    return reportingCurrency;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CalculationTaskCell other = (CalculationTaskCell) obj;
      return (rowIndex == other.rowIndex) &&
          (columnIndex == other.columnIndex) &&
          JodaBeanUtils.equal(measure, other.measure) &&
          JodaBeanUtils.equal(reportingCurrency, other.reportingCurrency);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(rowIndex);
    hash = hash * 31 + JodaBeanUtils.hashCode(columnIndex);
    hash = hash * 31 + JodaBeanUtils.hashCode(measure);
    hash = hash * 31 + JodaBeanUtils.hashCode(reportingCurrency);
    return hash;
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
