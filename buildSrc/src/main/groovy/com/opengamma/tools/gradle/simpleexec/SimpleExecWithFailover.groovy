/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.simpleexec

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class SimpleExecWithFailover extends SimpleExec
{
    @Input @Optional
    Object failoverCommand

    @TaskAction
    void exec()
    {
        output = execWithCommand(command)
        if(output.exitCode == 0)
            return

        Throwable originalFailure = buildException("running original command", command.toString())
        output = execWithCommand(failoverCommand)

        if(output.exitCode != 0 && throwOnFailure)
            throw buildException("running backup command", failoverCommand.toString(), originalFailure)
    }
}
