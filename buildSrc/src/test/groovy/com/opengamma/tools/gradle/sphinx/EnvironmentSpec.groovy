/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx

import com.opengamma.tools.gradle.simpleexec.ShellExecutor
import com.opengamma.tools.gradle.simpleexec.ShellResult
import com.opengamma.tools.gradle.sphinx.error.ApplicationVersionError
import com.opengamma.tools.gradle.sphinx.error.EnvironmentError
import com.opengamma.tools.gradle.sphinx.error.MissingApplicationError
import com.opengamma.tools.gradle.sphinx.error.MissingPythonPackageError
import spock.lang.Specification

class EnvironmentSpec extends Specification
{
	void "Not return error if Python on path and is correct version"()
	{
		setup:
		ShellExecutor executor = Stub(ShellExecutor)
		executor.execute(_, _, _) >> { return new ShellResult(0, null, "Python 2.7") }
		Environment e = new Environment(executor)

		when:
		List<EnvironmentError> errors = e.checkPythonOnPath()

		then:
		errors.empty
	}

	void "Return correct error when Python is missing"()
	{
		setup:
		ShellExecutor executor = Stub(ShellExecutor)
		executor.execute(_, _, _) >> { throw new IOException() }
		Environment e = new Environment(executor)

		when:
		List<EnvironmentError> errors = e.checkPythonOnPath()

		then:
		errors.size() == 1
		errors.first() instanceof MissingApplicationError
	}

	void "Return correct error when Python is wrong version"(String badVersion)
	{
		setup:
		ShellExecutor executor = Stub(ShellExecutor)
		executor.execute(_, _, _) >> { return new ShellResult(0, null, "Python ${badVersion}") }
		Environment e = new Environment(executor)

		when:
		List<EnvironmentError> errors = e.checkPythonOnPath()

		then:
		errors.size() == 1
		errors.first() instanceof ApplicationVersionError
		errors.first().foundVersion == badVersion

		where:
		badVersion << ["1.0", "2.4", "2.5", "3.0", "3.1"]
	}

	void "Return correct error when Pip is not available"()
	{
		setup:
		ShellExecutor executor = Stub(ShellExecutor)
		executor.execute(_, _, _) >> { throw new IOException() }
		Environment e = new Environment(executor)

		when:
		List<EnvironmentError> errors = e.checkPythonPackage("dummy")

		then:
		! errors.empty
		errors.first() instanceof MissingApplicationError
	}

	void "Return correct error when Python package not available"()
	{
		setup:
		ShellExecutor executor = Stub(ShellExecutor)
		executor.execute(_, _, _) >> { new ShellResult(0, "", "") }
		Environment e = new Environment(executor)

		when:
		List<EnvironmentError> errors = e.checkPythonPackage("dummy")

		then:
		errors.size() == 1
		errors.first() instanceof MissingPythonPackageError
		errors.first().packageName == "dummy"
	}

	void "Not return error when Python package is available"()
	{
		setup:
		ShellExecutor executor = Stub(ShellExecutor)
		executor.execute(_, _, _) >> { new ShellResult(0, """\
---
Name: Dummy
Version: 1.0.1
""", "") }
		Environment e = new Environment(executor)

		when:
		List<EnvironmentError> errors = e.checkPythonPackage("dummy")

		then:
		errors.empty
	}

	void "Not return error if Make on path and is correct version"()
	{
		setup:
		ShellExecutor executor = Stub(ShellExecutor)
		executor.execute(_) >> { return new ShellResult(0, "GNU Make 4.0", null) }
		Environment e = new Environment(executor)

		when:
		List<EnvironmentError> errors = e.checkMake()

		then:
		errors.empty
	}

	void "Return correct error when Make is missing"()
	{
		setup:
		ShellExecutor executor = Stub(ShellExecutor)
		executor.execute(_) >> { throw new IOException() }
		Environment e = new Environment(executor)

		when:
		List<EnvironmentError> errors = e.checkMake()

		then:
		errors.size() == 1
		errors.first() instanceof MissingApplicationError
	}

	void "Return correct error when Make is wrong version"()
	{
		setup:
		ShellExecutor executor = Stub(ShellExecutor)
		executor.execute(_) >> { return new ShellResult(0, "Sun CCS Make", null) }
		Environment e = new Environment(executor)

		when:
		List<EnvironmentError> errors = e.checkMake()

		then:
		errors.size() == 1
		errors.first() instanceof ApplicationVersionError
	}

	void "Not have any errors for good environment"()
	{
		setup:
		List<EnvironmentError> errors = []
		SphinxExtension ext = new SphinxExtension()
		ext.requiredPackages = ["pip-one", "pip-two"].toSet()
		ShellExecutor executor = Stub(ShellExecutor)
		executor.execute({it.startsWith("python")}, _, _) >> { return new ShellResult(0, null, "Python 2.7") }
		executor.execute("pip show pip-one", _, _) >> { return new ShellResult(0, "Version: 1.0.0", null) }
		executor.execute("pip show pip-two", _, _) >> { return new ShellResult(0, "Version: 1.0.0", null) }
		executor.execute({it.startsWith("make")}) >> { return new ShellResult(0, "GNU Make", null) }
		Environment e = new Environment(executor)

		when:
		errors.addAll e.checkPythonOnPath()
		ext.requiredPackages.each {
			errors.addAll e.checkPythonPackage(it)
		}
		errors.addAll e.checkMake()

		then:
		errors.empty
	}
}
