/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.taskmanager.internal.macro;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xwiki.taskmanager.TaskException;
import com.xwiki.taskmanager.TaskManager;
import com.xwiki.taskmanager.TaskManagerConfiguration;
import com.xwiki.taskmanager.internal.TaskBlockProcessor;
import com.xwiki.taskmanager.macro.TasksMacroParameters;
import com.xwiki.taskmanager.model.Task;

/**
 * Task macro that will enable the users to assign tasks to each other.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("tasks")
@Singleton
public class TasksMacro extends AbstractMacro<TasksMacroParameters>
{
    @Inject
    private TaskBlockProcessor blockProcessor;

    @Inject
    private TaskManager taskManager;

    @Inject
    private TaskManagerConfiguration configuration;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    private ContextualLocalizationManager localizationManager;

    /**
     * Default constructor.
     */
    public TasksMacro()
    {
        super("Tasks", "Display one or more tasks identified by their IDs.", TasksMacroParameters.class);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public List<Block> execute(TasksMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        String ids = parameters.getIds();

        List<Block> blocks = new ArrayList<>();

        SimpleDateFormat storageFormat = new SimpleDateFormat(configuration.getStorageDateFormat());
        for (String id : ids.split("\\s*,\\s*")) {
            try {
                Task task = taskManager.getTask(Integer.parseInt(id));
                if (!authorizationManager.hasAccess(Right.VIEW, task.getReference())) {
                    throw new TaskException(
                        localizationManager.getTranslationPlain("taskmanager.macro.tasks.noRights", id));
                }
                Map<String, String> taskParams = new HashMap<>();
                taskParams.put(Task.REFERENCE, serializer.serialize(task.getReference()));
                taskParams.put(Task.STATUS, task.getStatus());
                taskParams.put(Task.REPORTER, serializer.serialize(task.getReporter()));
                taskParams.put(Task.CREATE_DATE,
                    task.getCreateDate() != null ? storageFormat.format(task.getCreateDate()) : "");
                taskParams.put(Task.COMPLETE_DATE,
                    task.getCompleteDate() != null ? storageFormat.format(task.getCompleteDate()) : "");

                String taskContent = blockProcessor.renderTaskContent(
                    blockProcessor.generateTaskContentBlocks(
                        task.getAssignee() != null ? serializer.serialize(task.getAssignee()) : null,
                        task.getDueDate(),
                        task.getName(),
                        storageFormat),
                    context.getSyntax()
                );

                blocks.add(new MacroBlock("task", taskParams, taskContent, false));
            } catch (NumberFormatException | TaskException e) {
                blocks.add(
                    new MacroBlock("error", Collections.emptyMap(), e.getMessage(), false));
            }
        }
        return blocks;
    }
}
