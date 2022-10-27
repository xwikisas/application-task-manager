package com.xwiki.taskmanager;

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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.taskmanager.internal.TaskObjectEventListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
@RunWith(MockitoJUnitRunner.class)
public class TaskObjectEventListenerTests
{
    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<>(TaskObjectEventListener.class);

    private EventListener listener;

    private DocumentReferenceResolver<String> resolver;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument document;

    @Mock
    private XDOM docXDOM;

    @Mock
    private MacroBlock macro1;

    @Mock
    private BaseObject obj1;

    private final DocumentReference adminRef = new DocumentReference("xwiki", "XWiki", "Admin");

    public final DateFormat deadlineDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm");


    @Before
    public void setup() throws ComponentLookupException
    {
        this.listener = mocker.getComponentUnderTest();
        this.resolver = mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);


        when(this.document.getXDOM()).thenReturn(this.docXDOM);
        when(this.document.getXObjects(TaskObjectEventListener.TASK_OBJECT_CLASS_REFERENCE)).thenReturn(Collections.singletonList(obj1));
        when(this.docXDOM.getBlocks(any(), any())).thenReturn(Collections.singletonList(macro1));
        when(this.obj1.getStringValue("id")).thenReturn("someid1");
        when(this.resolver.resolve("XWiki.Admin")).thenReturn(adminRef);
        when(this.macro1.getContent()).thenReturn("Important task! {{mention reference=\"XWiki.Admin\" "
            + "style=\"FULL_NAME\" anchor=\"XWiki-Admin-zmrgl3\"/}}{{date date=\"2022/10/12 18:16\"/}} lesgooo it works");

    }

    @Test
    public void updateExistingMacroObjectTest() throws ParseException, XWikiException
    {

        Map<String, String> macro1params = new HashMap<>();
        macro1params.put("id", "someid1");
        macro1params.put("creator", "XWiki.Admin");
        macro1params.put("createDate", "14/10/2022");
        macro1params.put("status", "onGoing");

        when(this.macro1.getParameters()).thenReturn(macro1params);

        this.listener.onEvent(null, document, context);

        verify(document, never()).newXObject(TaskObjectEventListener.TASK_OBJECT_CLASS_REFERENCE, context);
        verify(obj1).set("assignee", Collections.singletonList(adminRef), context);
        verify(obj1).set("deadline", deadlineDateFormat.parse("2022/10/12 18:16"), context);
        verify(obj1).set("description", "Important task!  lesgooo it works", context);
    }

    @Test
    public void updateExistingMacroObjectWithMultipleAssignees() throws XWikiException, ParseException
    {
        Map<String, String> macro1params = new HashMap<>();
        macro1params.put("id", "someid1");
        macro1params.put("creator", "XWiki.Admin");
        macro1params.put("createDate", "14/10/2022");
        macro1params.put("status", "onGoing");

        when(this.macro1.getParameters()).thenReturn(macro1params);

        DocumentReference assignee2ref = new DocumentReference("xwiki", "XWiki", "User1");
        when(this.resolver.resolve("XWiki.User1")).thenReturn(assignee2ref);
        when(this.macro1.getContent()).thenReturn("Important task! {{mention reference=\"XWiki.Admin\" /}} {{mention "
            + "reference=\"XWiki.User1\" /}} {{date date=\"2022/10/12 18:16\"/}} lesgooo it works");

        this.listener.onEvent(null, document, context);

        verify(document, never()).newXObject(TaskObjectEventListener.TASK_OBJECT_CLASS_REFERENCE, context);
        verify(obj1).set("assignee", Arrays.asList(adminRef, assignee2ref), context);
        verify(obj1).set("deadline", deadlineDateFormat.parse("2022/10/12 18:16"), context);
        verify(obj1).set("description", "Important task!    lesgooo it works", context);
    }

    @Test
    public void createMacroObjectTest() throws XWikiException, ParseException
    {
        Map<String, String> macro1params = new HashMap<>();
        macro1params.put("id", "someid1");
        macro1params.put("creator", "XWiki.Admin");
        macro1params.put("createDate", "14/10/2022");
        macro1params.put("status", "onGoing");

        when(this.document.getXObjects(TaskObjectEventListener.TASK_OBJECT_CLASS_REFERENCE)).thenReturn(Collections.emptyList());
        when(this.document.newXObject(TaskObjectEventListener.TASK_OBJECT_CLASS_REFERENCE, context)).thenReturn(obj1);
        when(this.macro1.getParameters()).thenReturn(macro1params);

        this.listener.onEvent(null, document, context);

        verify(document).newXObject(TaskObjectEventListener.TASK_OBJECT_CLASS_REFERENCE, context);
        verify(obj1).set("assignee", Collections.singletonList(adminRef), context);
        verify(obj1).set("deadline", deadlineDateFormat.parse("2022/10/12 18:16"), context);
        verify(obj1).set("description", "Important task!  lesgooo it works", context);
    }

    @Test
    public void removeMacroObjectWhenThereIsNoCorrespondingMacroOnPageTest() {
        when(this.docXDOM.getBlocks(any(), any())).thenReturn(Collections.emptyList());

        this.listener.onEvent(null, document, context);

        verify(this.document).removeXObject(obj1);
    }
}
