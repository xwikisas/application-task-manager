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

package com.xwiki.taskmanager;

import java.text.SimpleDateFormat;

import javax.inject.Provider;

import org.junit.runner.RunWith;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.taskmanager.model.Task;
import com.xwiki.taskmanager.script.TaskManagerScriptService;

import static org.mockito.Mockito.when;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 1.0
 */
@RunWith(RenderingTestSuite.class)
@AllComponents
public class IntegrationTests
{
    @RenderingTestSuite.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerMockComponent(SkinExtension.class, "ssx");
        componentManager.registerMockComponent(SkinExtension.class, "jsx");
        componentManager.registerMockComponent(ConfigurationSource.class, "taskmanager");
        componentManager.registerMockComponent(ScriptService.class, "taskmanager");
        componentManager.registerMockComponent(TaskManagerScriptService.class);

        Provider<XWikiContext> contextProvider =
            componentManager.registerMockComponent(
                new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        XWikiContext context = componentManager.registerMockComponent(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        ContextualAuthorizationManager authorizationManager =
            componentManager.registerMockComponent(ContextualAuthorizationManager.class);
        TaskManager taskManager = componentManager.registerMockComponent(TaskManager.class);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User1");
        DocumentReference ref1 = new DocumentReference("xwiki", "Sandbox", "Task");
        DocumentReference ref2 = new DocumentReference("xwiki", "Sandbox", "Task2");

        Task task = new Task();
        task.setReference(ref1);
        task.setName("Test name");
        task.setDuedate(dateFormat.parse("01/01/2023"));
        task.setNumber(1);
        task.setStatus("Done");
        task.setReporter(user);
        task.setCreateDate(dateFormat.parse("01/01/2023"));
        task.setCompleteDate(dateFormat.parse("01/01/2023"));

        Task task2 = new Task();
        task2.setReference(ref2);
        task2.setName("Test name");
        task2.setDuedate(dateFormat.parse("01/01/2023"));
        task2.setNumber(2);
        task2.setStatus("Done");
        task2.setReporter(user);
        task2.setCreateDate(dateFormat.parse("01/01/2023"));
        task2.setCompleteDate(dateFormat.parse("01/01/2023"));

        when(taskManager.getTask(2)).thenReturn(task2);
        when(taskManager.getTask(1)).thenReturn(task);
        when(taskManager.getTask(ref1)).thenReturn(task);
        when(taskManager.getTask(ref2)).thenReturn(task2);
        when(context.getUserReference()).thenReturn(user);
        when(authorizationManager.hasAccess(Right.VIEW, ref1)).thenReturn(true);
        when(authorizationManager.hasAccess(Right.VIEW, ref2)).thenReturn(true);

        ConfigurationSource prefs = componentManager.registerMockComponent(ConfigurationSource.class, "wiki");
        when(prefs.getProperty("dateformat", "yyyy/MM/dd HH:mm")).thenReturn("yyyy/MM/dd HH:mm");

        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
        componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "macro");
    }
}
