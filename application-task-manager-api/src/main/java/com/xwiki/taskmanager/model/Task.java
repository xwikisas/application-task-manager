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

import java.util.Date;

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
    public static final String MACRO_NAME = "task";

    /**
     * The name of the NAME field.
     */
    public static final String NAME = "name";

    /**
     * The name of the NUMBER field.
     */
    public static final String NUMBER = "number";

    /**
     * The name of the OWNER field.
     */
    public static final String OWNER = "owner";

    /**
     * The name of the ID field.
     */
    public static final String REFERENCE = "reference";

    /**
     * The name of the STATUS field.
     */
    public static final String STATUS = "status";

    /**
     * The value of the STATUS field denoting that the task has been completed.
     */
    // TODO: The value of the "Done" state should be configurable in the administration section.
    public static final String STATUS_DONE = "done";

    /**
     * The name of the REPORTER field.
     */
    public static final String REPORTER = "reporter";

    /**
     * The name of the ASSIGNEE field.
     */
    public static final String ASSIGNEE = "assignee";

    /**
     * The name of the CREATE_DATE field.
     */
    public static final String CREATE_DATE = "createDate";

    /**
     * The name of the DUE_DATE field.
     */
    public static final String DUE_DATE = "duedate";

    /**
     * The name of the COMPLETE_DATE field.
     */
    public static final String COMPLETE_DATE = "completeDate";

    private String name;

    private int number;

    private DocumentReference owner;

    private DocumentReference reference;

    private String status;

    private DocumentReference reporter;

    private DocumentReference assignee;

    private Date createDate;

    private Date duedate;

    private Date completeDate;

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
    public DocumentReference getReference()
    {
        return reference;
    }

    /**
     * @param reference the Id of the task. Grouping the tasks by the document reference, their Ids have to be
     *     unique.
     */
    public void setReference(DocumentReference reference)
    {
        this.reference = reference;
    }

    /**
     * @return whether the task has been completed or not.
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * @param status the current state of the task - true: the task is completed; false: the task is not done.
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * @return the reference to the user that created this task.
     */
    public DocumentReference getReporter()
    {
        return reporter;
    }

    /**
     * @param reporter the reference to the user that created this task.
     */
    public void setReporter(DocumentReference reporter)
    {
        this.reporter = reporter;
    }

    /**
     * @return a list of references to the users that are assigned to this task.
     */
    public DocumentReference getAssignee()
    {
        return assignee;
    }

    /**
     * @param assignee a list of references to the users that are assigned to this task.
     */
    public void setAssignee(DocumentReference assignee)
    {
        this.assignee = assignee;
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
    public Date getDueDate()
    {
        return duedate;
    }

    /**
     * @param duedate the deadline of the task.
     */
    public void setDuedate(Date duedate)
    {
        this.duedate = duedate;
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
     * @return the name/title given to this task.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name the name/title given to this task.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the unique number identifying this Task on the current wiki.
     */
    public int getNumber()
    {
        return number;
    }

    /**
     * @param number the number that uniquely identifies the task.
     */
    public void setNumber(int number)
    {
        this.number = number;
    }
}
