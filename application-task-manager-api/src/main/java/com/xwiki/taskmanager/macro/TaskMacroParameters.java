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

import com.xwiki.taskmanager.TaskReference;

/**
 * Ur mom.
 * @version $Id$
 */
public class TaskMacroParameters
{
    private String id;

    private String creator;

    private String createDate;

    private String status = "onGoing";

    /**
     *
     * @return ur mom
     */
    public String getId()
    {
        return id;
    }

    /**
     *
     * @param id ur mom
     */
    @PropertyDisplayHidden
    @PropertyDisplayType(TaskReference.class)
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     *
     * @return ur mom
     */
    public String getCreator()
    {
        return creator;
    }

    /**
     *
     * @param creator ur mom
     */
    @PropertyDisplayHidden
    public void setCreator(String creator)
    {
        this.creator = creator;
    }

    /**
     *
     * @return ur mom
     */
    public String getCreateDate()
    {
        return createDate;
    }

    /**
     *
     * @param createDate ur mom
     */
    @PropertyDisplayHidden
    public void setCreateDate(String createDate)
    {
        this.createDate = createDate;
    }

    /**
     *
     * @return ur mom
     */
    public String getStatus()
    {
        return status;
    }

    /**
     *
     * @param status ur mom
     */
    @PropertyDisplayHidden
    public void setStatus(String status)
    {
        this.status = status;
    }
}
