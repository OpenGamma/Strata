/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.simpleexec

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class SimpleExec extends DefaultTask
{

    @Input
    Object command

    @Input @Optional
    Map<String, String> environment

    @Input @Optional
    File workingDirectory

    @Input @Optional
    boolean inheritEnvironment = true

    @Input @Optional
    boolean throwOnFailure = true

    ShellExecutor executor
    ShellResult output

    @TaskAction
    void exec()
    {
        output = execWithCommand(command)

        if(output.exitCode != 0 && throwOnFailure)
            throw buildException("running system command", command.toString())
    }

    protected Throwable buildException(String action, String attempt, Throwable cause = null)
    {
        return new GradleException("""\
Error ${action} (${attempt.toString()})
=== STDOUT ===
${output.stdOut}
=== /STDOUT ===
=== STDERR ===
${output.stdErr}
=== /STDERR ===
""", cause)
    }

    protected ShellResult execWithCommand(command)
    {
        if( ! executor)
            executor = new DefaultShellExecutor()

        Map<String, String> effectiveEnvironment = [:]
        File wd = workingDirectory ?: new File(System.properties['user.dir'])

        if(inheritEnvironment)
            effectiveEnvironment.putAll System.getenv()
        if(environment)
            effectiveEnvironment.putAll environment

        return executor.execute(command, effectiveEnvironment, wd)
    }
}
