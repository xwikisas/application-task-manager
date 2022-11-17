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
package com.xwiki.taskmanager.script;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xwiki.taskmanager.TaskManagerConfiguration;
import com.xwiki.taskmanager.TaskManagerException;
import com.xwiki.taskmanager.internal.TaskExtractor;
import com.xwiki.taskmanager.internal.TaskQueryManager;
import com.xwiki.taskmanager.model.Task;

/**
 * Script service for retrieving information about the Task Manager Application.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("taskmanager")
@Singleton
@Unstable
public class TaskManagerScriptService implements ScriptService
{
    @Inject
    private TaskManagerConfiguration configuration;

    @Inject
    private TaskExtractor taskExtractor;

    @Inject
    private TaskQueryManager taskQueryManager;
    /**
     * @return the configured storage date format.
     */
    public String getStorageDateFormat()
    {
        return this.configuration.getStorageDateFormat();
    }

    /**
     * @return the configured display date format.
     */
    public String getDisplayDateFormat()
    {
        return this.configuration.getDisplayDateFormat();
    }

    /**
     * @param shortQuery the short version of a query (without SELECT) that retrieves the documents with Task
     * objects, filtered in an arbitrary manner.
     * @param params the parameters that will be injected in the query.
     * @param offset the offset of the query.
     * @param limit the limit of the resulting set returned by the query.
     * @return a list of Task objects, alongside the offset and the total possible entries.
     * @throws TaskManagerException if the task query fails or returns documents that can't be resolved.
     */
    public CollectionIterableResult<Task> searchTasks(String shortQuery, Map<String, Object> params, int offset,
        int limit) throws TaskManagerException
    {
        return taskQueryManager.searchTasks(shortQuery, params, offset, limit);
    }
}
