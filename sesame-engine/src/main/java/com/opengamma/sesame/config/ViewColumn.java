/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableMap;
import com.opengamma.sesame.OutputName;
import com.opengamma.util.ArgumentChecker;

/**
 * Configuration object that defines a column in a view.
 * <p>
 * Contains the column name and the configuration.
 * A different {@code ViewOutput} configuration object may be supplied
 * for each target input type.
 */
@BeanDefinition
public final class ViewColumn implements ImmutableBean {

  /**
   * The column name.
   */
  @PropertyDefinition(validate = "notNull")
  private final String _name;

  /**
   * The default configuration for the entire column.
   */
  @PropertyDefinition(get = "manual")
  private final ViewOutput _defaultOutput;

  /**
   * The outputs keyed by target input type.
   */
  @PropertyDefinition(validate = "notNull", get = "manual")
  private final ImmutableMap<Class<?>, ViewOutput> _outputs;

  /**
   * Creates an instance.
   * 
   * @param columnName  the column name, not null
   * @param defaultOutput  the default values for outputs, may be null
   * @param outputs  the map of outputs by input type, no nulls, not null
   */
  @ImmutableConstructor
  public ViewColumn(String columnName, ViewOutput defaultOutput, Map<Class<?>, ViewOutput> outputs) {
    _name = ArgumentChecker.notEmpty(columnName, "columnName");
    _outputs = ImmutableMap.copyOf(ArgumentChecker.notNull(outputs, "outputs"));
    _defaultOutput = defaultOutput;
  }

  /**
   * Gets the output name for an input type.
   * 
   * @param inputType  the input type, not null
   * @return the output name, null if not found
   */
  public OutputName getOutputName(Class<?> inputType) {
    ViewOutput viewOutput = _outputs.get(inputType);
    if (viewOutput != null && viewOutput.getOutputName() != null) {
      return viewOutput.getOutputName();
    } else if (_defaultOutput != null) {
      return _defaultOutput.getOutputName();
    } else {
      return null;
    }
  }

  /**
   * Gets the function configuration for an input type.
   * 
   * @param inputType  the input type, not null
   * @return the function configuration, null if not found
   */
  public FunctionModelConfig getFunctionConfig(Class<?> inputType) {
    ViewOutput viewOutput = _outputs.get(inputType);
    if (viewOutput == null && _defaultOutput == null) {
      return FunctionModelConfig.EMPTY;
    } else if (viewOutput == null) {
      return _defaultOutput.getFunctionModelConfig();
    } else if (_defaultOutput == null) {
      return viewOutput.getFunctionModelConfig();
    } else {
      return new CompositeFunctionModelConfig(
          viewOutput.getFunctionModelConfig(), _defaultOutput.getFunctionModelConfig());
    }
  }

  // private as clients should be calling getOutputName and getFunctionConfig,
  // only required for the Joda beans internals
  private ViewOutput getDefaultOutput() {
    return _defaultOutput;
  }

  // private as clients should be calling getOutputName and getFunctionConfig,
  // only required for the Joda beans internals
  private ImmutableMap<Class<?>, ViewOutput> getOutputs() {
    return _outputs;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ViewColumn}.
   * @return the meta-bean, not null
   */
  public static ViewColumn.Meta meta() {
    return ViewColumn.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ViewColumn.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ViewColumn.Builder builder() {
    return new ViewColumn.Builder();
  }

  @Override
  public ViewColumn.Meta metaBean() {
    return ViewColumn.Meta.INSTANCE;
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
   * @return the value of the property, not null
   */
  public String getName() {
    return _name;
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
  public ViewColumn clone() {
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ViewColumn other = (ViewColumn) obj;
      return JodaBeanUtils.equal(getName(), other.getName()) &&
          JodaBeanUtils.equal(getDefaultOutput(), other.getDefaultOutput()) &&
          JodaBeanUtils.equal(getOutputs(), other.getOutputs());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getName());
    hash += hash * 31 + JodaBeanUtils.hashCode(getDefaultOutput());
    hash += hash * 31 + JodaBeanUtils.hashCode(getOutputs());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("ViewColumn{");
    buf.append("name").append('=').append(getName()).append(',').append(' ');
    buf.append("defaultOutput").append('=').append(getDefaultOutput()).append(',').append(' ');
    buf.append("outputs").append('=').append(JodaBeanUtils.toString(getOutputs()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ViewColumn}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> _name = DirectMetaProperty.ofImmutable(
        this, "name", ViewColumn.class, String.class);
    /**
     * The meta-property for the {@code defaultOutput} property.
     */
    private final MetaProperty<ViewOutput> _defaultOutput = DirectMetaProperty.ofImmutable(
        this, "defaultOutput", ViewColumn.class, ViewOutput.class);
    /**
     * The meta-property for the {@code outputs} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Class<?>, ViewOutput>> _outputs = DirectMetaProperty.ofImmutable(
        this, "outputs", ViewColumn.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "defaultOutput",
        "outputs");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return _name;
        case 860251810:  // defaultOutput
          return _defaultOutput;
        case -1106114670:  // outputs
          return _outputs;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ViewColumn.Builder builder() {
      return new ViewColumn.Builder();
    }

    @Override
    public Class<? extends ViewColumn> beanType() {
      return ViewColumn.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return _name;
    }

    /**
     * The meta-property for the {@code defaultOutput} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ViewOutput> defaultOutput() {
      return _defaultOutput;
    }

    /**
     * The meta-property for the {@code outputs} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Class<?>, ViewOutput>> outputs() {
      return _outputs;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((ViewColumn) bean).getName();
        case 860251810:  // defaultOutput
          return ((ViewColumn) bean).getDefaultOutput();
        case -1106114670:  // outputs
          return ((ViewColumn) bean).getOutputs();
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
   * The bean-builder for {@code ViewColumn}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ViewColumn> {

    private String _name;
    private ViewOutput _defaultOutput;
    private Map<Class<?>, ViewOutput> _outputs = new HashMap<Class<?>, ViewOutput>();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ViewColumn beanToCopy) {
      this._name = beanToCopy.getName();
      this._defaultOutput = beanToCopy.getDefaultOutput();
      this._outputs = new HashMap<Class<?>, ViewOutput>(beanToCopy.getOutputs());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return _name;
        case 860251810:  // defaultOutput
          return _defaultOutput;
        case -1106114670:  // outputs
          return _outputs;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this._name = (String) newValue;
          break;
        case 860251810:  // defaultOutput
          this._defaultOutput = (ViewOutput) newValue;
          break;
        case -1106114670:  // outputs
          this._outputs = (Map<Class<?>, ViewOutput>) newValue;
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
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public ViewColumn build() {
      return new ViewColumn(
          _name,
          _defaultOutput,
          _outputs);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code name} property in the builder.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notNull(name, "name");
      this._name = name;
      return this;
    }

    /**
     * Sets the {@code defaultOutput} property in the builder.
     * @param defaultOutput  the new value
     * @return this, for chaining, not null
     */
    public Builder defaultOutput(ViewOutput defaultOutput) {
      this._defaultOutput = defaultOutput;
      return this;
    }

    /**
     * Sets the {@code outputs} property in the builder.
     * @param outputs  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder outputs(Map<Class<?>, ViewOutput> outputs) {
      JodaBeanUtils.notNull(outputs, "outputs");
      this._outputs = outputs;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ViewColumn.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(_name)).append(',').append(' ');
      buf.append("defaultOutput").append('=').append(JodaBeanUtils.toString(_defaultOutput)).append(',').append(' ');
      buf.append("outputs").append('=').append(JodaBeanUtils.toString(_outputs));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
