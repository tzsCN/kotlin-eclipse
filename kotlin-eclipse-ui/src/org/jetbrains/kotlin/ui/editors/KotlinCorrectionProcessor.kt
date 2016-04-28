/*******************************************************************************
* Copyright 2000-2016 JetBrains s.r.o.
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
*
*******************************************************************************/
package org.jetbrains.kotlin.ui.editors

import java.util.ArrayList
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.runtime.CoreException
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.text.source.IAnnotationModel
import org.eclipse.ui.IMarkerResolution
import org.eclipse.ui.ide.IDE
import org.eclipse.ui.texteditor.AbstractTextEditor
import org.eclipse.ui.texteditor.IDocumentProvider
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProcessor
import com.google.common.collect.Lists
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal
import org.eclipse.ui.texteditor.MarkerAnnotation
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotation
import org.jetbrains.kotlin.ui.editors.annotations.endOffset
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolutionGenerator
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolution
import org.eclipse.swt.graphics.Image
import org.eclipse.jface.text.IDocument
import org.eclipse.swt.graphics.Point
import org.eclipse.jface.text.contentassist.IContextInformation

class KotlinCorrectionProcessor(val editor: KotlinFileEditor) : IQuickAssistProcessor {
    
    override fun getErrorMessage(): String? = null
    
    override fun canFix(annotation: Annotation): Boolean {
        return annotation is MarkerAnnotation && IDE.getMarkerHelpRegistry().hasResolutions(annotation.marker)
    }
    
    override fun canAssist(invocationContext: IQuickAssistInvocationContext): Boolean = true
    
    override fun computeQuickAssistProposals(invocationContext: IQuickAssistInvocationContext): Array<ICompletionProposal> {
        val diagnostics = findDiagnosticsBy(invocationContext)
        val quickFixResolutions = KotlinMarkerResolutionGenerator.getResolutions(diagnostics)
        
        return arrayListOf<ICompletionProposal>().apply { 
            val file = editor.getFile()
            if (file != null) {
                addAll(quickFixResolutions.map { KotlinMarkerResolutionProposal(file, it) })
            }
            
            addAll(KotlinQuickAssistProcessor.getAssists(null, null))
        }.toTypedArray()
    }
}

private class KotlinMarkerResolutionProposal(
        private val file: IFile,
        private val resolution: KotlinMarkerResolution) : ICompletionProposal {
    override fun getImage(): Image? = resolution.image
    
    override fun getAdditionalProposalInfo(): String? = resolution.description
    
    override fun apply(document: IDocument?) {
        resolution.apply(file)
    }
    
    override fun getContextInformation(): IContextInformation? = null
    
    override fun getDisplayString(): String? = resolution.label
    
    override fun getSelection(document: IDocument?): Point? = null
}

private fun findDiagnosticsBy(invocationContext: IQuickAssistInvocationContext): ArrayList<Diagnostic> {
    val offset = invocationContext.offset
    val annotations = arrayListOf<Diagnostic>()
    for (ann in invocationContext.sourceViewer.annotationModel.getAnnotationIterator()) {
        if (ann is DiagnosticAnnotation) {
            if (ann.offset <= offset && offset <= ann.endOffset && ann.diagnostic != null) {
                annotations.add(ann.diagnostic)
            }
        }
    }
    
    return annotations
}