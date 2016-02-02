/**
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

import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.config.pricing.PricingRules;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Defines a column in a set of calculation results. A column specifies a measure and may specify
 * overrides for the pricing rules and market data rules.
 */
@BeanDefinition
public final class Column implements ImmutableBean {

  /**
   * The measure to be calculated.
   * <p>
   * This defines the calculation being performed, such as 'PresentValue' or 'ParRate'.
   */
  @PropertyDefinition(validate = "notNull")
  private final Measure measure;
  /**
   * The column name.
   * <p>
   * This is the name of the column, and should be unique in a list of columns.
   */
  @PropertyDefinition(validate = "notNull")
  private final ColumnName name;
  /**
   * The pricing rules that apply to this column, merged with the default rules.
   * <p>
   * The final set of rules for a column consist of the default rules merged with
   * the column-specific rules, with the column-specific rules taking precedence.
   * In most cases, there is no need to specify column-specific rules.
   */
  @PropertyDefinition(validate = "notNull")
  private final PricingRules pricingRules;
  /**
   * The market data rules that apply to this column, merged with the default rules.
   * <p>
   * The final set of rules for a column consist of the default rules merged with
   * the column-specific rules, with the column-specific rules taking precedence.
   * In most cases, there is no need to specify column-specific rules.
   */
  @PropertyDefinition(validate = "notNull")
  private final MarketDataRules marketDataRules;
  /**
   * The reporting currency that applies to this column, overriding the default reporting currency.
   * <p>
   * In most cases, there is no need to specify a column-specific reporting currency.
   */
  @PropertyDefinition(get = "optional")
  private final ReportingCurrency reportingCurrency;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that will calculate the specified measure.
   * <p>
   * The column name will be the name of the measure.
   * The rules will be empty, thus the column will use the default rules.
   * The reporting currency will be empty, thus the column will use the default reporting currency.
   * <p>
   * If a column is required with rules overrides, use a {@linkplain #builder() builder}.
   *
   * @param measure  the measure to be calculated
   * @return a column with the specified measure
   */
  public static Column of(Measure measure) {
    ArgChecker.notNull(measure, "measure");
    return new Column(measure, ColumnName.of(measure.toString()), PricingRules.empty(), MarketDataRules.empty(), null);
  }

  /**
   * Obtains an instance that will calculate the specified measure, defining the reporting currency.
   * <p>
   * The column name will be the name of the measure.
   * The rules will be empty, thus the column will use the default rules.
   * <p>
   * If a column is required with rules overrides, use a {@linkplain #builder() builder}.
   *
   * @param measure  the measure to be calculated
   * @param reportingCurrency  the reporting currency to use
   * @return a column with the specified measure and reporting currency
   */
  public static Column of(Measure measure, ReportingCurrency reportingCurrency) {
    ArgChecker.notNull(measure, "measure");
    ArgChecker.notNull(reportingCurrency, "reportingCurrency");
    return new Column(
        measure, ColumnName.of(measure.toString()), PricingRules.empty(), MarketDataRules.empty(), reportingCurrency);
  }

  /**
   * Obtains an instance that will calculate the specified measure, defining the column name.
   * <p>
   * The rules will be empty, thus the column will use the default rules.
   * The reporting currency will be empty, thus the column will use the default reporting currency.
   * <p>
   * If a column is required with rules overrides, use a {@linkplain #builder() builder}.
   *
   * @param measure  the measure to be calculated
   * @param columnName  the column name
   * @return a column with the specified measure and column name
   */
  public static Column of(Measure measure, String columnName) {
    ArgChecker.notNull(measure, "measure");
    ArgChecker.notNull(columnName, "columnName");
    return new Column(measure, ColumnName.of(columnName), PricingRules.empty(), MarketDataRules.empty(), null);
  }

  /**
   * Obtains an instance that will calculate the specified measure, defining the column name and reporting currency.
   * <p>
   * The column name will be the name of the measure.
   * The rules will be empty, thus the column will use the default rules.
   * <p>
   * If a column is required with rules overrides, use a {@linkplain #builder() builder}.
   *
   * @param measure  the measure to be calculated
   * @param columnName  the column name
   * @param reportingCurrency  the reporting currency to use
   * @return a column with the specified measure, column name and reporting currency
   */
  public static Column of(Measure measure, String columnName, ReportingCurrency reportingCurrency) {
    ArgChecker.notNull(measure, "measure");
    ArgChecker.notNull(columnName, "columnName");
    ArgChecker.notNull(reportingCurrency, "reportingCurrency");
    return new Column(
        measure, ColumnName.of(measure.toString()), PricingRules.empty(), MarketDataRules.empty(), reportingCurrency);
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.pricingRules(PricingRules.empty());
    builder.marketDataRules(MarketDataRules.empty());
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.name == null && builder.measure != null) {
      builder.name(ColumnName.of(builder.measure.getName()));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a column whose rules are derived from the rules in this column composed with the default rules.
   *
   * @param defaultCalculationRules  the default rules
   * @return a column whose rules are derived from the rules in this column composed with the default rules
   */
  public Column withDefaultRules(CalculationRules defaultCalculationRules) {
    PricingRules pricingRules = getPricingRules().composedWith(defaultCalculationRules.getPricingRules());
    MarketDataRules marketDataRules = getMarketDataRules().composedWith(defaultCalculationRules.getMarketDataRules());
    ReportingCurrency reportingCurrency = getReportingCurrency().orElse(defaultCalculationRules.getReportingCurrency());
    return toBuilder()
        .pricingRules(pricingRules)
        .marketDataRules(marketDataRules)
        .reportingCurrency(reportingCurrency)
        .build();
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
      Measure measure,
      ColumnName name,
      PricingRules pricingRules,
      MarketDataRules marketDataRules,
      ReportingCurrency reportingCurrency) {
    JodaBeanUtils.notNull(measure, "measure");
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(pricingRules, "pricingRules");
    JodaBeanUtils.notNull(marketDataRules, "marketDataRules");
    this.measure = measure;
    this.name = name;
    this.pricingRules = pricingRules;
    this.marketDataRules = marketDataRules;
    this.reportingCurrency = reportingCurrency;
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
   * Gets the pricing rules that apply to this column, merged with the default rules.
   * <p>
   * The final set of rules for a column consist of the default rules merged with
   * the column-specific rules, with the column-specific rules taking precedence.
   * In most cases, there is no need to specify column-specific rules.
   * @return the value of the property, not null
   */
  public PricingRules getPricingRules() {
    return pricingRules;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market data rules that apply to this column, merged with the default rules.
   * <p>
   * The final set of rules for a column consist of the default rules merged with
   * the column-specific rules, with the column-specific rules taking precedence.
   * In most cases, there is no need to specify column-specific rules.
   * @return the value of the property, not null
   */
  public MarketDataRules getMarketDataRules() {
    return marketDataRules;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reporting currency that applies to this column, overriding the default reporting currency.
   * <p>
   * In most cases, there is no need to specify a column-specific reporting currency.
   * @return the optional value of the property, not null
   */
  public Optional<ReportingCurrency> getReportingCurrency() {
    return Optional.ofNullable(reportingCurrency);
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
      return JodaBeanUtils.equal(measure, other.measure) &&
          JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(pricingRules, other.pricingRules) &&
          JodaBeanUtils.equal(marketDataRules, other.marketDataRules) &&
          JodaBeanUtils.equal(reportingCurrency, other.reportingCurrency);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(measure);
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(pricingRules);
    hash = hash * 31 + JodaBeanUtils.hashCode(marketDataRules);
    hash = hash * 31 + JodaBeanUtils.hashCode(reportingCurrency);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("Column{");
    buf.append("measure").append('=').append(measure).append(',').append(' ');
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("pricingRules").append('=').append(pricingRules).append(',').append(' ');
    buf.append("marketDataRules").append('=').append(marketDataRules).append(',').append(' ');
    buf.append("reportingCurrency").append('=').append(JodaBeanUtils.toString(reportingCurrency));
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
     * The meta-property for the {@code measure} property.
     */
    private final MetaProperty<Measure> measure = DirectMetaProperty.ofImmutable(
        this, "measure", Column.class, Measure.class);
    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<ColumnName> name = DirectMetaProperty.ofImmutable(
        this, "name", Column.class, ColumnName.class);
    /**
     * The meta-property for the {@code pricingRules} property.
     */
    private final MetaProperty<PricingRules> pricingRules = DirectMetaProperty.ofImmutable(
        this, "pricingRules", Column.class, PricingRules.class);
    /**
     * The meta-property for the {@code marketDataRules} property.
     */
    private final MetaProperty<MarketDataRules> marketDataRules = DirectMetaProperty.ofImmutable(
        this, "marketDataRules", Column.class, MarketDataRules.class);
    /**
     * The meta-property for the {@code reportingCurrency} property.
     */
    private final MetaProperty<ReportingCurrency> reportingCurrency = DirectMetaProperty.ofImmutable(
        this, "reportingCurrency", Column.class, ReportingCurrency.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "measure",
        "name",
        "pricingRules",
        "marketDataRules",
        "reportingCurrency");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 938321246:  // measure
          return measure;
        case 3373707:  // name
          return name;
        case 1055696081:  // pricingRules
          return pricingRules;
        case 363016849:  // marketDataRules
          return marketDataRules;
        case -1287844769:  // reportingCurrency
          return reportingCurrency;
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
     * The meta-property for the {@code measure} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Measure> measure() {
      return measure;
    }

    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ColumnName> name() {
      return name;
    }

    /**
     * The meta-property for the {@code pricingRules} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PricingRules> pricingRules() {
      return pricingRules;
    }

    /**
     * The meta-property for the {@code marketDataRules} property.
     * @return the meta-property, not null
     */
    public MetaProperty<MarketDataRules> marketDataRules() {
      return marketDataRules;
    }

    /**
     * The meta-property for the {@code reportingCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ReportingCurrency> reportingCurrency() {
      return reportingCurrency;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 938321246:  // measure
          return ((Column) bean).getMeasure();
        case 3373707:  // name
          return ((Column) bean).getName();
        case 1055696081:  // pricingRules
          return ((Column) bean).getPricingRules();
        case 363016849:  // marketDataRules
          return ((Column) bean).getMarketDataRules();
        case -1287844769:  // reportingCurrency
          return ((Column) bean).reportingCurrency;
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

    private Measure measure;
    private ColumnName name;
    private PricingRules pricingRules;
    private MarketDataRules marketDataRules;
    private ReportingCurrency reportingCurrency;

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
      this.measure = beanToCopy.getMeasure();
      this.name = beanToCopy.getName();
      this.pricingRules = beanToCopy.getPricingRules();
      this.marketDataRules = beanToCopy.getMarketDataRules();
      this.reportingCurrency = beanToCopy.reportingCurrency;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 938321246:  // measure
          return measure;
        case 3373707:  // name
          return name;
        case 1055696081:  // pricingRules
          return pricingRules;
        case 363016849:  // marketDataRules
          return marketDataRules;
        case -1287844769:  // reportingCurrency
          return reportingCurrency;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 938321246:  // measure
          this.measure = (Measure) newValue;
          break;
        case 3373707:  // name
          this.name = (ColumnName) newValue;
          break;
        case 1055696081:  // pricingRules
          this.pricingRules = (PricingRules) newValue;
          break;
        case 363016849:  // marketDataRules
          this.marketDataRules = (MarketDataRules) newValue;
          break;
        case -1287844769:  // reportingCurrency
          this.reportingCurrency = (ReportingCurrency) newValue;
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
          measure,
          name,
          pricingRules,
          marketDataRules,
          reportingCurrency);
    }

    //-----------------------------------------------------------------------
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
     * Sets the pricing rules that apply to this column, merged with the default rules.
     * <p>
     * The final set of rules for a column consist of the default rules merged with
     * the column-specific rules, with the column-specific rules taking precedence.
     * In most cases, there is no need to specify column-specific rules.
     * @param pricingRules  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder pricingRules(PricingRules pricingRules) {
      JodaBeanUtils.notNull(pricingRules, "pricingRules");
      this.pricingRules = pricingRules;
      return this;
    }

    /**
     * Sets the market data rules that apply to this column, merged with the default rules.
     * <p>
     * The final set of rules for a column consist of the default rules merged with
     * the column-specific rules, with the column-specific rules taking precedence.
     * In most cases, there is no need to specify column-specific rules.
     * @param marketDataRules  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder marketDataRules(MarketDataRules marketDataRules) {
      JodaBeanUtils.notNull(marketDataRules, "marketDataRules");
      this.marketDataRules = marketDataRules;
      return this;
    }

    /**
     * Sets the reporting currency that applies to this column, overriding the default reporting currency.
     * <p>
     * In most cases, there is no need to specify a column-specific reporting currency.
     * @param reportingCurrency  the new value
     * @return this, for chaining, not null
     */
    public Builder reportingCurrency(ReportingCurrency reportingCurrency) {
      this.reportingCurrency = reportingCurrency;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("Column.Builder{");
      buf.append("measure").append('=').append(JodaBeanUtils.toString(measure)).append(',').append(' ');
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("pricingRules").append('=').append(JodaBeanUtils.toString(pricingRules)).append(',').append(' ');
      buf.append("marketDataRules").append('=').append(JodaBeanUtils.toString(marketDataRules)).append(',').append(' ');
      buf.append("reportingCurrency").append('=').append(JodaBeanUtils.toString(reportingCurrency));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
