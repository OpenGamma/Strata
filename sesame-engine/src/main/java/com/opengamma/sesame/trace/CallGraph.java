/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
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

import com.google.common.collect.ImmutableList;

/**
 * A tree tracing a method call and any calls made during its execution.
 * Contains the arguments, return values and details of any exceptions thrown for all the executed methods.
 * The stack trace isn't used in {@link #hashCode()} or {@link #equals(Object)}.
 */
@BeanDefinition
public final class CallGraph implements ImmutableBean {

  /** The type of the object implementing the called method. */
  @PropertyDefinition(validate = "notNull")
  private final Class<?> _receiverClass;

  /** The name of the method. */
  @PropertyDefinition(validate = "notNull")
  private final String _methodName;

  /** The method's parameter types. */
  @PropertyDefinition(validate = "notNull")
  private final List<Class<?>> _parameterTypes;

  /** The arguments passed to the method call. */
  @PropertyDefinition(validate = "notNull")
  private final List<Object> _arguments;

  /** The return value from the method call. */
  @PropertyDefinition
  private final Object _returnValue;

  /** Throwable thrown when the method was called. */
  @PropertyDefinition
  private final Class<?> _throwableClass;

  /** Error message of the throwable thrown when the method was called. */
  @PropertyDefinition
  private final String _errorMessage;

  /** Stack trace of the throwable thrown when the method was called. */
  @PropertyDefinition
  private final String _stackTrace;

  /** Calls made to other functions during execution of the method. */
  @PropertyDefinition(validate = "notNull")
  private final List<CallGraph> _calls;

  //-------------------------------------------------------------------------
  /**
   * Provides a pretty-printed version of the call graph as a string.
   *
   * @return a string representation of the call graph, not null
   */
  public String prettyPrint() {
    return prettyPrint(new StringBuilder(), this, "", "").toString();
  }

  private static StringBuilder prettyPrint(StringBuilder builder, CallGraph trace, String indent, String childIndent) {
    builder.append('\n').append(indent).append(trace.toString());
    for (Iterator<CallGraph> itr = trace.getCalls().iterator(); itr.hasNext(); ) {
      CallGraph next = itr.next();
      String newIndent;
      String newChildIndent;
      boolean isFinalChild = !itr.hasNext();
      if (!isFinalChild) {
        newIndent = childIndent + " |--";
        newChildIndent = childIndent + " |  ";
      } else {
        newIndent = childIndent + " `--";
        newChildIndent = childIndent + "    ";
      }
      // these are unicode characters for box drawing
/*
      if (!isFinalChild) {
        newIndent = childIndent + " \u251c\u2500\u2500";
        newChildIndent = childIndent + " \u2502  ";
      } else {
        newIndent = childIndent + " \u2514\u2500\u2500";
        newChildIndent = childIndent + "    ";
      }
*/
      prettyPrint(builder, next, newIndent, newChildIndent);
    }
    return builder;
  }

  @Override
  public String toString() {
    String errorMessage;

    if (_errorMessage == null) {
      errorMessage = "";
    } else {
      errorMessage = " '" + _errorMessage + "'";
    }
    return _receiverClass.getSimpleName() + "." + _methodName + "()" +
        (_throwableClass == null ? " -> " + _returnValue : " threw " + _throwableClass + errorMessage) +
        (_arguments == null ? "" : ", args: " + _arguments);
  }

  @ImmutableConstructor
  /* package */ CallGraph(Class<?> receiverClass,
                          String methodName,
                          List<Class<?>> parameterTypes,
                          List<Object> arguments,
                          Object returnValue,
                          Class<?> throwableClass,
                          String errorMessage,
                          String stackTrace,
                          List<CallGraph> calls) {
    JodaBeanUtils.notNull(receiverClass, "receiverClass");
    JodaBeanUtils.notNull(methodName, "methodName");
    JodaBeanUtils.notNull(parameterTypes, "parameterTypes");
    JodaBeanUtils.notNull(calls, "calls");
    _receiverClass = receiverClass;
    _methodName = methodName;
    _parameterTypes = ImmutableList.copyOf(parameterTypes);
    _arguments = ImmutableList.copyOf(arguments);
    _returnValue = returnValue;
    _throwableClass = throwableClass;
    _errorMessage = errorMessage;
    _stackTrace = stackTrace;
    _calls = ImmutableList.copyOf(calls);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_receiverClass, _methodName, _parameterTypes, _arguments, _returnValue, _throwableClass, _errorMessage, _calls);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CallGraph other = (CallGraph) obj;
    return
        Objects.equals(this._receiverClass, other._receiverClass) &&
        Objects.equals(this._methodName, other._methodName) &&
        Objects.equals(this._parameterTypes, other._parameterTypes) &&
        Objects.equals(this._arguments, other._arguments) &&
        Objects.equals(this._returnValue, other._returnValue) &&
        Objects.equals(this._throwableClass, other._throwableClass) &&
        Objects.equals(this._errorMessage, other._errorMessage) &&
        Objects.equals(this._calls, other._calls);
  }
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CallGraph}.
   * @return the meta-bean, not null
   */
  public static CallGraph.Meta meta() {
    return CallGraph.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CallGraph.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CallGraph.Builder builder() {
    return new CallGraph.Builder();
  }

  @Override
  public CallGraph.Meta metaBean() {
    return CallGraph.Meta.INSTANCE;
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
   * Gets the type of the object implementing the called method.
   * @return the value of the property, not null
   */
  public Class<?> getReceiverClass() {
    return _receiverClass;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name of the method.
   * @return the value of the property, not null
   */
  public String getMethodName() {
    return _methodName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the method's parameter types.
   * @return the value of the property, not null
   */
  public List<Class<?>> getParameterTypes() {
    return _parameterTypes;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the arguments passed to the method call.
   * @return the value of the property, not null
   */
  public List<Object> getArguments() {
    return _arguments;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the return value from the method call.
   * @return the value of the property
   */
  public Object getReturnValue() {
    return _returnValue;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets throwable thrown when the method was called.
   * @return the value of the property
   */
  public Class<?> getThrowableClass() {
    return _throwableClass;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets error message of the throwable thrown when the method was called.
   * @return the value of the property
   */
  public String getErrorMessage() {
    return _errorMessage;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets stack trace of the throwable thrown when the method was called.
   * @return the value of the property
   */
  public String getStackTrace() {
    return _stackTrace;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets calls made to other functions during execution of the method.
   * @return the value of the property, not null
   */
  public List<CallGraph> getCalls() {
    return _calls;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CallGraph}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code receiverClass} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Class<?>> _receiverClass = DirectMetaProperty.ofImmutable(
        this, "receiverClass", CallGraph.class, (Class) Class.class);
    /**
     * The meta-property for the {@code methodName} property.
     */
    private final MetaProperty<String> _methodName = DirectMetaProperty.ofImmutable(
        this, "methodName", CallGraph.class, String.class);
    /**
     * The meta-property for the {@code parameterTypes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Class<?>>> _parameterTypes = DirectMetaProperty.ofImmutable(
        this, "parameterTypes", CallGraph.class, (Class) List.class);
    /**
     * The meta-property for the {@code arguments} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Object>> _arguments = DirectMetaProperty.ofImmutable(
        this, "arguments", CallGraph.class, (Class) List.class);
    /**
     * The meta-property for the {@code returnValue} property.
     */
    private final MetaProperty<Object> _returnValue = DirectMetaProperty.ofImmutable(
        this, "returnValue", CallGraph.class, Object.class);
    /**
     * The meta-property for the {@code throwableClass} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Class<?>> _throwableClass = DirectMetaProperty.ofImmutable(
        this, "throwableClass", CallGraph.class, (Class) Class.class);
    /**
     * The meta-property for the {@code errorMessage} property.
     */
    private final MetaProperty<String> _errorMessage = DirectMetaProperty.ofImmutable(
        this, "errorMessage", CallGraph.class, String.class);
    /**
     * The meta-property for the {@code stackTrace} property.
     */
    private final MetaProperty<String> _stackTrace = DirectMetaProperty.ofImmutable(
        this, "stackTrace", CallGraph.class, String.class);
    /**
     * The meta-property for the {@code calls} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<CallGraph>> _calls = DirectMetaProperty.ofImmutable(
        this, "calls", CallGraph.class, (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "receiverClass",
        "methodName",
        "parameterTypes",
        "arguments",
        "returnValue",
        "throwableClass",
        "errorMessage",
        "stackTrace",
        "calls");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1946768183:  // receiverClass
          return _receiverClass;
        case -723163380:  // methodName
          return _methodName;
        case 1123325520:  // parameterTypes
          return _parameterTypes;
        case -2035517098:  // arguments
          return _arguments;
        case -1495129567:  // returnValue
          return _returnValue;
        case -1147066344:  // throwableClass
          return _throwableClass;
        case 1203236063:  // errorMessage
          return _errorMessage;
        case 2026279837:  // stackTrace
          return _stackTrace;
        case 94425557:  // calls
          return _calls;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CallGraph.Builder builder() {
      return new CallGraph.Builder();
    }

    @Override
    public Class<? extends CallGraph> beanType() {
      return CallGraph.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code receiverClass} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Class<?>> receiverClass() {
      return _receiverClass;
    }

    /**
     * The meta-property for the {@code methodName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> methodName() {
      return _methodName;
    }

    /**
     * The meta-property for the {@code parameterTypes} property.
     * @return the meta-property, not null
     */
    public MetaProperty<List<Class<?>>> parameterTypes() {
      return _parameterTypes;
    }

    /**
     * The meta-property for the {@code arguments} property.
     * @return the meta-property, not null
     */
    public MetaProperty<List<Object>> arguments() {
      return _arguments;
    }

    /**
     * The meta-property for the {@code returnValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Object> returnValue() {
      return _returnValue;
    }

    /**
     * The meta-property for the {@code throwableClass} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Class<?>> throwableClass() {
      return _throwableClass;
    }

    /**
     * The meta-property for the {@code errorMessage} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> errorMessage() {
      return _errorMessage;
    }

    /**
     * The meta-property for the {@code stackTrace} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> stackTrace() {
      return _stackTrace;
    }

    /**
     * The meta-property for the {@code calls} property.
     * @return the meta-property, not null
     */
    public MetaProperty<List<CallGraph>> calls() {
      return _calls;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1946768183:  // receiverClass
          return ((CallGraph) bean).getReceiverClass();
        case -723163380:  // methodName
          return ((CallGraph) bean).getMethodName();
        case 1123325520:  // parameterTypes
          return ((CallGraph) bean).getParameterTypes();
        case -2035517098:  // arguments
          return ((CallGraph) bean).getArguments();
        case -1495129567:  // returnValue
          return ((CallGraph) bean).getReturnValue();
        case -1147066344:  // throwableClass
          return ((CallGraph) bean).getThrowableClass();
        case 1203236063:  // errorMessage
          return ((CallGraph) bean).getErrorMessage();
        case 2026279837:  // stackTrace
          return ((CallGraph) bean).getStackTrace();
        case 94425557:  // calls
          return ((CallGraph) bean).getCalls();
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
   * The bean-builder for {@code CallGraph}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CallGraph> {

    private Class<?> _receiverClass;
    private String _methodName;
    private List<Class<?>> _parameterTypes = new ArrayList<Class<?>>();
    private List<Object> _arguments = new ArrayList<Object>();
    private Object _returnValue;
    private Class<?> _throwableClass;
    private String _errorMessage;
    private String _stackTrace;
    private List<CallGraph> _calls = new ArrayList<CallGraph>();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(CallGraph beanToCopy) {
      this._receiverClass = beanToCopy.getReceiverClass();
      this._methodName = beanToCopy.getMethodName();
      this._parameterTypes = new ArrayList<Class<?>>(beanToCopy.getParameterTypes());
      this._arguments = new ArrayList<Object>(beanToCopy.getArguments());
      this._returnValue = beanToCopy.getReturnValue();
      this._throwableClass = beanToCopy.getThrowableClass();
      this._errorMessage = beanToCopy.getErrorMessage();
      this._stackTrace = beanToCopy.getStackTrace();
      this._calls = new ArrayList<CallGraph>(beanToCopy.getCalls());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1946768183:  // receiverClass
          return _receiverClass;
        case -723163380:  // methodName
          return _methodName;
        case 1123325520:  // parameterTypes
          return _parameterTypes;
        case -2035517098:  // arguments
          return _arguments;
        case -1495129567:  // returnValue
          return _returnValue;
        case -1147066344:  // throwableClass
          return _throwableClass;
        case 1203236063:  // errorMessage
          return _errorMessage;
        case 2026279837:  // stackTrace
          return _stackTrace;
        case 94425557:  // calls
          return _calls;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1946768183:  // receiverClass
          this._receiverClass = (Class<?>) newValue;
          break;
        case -723163380:  // methodName
          this._methodName = (String) newValue;
          break;
        case 1123325520:  // parameterTypes
          this._parameterTypes = (List<Class<?>>) newValue;
          break;
        case -2035517098:  // arguments
          this._arguments = (List<Object>) newValue;
          break;
        case -1495129567:  // returnValue
          this._returnValue = (Object) newValue;
          break;
        case -1147066344:  // throwableClass
          this._throwableClass = (Class<?>) newValue;
          break;
        case 1203236063:  // errorMessage
          this._errorMessage = (String) newValue;
          break;
        case 2026279837:  // stackTrace
          this._stackTrace = (String) newValue;
          break;
        case 94425557:  // calls
          this._calls = (List<CallGraph>) newValue;
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
    public CallGraph build() {
      return new CallGraph(
          _receiverClass,
          _methodName,
          _parameterTypes,
          _arguments,
          _returnValue,
          _throwableClass,
          _errorMessage,
          _stackTrace,
          _calls);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code receiverClass} property in the builder.
     * @param receiverClass  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder receiverClass(Class<?> receiverClass) {
      JodaBeanUtils.notNull(receiverClass, "receiverClass");
      this._receiverClass = receiverClass;
      return this;
    }

    /**
     * Sets the {@code methodName} property in the builder.
     * @param methodName  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder methodName(String methodName) {
      JodaBeanUtils.notNull(methodName, "methodName");
      this._methodName = methodName;
      return this;
    }

    /**
     * Sets the {@code parameterTypes} property in the builder.
     * @param parameterTypes  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder parameterTypes(List<Class<?>> parameterTypes) {
      JodaBeanUtils.notNull(parameterTypes, "parameterTypes");
      this._parameterTypes = parameterTypes;
      return this;
    }

    /**
     * Sets the {@code arguments} property in the builder.
     * @param arguments  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder arguments(List<Object> arguments) {
      JodaBeanUtils.notNull(arguments, "arguments");
      this._arguments = arguments;
      return this;
    }

    /**
     * Sets the {@code returnValue} property in the builder.
     * @param returnValue  the new value
     * @return this, for chaining, not null
     */
    public Builder returnValue(Object returnValue) {
      this._returnValue = returnValue;
      return this;
    }

    /**
     * Sets the {@code throwableClass} property in the builder.
     * @param throwableClass  the new value
     * @return this, for chaining, not null
     */
    public Builder throwableClass(Class<?> throwableClass) {
      this._throwableClass = throwableClass;
      return this;
    }

    /**
     * Sets the {@code errorMessage} property in the builder.
     * @param errorMessage  the new value
     * @return this, for chaining, not null
     */
    public Builder errorMessage(String errorMessage) {
      this._errorMessage = errorMessage;
      return this;
    }

    /**
     * Sets the {@code stackTrace} property in the builder.
     * @param stackTrace  the new value
     * @return this, for chaining, not null
     */
    public Builder stackTrace(String stackTrace) {
      this._stackTrace = stackTrace;
      return this;
    }

    /**
     * Sets the {@code calls} property in the builder.
     * @param calls  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder calls(List<CallGraph> calls) {
      JodaBeanUtils.notNull(calls, "calls");
      this._calls = calls;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("CallGraph.Builder{");
      buf.append("receiverClass").append('=').append(JodaBeanUtils.toString(_receiverClass)).append(',').append(' ');
      buf.append("methodName").append('=').append(JodaBeanUtils.toString(_methodName)).append(',').append(' ');
      buf.append("parameterTypes").append('=').append(JodaBeanUtils.toString(_parameterTypes)).append(',').append(' ');
      buf.append("arguments").append('=').append(JodaBeanUtils.toString(_arguments)).append(',').append(' ');
      buf.append("returnValue").append('=').append(JodaBeanUtils.toString(_returnValue)).append(',').append(' ');
      buf.append("throwableClass").append('=').append(JodaBeanUtils.toString(_throwableClass)).append(',').append(' ');
      buf.append("errorMessage").append('=').append(JodaBeanUtils.toString(_errorMessage)).append(',').append(' ');
      buf.append("stackTrace").append('=').append(JodaBeanUtils.toString(_stackTrace)).append(',').append(' ');
      buf.append("calls").append('=').append(JodaBeanUtils.toString(_calls));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
