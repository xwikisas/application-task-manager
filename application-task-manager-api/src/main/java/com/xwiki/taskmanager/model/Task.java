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
package com.xwiki.taskmanager.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * The model of a Task.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class Task
{
    /**
     * The name of the model.
     */
    public static final String NAME = "task";

    /**
     * The name of the OWNER field.
     */
    public static final String OWNER = "owner";
    /**
     * The name of the ID field.
     */
    public static final String ID = "id";

    /**
     * The name of the STATUS field.
     */
    public static final String STATUS = "completed";

    /**
     * The name of the CREATOR field.
     */
    public static final String CREATOR = "creator";

    /**
     * The name of the ASSIGNEES field.
     */
    public static final String ASSIGNEES = "assignees";

    /**
     * The name of the CREATE_DATE field.
     */
    public static final String CREATE_DATE = "createDate";

    /**
     * The name of the DEADLINE field.
     */
    public static final String DEADLINE = "deadline";

    /**
     * The name of the COMPLETE_DATE field.
     */
    public static final String COMPLETE_DATE = "completeDate";

    /**
     * The name of the DESCRIPTION field.
     */
    public static final String DESCRIPTION = "description";

    private DocumentReference owner;

    private String id;

    private boolean completed;

    private DocumentReference creator;

    private List<DocumentReference> assignees;

    private Date createDate;

    private Date deadline;

    private Date completeDate;

    private String description;

    /**
     * @return the reference of the document where this task resides.
     */
    public DocumentReference getOwner()
    {
        return owner;
    }

    /**
     * @param owner the reference of the document that contains this task.
     */
    public void setOwner(DocumentReference owner)
    {
        this.owner = owner;
    }

    /**
     * @return a unique identifier for the task within the document.
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id the Id of the task. Grouping the tasks by the document reference, their Ids have to be unique.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return whether the task has been completed or not.
     */
    public boolean isCompleted()
    {
        return completed;
    }

    /**
     * @param completed the current state of the task - true: the task is completed; false: the task is not done.
     */
    public void setCompleted(boolean completed)
    {
        this.completed = completed;
    }

    /**
     * @return the reference to the user that created this task.
     */
    public DocumentReference getCreator()
    {
        return creator;
    }

    /**
     * @param creator the reference to the user that created this task.
     */
    public void setCreator(DocumentReference creator)
    {
        this.creator = creator;
    }

    /**
     * @return a list of references to the users that are assigned to this task.
     */
    public List<DocumentReference> getAssignees()
    {
        if (assignees == null) {
            return new ArrayList<>();
        }
        return assignees;
    }

    /**
     * @param assignees a list of references to the users that are assigned to this task.
     */
    public void setAssignees(List<DocumentReference> assignees)
    {
        this.assignees = assignees;
    }

    /**
     * @return the timestamp for the creation of the task.
     */
    public Date getCreateDate()
    {
        return createDate;
    }

    /**
     * @param createDate the moment the task was created.
     */
    public void setCreateDate(Date createDate)
    {
        this.createDate = createDate;
    }

    /**
     * @return the deadline of the task.
     */
    public Date getDeadline()
    {
        return deadline;
    }

    /**
     * @param deadline the deadline of the task.
     */
    public void setDeadline(Date deadline)
    {
        this.deadline = deadline;
    }

    /**
     * @return the date when the task was marked as completed.
     */
    public Date getCompleteDate()
    {
        return completeDate;
    }

    /**
     * @param completeDate the date when the task was completed.
     */
    public void setCompleteDate(Date completeDate)
    {
        this.completeDate = completeDate;
    }

    /**
     * @return the description of this task. It can be saved in a particular syntax.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description the description of this task.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }
}
