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
import org.joda.beans.BeanBuilder;
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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.function.CalculationFunction;

/**
 * A set of rules that define how the calculation engine should perform calculations.
 * <p>
 * {@link CalculationRunner} provides the ability to perform calculations on many targets,
 * such as trades and positions. It returns a grid of results, with the targets as rows.
 * Each individual calculation is controlled by three things:
 * <ul>
 *   <li>The {@linkplain CalculationFunction function}, selected by the target type</li>
 *   <li>The {@linkplain Measure measure}, the high-level output to be calculated</li>
 *   <li>The {@linkplain CalculationParameters parameters}, adjust how the measure is to be calculated</li>
 * </ul>
 * {@code CalculationRules} operates in association with {@link Column}.
 * The column is used to define the measure. It can also be used to specify column-specific parameters.
 * The rules contain the complete set of functions and the default set of parameters.
 */
@BeanDefinition(builderScope = "private")
public final class CalculationRules implements ImmutableBean {

  /**
   * The calculation functions.
   * <p>
   * Functions provide the logic of the calculation.
   * Each type of target must have an associated function in order for calculations to be performed.
   */
  @PropertyDefinition(validate = "notNull")
  private final CalculationFunctions functions;
  /**
   * The market data rules.
   * <p>
   * Market data rules provide logic that selects the appropriate market data for the calculation
   */
  @PropertyDefinition(validate = "notNull")
  private final MarketDataRules marketDataRules;
  /**
   * The calculation parameters, used to control the how the calculation is performed.
   * <p>
   * Parameters are used to parameterize the {@link Measure} to be calculated.
   * They may be specified in two places - here and in the {@link Column}.
   * The parameters specified here are the defaults that apply to all columns.
   * <p>
   * If a parameter is defined here and in the column with the same
   * {@linkplain CalculationParameter#queryType() query type}, then the column parameter takes precedence.
   * <p>
   * There are many possible parameter implementations, for example {@link ReportingCurrency}.
   */
  @PropertyDefinition(validate = "notNull")
  private final CalculationParameters parameters;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance specifying the functions and market data rules.
   * <p>
   * The output will uses the "natural" {@linkplain ReportingCurrency reporting currency}.
   * 
   * @param functions  the calculation functions
   * @param marketDataRules  the market data rules
   * @return the rules
   */
  public static CalculationRules of(CalculationFunctions functions, MarketDataRules marketDataRules) {
    return new CalculationRules(functions, marketDataRules, CalculationParameters.empty());
  }

  /**
   * Obtains an instance specifying the functions, market data rules and reporting currency.
   * 
   * @param functions  the calculation functions
   * @param marketDataRules  the market data rules
   * @param reportingCurrency  the reporting currency
   * @return the rules
   */
  public static CalculationRules of(
      CalculationFunctions functions,
      MarketDataRules marketDataRules,
      Currency reportingCurrency) {

    CalculationParameters params = CalculationParameters.of(ReportingCurrency.of(reportingCurrency));
    return new CalculationRules(functions, marketDataRules, params);
  }

  /**
   * Obtains an instance specifying the functions, market data rules, reporting currency and additional parameters.
   * 
   * @param functions  the calculation functions
   * @param marketDataRules  the market data rules
   * @param reportingCurrency  the reporting currency
   * @param parameters  the parameters that control the calculation, may be empty
   * @return the rules
   */
  public static CalculationRules of(
      CalculationFunctions functions,
      MarketDataRules marketDataRules,
      Currency reportingCurrency,
      CalculationParameter... parameters) {

    ReportingCurrency ccy = ReportingCurrency.of(reportingCurrency);
    CalculationParameters input = CalculationParameters.of(parameters);
    CalculationParameters params = CalculationParameters.of(ccy).combinedWith(input);
    return new CalculationRules(functions, marketDataRules, params);
  }

  /**
   * Obtains an instance specifying the functions to use and some additional parameters.
   * <p>
   * The additional parameters are used to control how the calculation is performed.
   * There are many possible parameter implementations, for example {@link ReportingCurrency}.
   * 
   * @param functions  the calculation functions
   * @param marketDataRules  the market data rules
   * @param parameters  the parameters that control the calculation, may be empty
   * @return the rules
   */
  public static CalculationRules of(
      CalculationFunctions functions,
      MarketDataRules marketDataRules,
      CalculationParameter... parameters) {

    return new CalculationRules(functions, marketDataRules, CalculationParameters.of(parameters));
  }

  /**
   * Obtains an instance specifying the functions to use and some additional parameters.
   * <p>
   * The additional parameters are used to control how the calculation is performed.
   * There are many possible parameter implementations, for example {@link ReportingCurrency}.
   * 
   * @param functions  the calculation functions
   * @param marketDataRules  the market data rules
   * @param parameters  the parameters that control the calculation, may be empty
   * @return the rules
   */
  public static CalculationRules of(
      CalculationFunctions functions,
      MarketDataRules marketDataRules,
      CalculationParameters parameters) {

    return new CalculationRules(functions, marketDataRules, parameters);
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.set(meta().parameters, CalculationParameters.empty());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CalculationRules}.
   * @return the meta-bean, not null
   */
  public static CalculationRules.Meta meta() {
    return CalculationRules.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CalculationRules.Meta.INSTANCE);
  }

  private CalculationRules(
      CalculationFunctions functions,
      MarketDataRules marketDataRules,
      CalculationParameters parameters) {
    JodaBeanUtils.notNull(functions, "functions");
    JodaBeanUtils.notNull(marketDataRules, "marketDataRules");
    JodaBeanUtils.notNull(parameters, "parameters");
    this.functions = functions;
    this.marketDataRules = marketDataRules;
    this.parameters = parameters;
  }

  @Override
  public CalculationRules.Meta metaBean() {
    return CalculationRules.Meta.INSTANCE;
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
   * Gets the calculation functions.
   * <p>
   * Functions provide the logic of the calculation.
   * Each type of target must have an associated function in order for calculations to be performed.
   * @return the value of the property, not null
   */
  public CalculationFunctions getFunctions() {
    return functions;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market data rules.
   * <p>
   * Market data rules provide logic that selects the appropriate market data for the calculation
   * @return the value of the property, not null
   */
  public MarketDataRules getMarketDataRules() {
    return marketDataRules;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calculation parameters, used to control the how the calculation is performed.
   * <p>
   * Parameters are used to parameterize the {@link Measure} to be calculated.
   * They may be specified in two places - here and in the {@link Column}.
   * The parameters specified here are the defaults that apply to all columns.
   * <p>
   * If a parameter is defined here and in the column with the same
   * {@linkplain CalculationParameter#queryType() query type}, then the column parameter takes precedence.
   * <p>
   * There are many possible parameter implementations, for example {@link ReportingCurrency}.
   * @return the value of the property, not null
   */
  public CalculationParameters getParameters() {
    return parameters;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CalculationRules other = (CalculationRules) obj;
      return JodaBeanUtils.equal(functions, other.functions) &&
          JodaBeanUtils.equal(marketDataRules, other.marketDataRules) &&
          JodaBeanUtils.equal(parameters, other.parameters);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(functions);
    hash = hash * 31 + JodaBeanUtils.hashCode(marketDataRules);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameters);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("CalculationRules{");
    buf.append("functions").append('=').append(functions).append(',').append(' ');
    buf.append("marketDataRules").append('=').append(marketDataRules).append(',').append(' ');
    buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CalculationRules}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code functions} property.
     */
    private final MetaProperty<CalculationFunctions> functions = DirectMetaProperty.ofImmutable(
        this, "functions", CalculationRules.class, CalculationFunctions.class);
    /**
     * The meta-property for the {@code marketDataRules} property.
     */
    private final MetaProperty<MarketDataRules> marketDataRules = DirectMetaProperty.ofImmutable(
        this, "marketDataRules", CalculationRules.class, MarketDataRules.class);
    /**
     * The meta-property for the {@code parameters} property.
     */
    private final MetaProperty<CalculationParameters> parameters = DirectMetaProperty.ofImmutable(
        this, "parameters", CalculationRules.class, CalculationParameters.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "functions",
        "marketDataRules",
        "parameters");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -140572773:  // functions
          return functions;
        case 363016849:  // marketDataRules
          return marketDataRules;
        case 458736106:  // parameters
          return parameters;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CalculationRules> builder() {
      return new CalculationRules.Builder();
    }

    @Override
    public Class<? extends CalculationRules> beanType() {
      return CalculationRules.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code functions} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CalculationFunctions> functions() {
      return functions;
    }

    /**
     * The meta-property for the {@code marketDataRules} property.
     * @return the meta-property, not null
     */
    public MetaProperty<MarketDataRules> marketDataRules() {
      return marketDataRules;
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
        case -140572773:  // functions
          return ((CalculationRules) bean).getFunctions();
        case 363016849:  // marketDataRules
          return ((CalculationRules) bean).getMarketDataRules();
        case 458736106:  // parameters
          return ((CalculationRules) bean).getParameters();
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
   * The bean-builder for {@code CalculationRules}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CalculationRules> {

    private CalculationFunctions functions;
    private MarketDataRules marketDataRules;
    private CalculationParameters parameters;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -140572773:  // functions
          return functions;
        case 363016849:  // marketDataRules
          return marketDataRules;
        case 458736106:  // parameters
          return parameters;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -140572773:  // functions
          this.functions = (CalculationFunctions) newValue;
          break;
        case 363016849:  // marketDataRules
          this.marketDataRules = (MarketDataRules) newValue;
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
    public CalculationRules build() {
      return new CalculationRules(
          functions,
          marketDataRules,
          parameters);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("CalculationRules.Builder{");
      buf.append("functions").append('=').append(JodaBeanUtils.toString(functions)).append(',').append(' ');
      buf.append("marketDataRules").append('=').append(JodaBeanUtils.toString(marketDataRules)).append(',').append(' ');
      buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
