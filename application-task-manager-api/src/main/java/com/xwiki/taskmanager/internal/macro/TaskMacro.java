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

package com.xwiki.taskmanager.internal.macro;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;

import com.xwiki.taskmanager.macro.TaskMacroParameters;

/**
 * Task macro that will enable the users to assign tasks to each other.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("task")
@Singleton
public class TaskMacro extends AbstractMacro<TaskMacroParameters>
{
    private static final String HTML_CLASS = "class";
    @Inject
    private MacroContentParser contentParser;

    private final Map<String, String> blockParameters = new HashMap<>();

    @Inject
    @Named("ssx")
    private SkinExtension ssx;

    @Inject
    @Named("jsx")
    private SkinExtension jsx;

    /**
     * Ur mom.
     */
    public TaskMacro()
    {
        super("name", "description", new DefaultContentDescriptor("Content of the task.", false,
            Block.LIST_BLOCK_TYPE), TaskMacroParameters.class);
        blockParameters.put(HTML_CLASS, "task-macro");
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public List<Block> execute(TaskMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        this.ssx.use(DateMacro.SKIN_RESOURCES_DOCUMENT_REFERENCE);
        this.jsx.use(DateMacro.SKIN_RESOURCES_DOCUMENT_REFERENCE);

        Block ret = null;

        if (content == null) {
            return Collections.emptyList();
        }

        List<Block> macroContent = contentParser.parse(content, context, false, context.isInline())
            .getChildren();

        List<Block> contentBlocks =
            Collections.singletonList(new MetaDataBlock(macroContent, this.getNonGeneratedContentMetaData()));

        ret = context.isInline() ? new FormatBlock() : new GroupBlock();
        ret.setParameters(blockParameters);
        String htmlCheckbox = String.format("<input type=\"checkbox\" data-taskId=\"%s\" %s class=\"task-status\">",
            parameters.getId(), parameters.getStatus().equals("done") ? "checked" : "");
        Block checkBoxBlock = new RawBlock(htmlCheckbox, Syntax.HTML_5_0);

        ret.addChild(new FormatBlock(Collections.singletonList(checkBoxBlock), Format.NONE));
        ret.addChild(new GroupBlock(contentBlocks, Collections.singletonMap(HTML_CLASS, "task-content")));
        return Collections.singletonList(ret);
    }
}
