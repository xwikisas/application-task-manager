package com.xwiki.taskmanager.internal.rest;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.resources.pages.ModifiablePageResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.taskmanager.TaskManagerConfiguration;
import com.xwiki.taskmanager.model.Task;
import com.xwiki.taskmanager.rest.TaskResource;

/**
 * Default implementation of {@link TaskResource}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("com.xwiki.taskmanager.internal.rest.DefaultTaskResource")
@Singleton
public class DefaultTaskResource extends ModifiablePageResource implements TaskResource
{
    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private TaskManagerConfiguration configuration;

    @Override
    public Response changeTaskStatus(String wikiName, String spaces, String pageName, String taskId, Boolean completed)
        throws XWikiRestException
    {
        DocumentReference docRef = new DocumentReference(pageName, getSpaceReference(spaces, wikiName));

        if (!contextualAuthorizationManager.hasAccess(Right.EDIT, docRef)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            XWikiDocument document = getXWikiContext().getWiki().getDocument(docRef, getXWikiContext()).clone();
            XDOM documentContent = document.getXDOM();
            List<MacroBlock> macros = documentContent.getBlocks(new MacroBlockMatcher("task"), Block.Axes.DESCENDANT);

            Optional<MacroBlock> selectedMacro = macros.stream()
                .filter((macroBlock) -> macroBlock.getParameters().getOrDefault(Task.ID, "").equals(taskId))
                .findFirst();

            if (!selectedMacro.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String completeDate = new SimpleDateFormat(configuration.getStorageDateFormat()).format(new Date());
            selectedMacro.get().setParameter(Task.STATUS, completed.toString());

            selectedMacro.get().setParameter(Task.COMPLETE_DATE, completed ? completeDate : "");

            document.setContent(documentContent);

            getXWikiContext().getWiki().saveDocument(document, "Changed task status!", getXWikiContext());

            return Response.ok().build();
        } catch (XWikiException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
