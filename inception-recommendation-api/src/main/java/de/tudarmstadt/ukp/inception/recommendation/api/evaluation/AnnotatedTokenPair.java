/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.api.evaluation;

/**
 * Holds information on an annotated token: its gold label and its predicted label.
 */
public class AnnotatedTokenPair
{
    private final String goldLabel;
    private final String predictedLabel;

    public AnnotatedTokenPair(String aGoldLabel, String aPredictedLabel)
    {
        goldLabel = aGoldLabel;
        predictedLabel = aPredictedLabel;
    }

    public String getGoldLabel()
    {
        return goldLabel;
    }

    public String getPredictedLabel()
    {
        return predictedLabel;
    }

}
