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
package com.xwiki.taskmanager.internal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.xwiki.taskmanager.TaskManagerConfiguration;
import com.xwiki.taskmanager.internal.macro.DateMacro;
import com.xwiki.taskmanager.model.Task;

/**
 * Class that will handle the retrieval of tasks from different mediums.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = TaskExtractor.class)
@Singleton
public class TaskExtractor
{
    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private TaskManagerConfiguration configuration;

    @Inject
    private Logger logger;

    @Inject
    @Named("xwiki/2.1")
    private BlockRenderer renderer;

    @Inject
    private MacroContentParser contentParser;

    /**
     * Extracts the existing Tasks from a given XDOM.
     * @param content the XDOM from which one desires to extract or check for the existence of Tasks.
     * @return a list of found Tasks or an empty list if the XDOM didn't contain any valid task. Where a valid task
     * is one that has an id.
     */
    public List<Task> extract(XDOM content)
    {
        List<MacroBlock> macros = content.getBlocks(new MacroBlockMatcher("task"), Block.Axes.DESCENDANT);
        List<Task> tasks = new ArrayList<>();

        for (MacroBlock macro : macros) {
            Map<String, String> macroParams = macro.getParameters();
            String macroId = macroParams.get(Task.ID);

            if (macroId == null) {
                continue;
            }

            Task task = new Task();

            extractBasicProperties(macroParams, macroId, task);

            String macroContent = macro.getContent();

            WikiPrinter printer = new DefaultWikiPrinter(new StringBuffer());
            renderer.render(macro, printer);

            task.setDescription(printer.toString());

            try {
                Syntax syntax =
                    (Syntax) content.getMetaData().getMetaData().getOrDefault(MetaData.SYNTAX, Syntax.XWIKI_2_1);

                XDOM parsedContent = getMacroContentXDOM(macro, macroContent, syntax);

                task.setAssignees(extractAssignedUsers(parsedContent));

                Date deadline = extractDeadlineDate(parsedContent);

                task.setDeadline(deadline);
            } catch (MacroExecutionException e) {
                logger.warn(
                    "Failed to parse the content of the macro with id [{}] and extract the assignee and deadline.",
                    macroId);
                continue;
            }

            tasks.add(task);
        }
        return tasks;
    }

    private void extractBasicProperties(Map<String, String> macroParams, String macroId, Task task)
    {
        task.setId(macroId);

        task.setCreator(resolver.resolve(macroParams.get(Task.CREATOR)));

        task.setCompleted(Boolean.parseBoolean(macroParams.get(Task.STATUS)));

        String strCreateDate = macroParams.getOrDefault(Task.CREATE_DATE, "");
        String strCompletedDate = macroParams.getOrDefault(Task.COMPLETE_DATE, "");

        SimpleDateFormat dateFormat = new SimpleDateFormat(configuration.getStorageDateFormat());

        Date createDate;
        try {
            createDate = dateFormat.parse(strCreateDate);
        } catch (ParseException e) {
            logger.warn("Failed to parse the createDate macro parameter [{}]. Expected format is [{}]",
                strCreateDate, configuration.getStorageDateFormat());
            createDate = new Date();
        }
        task.setCreateDate(createDate);

        if (!"".equals(strCompletedDate)) {
            Date completedDate;
            try {
                completedDate = dateFormat.parse(strCompletedDate);
            } catch (ParseException e) {
                logger.warn("Failed to parse the completeDate macro parameter [{}]. Expected format is [{}]",
                    strCreateDate, configuration.getStorageDateFormat());
                completedDate = new Date();
            }
            task.setCompleteDate(completedDate);
        }
    }

    private XDOM getMacroContentXDOM(MacroBlock macro, String content, Syntax syntax)
        throws MacroExecutionException
    {
        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setCurrentMacroBlock(macro);
        macroContext.setSyntax(syntax);
        return this.contentParser.parse(content, macroContext, true, false);
    }

    private List<DocumentReference> extractAssignedUsers(XDOM taskContent)
    {
        List<DocumentReference> usersAssigned = new ArrayList<>();
        List<MacroBlock> macros = taskContent.getBlocks(new MacroBlockMatcher("mention"), Block.Axes.DESCENDANT);

        for (MacroBlock macro : macros) {
            usersAssigned.add(resolver.resolve(macro.getParameters().get("reference")));
        }
        return usersAssigned;
    }

    private Date extractDeadlineDate(XDOM taskContent)
    {
        Date deadline = new Date();

        MacroBlock macro =
            taskContent.getFirstBlock(new MacroBlockMatcher(DateMacro.MACRO_NAME), Block.Axes.DESCENDANT);

        if (macro == null) {
            return deadline;
        }

        String dateValue = macro.getParameters().get(DateMacro.MACRO_NAME);
        try {
            deadline = new SimpleDateFormat(configuration.getStorageDateFormat()).parse(dateValue);
        } catch (ParseException e) {
            logger.warn("Failed to parse the deadline date [{}] of the Task macro! Expected format is [{}]",
                dateValue, configuration.getStorageDateFormat());
        }
        return deadline;
    }
}
