/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.ReportingRules;
import com.opengamma.strata.calc.config.pricing.PricingRules;

/**
 * Defines a column in a set of calculation results. A column specifies a measure and may specify
 * overrides for the pricing rules and market data rules.
 */
@BeanDefinition
public final class Column implements ImmutableBean {

  /** The definition of the column which specifies the column name and the measures it contains. */
  @PropertyDefinition(validate = "notNull")
  private final ColumnDefinition definition;

  /** The pricing rules that apply to this column in addition to the default rules. */
  @PropertyDefinition(validate = "notNull")
  private final PricingRules pricingRules;

  /** The market data rules that apply to this column in addition to the default rules. */
  @PropertyDefinition(validate = "notNull")
  private final MarketDataRules marketDataRules;

  /** The reporting rules that apply to this column in addition to the default rules. */
  @PropertyDefinition(validate = "notNull")
  private final ReportingRules reportingRules;

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.pricingRules(PricingRules.empty());
    builder.marketDataRules(MarketDataRules.empty());
    builder.reportingRules(ReportingRules.empty());
  }

  /**
   * Returns a column that contains the specified measure and uses the default calculation rules.
   * <p>
   * If a column is required with rules overrides, use a {@linkplain #builder() builder}.
   *
   * @param measure  a measure
   * @return a column containing that specified measure that uses the default calculation rules
   */
  public static Column of(Measure measure) {
    return Column.builder().definition(ColumnDefinition.of(measure)).build();
  }

  /**
   * Returns a column that contains the specified measure and uses the default calculation rules.
   * <p>
   * If a column is required with rules overrides, use a {@linkplain #builder() builder}.
   *
   * @param measure  a measure
   * @param name  the column name
   * @return a column with the specified measure and name that uses the default calculation rules
   */
  public static Column of(Measure measure, String name) {
    return Column.builder().definition(ColumnDefinition.of(measure, name)).build();
  }

  /**
   * Returns a column defined by the definition that uses the default calculation rules.
   * <p>
   * If a column is required with rules overrides, use a {@linkplain #builder() builder}.
   *
   * @param definition  the column definition to base the column on
   * @return a column defined by the definition that uses the default calculation rules
   */
  public static Column of(ColumnDefinition definition) {
    return Column.builder().definition(definition).build();
  }

  /**
   * Returns a column whose rules are derived from the rules in this column composed with the default rules.
   *
   * @param defaultPricingRules  the default pricing rules
   * @param defaultMarketDataRules  the default market data rules
   * @param defaultReportingRules  the default reporting currency rules
   * @return a column whose rules are derived from the rules in this column composed with the default rules
   */
  public Column withDefaultRules(
      PricingRules defaultPricingRules,
      MarketDataRules defaultMarketDataRules,
      ReportingRules defaultReportingRules) {

    PricingRules pricingRules = getPricingRules().composedWith(defaultPricingRules);
    MarketDataRules marketDataRules = getMarketDataRules().composedWith(defaultMarketDataRules);
    ReportingRules reportingRules = getReportingRules().composedWith(defaultReportingRules);
    return toBuilder()
        .pricingRules(pricingRules)
        .marketDataRules(marketDataRules)
        .reportingRules(reportingRules)
        .build();
  }

  /**
   * Returns the column name
   *
   * @return the column name
   */
  public ColumnName getName() {
    return definition.getName();
  }

  /**
   * Returns the measure displayed in the column for the target.
   * 
   * @param target  a calculation target
   * @return the measure displayed in the column for the target
   */
  public Measure getMeasure(CalculationTarget target) {
    return definition.getMeasure(target);
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
      ColumnDefinition definition,
      PricingRules pricingRules,
      MarketDataRules marketDataRules,
      ReportingRules reportingRules) {
    JodaBeanUtils.notNull(definition, "definition");
    JodaBeanUtils.notNull(pricingRules, "pricingRules");
    JodaBeanUtils.notNull(marketDataRules, "marketDataRules");
    JodaBeanUtils.notNull(reportingRules, "reportingRules");
    this.definition = definition;
    this.pricingRules = pricingRules;
    this.marketDataRules = marketDataRules;
    this.reportingRules = reportingRules;
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
   * Gets the definition of the column which specifies the column name and the measures it contains.
   * @return the value of the property, not null
   */
  public ColumnDefinition getDefinition() {
    return definition;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the pricing rules that apply to this column in addition to the default rules.
   * @return the value of the property, not null
   */
  public PricingRules getPricingRules() {
    return pricingRules;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market data rules that apply to this column in addition to the default rules.
   * @return the value of the property, not null
   */
  public MarketDataRules getMarketDataRules() {
    return marketDataRules;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reporting rules that apply to this column in addition to the default rules.
   * @return the value of the property, not null
   */
  public ReportingRules getReportingRules() {
    return reportingRules;
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
      return JodaBeanUtils.equal(getDefinition(), other.getDefinition()) &&
          JodaBeanUtils.equal(getPricingRules(), other.getPricingRules()) &&
          JodaBeanUtils.equal(getMarketDataRules(), other.getMarketDataRules()) &&
          JodaBeanUtils.equal(getReportingRules(), other.getReportingRules());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getDefinition());
    hash = hash * 31 + JodaBeanUtils.hashCode(getPricingRules());
    hash = hash * 31 + JodaBeanUtils.hashCode(getMarketDataRules());
    hash = hash * 31 + JodaBeanUtils.hashCode(getReportingRules());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("Column{");
    buf.append("definition").append('=').append(getDefinition()).append(',').append(' ');
    buf.append("pricingRules").append('=').append(getPricingRules()).append(',').append(' ');
    buf.append("marketDataRules").append('=').append(getMarketDataRules()).append(',').append(' ');
    buf.append("reportingRules").append('=').append(JodaBeanUtils.toString(getReportingRules()));
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
     * The meta-property for the {@code definition} property.
     */
    private final MetaProperty<ColumnDefinition> definition = DirectMetaProperty.ofImmutable(
        this, "definition", Column.class, ColumnDefinition.class);
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
     * The meta-property for the {@code reportingRules} property.
     */
    private final MetaProperty<ReportingRules> reportingRules = DirectMetaProperty.ofImmutable(
        this, "reportingRules", Column.class, ReportingRules.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "definition",
        "pricingRules",
        "marketDataRules",
        "reportingRules");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1014418093:  // definition
          return definition;
        case 1055696081:  // pricingRules
          return pricingRules;
        case 363016849:  // marketDataRules
          return marketDataRules;
        case -1647034519:  // reportingRules
          return reportingRules;
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
     * The meta-property for the {@code definition} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ColumnDefinition> definition() {
      return definition;
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
     * The meta-property for the {@code reportingRules} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ReportingRules> reportingRules() {
      return reportingRules;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1014418093:  // definition
          return ((Column) bean).getDefinition();
        case 1055696081:  // pricingRules
          return ((Column) bean).getPricingRules();
        case 363016849:  // marketDataRules
          return ((Column) bean).getMarketDataRules();
        case -1647034519:  // reportingRules
          return ((Column) bean).getReportingRules();
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

    private ColumnDefinition definition;
    private PricingRules pricingRules;
    private MarketDataRules marketDataRules;
    private ReportingRules reportingRules;

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
      this.definition = beanToCopy.getDefinition();
      this.pricingRules = beanToCopy.getPricingRules();
      this.marketDataRules = beanToCopy.getMarketDataRules();
      this.reportingRules = beanToCopy.getReportingRules();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1014418093:  // definition
          return definition;
        case 1055696081:  // pricingRules
          return pricingRules;
        case 363016849:  // marketDataRules
          return marketDataRules;
        case -1647034519:  // reportingRules
          return reportingRules;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1014418093:  // definition
          this.definition = (ColumnDefinition) newValue;
          break;
        case 1055696081:  // pricingRules
          this.pricingRules = (PricingRules) newValue;
          break;
        case 363016849:  // marketDataRules
          this.marketDataRules = (MarketDataRules) newValue;
          break;
        case -1647034519:  // reportingRules
          this.reportingRules = (ReportingRules) newValue;
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
      return new Column(
          definition,
          pricingRules,
          marketDataRules,
          reportingRules);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the definition of the column which specifies the column name and the measures it contains.
     * @param definition  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder definition(ColumnDefinition definition) {
      JodaBeanUtils.notNull(definition, "definition");
      this.definition = definition;
      return this;
    }

    /**
     * Sets the pricing rules that apply to this column in addition to the default rules.
     * @param pricingRules  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder pricingRules(PricingRules pricingRules) {
      JodaBeanUtils.notNull(pricingRules, "pricingRules");
      this.pricingRules = pricingRules;
      return this;
    }

    /**
     * Sets the market data rules that apply to this column in addition to the default rules.
     * @param marketDataRules  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder marketDataRules(MarketDataRules marketDataRules) {
      JodaBeanUtils.notNull(marketDataRules, "marketDataRules");
      this.marketDataRules = marketDataRules;
      return this;
    }

    /**
     * Sets the reporting rules that apply to this column in addition to the default rules.
     * @param reportingRules  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder reportingRules(ReportingRules reportingRules) {
      JodaBeanUtils.notNull(reportingRules, "reportingRules");
      this.reportingRules = reportingRules;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("Column.Builder{");
      buf.append("definition").append('=').append(JodaBeanUtils.toString(definition)).append(',').append(' ');
      buf.append("pricingRules").append('=').append(JodaBeanUtils.toString(pricingRules)).append(',').append(' ');
      buf.append("marketDataRules").append('=').append(JodaBeanUtils.toString(marketDataRules)).append(',').append(' ');
      buf.append("reportingRules").append('=').append(JodaBeanUtils.toString(reportingRules));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
