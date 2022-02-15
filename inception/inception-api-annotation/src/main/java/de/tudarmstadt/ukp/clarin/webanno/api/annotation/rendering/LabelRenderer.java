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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil.getUiLabelText;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationAutoConfiguration#labelRenderer}.
 * </p>
 */
@Order(RenderStep.RENDER_LABELS)
public class LabelRenderer
    implements RenderStep
{
    public static final String ID = "LabelRenderer";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public void render(VDocument aVDoc, RenderRequest aRequest)
    {
        for (AnnotationLayer layer : aVDoc.getAnnotationLayers()) {
            for (VObject vobj : aVDoc.objects(layer.getId())) {
                vobj.setLabelHint(getUiLabelText(vobj));
            }
        }
    }
}
