/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.isSame;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class RelationStackingBehavior
    implements RelationLayerBehavior
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public CreateRelationAnnotationRequest apply(ArcAdapter aAdapter,
            CreateRelationAnnotationRequest aRequest)
        throws AnnotationException
    {
        final AnnotationLayer layer = aAdapter.getLayer();
        final JCas jcas = aRequest.getJcas();
        final int windowBegin = aRequest.getWindowBegin();
        final int windowEnd = aRequest.getWindowEnd();
        final AnnotationFS originFS = aRequest.getOriginFs();
        final AnnotationFS targetFS = aRequest.getTargetFs();
        final Type type = getType(jcas.getCas(), aAdapter.getLayer().getName());
        final Feature dependentFeature = type.getFeatureByBaseName(aAdapter.getTargetFeatureName());
        final Feature governorFeature = type.getFeatureByBaseName(aAdapter.getSourceFeatureName());
        
        // Locate the governor and dependent annotations - looking at the annotations that are
        // presently visible on screen is sufficient - we don't have to scan the whole CAS.
        for (AnnotationFS fs : selectCovered(jcas.getCas(), type, windowBegin, windowEnd)) {
            AnnotationFS existingTargetFS;
            AnnotationFS existingOriginFS;
            
            if (layer.getAttachFeature() != null) {
                final Type spanType = getType(jcas.getCas(), layer.getAttachType().getName());
                Feature arcSpanFeature = spanType
                        .getFeatureByBaseName(layer.getAttachFeature().getName());
                existingTargetFS = (AnnotationFS) fs.getFeatureValue(dependentFeature)
                        .getFeatureValue(arcSpanFeature);
                existingOriginFS = (AnnotationFS) fs.getFeatureValue(governorFeature)
                        .getFeatureValue(arcSpanFeature);
            }
            else {
                existingTargetFS = (AnnotationFS) fs.getFeatureValue(dependentFeature);
                existingOriginFS = (AnnotationFS) fs.getFeatureValue(governorFeature);
            }
        
            if (existingTargetFS == null || existingOriginFS == null) {
                log.warn("Relation [" + layer.getName() + "] with id [" + getAddr(fs)
                        + "] has loose ends - ignoring during while checking for duplicates.");
                continue;
            }

            // If stacking is not allowed and we would be creating a duplicate arc, then instead
            // update the label of the existing arc
            if (!layer.isAllowStacking()
                    && isDuplicate(existingOriginFS, originFS, existingTargetFS, targetFS)) {
                throw new AnnotationException("Cannot create another annotation of layer ["
                        + layer.getUiName() + "] at this location - stacking is not "
                        + "enabled for this layer.");
            }
        }
        
        return aRequest;
    }

    private boolean isDuplicate(AnnotationFS aAnnotationFSOldOrigin,
            AnnotationFS aAnnotationFSNewOrigin, AnnotationFS aAnnotationFSOldTarget,
            AnnotationFS aAnnotationFSNewTarget)
    {
        return isSame(aAnnotationFSOldOrigin, aAnnotationFSNewOrigin)
                && isSame(aAnnotationFSOldTarget, aAnnotationFSNewTarget);
    }
}
