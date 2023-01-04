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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomStringUtils;
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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.taskmanager.TaskCounter;
import com.xwiki.taskmanager.TaskManagerConfiguration;
import com.xwiki.taskmanager.internal.macro.DateMacro;
import com.xwiki.taskmanager.model.Task;

/**
 * Class that will handle the retrieval of tasks from different mediums.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = TaskProcessor.class)
@Singleton
public class TaskProcessor
{
    private static final String MENTION_MACRO_ID = "mention";

    private static final String MENTION_REFERENCE_PARAMETER = "reference";

    private static final String DATE_MACRO_ID = "date";

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
     *
     * @param content the XDOM from which one desires to extract or check for the existence of Tasks.
     * @param taskCounter a counter used to assign an ID to the macros that do not have one assigned.
     * @return a list of found Tasks or an empty list if the XDOM didn't contain any valid task. Where a valid task is
     *     one that has an id.
     */
    public List<Task> extract(XDOM content, TaskCounter taskCounter)
    {
        List<MacroBlock> macros = content.getBlocks(new MacroBlockMatcher(Task.NAME), Block.Axes.DESCENDANT);
        List<Task> tasks = new ArrayList<>();

        for (MacroBlock macro : macros) {
            Map<String, String> macroParams = macro.getParameters();
            String macroId = macroParams.get(Task.ID);

            if (macroId == null) {
                macroId = String.valueOf(taskCounter.getAndIncrement());
                macro.setParameter(Task.ID, macroId);
            }

            Task task = new Task();

            extractBasicProperties(macroParams, macroId, task);

            String macroContent = macro.getContent();

            // Store the macro as wiki syntax inside the task description, so we can display the rendered macro
            // inside the live table.
            WikiPrinter printer = new DefaultWikiPrinter(new StringBuffer());
            renderer.render(macro, printer);

            task.setDescription(printer.toString());

            try {

                XDOM parsedContent = getMacroContentXDOM(macro, macroContent, content);

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

    /**
     * Parse the content of a document and sync the task macro with a given task object.
     *
     * @param documentReference the reference to the document that contains the task macro that needs updating.
     * @param taskObject the task object that will be used to update task macro.
     * @param context the current context.
     */
    public void updateTask(DocumentReference documentReference, BaseObject taskObject, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument ownerDocument = context.getWiki().getDocument(documentReference, context).clone();
        String taskId = taskObject.getStringValue(Task.ID);
        XDOM content = ownerDocument.getXDOM();
        List<MacroBlock> macros = content.getBlocks(new MacroBlockMatcher(Task.NAME), Block.Axes.DESCENDANT);
        SimpleDateFormat storageFormat = new SimpleDateFormat(configuration.getStorageDateFormat());
        for (MacroBlock macro : macros) {
            if (macro.getParameters().getOrDefault(Task.ID, "").equals(taskId)) {

                setBasicMacroParameters(taskObject, storageFormat, macro);

                try {
                    List<Block> siblings = macro.getParent().getChildren();
                    int macroIndex = siblings.indexOf(macro);
                    siblings.remove(macroIndex);
                    XDOM macroXDOM = getMacroContentXDOM(macro, macro.getContent(), content);

                    List<String> assignees = new ArrayList<>(
                        Arrays.asList(taskObject.getLargeStringValue(Task.ASSIGNEES).split("\\s*,\\s*")));

                    List<Block> macroChildren = macroXDOM.getChildren();
                    List<MacroBlock> mentionMacros = macroXDOM.getBlocks(new MacroBlockMatcher(MENTION_MACRO_ID),
                        Block.Axes.DESCENDANT);
                    List<MacroBlock> dateMacros = macroXDOM.getBlocks(new MacroBlockMatcher(DATE_MACRO_ID),
                        Block.Axes.DESCENDANT);

                    updateMentionMacros(assignees, mentionMacros);
                    updateDateMacros(taskObject, storageFormat, dateMacros);

                    addNewMentions(assignees, macroChildren);

                    WikiPrinter printer = new DefaultWikiPrinter(new StringBuffer());
                    renderer.render(macroChildren.get(0), printer);

                    MacroBlock newMacroBlock =
                        new MacroBlock(macro.getId(), macro.getParameters(), printer.toString(), macro.isInline());
                    siblings.add(macroIndex, newMacroBlock);
                } catch (MacroExecutionException e) {

                }
                break;
            }
        }
        ownerDocument.setContent(content);
        context.getWiki().saveDocument(ownerDocument, String.format("Task [%s] has been updated!", taskId), context);
    }

    private void addNewMentions(List<String> assignees, List<Block> macroChildren)
    {
        for (String assignee : assignees) {
            Map<String, String> params = new HashMap<>();
            params.put("style", "FULL_NAME");
            params.put(MENTION_REFERENCE_PARAMETER, assignee);
            params.put("anchor", assignee.replace('.', '-') + RandomStringUtils.random(5, true, false));
            MacroBlock macroBlock = new MacroBlock(MENTION_MACRO_ID, params, true);
            macroChildren.get(0).addChild(macroBlock);
        }
    }

    private void updateMentionMacros(List<String> assignees, List<MacroBlock> mentionMacros)
    {
        for (MacroBlock mentionMacro : mentionMacros) {
            if (!assignees.contains(mentionMacro.getParameter(
                MENTION_REFERENCE_PARAMETER)))
            {
                mentionMacro.getParent().getChildren().remove(mentionMacro);
            } else {
                assignees.remove(mentionMacro.getParameter(MENTION_REFERENCE_PARAMETER));
            }
        }
    }

    private void updateDateMacros(BaseObject taskObject, SimpleDateFormat storageFormat, List<MacroBlock> dateMacros)
    {
        for (int i = 0; i < dateMacros.size(); i++) {
            MacroBlock macroBlock = dateMacros.get(i);
            if (i > 0) {
                macroBlock.getParent().getChildren().remove(macroBlock);
            } else {
                macroBlock.setParameter(DATE_MACRO_ID,
                    storageFormat.format(taskObject.getDateValue(Task.DEADLINE)));
            }
        }
    }

    private void setBasicMacroParameters(BaseObject taskObject, SimpleDateFormat storageFormat, MacroBlock macro)
    {
        Date completeDate = taskObject.getDateValue(Task.COMPLETE_DATE);
        if (completeDate != null) {
            macro.setParameter(Task.COMPLETE_DATE,
                storageFormat.format(taskObject.getDateValue(Task.COMPLETE_DATE)));
        }
        macro.setParameter(Task.CREATE_DATE, storageFormat.format(taskObject.getDateValue(Task.CREATE_DATE)));
        macro.setParameter(Task.STATUS, taskObject.getStringValue(Task.STATUS));
        macro.setParameter(Task.CREATOR, taskObject.getStringValue(Task.CREATOR));
    }

    private void extractBasicProperties(Map<String, String> macroParams, String macroId, Task task)
    {
        task.setId(macroId);

        task.setCreator(resolver.resolve(macroParams.get(Task.CREATOR)));

        task.setCompleted(
            "true".equalsIgnoreCase(macroParams.get(Task.STATUS)) || "1".equals(macroParams.get(Task.STATUS)));

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

    private XDOM getMacroContentXDOM(MacroBlock macro, String content, XDOM parent)
        throws MacroExecutionException
    {
        Syntax syntax =
            (Syntax) parent.getMetaData().getMetaData().getOrDefault(MetaData.SYNTAX, Syntax.XWIKI_2_1);
        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setCurrentMacroBlock(macro);
        macroContext.setSyntax(syntax);
        return this.contentParser.parse(content, macroContext, true, false);
    }

    private List<DocumentReference> extractAssignedUsers(XDOM taskContent)
    {
        List<DocumentReference> usersAssigned = new ArrayList<>();
        List<MacroBlock> macros = taskContent.getBlocks(new MacroBlockMatcher(MENTION_MACRO_ID), Block.Axes.DESCENDANT);

        for (MacroBlock macro : macros) {
            usersAssigned.add(resolver.resolve(macro.getParameters().get(MENTION_REFERENCE_PARAMETER)));
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
