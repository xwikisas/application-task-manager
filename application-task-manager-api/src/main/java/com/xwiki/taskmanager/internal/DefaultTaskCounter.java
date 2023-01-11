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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xwiki.taskmanager.TaskCounter;

/**
 * The default implementation of {@link com.xwiki.taskmanager.TaskCounter}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultTaskCounter implements TaskCounter, Initializable
{
    private int counter;
    @Inject
    private Provider<QueryManager> queryManagerProvider;

    @Inject
    private Logger logger;
    @Override
    public void initialize() throws InitializationException
    {
        String statement =
                "SELECT taskId.value "
                + "FROM XWikiDocument doc, BaseObject as obj, StringProperty as taskId "
                + "WHERE doc.fullName = obj.name AND obj.className = 'TaskManager.Code.TaskClass' "
                + "AND obj.id=taskId.id.id AND taskId.id.name='id' "
                + "ORDER BY taskId.value DESC";
        try {
            List<String> result = queryManagerProvider.get().createQuery(statement, Query.HQL).setLimit(1).execute();
            if (result.size() > 0) {
                counter = Integer.parseInt(result.get(0)) + 1;
            }
        } catch (QueryException | NumberFormatException e) {
            logger.warn("Query {} failed to execute. Setting counter to 0.", statement);
        }
    }
    @Override
    public synchronized int getAndIncrement()
    {
        return counter++;
    }
}
