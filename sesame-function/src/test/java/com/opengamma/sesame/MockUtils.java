package com.opengamma.sesame;

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Lists;
import com.google.common.collect.MutableClassToInstanceMap;
import com.opengamma.sesame.engine.ComponentMap;

/**
 * Utils for mocking. Creates mock maps useful for insertion in 
 * {@link ComponentMap}. Note, strict features are not threadsafe.
 */
public final class MockUtils {

  private MockUtils() {}
  
  
  /**
   * Returns a map of mock instances, indexed by class.
   * @param classes the classes to use to mock
   * @return a map of class to mock
   */
  public static MutableClassToInstanceMap<Object> mocks(Class<?>...classes ) {
    return doMock(Arrays.asList(classes), false);
  }
  
  
  /**
   * Returns a map of mock instances, indexed by class.
   * @param classes the classes to use to mock
   * @return a map of class to mock
   */
  public static MutableClassToInstanceMap<Object> mocks(List<Class<?>> classes ) {
    return doMock(classes, false);
  }

  /**
   * Returns a map of strict mock instances, indexed by class.
   * @param classes the classes to use to mock
   * @return a map of class to mock
   */
  public static MutableClassToInstanceMap<Object> strictMocks(List<Class<?>> classes ) {
    return doMock(classes, true);
  }

  
  /**
   * Returns a map of strict mock instances, indexed by class.
   * @param classes the classes to use to mock
   * @return a map of class to mock
   */
  public static MutableClassToInstanceMap<Object> strictMocks(Class<?>... classes) {
    return doMock(Arrays.asList(classes), true);
  }
  
  private static MutableClassToInstanceMap<Object> doMock(List<Class<?>> classes, boolean isStrict) {
    
    MutableClassToInstanceMap<Object> mocks = MutableClassToInstanceMap.create();
    for (Class<?> clazz : classes) {
      if (isStrict) {
        mocks.put(clazz, strictMock(clazz));
      } else {
        mocks.put(clazz, mock(clazz));
      }
    }
    
    return mocks;
  }
  
  
  
  
  /**
   * Creates a 'strict' mock, i.e. a mock which throws if
   * called with an un-mocked method.
   * @param clazz the class to mock
   * @return the mock
   */
  public static <T> T strictMock(Class<T> clazz) {
    return mock(clazz, new FailAnswer());
  }
  
  
  private static final List<FailAnswer> _allAnswers = Lists.newArrayList();
  
  /**
   * Enable strict mode in all registered mocks.
   */
  public static void enableStrict() {
    for (FailAnswer answer : _allAnswers) {
      answer._strictEnabled = true;
    }
  }
  
  private static class FailAnswer implements Answer<Void> {
    
    private boolean _strictEnabled = false;
    
    private FailAnswer(){
      _allAnswers.add(this);
    }
    
    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
      if (_strictEnabled) {
        throw new IllegalStateException(invocation.getMethod() + " not implemented for args " + Arrays.toString(invocation.getArguments()));
      } else {
        return null;
      }
    }
  }

  
}
