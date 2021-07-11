package com.processor.annotations.processor;

import com.processor.annotations.AutoImplement;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.lang.model.type.TypeKind.INT;

public class AutoImplementProcessor extends AbstractProcessor {

    private Messager messager;
    private Filer filer;
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getElementsAnnotatedWith(AutoImplement.class)) {

            if(element.getKind().isInterface()) {
                try {
                    PackageElement packageElement = elementUtils.getPackageOf(element);
                    String packageName = packageElement.getQualifiedName().toString(); // com.practice.repository
                    String className = element.getSimpleName().toString();
                    AutoImplement annotation = element.getAnnotation(AutoImplement.class);
                    if(!"".equals(annotation.name()))
                        className = annotation.name();

                    JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + className + "Impl");
                    // generate code
                    generateCode(sourceFile, element, className);

                } catch (IOException exc) {
                    messager.printMessage(Diagnostic.Kind.ERROR, exc.toString());
                }
            }

        }

        return true;
    }

    private void generateCode(JavaFileObject source, Element element, String className) {

        Objects.requireNonNull(source);
        if (!element.getKind().isInterface())
            throw new RuntimeException("Not interface!");

        try(BufferedWriter out = new BufferedWriter(source.openWriter())) {

            out.write("package " + elementUtils.getPackageOf(element).getQualifiedName().toString() + ";");
            out.newLine();
            out.write("import com.practice.repository." + element.getSimpleName().toString() +  ";");
            out.newLine();
            out.write("public class " + className + "Impl implements " + element.getSimpleName().toString() + " {");
            out.newLine();
            out.write("   public " + className + "Impl(){}");
            out.newLine();
            for (Element enclosedElement : element.getEnclosedElements()) {
                ExecutableElement exeElm = ((ExecutableElement) enclosedElement);

                if (exeElm.getModifiers().contains(Modifier.ABSTRACT)) {

                    List<String> params = new ArrayList<>();
                    List<? extends VariableElement> parameters = exeElm.getParameters();
                    for (VariableElement param : parameters) {
                        if(param.getKind() == ElementKind.PARAMETER) {
                            params.add(param.asType().toString() + " " + param.getSimpleName().toString());
                        }
                    }

                    out.newLine();

                    StringBuffer buffer = new StringBuffer();

                    out.write("   public " +
                            exeElm.getReturnType().toString() + " " +
                            exeElm.getSimpleName().toString() +
                            "(" +
                            String.join(", ", params) +
                            ") {"
                    );

                    out.newLine();
                    switch (exeElm.getReturnType().getKind()) {
                        case BYTE:
                        case SHORT:
                        case INT:
                        case LONG:
                            out.write("     return 0;");
                            break;
                        case FLOAT:
                        case DOUBLE:
                            out.write("     return 0.0;");
                            break;
                        case BOOLEAN:
                            out.write("     return false;");
                            break;
                        case CHAR:
                            out.write("     return '\u0000';");
                            break;
                        case VOID:
                            break;
                        default: out.write("       return null;");
                    }

                    out.newLine();
                    out.write("   }");
                }
            }

            out.newLine();

            out.write("}");

        } catch (IOException exc) {
            messager.printMessage(Diagnostic.Kind.ERROR, exc.toString());
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Stream.of(AutoImplement.class.getName())
                .collect(Collectors.toSet());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

}
