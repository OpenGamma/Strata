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
    /**
     * The command to execute, including arguments. Unless this is a {@link List} (which will be used as-is}, the command
     * will be the result of calling {@link Object#toString}
     */
    @Input
    Object command

    /**
     * OPTIONAL
     * The Environment to use when executing this command.
     *
     * The default value varies depending on how this task is configured. {@see inheritEnvironment}
     */
    @Input @Optional
    Map<String, String> environment

    /**
     * OPTIONAL
     * The working directory (ie. user.dir) to run this command in. Defaults to the current working directory.
     */
    @Input @Optional
    File workingDirectory

    /**
     * OPTIONAL
     * Whether or not to inherit the outer environment for this task.
     *
     * Defaults to true.
     */
    @Input @Optional
    boolean inheritEnvironment = true

    /**
     * OPTIONAL
     * Whether or not failure to execute this command should result in an exception.
     */
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

    void command(String... bits)
    {
        command = Arrays.asList(bits)
    }

    void command(Object command)
    {
        this.command = command
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
