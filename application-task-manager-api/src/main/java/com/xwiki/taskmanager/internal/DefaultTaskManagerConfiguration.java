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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.xwiki.taskmanager.TaskManagerConfiguration;

/**
 * The default implementation of {@link TaskManagerConfiguration}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultTaskManagerConfiguration implements TaskManagerConfiguration
{
    private static final String STORAGE_FORMAT_KEY = "storageDateFormat";

    private static final String DISPLAY_FORMAT_KEY = "displayDateFormat";

    private static final String DEFAULT_DATE_FORMAT = "yyyy/MM/dd HH:mm";

    private static final String PROPERTIES_PREFIX = "taskmanager.";

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    @Inject
    @Named("taskmanager")
    private ConfigurationSource configurationSource;

    @Override
    public String getStorageDateFormat()
    {
        if (this.configurationSource.containsKey(STORAGE_FORMAT_KEY)) {
            return this.configurationSource.getProperty(STORAGE_FORMAT_KEY, DEFAULT_DATE_FORMAT);
        } else {
            return this.xwikiProperties.getProperty(PROPERTIES_PREFIX + STORAGE_FORMAT_KEY, DEFAULT_DATE_FORMAT);
        }
    }

    @Override
    public String getDisplayDateFormat()
    {
        if (this.configurationSource.containsKey(DISPLAY_FORMAT_KEY)) {
            return this.configurationSource.getProperty(DISPLAY_FORMAT_KEY, DEFAULT_DATE_FORMAT);
        } else {
            return this.xwikiProperties.getProperty(PROPERTIES_PREFIX + DISPLAY_FORMAT_KEY, DEFAULT_DATE_FORMAT);
        }
    }
}