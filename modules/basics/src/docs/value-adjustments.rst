=================
Value Adjustments
=================

Introduction
============

Once a :doc:`schedule <schedules>` of dates has been determined, a key task is associating a value with the schedule.
For example, a fixed interest rate may increase by 0.1% every 6 months.

To manage a value that can change over time with the schedule, a *value schedule* is used.


Value Schedule
==============

The Strata-Basics project includes a value schedule handles values that change over time.
The key class is ``ValueSchedule``.

The ``ValueSchedule`` class contains two items:

* ``initialValue``, the initial value.
* ``steps``, the list of steps indicating how the value changes.

While the ``ValueSchedule`` class is not directly linked to a ``Schedule``, it is closely related.
A convenient method is provided to allow the value schedule to be resovled against an actual schedule.

Value Step
----------

A value step consists of two parts:

* when the step occurs
* what the step change is

The "when" part can be specified using an exact date, or by specifying the index of the change within the schedule.

The "what" part is defined using a ``ValueAdjustment``.
This provides four different ways to express the change in value that occurs:

* 'Absolute', defining the new absolute value that is to be used from the value step date
* 'DeltaAmount', defines an amount to be added to the previous amount
* 'DeltaMultiplier', defines a multiplication factor applied and then added to the previous amount
* 'Multiplier', defines a multiplication factor applied to the previous amount

For example, a value schedule might be as follows:

* initial value = 200
* first value step, delta amount of +20, new value = ``200 + 20`` = 220
* second value step, delta multiplier of +0.1 (+10%), new value = ``220 + (220 * 0.1)`` = 242

In combination, the schedule builder and value schedules allow many complex financial instrument
calculations to be abstracted.

