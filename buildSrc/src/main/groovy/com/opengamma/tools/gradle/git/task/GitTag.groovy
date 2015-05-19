/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.git.task

import com.opengamma.tools.gradle.simpleexec.SimpleExec
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class GitTag extends BaseGitTask
{
    @Input
    String message

    @Input
    String tagName

    @Input
    File repositoryLocation

    @TaskAction
    void doTag()
    {
        Task doTag = project.tasks.create("execGitTag", SimpleExec)
        doTag.configure {
            command "git tag -a -m \"${message}\" ${tagName}"
            workingDirectory repositoryLocation
        }

        doTag.execute()
    }
}
