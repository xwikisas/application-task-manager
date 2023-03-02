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

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.taskmanager.TaskCounter;
import com.xwiki.taskmanager.TaskException;
import com.xwiki.taskmanager.model.Task;

/**
 * Listener that will modify the task macro associated with the updated task object.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("TaskObjectUpdateEventListener")
@Singleton
public class TaskObjectUpdateEventListener extends AbstractTaskEventListener
{
    @Inject
    private TaskCounter taskCounter;

    /**
     * Constructor.
     */
    public TaskObjectUpdateEventListener()
    {
        super(TaskObjectUpdateEventListener.class.getName(), Arrays.asList(new DocumentUpdatingEvent(),
            new DocumentCreatingEvent(), new DocumentDeletingEvent()));
    }

    @Override
    protected void processEvent(XWikiDocument document, XWikiContext context, Event event)
    {
        if (event instanceof DocumentDeletingEvent) {
            try {
                XWikiDocument actualDoc = context.getWiki().getDocument(document.getDocumentReference(), context);
                BaseObject object = actualDoc.getXObject(TASK_CLASS_REFERENCE);
                if (object != null && !object.getStringValue(Task.OWNER).isEmpty()) {
                    taskXDOMProcessor.removeTaskMacroCall(document.getDocumentReference(),
                        resolver.resolve(object.getStringValue(Task.OWNER), document.getDocumentReference()), context);
                }
            } catch (XWikiException e) {
                logger.warn("Failed to remove the macro call from the owner document of the task [{}]",
                    document.getDocumentReference());
            }
            return;
        }
        BaseObject taskObj = document.getXObject(TASK_CLASS_REFERENCE);

        if (taskObj == null) {
            return;
        }

        maybeSetTaskNumber(context, taskObj);

        if (context.get(TASK_UPDATE_FLAG) != null || taskObj.getStringValue(Task.OWNER).isEmpty()) {
            return;
        }

        String taskOwner = taskObj.getStringValue(Task.OWNER);

        DocumentReference taskOwnerRef = resolver.resolve(taskOwner, document.getDocumentReference());

        try {
            context.put(TASK_UPDATE_FLAG, true);
            if (!taskOwner.isEmpty()) {
                taskXDOMProcessor.updateTaskMacroCall(taskOwnerRef, taskObj, context);
            }
            context.put(TASK_UPDATE_FLAG, null);
        } catch (XWikiException e) {
            logger.error("Failed to process the owner document of the task [{}].", taskOwnerRef);
        }
    }

    private void maybeSetTaskNumber(XWikiContext context, BaseObject taskObj)
    {
        if (taskObj.getIntValue(Task.NUMBER, -1) == -1) {
            try {
                taskObj.set(Task.NUMBER, taskCounter.getNextNumber(), context);
            } catch (TaskException e) {
                logger.warn("Failed to set a number to the task [{}]. Reason: [{}].", taskObj.getDocumentReference(),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }
}
