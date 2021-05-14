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
package de.tudarmstadt.ukp.inception.workload.dynamic.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension_Impl;
import de.tudarmstadt.ukp.inception.workload.dynamic.annotation.DynamicWorkflowActionBarExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.annotation.DynamicWorkflowDocumentNavigationActionBarExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtensionPointImpl;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.DefaultWorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.RandomizedWorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@Configuration
@ConditionalOnProperty(prefix = "workload.dynamic", name = "enabled", havingValue = "true")
public class DynamicWorkloadManagerAutoConfiguration
{
    @Bean
    public DynamicWorkloadExtension dynamicWorkloadExtension(DocumentService documentService,
            WorkloadManagementService aWorkloadManagementService,
            WorkflowExtensionPoint aWorkflowExtensionPoint, ProjectService aProjectService,
            UserDao aUserRepository)
    {
        return new DynamicWorkloadExtension_Impl(aWorkloadManagementService,
                aWorkflowExtensionPoint, documentService, aProjectService, aUserRepository);
    }

    @Bean
    public WorkflowExtensionPoint workflowExtensionPoint(
            @Lazy @Autowired(required = false) List<WorkflowExtension> aWorkflowExtension)
    {
        return new WorkflowExtensionPointImpl(aWorkflowExtension);
    }

    @Bean
    public DefaultWorkflowExtension defaultWorkflowExtension()
    {
        return new DefaultWorkflowExtension();
    }

    @Bean
    public RandomizedWorkflowExtension randomizedWorkflowExtension()
    {
        return new RandomizedWorkflowExtension();
    }

    @Bean
    public DynamicWorkflowActionBarExtension DynamicWorkflowActionBarExtension(
            WorkloadManagementService aWorkloadManagementService, ProjectService aProjectService)
    {
        return new DynamicWorkflowActionBarExtension(aWorkloadManagementService, aProjectService);
    }

    @Bean
    public DynamicWorkflowDocumentNavigationActionBarExtension dynamicWorkflowDocumentNavigationActionBarExtension(
            DocumentService aDocumentService, WorkloadManagementService aWorkloadManagementService,
            DynamicWorkloadExtension aDynamicWorkloadExtension, ProjectService aProjectService)
    {
        return new DynamicWorkflowDocumentNavigationActionBarExtension(aDocumentService,
                aWorkloadManagementService, aDynamicWorkloadExtension, aProjectService);
    }
}
