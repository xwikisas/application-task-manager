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

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xwiki.taskmanager.TaskCounter;
import com.xwiki.taskmanager.TaskException;

/**
 * The default implementation of {@link com.xwiki.taskmanager.TaskCounter}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultTaskCounter implements TaskCounter
{
    @Inject
    private Provider<QueryManager> queryManagerProvider;

    private int lastReturnedNumber = -1;

    @Override
    public synchronized int getNextNumber() throws TaskException
    {
        String statement =
            "select max(taskObject.number) "
                + "from Document doc, doc.object(TaskManager.Code.TaskClass) as taskObject";
        try {
            List<Integer> result = queryManagerProvider.get().createQuery(statement, Query.XWQL).execute();

            int number = 0;
            if (result.size() > 0 && result.get(0) != null) {
                number = result.get(0);
            }
            lastReturnedNumber = Integer.max(number, lastReturnedNumber) + 1;
            return lastReturnedNumber;
        } catch (QueryException e) {
            throw new TaskException("Failed to get the next valid number.", e);
        }
    }
}
