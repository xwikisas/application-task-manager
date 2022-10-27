package com.xwiki.taskmanager.internal;

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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MacroBlockMatcher;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener that will create/modify/remove the TaskObjects associated with the TaskMacros inside a page. The TaskObjects
 * are created to be used by the Task Report Macro.
 *
 * @version $Id$
 */
@Component
@Named("com.xwiki.taskmanager.internal.TaskObjectEventListener")
@Singleton
public class TaskObjectEventListener extends AbstractEventListener
{
    /**
     * The reference to the TaskClass page.
     */
    public static final LocalDocumentReference TASK_OBJECT_CLASS_REFERENCE =
        new LocalDocumentReference(Arrays.asList("TaskManager", "Code"), "TaskClass");

    private static final String ID = "id";

    private static final String CREATOR = "creator";

    private static final String STATUS = "status";

    private static final String CREATE_DATE = "createDate";

    private static final DateFormat CREATE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private static final DateFormat DEADLINE_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd hh:mm");

    private static final Pattern MENTION_PATTERN = Pattern.compile("\\{\\{mention[^/]*reference=\"([^\"]+)\"[^/]*/}}",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern DATE_PATTERN =
        Pattern.compile("\\{\\{date[^/]*date=\"([^\"]+)\"[^/]*/}}", Pattern.CASE_INSENSITIVE);

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public TaskObjectEventListener()
    {
        super(TaskObjectEventListener.class.getName(), Collections.singletonList(new DocumentUpdatingEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        XDOM documentContent = document.getXDOM();
        List<MacroBlock> macros = documentContent.getBlocks(new MacroBlockMatcher("task"), Block.Axes.DESCENDANT);
        document.obje

        List<BaseObject> taskObjects = new ArrayList<>(document.getXObjects(TASK_OBJECT_CLASS_REFERENCE));
        taskObjects.removeAll(Collections.singletonList(null));

        for (MacroBlock macro : macros) {
            Map<String, String> macroParams = macro.getParameters();
            String macroId = macroParams.get(ID);

            if (macroId == null) {
                continue;
            }

            BaseObject object = null;
            try {
                object = findOrCreateAssociatedObject(taskObjects, macroId, document, context);
            } catch (XWikiException e) {
                logger.warn("Failed to create a new TaskObject for the Task macro with id [{}]", macroId);
                continue;
            }

            populateObjectWithMacroParams(context, macroParams, object);

            String content = macro.getContent();

            StringBuilder description = new StringBuilder();
            List<DocumentReference> usersAssigned = extractAssignedUsers(content, description);

            object.set("assignee", usersAssigned, context);

            content = description.toString();
            description.setLength(0);

            Date deadline = extractDeadlineDate(content, description);

            object.set("deadline", deadline, context);

            object.set("description", description.toString().trim(), context);

            taskObjects.remove(object);
        }

        for (BaseObject taskObject : taskObjects) {
            document.removeXObject(taskObject);
        }
    }

    private void populateObjectWithMacroParams(XWikiContext context, Map<String, String> macroParams, BaseObject object)
    {
        object.set(ID, macroParams.get(ID), context);

        object.set(CREATOR, resolver.resolve(macroParams.get(CREATOR)), context);

        object.set(STATUS, macroParams.getOrDefault(STATUS, "onGoing"), context);

        String strCreateDate = macroParams.getOrDefault(CREATE_DATE, "");
        Date createDate;
        try {
            createDate = CREATE_DATE_FORMAT.parse(strCreateDate);
        } catch (ParseException e) {
            logger.warn("Failed to parse the createDate macro parameter [{}]. Expected format is [{}]",
                strCreateDate, CREATE_DATE_FORMAT);
            createDate = new Date();
        }
        object.set(CREATE_DATE, createDate, context);
    }

    private BaseObject findOrCreateAssociatedObject(List<BaseObject> taskObjects, String id, XWikiDocument document,
        XWikiContext context) throws XWikiException
    {
        Optional<BaseObject> objectOptional = taskObjects
            .stream()
            .filter(obj -> obj.getStringValue(ID).equals(id))
            .findFirst();
        BaseObject object;

        if (objectOptional.isPresent()) {
            object = objectOptional.get();
        } else {
            object = document.newXObject(TASK_OBJECT_CLASS_REFERENCE, context);
        }
        return object;
    }

    private Date extractDeadlineDate(String description, StringBuilder newContent)
    {
        Matcher matcher = DATE_PATTERN.matcher(description);

        Date deadline = new Date();
        int startIndex = 0;
        if (matcher.find()) {
            try {
                deadline = DEADLINE_DATE_FORMAT.parse(matcher.group(1));
            } catch (ParseException e) {
                logger.warn("Failed to parse the deadline date [{}] of the Task macro! Expected format is [{}]",
                    matcher.group(1), DEADLINE_DATE_FORMAT);
            }
            newContent.append(description, startIndex, matcher.start());
            startIndex = matcher.end();
        }

        while (matcher.find()) {
            newContent.append(description, startIndex, matcher.start());
            startIndex = matcher.end();
        }
        newContent.append(description, startIndex, description.length());
        return deadline;
    }

    private List<DocumentReference> extractAssignedUsers(String description, StringBuilder newContent)
    {
        Matcher matcher = MENTION_PATTERN.matcher(description);

        List<DocumentReference> usersAssigned = new ArrayList<>();

        int startIndex = 0;
        while (matcher.find()) {
            newContent.append(description, startIndex, matcher.start());
            startIndex = matcher.end();
            usersAssigned.add(resolver.resolve(matcher.group(1)));
        }
        newContent.append(description, startIndex, description.length());
        return usersAssigned;
    }
}
