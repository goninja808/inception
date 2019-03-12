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
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.render;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Offset;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Span;

/**
 * Intermediate representation / wrapper for VSpan, Span and Offset.
 * Used for converting representations of INCEpTION and PDFAnno
 */
public class RenderSpan
{

    private VSpan vSpan;
    private Span span;
    private String text;
    private int begin;
    private int end;
    private String windowBeforeText;
    private String windowAfterText;

    public RenderSpan()
    {
        this.vSpan = null;
        this.span = null;
        text = "";
        begin = 0;
        end = 0;
        windowBeforeText = "";
        windowAfterText = "";
    }

    public RenderSpan(Offset offset)
    {
        super();
        // search for begin of the first range and end of the last range
        begin = offset.getBegin();
        end = offset.getEnd();
    }

    public RenderSpan(VSpan aVSpan, Span aSpan)
    {
        super();
        this.vSpan = aVSpan;
        this.span = aSpan;
        // search for begin of the first range and end of the last range
        begin = vSpan.getRanges().stream().mapToInt(VRange::getBegin).min().getAsInt();
        end = vSpan.getRanges().stream().mapToInt(VRange::getEnd).max().getAsInt();
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setBegin(int begin)
    {
        this.begin = begin;
    }

    public void setEnd(int end)
    {
        this.end = end;
    }

    public void setWindowBeforeText(String windowBeforeText)
    {
        this.windowBeforeText = windowBeforeText;
    }

    public void setWindowAfterText(String windowAfterText)
    {
        this.windowAfterText = windowAfterText;
    }

    public VSpan getVSpan()
    {
        return vSpan;
    }

    public Span getSpan()
    {
        return span;
    }

    public String getText()
    {
        return text;
    }

    public String getTextWithWindow()
    {
        return windowBeforeText + text + windowAfterText;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public String getWindowBeforeText()
    {
        return windowBeforeText;
    }

    public String getWindowAfterText()
    {
        return windowAfterText;
    }

}
