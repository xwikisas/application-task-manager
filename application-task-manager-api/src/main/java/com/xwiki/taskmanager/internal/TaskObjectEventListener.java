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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.block.XDOM;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.taskmanager.model.Task;

/**
 * Listener that will create/modify/remove the TaskObjects associated with the TaskMacros inside a page. The TaskObjects
 * are created to be used by the Task Report Macro.
 *
 * @version $Id$
 * @since 1.0
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

    @Inject
    private Logger logger;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private TaskExtractor taskExtractor;

    /**
     * Default constructor.
     */
    public TaskObjectEventListener()
    {
        super(TaskObjectEventListener.class.getName(), Arrays.asList(new DocumentUpdatingEvent(),
            new DocumentCreatingEvent()));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        XDOM documentContent = document.getXDOM();

        List<Task> tasks = this.taskExtractor.extract(documentContent);

        List<BaseObject> taskObjects = new ArrayList<>(document.getXObjects(TASK_OBJECT_CLASS_REFERENCE));
        taskObjects.removeAll(Collections.singletonList(null));

        for (Task task : tasks) {
            String macroId = task.getId();

            BaseObject object = null;
            try {
                object = findOrCreateAssociatedObject(taskObjects, macroId, document, context);
            } catch (XWikiException e) {
                logger.warn("Failed to create a new TaskObject for the Task macro with id [{}]", macroId);
                continue;
            }

            populateObjectWithMacroParams(context, task, object);

            taskObjects.remove(object);
        }

        for (BaseObject taskObject : taskObjects) {
            document.removeXObject(taskObject);
        }
    }

    private void populateObjectWithMacroParams(XWikiContext context, Task task, BaseObject object)
    {
        object.set(Task.ID, task.getId(), context);

        object.set(Task.CREATOR, serializer.serialize(task.getCreator()), context);

        object.set(Task.STATUS, task.isCompleted() ? 1 : 0, context);

        object.set(Task.CREATE_DATE, task.getCreateDate(), context);

        object.set(Task.DESCRIPTION, task.getDescription(), context);

        List<String> serializedAssignees = new ArrayList<>(task.getAssignees().size());
        for (DocumentReference assignee : task.getAssignees()) {
            serializedAssignees.add(serializer.serialize(assignee));
        }
        object.set(Task.ASSIGNEES, serializedAssignees, context);

        object.set(Task.DEADLINE, task.getDeadline(), context);

        object.set(Task.COMPLETE_DATE, task.getCompleteDate(), context);
    }

    private BaseObject findOrCreateAssociatedObject(List<BaseObject> taskObjects, String id, XWikiDocument document,
        XWikiContext context) throws XWikiException
    {
        Optional<BaseObject> objectOptional = taskObjects
            .stream()
            .filter(obj -> obj.getStringValue(Task.ID).equals(id))
            .findFirst();
        BaseObject object;

        if (objectOptional.isPresent()) {
            object = objectOptional.get();
        } else {
            object = document.newXObject(TASK_OBJECT_CLASS_REFERENCE, context);
        }
        return object;
    }
}
