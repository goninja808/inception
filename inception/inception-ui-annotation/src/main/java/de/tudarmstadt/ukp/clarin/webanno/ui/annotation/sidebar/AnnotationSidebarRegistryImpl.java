/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.support.logging.BaseLoggers;

@Component
public class AnnotationSidebarRegistryImpl
    implements AnnotationSidebarRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<AnnotationSidebarFactory> extensionsProxy;

    private List<AnnotationSidebarFactory> extensions;

    public AnnotationSidebarRegistryImpl(
            @Lazy @Autowired(required = false) List<AnnotationSidebarFactory> aExtensions)
    {
        extensionsProxy = aExtensions;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    /* package private */ void init()
    {
        List<AnnotationSidebarFactory> exts = new ArrayList<>();

        if (extensionsProxy != null) {
            exts.addAll(extensionsProxy);
            exts.sort(buildComparator());

            for (AnnotationSidebarFactory fs : exts) {
                log.debug("Found annotation sidebar extension: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }

        BaseLoggers.BOOT_LOG.info("Found [{}] annotation sidebar extensions", exts.size());

        extensions = unmodifiableList(exts);
    }

    @Override
    public List<AnnotationSidebarFactory> getSidebarFactories()
    {
        return extensions;
    }

    @Override
    public AnnotationSidebarFactory getSidebarFactory(String aId)
    {
        if (aId == null) {
            return null;
        }
        else {
            return extensions.stream().filter(f -> aId.equals(f.getBeanName())).findFirst()
                    .orElse(null);
        }
    }

    @Override
    public AnnotationSidebarFactory getDefaultSidebarFactory()
    {
        return getSidebarFactories().get(0);
    }

    /**
     * Builds a comparator that sorts first by the order, if specified, then by the display name to
     * break ties. It is assumed that the compared elements are all non-null
     * 
     * @return The comparator
     */
    private Comparator<AnnotationSidebarFactory> buildComparator()
    {
        return (asf1, asf2) -> new CompareToBuilder()
                .appendSuper(AnnotationAwareOrderComparator.INSTANCE.compare(asf1, asf2))
                .append(asf1.getDisplayName(), asf2.getDisplayName()).toComparison();
    }
}
