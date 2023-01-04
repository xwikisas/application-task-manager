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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.taskmanager.TaskCounter;
import com.xwiki.taskmanager.model.Task;

/**
 * Listener that will create/modify the Task pages associated with the TaskMacros inside a page.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("com.xwiki.taskmanager.internal.TaskMacroUpdateEventListener")
@Singleton
public class TaskMacroUpdateEventListener extends AbstractTaskEventListener
{
    private static final String TASK_PAGE_PREFIX = "Task";

    @Inject
    private Provider<TaskCounter> taskCounter;

    @Inject
    private QueryManager queryManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;


    /**
     * Default constructor.
     */
    public TaskMacroUpdateEventListener()
    {
        super(TaskMacroUpdateEventListener.class.getName(), Arrays.asList(new DocumentUpdatingEvent(),
            new DocumentCreatingEvent()));
    }

    @Override
    protected void processEvent(XWikiDocument document, XWikiContext context)
    {
        XDOM documentContent = document.getXDOM();

        List<Task> tasks = this.taskProcessor.extract(documentContent, taskCounter.get());

        if (tasks.size() > 0) {
            // TaskExtractor will add new IDs to the tasks if they don't have one, so we need to update the content.
            try {
                document.setContent(documentContent);
            } catch (XWikiException e) {
                logger.error("Could not update the content of the document!");
            }
            context.put(TASK_UPDATE_FLAG, true);
            createOrUpdateTaskPage(document, context, tasks);
            context.put(TASK_UPDATE_FLAG, null);
        }

    }

    private void createOrUpdateTaskPage(XWikiDocument document, XWikiContext context, List<Task> tasks)
    {
        for (Task task : tasks) {
            String macroId = task.getId();
            try {
                DocumentReference taskDocRef =
                    getTaskDocumentById(macroId, document.getDocumentReference().getLastSpaceReference(),
                        context.getUserReference());

                XWikiDocument taskDoc = context.getWiki().getDocument(taskDocRef, context).clone();

                BaseObject taskObj = findOrCreateTaskObject(taskDoc, context);

                taskObj.set(Task.OWNER, document.getDocumentReference(), context);

                populateObjectWithMacroParams(context, task, taskObj);

                context.getWiki().saveDocument(taskDoc, "Task updated!", context);
            } catch (QueryException | XWikiException ignored) {
                logger.error("Failed to retrieve the document that contains the Task Object with id [{}].", macroId);
                return;
            }
        }
    }

    private DocumentReference getTaskDocumentById(String macroId, SpaceReference parent, DocumentReference user)
        throws QueryException
    {
        String statement = ", BaseObject as obj, StringProperty as taskId "
            + "where doc.fullName = obj.name and obj.className = 'TaskManager.Code.TaskClass' "
            + "and obj.id=taskId.id.id and taskId.id.name='id' and taskId.value = :taskId";
        List<String> result = queryManager.createQuery(statement, Query.HQL).bindValue("taskId", macroId).execute();
        // Try to retrieve the document that has an object with the given id.
        if (result.size() > 0) {
            return new DocumentReference(resolver.resolve(result.get(0), EntityType.DOCUMENT));
        }
        // If there is no document, try to create one as a child or sibling to the currently edited page.
        if (authorizationManager.hasAccess(Right.EDIT, user, parent)) {
            return new DocumentReference(TASK_PAGE_PREFIX + macroId, parent);
        }
        // Default case: create a task page as a child to the TaskManager homepage.
        return new DocumentReference(TASK_PAGE_PREFIX + macroId,
            new SpaceReference(parent.getWikiReference().getName(), TASK_MANAGER_SPACE));
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

    private BaseObject findOrCreateTaskObject(XWikiDocument document, XWikiContext context) throws XWikiException
    {
        BaseObject object = document.getXObject(TASK_OBJECT_CLASS_REFERENCE);

        if (object == null) {
            object = document.newXObject(TASK_OBJECT_CLASS_REFERENCE, context);
        }
        return object;
    }
}
