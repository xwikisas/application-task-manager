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

import java.util.Collections;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
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
    /**
     * Constructor.
     */
    public TaskObjectUpdateEventListener()
    {
        super(TaskObjectUpdateEventListener.class.getName(), Collections.singletonList(new DocumentUpdatedEvent()));
    }

    @Override
    protected void processEvent(XWikiDocument document, XWikiContext context)
    {
        if (context.get(TASK_UPDATE_FLAG) != null) {
            return;
        }

        BaseObject taskObj = document.getXObject(TASK_OBJECT_CLASS_REFERENCE);

        if (taskObj == null) {
            return;
        }

        String taskOwner = taskObj.getStringValue(Task.OWNER);

        if (taskOwner == null || taskOwner.isEmpty()) {
            return;
        }

        DocumentReference taskOwnerRef = new DocumentReference(resolver.resolve(taskOwner, EntityType.DOCUMENT));

        try {
            taskProcessor.updateTask(taskOwnerRef, taskObj, context);
        } catch (XWikiException e) {
            logger.error("Failed to process the owner document of the task [{}].", taskOwnerRef);
        }
    }
}
