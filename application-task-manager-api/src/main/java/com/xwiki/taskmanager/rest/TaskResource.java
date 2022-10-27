package com.xwiki.taskmanager.rest;

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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

/**
 * Provides the functionality of changing the status of a Task macro inside a page.
 *
 * @version $Id$
 * @since 1.0
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/tasks/{taskId}")
@Unstable
public interface TaskResource
{
    /**
     * Modify the status of a Task macro.
     *
     * @param wikiName the name of the wiki in which the page resides
     * @param spaces the spaces of the page
     * @param pageName the name of the page
     * @param taskId the id of the macro
     * @param status the new value for the status of the Task macro
     * @return 200 is the status has been changed successfully of 404 if the task was not found
     * @throws XWikiRestException when failing in retrieving the document or saving it
     */
    @GET
    Response changeTaskStatus(
        @PathParam("wikiName") String wikiName,
        @PathParam("spaceName") @Encoded String spaces,
        @PathParam("pageName") String pageName,
        @PathParam("taskId") String taskId,
        @QueryParam("status") @DefaultValue("done") String status
    ) throws XWikiRestException;
}
