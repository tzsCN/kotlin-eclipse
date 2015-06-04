/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.jetbrains.kotlin.jv;

import java.io.IOException;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseExternalAnnotationResolver;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElementFactory;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseMethodSignatureChecker;
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseTraceBasedJavaResolverCache;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaPropertyInitializerEvaluator;
import org.jetbrains.kotlin.generators.di.DependencyInjectorGenerator;
import org.jetbrains.kotlin.generators.di.DiType;
import org.jetbrains.kotlin.generators.di.Expression;
import org.jetbrains.kotlin.generators.di.GivenExpression;
import org.jetbrains.kotlin.generators.di.InjectorGeneratorUtil;
import org.jetbrains.kotlin.load.java.JavaFlexibleTypeCapabilitiesProvider;
import org.jetbrains.kotlin.load.java.components.TraceBasedErrorReporter;
import org.jetbrains.kotlin.load.java.components.TraceBasedExternalSignatureResolver;
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver;
import org.jetbrains.kotlin.load.java.sam.SamConversionResolverImpl;
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava;
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinder;
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory;
import org.jetbrains.kotlin.load.kotlin.KotlinJvmCheckerProvider;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer;
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzerForTopLevel;
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver;
import org.jetbrains.kotlin.resolve.jvm.JavaLazyAnalyzerPostConstruct;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.lazy.ScopeProvider;
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory;

import com.intellij.psi.search.GlobalSearchScope;

public class InjectorsGenerator {
    
    private final DependencyInjectorGenerator generator;
    
    private InjectorsGenerator(String targetSourceRoot, String injectorPackageName, 
            String injectorClassName) {
        generator = new DependencyInjectorGenerator();
        generator.configure(targetSourceRoot, injectorPackageName, injectorClassName, "org.jetbrains.kotlin.core.injectors.InjectorsGenerator");
    }
    
    public static InjectorsGenerator generatorForTopDownAnalyzerForJvm() {
    	InjectorsGenerator injectorsGenerator = new InjectorsGenerator("../kotlin-eclipse-core/src", 
    			"org.jetbrains.kotlin.core.injectors", "EclipseInjectorForTopDownAnalyzerForJvm");
    	injectorsGenerator.configureGeneratorForTopDownAnalyzerForJvm();
    	
    	return injectorsGenerator;
    }
    
    public void generate() throws IOException {
    	generator.generate();
    }
    
    private void configureGeneratorForTopDownAnalyzerForJvm() {
    	addPublicParameter(ModuleContext.class, true);
    	addParameter(IJavaProject.class, false);
    	addParameter(BindingTrace.class, false);
    	addParameter(DeclarationProviderFactory.class, false);
    	
    	addPublicField(ResolveSession.class);
    	addField(ScopeProvider.class);
    	
    	addParameter(GlobalSearchScope.class, "moduleContentScope");

    	addPublicField(LazyTopDownAnalyzer.class);
    	addPublicField(LazyTopDownAnalyzerForTopLevel.class);
        addPublicField(JavaDescriptorResolver.class);
        addPublicField(DeserializationComponentsForJava.class);
        
        addField(JvmVirtualFileFinder.class, 
                new GivenExpression(JvmVirtualFileFinderFactory.class.getName() 
                		+ ".SERVICE.getInstance(project).create(moduleContentScope)"));
        
        addFields(
        		EclipseJavaClassFinder.class, 
                TraceBasedExternalSignatureResolver.class,
                EclipseTraceBasedJavaResolverCache.class, 
                TraceBasedErrorReporter.class,
                EclipseMethodSignatureChecker.class, 
                EclipseExternalAnnotationResolver.class,
                EclipseJavaPropertyInitializerEvaluator.class,
                SamConversionResolverImpl.class,
                EclipseJavaSourceElementFactory.class, 
                SingleModuleClassResolver.class,
                JavaLazyAnalyzerPostConstruct.class,
                JavaFlexibleTypeCapabilitiesProvider.class);
        
        addField(KotlinJvmCheckerProvider.class, true);
        
        addField(JvmVirtualFileFinder.class, new GivenExpression(JvmVirtualFileFinder.class.getName()
                + ".SERVICE.getInstance(project)"));
    }
    
    private void addPublicField(Class<?> fieldType) {
        generator.addField(true, new DiType(fieldType), getDefaultName(fieldType), null, false);
    }
    
    private void addField(Class<?> fieldType) {
    	addField(fieldType, false);
    }
    
    private void addField(Class<?> fieldType, boolean useAsContext) {
    	generator.addField(false, new DiType(fieldType), getDefaultName(fieldType), null, useAsContext);
    }
    
    private void addField(Class<?> fieldType, Expression init) {
        generator.addField(false, new DiType(fieldType), getDefaultName(fieldType), init, false);
    }
    
    private void addFields(Class<?>... fieldsTypes) {
        for (Class<?> field : fieldsTypes) {
            addField(field);
        }
    }
    
    private void addParameter(Class<?> parameterType, boolean useAsContext) {
        generator.addParameter(false, new DiType(parameterType), getDefaultName(parameterType), true, useAsContext);
    }
    
    private void addParameter(Class<?> parameterType, String name) {
    	addPublicParameter(parameterType, name, false);
    }
    
    private void addPublicParameter(Class<?> parameterType, String name, boolean useAsContext) {
    	generator.addParameter(true, new DiType(parameterType), name, true, useAsContext);
    }
    
    private void addPublicParameter(Class<?> parameterType, boolean useAsContext) {
    	addPublicParameter(parameterType, getDefaultName(parameterType), useAsContext);
    }
    
    private String getDefaultName(Class<?> entityType) {
        return InjectorGeneratorUtil.var(new DiType(entityType));
    }
}