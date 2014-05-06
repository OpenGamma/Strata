/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.util.test.TestGroup;

/**
 * Tests the ability to create multiple instances of the same class in a function graph with different
 * configuration. Or multiple different implementations of an interface at different points in the graph.
 * In Google Guice (which heavily influenced the design of this code) this is known as
 * the "robot legs problem". This refers to the difficultly of building a robot using Guice and injecting
 * two different foot classes into two instances of the same leg class.
 * <p>
 * The solution is to specify an instance of {@link FunctionModelConfig} as an argument for a parameter
 * that isn't of type {@code FunctionModelConfig} and where the engine would normally create and inject the
 * argument. The engine still creates and injects the argument, but the config is used to override the default
 * config.
 * <p>
 * This is intended to be a workaround for unusual situations, not a common approach to function building.
 * It should normally be possible to design functions to avoid the need to do this.
 */
@Test(groups = TestGroup.UNIT)
public class RobotLegsTest {

  /**
   * Build a robot with two {@link FootImpl} instances, one with side=left and the other with side=right.
   * Tests overriding of function arguments.
   */
  @Test
  public void overrideDefaultArgument() {
    FunctionModelConfig rightConfig = config(arguments(function(FootImpl.class, argument("side", "right"))));
    FunctionModelConfig config = config(implementations(Robot.class, RobotImpl.class,
                                                        Leg.class, LegImpl.class,
                                                        Foot.class, FootImpl.class),
                                        arguments(
                                            function(FootImpl.class,
                                                     argument("side", "left")),
                                            function(RobotImpl.class,
                                                     argument("rightLeg", rightConfig),
                                                     argument("name", "Robbie"))));
    Robot robot = FunctionModel.build(Robot.class, config);
    assertEquals("left", robot.getLeftLeg().getFoot().getSide());
    assertEquals("right", robot.getRightLeg().getFoot().getSide());
    assertTrue(robot.getLeftLeg().getFoot() instanceof FootImpl);
    assertTrue(robot.getRightLeg().getFoot() instanceof FootImpl);
  }

  /**
   * Build a robot using {@link LegImpl} instances containing a {@link LeftFoot} and a {@link RightFoot}.
   * Tests overriding of function implementations.
   */
  @Test
  public void overrideDefaultImplementation() {
    FunctionModelConfig rightConfig = config(implementations(Foot.class, RightFoot.class));
    FunctionModelConfig config = config(implementations(Robot.class, RobotImpl.class,
                                                        Leg.class, LegImpl.class,
                                                        Foot.class, LeftFoot.class),
                                        arguments(
                                            function(RobotImpl.class,
                                                     argument("rightLeg", rightConfig),
                                                     argument("name", "Robbie"))));
    Robot robot = FunctionModel.build(Robot.class, config);
    assertTrue(robot.getLeftLeg().getFoot() instanceof LeftFoot);
    assertTrue(robot.getRightLeg().getFoot() instanceof RightFoot);
  }

  /**
   * Builds multiple robots. the robots are both instances of {@link RobotImpl} but with different names. One of them
   * has a {@link LeftFoot} and a {@link RightFoot}. The other has two instances of {@link FootImpl} called
   * 'left' and right'.
   * <p>
   * Do not try this at home. Any real code that needs to do this should almost certainly be refactored
   * so it doesn't need to any more.
   */
  @Test
  public void nestedOverrides() {
    // config for robotA's right leg
    FunctionModelConfig robotARightConfig = config(arguments(function(FootImpl.class, argument("side", "right"))));
    // config for robotB's right leg
    FunctionModelConfig robotBRightConfig = config(implementations(Foot.class, RightFoot.class));
    // config for robotB. uses LeftFoot as the default foot implementation. RightFoot is specified as an override
    FunctionModelConfig robotBConfig = config(arguments(function(RobotImpl.class,
                                                                argument("name", "RobotB"),
                                                                argument("rightLeg", robotBRightConfig))),
                                             implementations(Foot.class, LeftFoot.class));
    // default config. name=RobotA, foot=FootImpl[side=left]
    FunctionModelConfig config = config(implementations(Robot.class, RobotImpl.class,
                                                        Leg.class, LegImpl.class,
                                                        Foot.class, FootImpl.class),
                                        arguments(
                                            function(FootImpl.class,
                                                     argument("side", "left")),
                                            function(RobotImpl.class,
                                                     // override the config for the subtree below robotA's right leg
                                                     argument("rightLeg", robotARightConfig),
                                                     argument("name", "RobotA")),
                                            function(Robots.class,
                                                     // override the config for the subtree below robot2
                                                     argument("robot2", robotBConfig))));
    Robots robots = FunctionModel.build(Robots.class, config);

    Robot robot1 = robots.getRobot1();
    assertEquals("RobotA", robot1.getName());
    assertEquals("left", robot1.getLeftLeg().getFoot().getSide());
    assertEquals("right", robot1.getRightLeg().getFoot().getSide());
    assertTrue(robot1.getLeftLeg().getFoot() instanceof FootImpl);
    assertTrue(robot1.getRightLeg().getFoot() instanceof FootImpl);

    Robot robot2 = robots.getRobot2();
    assertEquals("RobotB", robot2.getName());
    assertTrue(robot2.getRightLeg().getFoot() instanceof RightFoot);
    assertTrue(robot2.getLeftLeg().getFoot() instanceof LeftFoot);
  }

  public interface Robot {

    String getName();
    Leg getLeftLeg();
    Leg getRightLeg();
  }

  public interface Leg {
    Foot getFoot();
  }

  public interface Foot {
    String getSide();
  }

  public static final class RobotImpl implements Robot {

    private final Leg _leftLeg;
    private final Leg _rightLeg;
    private final String _name;

    public RobotImpl(String name, Leg leftLeg, Leg rightLeg) {
      _leftLeg = leftLeg;
      _rightLeg = rightLeg;
      _name = name;
    }

    @Override
    public String getName() {
      return _name;
    }

    @Override
    public Leg getLeftLeg() {
      return _leftLeg;
    }

    @Override
    public Leg getRightLeg() {
      return _rightLeg;
    }
  }

  public static final class LegImpl implements Leg {

    private final Foot _foot;

    public LegImpl(Foot foot) {
      _foot = foot;
    }

    public Foot getFoot() {
      return _foot;
    }
  }

  /**
   * A foot that can be used on either side.
   */
  public static final class FootImpl implements Foot {

    private final String _side;

    public FootImpl(String side) {
      _side = side;
    }

    @Override
    public String getSide() {
      return _side;
    }
  }

  public static final class LeftFoot implements Foot {

    @Override
    public String getSide() {
      return "left";
    }
  }

  public static final class RightFoot implements Foot {

    @Override
    public String getSide() {
      return "right";
    }
  }

  public static final class Robots {

    private final Robot _robot1;
    private final Robot _robot2;


    public Robots(Robot robot1, Robot robot2) {
      _robot1 = robot1;
      _robot2 = robot2;
    }

    public Robot getRobot1() {
      return _robot1;
    }

    public Robot getRobot2() {
      return _robot2;
    }
  }
}
