/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Provider;

import com.opengamma.core.link.Link;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * A node in the function model.
 */
public abstract class Node {

  /**
   * The expected type of the object created by this node, not null.
   */
  private final Class<?> _type;
  /**
   * The parameter this node satisfies, null if it's the root node.
   */
  private final Parameter _parameter;

  /**
   * Creates an instance.
   * 
   * @param type  the expected type of the object created by this node, not null
   * @param parameter  the parameter this node satisfies, null if it's the root node
   */
  protected Node(Class<?> type, Parameter parameter) {
    _type = ArgumentChecker.notNull(type, "type");
    _parameter = parameter;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the expected type of the object created by this node.
   * 
   * @return the expected type, not null
   */
  public Class<?> getType() {
    return _type;
  }

  /**
   * Gets the parameter that this node satisfies.
   * 
   * @return the parameter, not null
   */
  public Parameter getParameter() {
    return _parameter;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the object represented by this node.
   * <p>
   * Implementations should override {@link #doCreate}, not this method.
   * 
   * @param componentMap  the map of infrastructure components, not null
   * @param dependencies  the dependencies of this node, not null
   * @return the object represented by this node, may be null
   */
  public Object create(ComponentMap componentMap, List<Object> dependencies) {
    Object object = doCreate(componentMap, dependencies);
    if (object instanceof Provider) {
      // TODO some slightly more robust checking of compatibility of types
      // TODO what's the logic I actually need here?
      return Provider.class.isAssignableFrom(_type) ? object : ((Provider<?>) object).get();
    } else if (object instanceof Link) {
      // Might we want to supply a Link as an argument?
      return ((Link<?, ?>) object).resolve();
    } else {
      return object;
    }
  }

  /**
   * Returns the object represented by this node, creating if necessary.
   * <p>
   * If this node's object is a {@link Provider} this method should return it,
   * not the results of calling {@link Provider#get()}. This class will use the
   * expected type to decide whether to call {@code get()} or
   * to inject the provider instance directly.
   * 
   * @param componentMap  the map of infrastructure components, not null
   * @param dependencies  the dependencies of this node, not null
   * @return the object represented by this node, may be null
   */
  protected abstract Object doCreate(ComponentMap componentMap, List<Object> dependencies);

  //-------------------------------------------------------------------------
  /**
   * Gets the dependencies of this node.
   * 
   * @return the dependencies, not null
   */
  public List<Node> getDependencies() {
    return Collections.emptyList();
  }

  /**
   * Gets the complete set of exceptions in the tree of this node.
   * 
   * @return the list of exceptions, not null
   */
  /* package */ List<AbstractGraphBuildException> getExceptions() {
    return getExceptions(this, new ArrayList<AbstractGraphBuildException>());
  }

  private static List<AbstractGraphBuildException> getExceptions(Node node, List<AbstractGraphBuildException> accumulator) {
    if (node instanceof ExceptionNode) {
      AbstractGraphBuildException exception = ((ExceptionNode) node).getException();
      accumulator.add(exception);
      return accumulator;
    } else {
      for (Node childNode : node.getDependencies()) {
        getExceptions(childNode, accumulator);
      }
      return accumulator;
    }
  }

  /**
   * Checks if this node represents a valid object that can be constructed.
   * <p>
   * A true result implies that this node and all nodes below it in the dependency tree are valid.
   * 
   * @return true if this node and all its dependencies are valid
   */
  public boolean isValid() {
    return true;
  }

  /**
   * Checks if this node is an error node.
   * 
   * @return true if this node represents an object that is the source of an error
   */
  public boolean isError() {
    return false;
  }

  //-------------------------------------------------------------------------
  /**
   * Pretty prints this node.
   * <p>
   * Implementations should override this to perform the pretty print.
   * 
   * @return the node structure, not null
   */
  public String prettyPrint() {
    return toString();
  }

  /**
   * Provides the name of the parameter being satisfied ready for pretty printing.
   * 
   * @return the parameter name, not null
   */
  protected final String getPrettyPrintParameterName() {
    if (getParameter() == null) {
      return "";
    } else {
      return getParameter().getName() + ": ";
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Node other = (Node) obj;
    return Objects.equals(this._type, other._type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_type);
  }

}
