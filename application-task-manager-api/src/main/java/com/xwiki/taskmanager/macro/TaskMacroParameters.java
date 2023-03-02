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

package com.xwiki.taskmanager.macro;

import org.xwiki.properties.annotation.PropertyDisplayHidden;
import org.xwiki.properties.annotation.PropertyDisplayType;
import org.xwiki.stability.Unstable;

import com.xwiki.taskmanager.TaskDate;
import com.xwiki.taskmanager.TaskReference;

/**
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class TaskMacroParameters
{
    private String reference;

    private String reporter;

    private String createDate;

    private String status;

    private String completeDate;

    /**
     * @return the id of the task.
     */
    public String getReference()
    {
        return reference;
    }

    /**
     * @param reference the id of the task.
     */
    @PropertyDisplayHidden
    @PropertyDisplayType(TaskReference.class)
    public void setReference(String reference)
    {
        this.reference = reference;
    }

    /**
     * @return the creator of the task.
     */
    public String getReporter()
    {
        return reporter;
    }

    /**
     * @param reporter the creator of the task.
     */
    @PropertyDisplayHidden
    public void setReporter(String reporter)
    {
        this.reporter = reporter;
    }

    /**
     * @return the creation date of the task.
     */
    public String getCreateDate()
    {
        return createDate;
    }

    /**
     * @param createDate the creation date of the task.
     */
    @PropertyDisplayHidden
    public void setCreateDate(String createDate)
    {
        this.createDate = createDate;
    }

    /**
     * @return the status of the task.
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * @param status the status of the task.
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * @return the date when the task has been completed.
     */
    public String getCompleteDate()
    {
        return completeDate;
    }

    /**
     * @param completeDate the date when the task has been completed.
     */
    @PropertyDisplayType(TaskDate.class)
    public void setCompleteDate(String completeDate)
    {
        this.completeDate = completeDate;
    }
}
