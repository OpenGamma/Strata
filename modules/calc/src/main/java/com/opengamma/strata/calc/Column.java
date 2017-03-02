/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;

/**
 * Defines a column in a set of calculation results.
 * <p>
 * {@link CalculationRunner} provides the ability to calculate a grid of results
 * for a given set targets and columns. This class is used to define the columns.
 * <p>
 * A column is defined in terms of a unique name, measure to be calculated and
 * a set of parameters that control the calculation. The functions to invoke
 * and the default set of parameters are defined on {@link CalculationRules}.
 */
@BeanDefinition
public final class Column implements ImmutableBean {

  /**
   * The column name.
   * <p>
   * This is the name of the column, and should be unique in a list of columns.
   */
  @PropertyDefinition(validate = "notNull")
  private final ColumnName name;
  /**
   * The measure to be calculated.
   * <p>
   * This defines the calculation being performed, such as 'PresentValue' or 'ParRate'.
   */
  @PropertyDefinition(validate = "notNull")
  private final Measure measure;
  /**
   * The reporting currency, used to control currency conversion, optional.
   * <p>
   * This is used to specify the currency that the result should be reporting in.
   * If the result is not associated with a currency, such as for "par rate", then the
   * reporting currency will effectively be ignored.
   * <p>
   * If empty, the reporting currency from {@link CalculationRules} will be used.
   */
  @PropertyDefinition(get = "optional")
  private final ReportingCurrency reportingCurrency;
  /**
   * The calculation parameters that apply to this column, used to control the how the calculation is performed.
   * <p>
   * The parameters from {@link CalculationRules} and {@code Column} are combined.
   * If a parameter is defined here and in the rules with the same
   * {@linkplain CalculationParameter#queryType() query type}, then the column parameter takes precedence.
   * <p>
   * When building, these will default to be empty.
   */
  @PropertyDefinition
  private final CalculationParameters parameters;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that will calculate the specified measure.
   * <p>
   * The column name will be the same as the name of the measure.
   * No calculation parameters are provided, thus the parameters from {@link CalculationRules} will be used.
   * Currency conversion is controlled by the reporting currency in {@code CalculationRules}.
   *
   * @param measure  the measure to be calculated
   * @return a column with the specified measure
   */
  public static Column of(Measure measure) {
    ColumnName name = ColumnName.of(measure);
    return new Column(name, measure, null, CalculationParameters.empty());
  }

  /**
   * Obtains an instance that will calculate the specified measure, converting to the specified currency.
   * <p>
   * The column name will be the same as the name of the measure.
   *
   * @param measure  the measure to be calculated
   * @param currency  the currency to convert to
   * @return a column with the specified measure
   */
  public static Column of(Measure measure, Currency currency) {
    ColumnName name = ColumnName.of(measure);
    return new Column(name, measure, ReportingCurrency.of(currency), CalculationParameters.empty());
  }

  /**
   * Obtains an instance that will calculate the specified measure, defining additional parameters.
   * <p>
   * The column name will be the same as the name of the measure.
   * The specified calculation parameters take precedence over those in {@link CalculationRules},
   * with the combined set being used for the column.
   * Currency conversion is controlled by the reporting currency in {@code CalculationRules}.
   *
   * @param measure  the measure to be calculated
   * @param parameters  the parameters that control the calculation, may be empty
   * @return a column with the specified measure and reporting currency
   */
  public static Column of(Measure measure, CalculationParameter... parameters) {
    ColumnName name = ColumnName.of(measure);
    return new Column(name, measure, null, CalculationParameters.of(parameters));
  }

  /**
   * Obtains an instance that will calculate the specified measure, converting to the specified currency,
   * defining additional parameters.
   * <p>
   * The column name will be the same as the name of the measure.
   * The specified calculation parameters take precedence over those in {@link CalculationRules},
   * with the combined set being used for the column.
   *
   * @param measure  the measure to be calculated
   * @param currency  the currency to convert to
   * @param parameters  the parameters that control the calculation, may be empty
   * @return a column with the specified measure and reporting currency
   */
  public static Column of(Measure measure, Currency currency, CalculationParameter... parameters) {
    ColumnName name = ColumnName.of(measure);
    return new Column(name, measure, ReportingCurrency.of(currency), CalculationParameters.of(parameters));
  }

  /**
   * Obtains an instance that will calculate the specified measure, defining the column name.
   * <p>
   * No calculation parameters are provided, thus the parameters from {@link CalculationRules} will be used.
   * Currency conversion is controlled by the reporting currency in {@code CalculationRules}.
   *
   * @param measure  the measure to be calculated
   * @param columnName  the column name
   * @return a column with the specified measure and column name
   */
  public static Column of(Measure measure, String columnName) {
    ColumnName name = ColumnName.of(columnName);
    return new Column(name, measure, null, CalculationParameters.empty());
  }

  /**
   * Obtains an instance that will calculate the specified measure, converting to the specified currency.
   * <p>
   * The specified currency will be wrapped in {@link ReportingCurrency} and added to the calculation parameters.
   *
   * @param measure  the measure to be calculated
   * @param columnName  the column name
   * @param currency  the currency to convert to
   * @return a column with the specified measure
   */
  public static Column of(Measure measure, String columnName, Currency currency) {
    ColumnName name = ColumnName.of(columnName);
    return new Column(name, measure, ReportingCurrency.of(currency), CalculationParameters.empty());
  }

  /**
   * Obtains an instance that will calculate the specified measure, defining the column name and parameters.
   * <p>
   * The specified calculation parameters take precedence over those in {@link CalculationRules},
   * with the combined set being used for the column.
   * Currency conversion is controlled by the reporting currency in {@code CalculationRules}.
   *
   * @param measure  the measure to be calculated
   * @param columnName  the column name
   * @param parameters  the parameters that control the calculation, may be empty
   * @return a column with the specified measure, column name and reporting currency
   */
  public static Column of(
      Measure measure,
      String columnName,
      CalculationParameter... parameters) {

    ColumnName name = ColumnName.of(columnName);
    return new Column(name, measure, null, CalculationParameters.of(parameters));
  }

  /**
   * Obtains an instance that will calculate the specified measure, converting to the specified currency,
   * defining the column name and parameters.
   * <p>
   * The specified calculation parameters take precedence over those in {@link CalculationRules},
   * with the combined set being used for the column.
   *
   * @param measure  the measure to be calculated
   * @param columnName  the column name
   * @param currency  the currency to convert to
   * @param parameters  the parameters that control the calculation, may be empty
   * @return a column with the specified measure, column name and reporting currency
   */
  public static Column of(
      Measure measure,
      String columnName,
      Currency currency,
      CalculationParameter... parameters) {

    ColumnName name = ColumnName.of(columnName);
    return new Column(name, measure, ReportingCurrency.of(currency), CalculationParameters.of(parameters));
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.parameters(CalculationParameters.empty());
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.name == null && builder.measure != null) {
      builder.name(ColumnName.of(builder.measure.getName()));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Combines the parameters with another reporting currency and set of parameters.
   * 
   * @param reportingCurrency  the default reporting currency
   * @param defaultParameters  the default parameters
   * @return the combined column
   */
  public Column combineWithDefaults(ReportingCurrency reportingCurrency, CalculationParameters defaultParameters) {
    CalculationParameters combinedParams = parameters.combinedWith(defaultParameters);
    return new Column(name, measure, getReportingCurrency().orElse(reportingCurrency), combinedParams);
  }

  /**
   * Converts this column to a column header.
   * <p>
   * The header is a reduced form of the column used in {@link Results}.
   * 
   * @return the column header
   */
  public ColumnHeader toHeader() {
    if (measure.isCurrencyConvertible()) {
      ReportingCurrency reportingCurrency = getReportingCurrency().orElse(ReportingCurrency.NATURAL);
      if (reportingCurrency.isSpecific()) {
        return ColumnHeader.of(name, measure, reportingCurrency.getCurrency());
      }
    }
    return ColumnHeader.of(name, measure);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Column}.
   * @return the meta-bean, not null
   */
  public static Column.Meta meta() {
    return Column.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(Column.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Column.Builder builder() {
    return new Column.Builder();
  }

  private Column(
      ColumnName name,
      Measure measure,
      ReportingCurrency reportingCurrency,
      CalculationParameters parameters) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(measure, "measure");
    this.name = name;
    this.measure = measure;
    this.reportingCurrency = reportingCurrency;
    this.parameters = parameters;
  }

  @Override
  public Column.Meta metaBean() {
    return Column.Meta.INSTANCE;
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
   * Gets the column name.
   * <p>
   * This is the name of the column, and should be unique in a list of columns.
   * @return the value of the property, not null
   */
  public ColumnName getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the measure to be calculated.
   * <p>
   * This defines the calculation being performed, such as 'PresentValue' or 'ParRate'.
   * @return the value of the property, not null
   */
  public Measure getMeasure() {
    return measure;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reporting currency, used to control currency conversion, optional.
   * <p>
   * This is used to specify the currency that the result should be reporting in.
   * If the result is not associated with a currency, such as for "par rate", then the
   * reporting currency will effectively be ignored.
   * <p>
   * If empty, the reporting currency from {@link CalculationRules} will be used.
   * @return the optional value of the property, not null
   */
  public Optional<ReportingCurrency> getReportingCurrency() {
    return Optional.ofNullable(reportingCurrency);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calculation parameters that apply to this column, used to control the how the calculation is performed.
   * <p>
   * The parameters from {@link CalculationRules} and {@code Column} are combined.
   * If a parameter is defined here and in the rules with the same
   * {@linkplain CalculationParameter#queryType() query type}, then the column parameter takes precedence.
   * <p>
   * When building, these will default to be empty.
   * @return the value of the property
   */
  public CalculationParameters getParameters() {
    return parameters;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Column other = (Column) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(measure, other.measure) &&
          JodaBeanUtils.equal(reportingCurrency, other.reportingCurrency) &&
          JodaBeanUtils.equal(parameters, other.parameters);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(measure);
    hash = hash * 31 + JodaBeanUtils.hashCode(reportingCurrency);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameters);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("Column{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("measure").append('=').append(measure).append(',').append(' ');
    buf.append("reportingCurrency").append('=').append(reportingCurrency).append(',').append(' ');
    buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Column}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<ColumnName> name = DirectMetaProperty.ofImmutable(
        this, "name", Column.class, ColumnName.class);
    /**
     * The meta-property for the {@code measure} property.
     */
    private final MetaProperty<Measure> measure = DirectMetaProperty.ofImmutable(
        this, "measure", Column.class, Measure.class);
    /**
     * The meta-property for the {@code reportingCurrency} property.
     */
    private final MetaProperty<ReportingCurrency> reportingCurrency = DirectMetaProperty.ofImmutable(
        this, "reportingCurrency", Column.class, ReportingCurrency.class);
    /**
     * The meta-property for the {@code parameters} property.
     */
    private final MetaProperty<CalculationParameters> parameters = DirectMetaProperty.ofImmutable(
        this, "parameters", Column.class, CalculationParameters.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "measure",
        "reportingCurrency",
        "parameters");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 938321246:  // measure
          return measure;
        case -1287844769:  // reportingCurrency
          return reportingCurrency;
        case 458736106:  // parameters
          return parameters;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Column.Builder builder() {
      return new Column.Builder();
    }

    @Override
    public Class<? extends Column> beanType() {
      return Column.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ColumnName> name() {
      return name;
    }

    /**
     * The meta-property for the {@code measure} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Measure> measure() {
      return measure;
    }

    /**
     * The meta-property for the {@code reportingCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ReportingCurrency> reportingCurrency() {
      return reportingCurrency;
    }

    /**
     * The meta-property for the {@code parameters} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CalculationParameters> parameters() {
      return parameters;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((Column) bean).getName();
        case 938321246:  // measure
          return ((Column) bean).getMeasure();
        case -1287844769:  // reportingCurrency
          return ((Column) bean).reportingCurrency;
        case 458736106:  // parameters
          return ((Column) bean).getParameters();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code Column}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Column> {

    private ColumnName name;
    private Measure measure;
    private ReportingCurrency reportingCurrency;
    private CalculationParameters parameters;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(Column beanToCopy) {
      this.name = beanToCopy.getName();
      this.measure = beanToCopy.getMeasure();
      this.reportingCurrency = beanToCopy.reportingCurrency;
      this.parameters = beanToCopy.getParameters();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 938321246:  // measure
          return measure;
        case -1287844769:  // reportingCurrency
          return reportingCurrency;
        case 458736106:  // parameters
          return parameters;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (ColumnName) newValue;
          break;
        case 938321246:  // measure
          this.measure = (Measure) newValue;
          break;
        case -1287844769:  // reportingCurrency
          this.reportingCurrency = (ReportingCurrency) newValue;
          break;
        case 458736106:  // parameters
          this.parameters = (CalculationParameters) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public Column build() {
      preBuild(this);
      return new Column(
          name,
          measure,
          reportingCurrency,
          parameters);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the column name.
     * <p>
     * This is the name of the column, and should be unique in a list of columns.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(ColumnName name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the measure to be calculated.
     * <p>
     * This defines the calculation being performed, such as 'PresentValue' or 'ParRate'.
     * @param measure  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder measure(Measure measure) {
      JodaBeanUtils.notNull(measure, "measure");
      this.measure = measure;
      return this;
    }

    /**
     * Sets the reporting currency, used to control currency conversion, optional.
     * <p>
     * This is used to specify the currency that the result should be reporting in.
     * If the result is not associated with a currency, such as for "par rate", then the
     * reporting currency will effectively be ignored.
     * <p>
     * If empty, the reporting currency from {@link CalculationRules} will be used.
     * @param reportingCurrency  the new value
     * @return this, for chaining, not null
     */
    public Builder reportingCurrency(ReportingCurrency reportingCurrency) {
      this.reportingCurrency = reportingCurrency;
      return this;
    }

    /**
     * Sets the calculation parameters that apply to this column, used to control the how the calculation is performed.
     * <p>
     * The parameters from {@link CalculationRules} and {@code Column} are combined.
     * If a parameter is defined here and in the rules with the same
     * {@linkplain CalculationParameter#queryType() query type}, then the column parameter takes precedence.
     * <p>
     * When building, these will default to be empty.
     * @param parameters  the new value
     * @return this, for chaining, not null
     */
    public Builder parameters(CalculationParameters parameters) {
      this.parameters = parameters;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("Column.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("measure").append('=').append(JodaBeanUtils.toString(measure)).append(',').append(' ');
      buf.append("reportingCurrency").append('=').append(JodaBeanUtils.toString(reportingCurrency)).append(',').append(' ');
      buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
