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
package de.tudarmstadt.ukp.inception.workload.dynamic;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SourceDocumentStateStats;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.workload.dynamic.config.DynamicWorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.dynamic.trait.DynamicWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.DefaultWorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DynamicWorkloadManagerAutoConfiguration#dynamicWorkloadExtension}
 * </p>
 */
public class DynamicWorkloadExtension_Impl
    implements DynamicWorkloadExtension
{
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final WorkloadManagementService workloadManagementService;
    private final WorkflowExtensionPoint workflowExtensionPoint;
    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userRepository;

    public DynamicWorkloadExtension_Impl(WorkloadManagementService aWorkloadManagementService,
            WorkflowExtensionPoint aWorkflowExtensionPoint, DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserRepository)
    {
        workloadManagementService = aWorkloadManagementService;
        workflowExtensionPoint = aWorkflowExtensionPoint;
        documentService = aDocumentService;
        projectService = aProjectService;
        userRepository = aUserRepository;
    }

    @Override
    public String getId()
    {
        return DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID;
    }

    @Override
    public String getLabel()
    {
        return "Dynamic assignment";
    }

    @Override
    public DynamicWorkloadTraits readTraits(WorkloadManager aWorkloadManager)
    {
        DynamicWorkloadTraits traits = null;

        try {
            traits = fromJsonString(DynamicWorkloadTraits.class, aWorkloadManager.getTraits());
        }
        catch (Exception e) {
            this.log.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new DynamicWorkloadTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(WorkloadManagementService aWorkloadManagementService,
            DynamicWorkloadTraits aTrait, Project aProject)
    {
        try {
            WorkloadManager manager = aWorkloadManagementService
                    .loadOrCreateWorkloadManagerConfiguration(aProject);
            manager.setTraits(JSONUtil.toJsonString(aTrait));
            aWorkloadManagementService.saveConfiguration(manager);
        }
        catch (Exception e) {
            this.log.error("Unable to write traits", e);
        }
    }

    @Override
    public Optional<SourceDocument> nextDocumentToAnnotate(Project aProject, User aUser)
    {
        // First, check if there are other documents which have been in the state INPROGRESS
        // Load the first one found
        List<AnnotationDocument> inProgressDocuments = workloadManagementService
                .getAnnotationDocumentListForUserWithState(aProject, aUser, IN_PROGRESS);
        if (!inProgressDocuments.isEmpty()) {
            return Optional.of(inProgressDocuments.get(0).getDocument());
        }

        WorkloadManager currentWorkload = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(aProject);

        // If there are no traits set yet, use the DefaultWorkflowExtension
        // otherwise select the current one
        DynamicWorkloadTraits traits = readTraits(currentWorkload);
        WorkflowExtension currentWorkflowExtension = workflowExtensionPoint
                .getExtension(traits.getWorkflowType()) //
                .orElseGet(DefaultWorkflowExtension::new);

        // Get all documents for which the state is NEW, or which have not been created yet.
        List<SourceDocument> sourceDocuments = workloadManagementService
                .listAnnotationDocumentsForUser(aProject, aUser);

        // Rearrange list of documents according to current workflow
        sourceDocuments = currentWorkflowExtension.rankDocuments(sourceDocuments);

        for (SourceDocument doc : sourceDocuments) {
            // FIXME: repeated query to DB should be optimized into a single query returning
            // a map of documents / annotator counts

            // Check if there are less annotators working on the selected document than
            // the target number of annotation set by the project manager
            if ((workloadManagementService.getNumberOfUsersWorkingOnADocument(doc)) < (traits
                    .getDefaultNumberOfAnnotations())) {
                return Optional.of(doc);
            }
        }

        return Optional.empty();
    }

    @Override
    @Transactional
    public ProjectState freshenStatus(Project aProject)
    {
        WorkloadManager currentWorkload = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(aProject);
        DynamicWorkloadTraits traits = readTraits(currentWorkload);

        // If the duration is not positive, then we can already stop here
        if (!traits.getAbandonationTimeout().negated().isNegative()) {
            return projectService.getProject(aProject.getId()).getState();
        }

        List<AnnotationDocument> inProgressDocuments = documentService
                .listAnnotationDocumentsInState(aProject, IN_PROGRESS);

        // Find abandoned annotation documents
        Map<SourceDocument, Set<AnnotationDocument>> abandonedDocuments = new LinkedHashMap<>();
        Instant now = Instant.now();
        Duration abandonationTimeout = traits.getAbandonationTimeout();
        for (AnnotationDocument doc : inProgressDocuments) {
            // If the SOURCE document is already in curation, we do not touch the state anymore
            if (doc.getDocument().getState() == CURATION_FINISHED
                    || doc.getDocument().getState() == CURATION_IN_PROGRESS) {
                continue;
            }

            // If the timestamp is still null, then the annotator may have accessed the document
            // but never actually made an annotation.
            if (doc.getTimestamp() == null) {
                abandonedDocuments.computeIfAbsent(doc.getDocument(), _doc -> new LinkedHashSet<>())
                        .add(doc);
                continue;
            }

            Duration idleTime = Duration.between(doc.getTimestamp().toInstant(), now);
            if (idleTime.compareTo(abandonationTimeout) > 0) {
                abandonedDocuments.computeIfAbsent(doc.getDocument(), _doc -> new LinkedHashSet<>())
                        .add(doc);
            }
        }

        if (abandonedDocuments.isEmpty()) {
            return projectService.getProject(aProject.getId()).getState();
        }

        // Update the annotation document and source document states for the abandoned documents
        Map<String, User> userCache = new HashMap<>();
        for (Entry<SourceDocument, Set<AnnotationDocument>> docSet : abandonedDocuments
                .entrySet()) {
            if (AnnotationDocumentState.NEW == traits.getAbandonationState()) {
                for (AnnotationDocument adoc : docSet.getValue()) {
                    User user = userCache.computeIfAbsent(adoc.getUser(),
                            username -> userRepository.get(username));
                    try {
                        documentService.resetAnnotationCas(adoc.getDocument(), user);
                    }
                    catch (UIMAException | IOException e) {
                        log.error("Unable to reset abandoned document {}", adoc);
                    }
                }
            }
            else {
                documentService.bulkSetAnnotationDocumentState(docSet.getValue(),
                        traits.getAbandonationState());
            }

            updateDocumentState(docSet.getKey(), traits.getDefaultNumberOfAnnotations());
        }

        // Refresh the project stats and recalculate them
        Project project = projectService.getProject(aProject.getId());
        SourceDocumentStateStats stats = documentService.getSourceDocumentStats(project);
        projectService.setProjectState(aProject, stats.getProjectState());

        return project.getState();
    }

    @Override
    @Transactional
    public void updateDocumentState(SourceDocument aDocument, int aRequiredAnnotatorCount)
    {
        Map<AnnotationDocumentState, Long> stats = documentService
                .getAnnotationDocumentStats(aDocument);
        long finishedCount = stats.get(AnnotationDocumentState.FINISHED);
        long inProgressCount = stats.get(AnnotationDocumentState.IN_PROGRESS);

        // If enough documents are finished, mark as finished
        if (finishedCount >= aRequiredAnnotatorCount) {
            documentService.setSourceDocumentState(aDocument, ANNOTATION_FINISHED);
        }
        // ... or if nobody has started yet, mark as new
        else if (finishedCount + inProgressCount == 0) {
            documentService.setSourceDocumentState(aDocument, SourceDocumentState.NEW);
        }
        else {
            documentService.setSourceDocumentState(aDocument, ANNOTATION_IN_PROGRESS);
        }
    }
}
