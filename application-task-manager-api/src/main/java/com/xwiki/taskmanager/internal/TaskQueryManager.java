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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.taskmanager.TaskManagerConfiguration;
import com.xwiki.taskmanager.TaskManagerException;
import com.xwiki.taskmanager.model.Task;

/**
 * Class that will handle the retrieval of tasks from different mediums.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = TaskQueryManager.class)
@Singleton
public class TaskQueryManager
{
    private static final String COMMA_DELIMITER_REGEX = "\\s*,\\s*";

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private TaskManagerConfiguration configuration;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * @param shortTaskQuery the short version of a query (without SELECT) that retrieves the documents with Task
     *     objects, filtered in an arbitrary manner.
     * @param params the parameters that will be injected in the query.
     * @param offset the offset of the query.
     * @param limit the limit of the resulting set returned by the query.
     * @return a list of Task objects, alongside the offset and the total possible entries.
     */
    public CollectionIterableResult<Task> searchTasks(String shortTaskQuery, Map<String, Object> params, int offset,
        int limit) throws TaskManagerException
    {

        XWikiContext context = contextProvider.get();
        DocumentReference currentUserRef = context.getUserReference();

        try {
            StringBuilder selectQuery = createSelectQuery(shortTaskQuery);
            String sqlQuery = selectQuery.append(shortTaskQuery).toString();
            Query query =
                queryManager.createQuery(sqlQuery, Query.HQL).setLimit(limit).setOffset(offset)
                    .bindValues(params);
            List<Object[]> thing = query.execute();

            Query countQuery = queryManager.createQuery(sqlQuery, Query.HQL).bindValues(params).addFilter(countFilter);
            List<Long> totalResults = countQuery.execute();

            List<Task> taskResults = new ArrayList<>();

            for (Object[] item : thing) {
                String docName = (String) item[0];
                String taskId = (String) item[1];
                DocumentReference docRef = resolver.resolve(docName);

                Task task = new Task();
                if (authorizationManager.hasAccess(Right.VIEW, currentUserRef, docRef)) {
                    XWikiDocument taskDoc = context.getWiki().getDocument(docRef, context);

                    BaseObject taskObject =
                        taskDoc.getXObject(TaskMacroUpdateEventListener.TASK_OBJECT_CLASS_REFERENCE, Task.ID, taskId,
                            false);

                    populateTask(taskId, docRef, taskObject, task);
                }

                taskResults.add(task);
            }
            return new CollectionIterableResult<>(totalResults.get(0).intValue(), offset, taskResults);
        } catch (QueryException e) {
            throw new TaskManagerException(String.format("The query [%s] could not be executed!", shortTaskQuery), e);
        } catch (XWikiException e) {
            throw new TaskManagerException(
                String.format("Failed a document returned by the query [%s].", shortTaskQuery), e);
        }
    }

    private StringBuilder createSelectQuery(String shortTaskQuery)
    {
        StringBuilder select = new StringBuilder("SELECT distinct doc.fullName, taskId.value");
        String from = " FROM XWikiDocument doc ";
        int orderByLocation = shortTaskQuery.indexOf("order by");
        if (orderByLocation >= 0) {
            String orderByClause = shortTaskQuery.substring(orderByLocation);
            String orderByValues = orderByClause.replaceAll("order by|asc|desc", "");
            for (String s : orderByValues.trim().split(COMMA_DELIMITER_REGEX)) {
                select.append(", ");
                select.append(s);
            }
        }
        return select.append(from);
    }

    private void populateTask(String taskId, DocumentReference docRef, BaseObject taskObject, Task task)
    {
        task.setId(taskId);
        task.setOwner(docRef);
        task.setDescription(taskObject.getStringValue(Task.DESCRIPTION));
        task.setCompleted(taskObject.getIntValue(Task.STATUS) == 1);

        task.setCompleteDate(taskObject.getDateValue(Task.COMPLETE_DATE));
        task.setDeadline(taskObject.getDateValue(Task.DEADLINE));
        task.setCreateDate(taskObject.getDateValue(Task.CREATE_DATE));

        task.setCreator(resolver.resolve(taskObject.getStringValue(Task.CREATOR)));
        List<DocumentReference> taskAssignees = new ArrayList<>();

        for (String serializedAssignee : taskObject.getLargeStringValue(Task.ASSIGNEES).split(COMMA_DELIMITER_REGEX)) {
            if (!"".equals(serializedAssignee)) {
                taskAssignees.add(resolver.resolve(serializedAssignee));
            }
        }
        task.setAssignees(taskAssignees);
    }
}
