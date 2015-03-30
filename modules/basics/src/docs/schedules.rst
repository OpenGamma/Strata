=========
Schedules
=========

Introduction
============

A common problem in finance is the derivation of a schedule of dates.
The schedule is generally used to determine accrued interest and payments.

A classic case is the interest rate swap, which typically accrues and pays interest on a regular basis.
For example, a swap might accrue and pay interest every 3 months.

Building a schedule is conceptually simple, however the devil is in the detail.
How should the end-of-month be handled?
What about following a specific pattern like "the 3rd Wednesday of the month"?
Should dates be adjusted to valid business days?
What happens when trying to divide 22 months into 3 month units?

To manage this complexity, a *schedule builder* is used.


Schedule Builder
================

The Strata-Basics project includes a schedule builder that provides a powerful mechanism to create schedules.
The key class is ``PeriodicSchedule`` which is used to create a ``Schedule``.

The difference between the two is that ``PeriodicSchedule`` describes the schedule in terms
of rules, whereas ``Schedule`` contains the actual expanded schedule periods.

The ``PeriodicSchedule`` class contains the following mandatory items:

* ``startDate``, the start of the first schedule period.
* ``endDate``, the end of the last schedule period.
* ``frequency``, regular periodic frequency to use.
* ``businessDayAdjustment``, the business day adjustment to apply.

The following optional items are also available to cutomize the schedule:

* ``startDateBusinessDayAdjustment``, overrides the business day adjustment to be used for the start date.
* ``endDateBusinessDayAdjustment``, overrides the business day adjustment to be used for the end date.
* ``stubConvention``, convention defining how to handle stubs.
* ``rollConvention``, convention defining how to roll dates.
* ``firstRegularStartDate``, start date of the first regular schedule period, which is the end date of the initial stub.
* ``lastRegularEndDate``, end date of the last regular schedule period, which is the start date of the final stub.

Using a ``PeriodicSchedule`` involves setting the mandatory items and as many optional ones as desired.
For example:

.. code-block:: java

    // example swap using builder
    PeriodicSchedule definition = PeriodicSchedule.builder()
        .startDate(LocalDate.of(2014, 2, 12))
        .endDate(LocalDate.of(2015, 3, 31))
        .businessDayAdjustment(BusinessDayAdjustment.of(
                BusinessDayConventions.MODIFIED_FOLLOWING, GlobalHolidayCalendars.EUTA))
        .frequency(Frequency.P3M)
        .stubConvention(StubConvention.LONG_INITIAL)
        .rollConvention(RollConventions.EOM)
        .build();
    Schedule schedule = definition.createSchedule();

.. note::

    If the ``PeriodicSchedule`` contains an invalid definition, then an exception will be thrown
    when creating the schedule.


Frequency
---------

The schedule is based on a *periodic frequency*.
This determines how many periods there are in a year.

For example, there might be four periods per year, which results in a periodic frequency of "3 months".
(Note that this can also be referred to as "quarterly").

The periodic frequency is represented by the ``Frequency`` class.
Constants are provided for all the common periodic frqeuencies.

As a special case, there is a periodic frequency of 'Term'.
This value means that the schedule is not to be divided.
Instead, the result will be a ``Schedule`` containing a single period matching the "term" of the input.


Stubs
-----

When creating the schedule, each date between the start date and end date is allocated into a period.
Most dates are allocated into a *regular* period, based on the periodic frequency.
Any dates left over are allocated to a *stub*, either at the start or end.

For example, a 15 month swap can be neatly divided into 5 regular periods of 3 months each.

::

          |          15 months between start and end dates            |
    start |-----------|-----------|-----------|-----------|-----------| end
     date | 3 months  | 3 months  | 3 months  | 3 months  | 3 months  | date
          | (regular) | (regular) | (regular) | (regular) | (regular) |

However, a 14 month swap cannot be split evenly into 3 month periods.
Instead, there will be a number of regular periods and a *stub*.

One option is to have an *initial stub* of 2 months, known as *short initial*:

::

          |          14 months between start and end dates            |
    start |-----------|-----------|-----------|-----------|-----------| end
     date | 2 months  | 3 months  | 3 months  | 3 months  | 3 months  | date
          | (STUB)    | (regular) | (regular) | (regular) | (regular) |

Another option is to have an *initial stub* of 5 months, known as *long initial*:

::

          |          14 months between start and end dates            |
    start |-----------|-----------|-----------|-----------|-----------| end
     date |       5 months        | 3 months  | 3 months  | 3 months  | date
          |       (STUB)          | (regular) | (regular) | (regular) |

Another option is to have an *final stub* of 2 months, known as *short final*:

::

          |          14 months between start and end dates            |
    start |-----------|-----------|-----------|-----------|-----------| end
     date | 3 months  | 3 months  | 3 months  | 3 months  | 2 months  | date
          | (regular) | (regular) | (regular) | (regular) | (STUB)    |

Another option is to have an *final stub* of 5 months, known as *long final*:

::

          |          14 months between start and end dates            |
    start |-----------|-----------|-----------|-----------|-----------| end
     date | 3 months  | 3 months  | 3 months  |       5 months        | date
          | (regular) | (regular) | (regular) |       (STUB)          |

Note that it is possible, although rare, to have a stub at the start and end.

The ``PeriodicSchedule`` class allows the stub to be controlled in two main ways.

The most convenient approach is to use a ``StubConvention``.
This has six possible values:

* 'ShortInitial'
* 'LongInitial'
* 'ShortFinal'
* 'LongFinal'
* 'None'
* 'Both'

The initial and final values correspond to the descriptions and diagrams above
and fully define the stub required.
The 'None' value specifies that there must be no stub, with an exception thrown if a stub is needed.
The 'Both' value specifies that there are two stubs, for which dates must be specified.

The second approach to defining stubs is to specify one or two additional dates.
The ``firstRegularStartDate`` is the start date of the first regular schedule period.
This is also the end date of the initial stub.
The ``lastRegularEndDate`` is the end date of the last regular schedule period.
This is also the start date of the final stub.

If neither the stub convention nor stub dates are set, then the stub convention is implicitly
set to be 'None', and no stubs are allowed.


Roll Convention
---------------

When building the schedule, there is a standard approach to creating the regular periods.
In most cases, the schedule is based on a whole number of months - 1, 2, 3, 4, 6 or 12.
In this case, the standard approach is to move to the same day-of-month.
For example, if the first date is the 15th January, then adding 3 months will result in the 15th April.

Where the periodic frequency is week-based, the standrad approach is to move to the same day-of-week.
For example, if the first date is a Tuesday, then adding 2 weeks will result in a date on Tuesday 2 weeks later.

A ``RollConvention`` can be specified to override the standard approach.

The most common override is to specify 'EOM', which causes the dates to be at the end-of-month
if the first date is at the end-of-month.
For example, if the first date is the 30th June, then adding 1 month will result in the 31st July.

The second most common override is to specify 'IMM', which causes the dates to be on the 3rd Wednesday of each month.
Other similar roll conventions exist for related rolling rules.


Date Adjustments
----------------

The schedule is always built initially using *unadjusted dates*.
This means that date addition is performed ignoring holidays and weekends.

When the schedule of unadjusted dates is fully determined, :doc:`date adjustments <date-adjustments>`
are applied to convert each schedule date to valid business day.

The ``businessDayAdjustment``, ``startDateBusinessDayAdjustment`` and ``endDateBusinessDayAdjustment``
properties of the ``PeriodicSchedule`` builder are used to control the adjustment.


Schedule
========

The resulting ``Schedule`` class contains three items:

* ``periods``, the list of schedule periods, each with a start and end date
* ``frequency``, the frequency used to create the schedule
* ``rollConvention``, the  roll convention used to create the schedule

The ``Schedule`` can also be used as the ``ScheduleInfo`` needed by a :doc:`day count <day-counts>`.

Methods are provided to merge and split the periods of the schedule.
For example, if a schedule is created with a 3 month regular periodic frequency, it is possible to
later split a period into a 1 month periodic frequency, or combine periods to make a 6 month periodic frequency.


