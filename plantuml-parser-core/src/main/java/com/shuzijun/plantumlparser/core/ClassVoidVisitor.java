package com.shuzijun.plantumlparser.core;

import static com.shuzijun.plantumlparser.core.Constant.VisibilityDefault;
import static com.shuzijun.plantumlparser.core.Constant.VisibilityPrivate;
import static com.shuzijun.plantumlparser.core.Constant.VisibilityPublic;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 类
 *
 * @author shuzijun
 */
public class ClassVoidVisitor extends VoidVisitorAdapter<PUml> implements MyVisitor {

    private final String packageName;

    private final ParserConfig parserConfig;

    private final Map<String, String> importMap;

    private PUmlView pUmlView;

    public ClassVoidVisitor(String packageName, ParserConfig parserConfig) {
        this.packageName = packageName;
        this.parserConfig = parserConfig;
        importMap = new HashMap<>();
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration cORid, PUml pUml) {
        if (!(pUml instanceof PUmlView)) {
            super.visit(cORid, pUml);
            return;
        }
        if (parserConfig.isExcludeClass(cORid.getNameAsString())){
            return;
        }
        pUmlView = (PUmlView) pUml;
        PUmlClass pUmlClass = createUmlClass();

        pUmlClass.setClassName(cORid.getNameAsString());
        if (cORid.isInterface()) {
            pUmlClass.setClassType("interface");
        } else {
            pUmlClass.setClassType("class");
            for (Modifier modifier : cORid.getModifiers()) {
                if (modifier.toString().trim().contains("abstract")) {
                    pUmlClass.setClassType("abstract class");
                    break;
                }
            }
        }

        if (parserConfig.isShowComment()) {
            cORid.getComment().ifPresent(comment -> {
                pUmlClass.setClassComment(comment.getContent());
            });
        }

        fillImports(cORid, pUmlClass);

        cORid.getFields().forEach(p -> p.accept(this, pUmlClass));

        if (cORid.getConstructors().isEmpty() && parserConfig.isShowDefaultConstructors() && !cORid.isInterface()) {
            ConstructorDeclaration defaultConstructor = new ConstructorDeclaration(cORid.getNameAsString());
            defaultConstructor.accept(this, pUmlClass);
        } else {
            cORid.getConstructors().forEach(p -> p.accept(this, pUmlClass));
        }

        cORid.getMethods().forEach(p -> p.accept(this, pUmlClass));

        pUmlView.addPUmlClass(pUmlClass);

        addRelation(cORid.getImplementedTypes(), pUmlClass, "<|..");
        addRelation(cORid.getExtendedTypes(), pUmlClass, "<|--");

        super.visit(cORid, pUml);
    }

    private void fillImports(Node cORid, PUmlClass pUmlClass) {
        Optional<Node> parentNode = cORid.getParentNode();
        parentNode.flatMap(node -> parseImport(node, pUmlClass))
                .ifPresent(list -> list.forEach(
                        importDeclaration -> importMap.put(importDeclaration.getName().getIdentifier(),
                        importDeclaration.getName().toString())));
    }

    private void addRelation(NodeList<ClassOrInterfaceType> types, PUmlClass pUmlClass, String relation) {
        types.forEach(type -> {
            String typeName = type.getNameAsString();
            PUmlRelation pUmlRelation = new PUmlRelation();
            pUmlRelation.setTarget(getPackageNamePrefix(pUmlClass.getPackageName()) + pUmlClass.getClassName());
            if (importMap.containsKey(typeName)) {
                if (parserConfig.isShowPackage()) {
                    pUmlRelation.setSource(importMap.get(typeName));
                } else {
                    pUmlRelation.setSource(typeName);
                }
            } else {
                pUmlRelation.setSource(getPackageNamePrefix(pUmlClass.getPackageName()) + typeName);
            }
            pUmlRelation.setRelation(relation);
            pUmlView.addPUmlRelation(pUmlRelation);
        });
    }

    @Override
    public void visit(EnumDeclaration enumDeclaration, PUml pUml) {
        if (!(pUml instanceof PUmlView)) {
            super.visit(enumDeclaration, pUml);
            return;
        }
        pUmlView = (PUmlView) pUml;
        PUmlClass pUmlClass = createUmlClass();

        pUmlClass.setClassName(enumDeclaration.getNameAsString());
        pUmlClass.setClassType("enum");

        if (parserConfig.isShowComment()) {
            enumDeclaration.getComment().ifPresent(comment -> {
                pUmlClass.setClassComment(comment.getContent());
            });
        }

        fillImports(enumDeclaration, pUmlClass);

        enumDeclaration.getEntries().forEach(p -> p.accept(this, pUmlClass));
        enumDeclaration.getFields().forEach(p -> p.accept(this, pUmlClass));
        enumDeclaration.getConstructors().forEach(p -> p.accept(this, pUmlClass));
        enumDeclaration.getMethods().forEach(p -> p.accept(this, pUmlClass));

        pUmlView.addPUmlClass(pUmlClass);

        addRelation(enumDeclaration.getImplementedTypes(), pUmlClass, "<|..");

        super.visit(enumDeclaration, pUml);
    }

    @Override
    public void visit(final RecordDeclaration recordDeclaration, PUml pUml) {
        if (!(pUml instanceof PUmlView)) {
            super.visit(recordDeclaration, pUml);
            return;
        }
        pUmlView = (PUmlView) pUml;
        PUmlClass pUmlClass = createUmlClass();

        pUmlClass.setClassName(recordDeclaration.getNameAsString());
        pUmlClass.setClassType("class");

        if (parserConfig.isShowComment()) {
            recordDeclaration.getComment().ifPresent(comment -> {
                pUmlClass.setClassComment(comment.getContent());
            });
        }

        fillImports(recordDeclaration, pUmlClass);

        // Create a constructor unless a default constructor was provided.
        boolean needsDefaultConstructor = recordDeclaration.getConstructors().stream().noneMatch(c ->
                                c.getParameters().equals(recordDeclaration.getParameters()));
        if (needsDefaultConstructor) {
            // Visibility should be the same as for the class itself.
            NodeList<Modifier> modifiers = new NodeList<>();
            for (Modifier modifier : recordDeclaration.getModifiers()) {
                if (VisibilityUtils.isVisibility(modifier.toString().trim())) {
                    modifiers.add(modifier);
                    break;
                }
            }
            ConstructorDeclaration c = new ConstructorDeclaration(modifiers, pUmlClass.getClassName());
            c.setParameters(recordDeclaration.getParameters());
            c.accept(this, pUmlClass);
        }

        // We need to convert the parameters to private final instance variables.
        Set<Parameter> parameters = new HashSet<>();
        recordDeclaration.getParameters().forEach(p -> {
            parameters.add(p);
            NodeList<Modifier> modifiers = new NodeList<>();
            modifiers.add(Modifier.privateModifier());
            modifiers.add(Modifier.finalModifier());
            FieldDeclaration field = new FieldDeclaration(modifiers, p.getType(), p.getName().asString());
            field.accept(this, pUmlClass);
        });

        // Add constructors and methods, removing parameters from set
        // if there are explicit getters.
        for (BodyDeclaration<?> m: recordDeclaration.getMembers()) {
            if (m instanceof MethodDeclaration) {
                MethodDeclaration md = (MethodDeclaration) m;
                Parameter parm = new Parameter(md.getType(), md.getName());
                parameters.remove(parm);
            }
            m.accept(this, pUmlClass);
        }
        // Add any getters that were not explicitly created.
        for (Parameter p : parameters) {
            MethodDeclaration md = new MethodDeclaration(
                    new NodeList<>(Modifier.publicModifier()), p.getType(), p.getNameAsString());
            md.accept(this, pUmlClass);
        }

        pUmlView.addPUmlClass(pUmlClass);

        addRelation(recordDeclaration.getImplementedTypes(), pUmlClass, "<|..");

        super.visit(recordDeclaration, pUml);
    }

    @Override
    public void visit(FieldDeclaration field, PUml pUml) {
        if (!(pUml instanceof PUmlClass pUmlClass)) {
            super.visit(field, pUml);
            return;
        }

        boolean isDeclaredInInterface = pUmlClass.getClassType().equals("interface");
        // Analyzes modifiers to determine the visibility level for the field declaration
        String visibility = field.getModifiers().stream()
                .map(m -> m.toString().trim())
                .filter(VisibilityUtils::isVisibility)
                .findFirst().orElse(isDeclaredInInterface ? VisibilityPublic : VisibilityDefault);

        if (!parserConfig.isFieldModifier(visibility)) {
            return;
        }

        NodeList<VariableDeclarator> variables = field.getVariables();
        for (int i = 0; i < variables.size(); i++) {
            PUmlField pUmlField = getPUmlField(field, visibility, variables.get(i));
            if (i == 0 && parserConfig.isShowComment()) {
                // Print comment only above the first variable in the declaration
                field.getComment().ifPresent(comment -> pUmlField.setComment(comment.getContent()));
            }
            pUmlClass.addPUmlFieldList(pUmlField);
        }

        // TODO: Add fields relation as Aggregation
        // Do I need to add only classes/interfaces that belong to the project? Do not add standard classes
        // like String, Long, List??? How to handle generics? Map<String, PUmlClass>, add all three or
        // only PUmlClass
        // field.getElementType().ifClassOrInterfaceType( type -> addRelation(new NodeList<>(type), pUmlClass, "o--"));
    }

    /**
     * Creates a PUmlField object representing a field declaration with its properties.
     * <p>
     * Optionally determines the constant value if the field is declared as static and final.
     * It is optional because it has not been tested with long strings or array/collection initializers.
     * The value is set exactly as it appears in the source code, which may be too long for the diagram.
     * </p>
     *
     * @param field      The field declaration to process.
     * @param visibility The visibility modifier of the field.
     * @param variable   The variable declared within the field.
     * @return A PUmlField object populated with field details.
     */
    private PUmlField getPUmlField(FieldDeclaration field, String visibility, VariableDeclarator variable) {
        PUmlField pUmlField = new PUmlField();
        pUmlField.setVisibility(visibility);
        pUmlField.setType(variable.getTypeAsString());
        pUmlField.setName(variable.getNameAsString());
        pUmlField.setStatic(field.isStatic());
        if (parserConfig.isShowConstantValues()) {
            setConstantValue(pUmlField, field, variable);
        }

        return pUmlField;
    }

    /**
     * Sets the constant value of a field if it is declared as static and final.
     * <p>
     * Checks whether the variable has an initializer. If not, it searches static
     * initializer blocks within the parent class or interface for assignment expressions
     * that initialize the field.
     * </p>
     *
     * @param pUmlField The PUmlField object to update with a constant value.
     * @param field     The field declaration containing the variable.
     * @param variable  The variable to check for a constant value.
     */
    private void setConstantValue(PUmlField pUmlField, FieldDeclaration field, VariableDeclarator variable) {
        if (!field.isFinal() || !field.isStatic()) {
            return;
        }

        // Add the value of the constant initializer to pUmlField.
        if (variable.getInitializer().isPresent()) {
            // A simple solution: if the variable has an initializer, just read its value as a string
            pUmlField.setValue(variable.getInitializer().get().toString());
        } else {
            // Otherwise, check all static initializer blocks
            ClassOrInterfaceDeclaration parentNode = (ClassOrInterfaceDeclaration) field.getParentNode().orElseThrow();
            for (BodyDeclaration<?> b : parentNode.getMembers()) {
                if (b.isInitializerDeclaration()) {
                    b.accept(this, pUmlField);
                    // Since the field is a constant, we do not need to check other initializer blocks
                    if (pUmlField.getValue() != null) break;
                }
            }
        }
    }

    @Override
    public void visit(InitializerDeclaration id, PUml pUml) {
        if (!(pUml instanceof PUmlField pUmlField)) {
            super.visit(id, pUml);
            return;
        }

        for (Statement stmt : id.getBody().getStatements()) {
            stmt.ifExpressionStmt(exprStmt ->
                    exprStmt.getExpression().ifAssignExpr(assignExpr -> {
                        if (assignExpr.getTarget().toString().equals(pUmlField.getName())) {
                            pUmlField.setValue(assignExpr.getValue().toString());
                        }
                    })
            );
            // Since the field is a constant, we do not need to check other statements
            if (pUmlField.getValue() != null) return;
        }
    }

    @Override
    public void visit(ConstructorDeclaration constructor, PUml pUml) {
        if (!(pUml instanceof PUmlClass pUmlClass)) {
            super.visit(constructor, pUml);
            return;
        }
        if (!parserConfig.isShowConstructors()) {
            return;
        }

        boolean isEnum = pUmlClass.getClassType().equals("enum");
        // Analyzes modifiers to determine the visibility level for the constructor declaration
        String visibility = constructor.getModifiers().stream()
                .map(m -> m.toString().trim())
                .filter(VisibilityUtils::isVisibility)
                .findFirst().orElse(isEnum ? VisibilityPrivate : VisibilityDefault);

        PUmlMethod pUmlMethod = new PUmlMethod();
        pUmlMethod.setVisibility(visibility);
        if (parserConfig.isMethodModifier(pUmlMethod.getVisibility())) {
            pUmlMethod.setStatic(constructor.isStatic());
            pUmlMethod.setAbstract(constructor.isAbstract());
            pUmlMethod.setReturnType("");
            pUmlMethod.setName(constructor.getNameAsString());
            for (Parameter parameter : constructor.getParameters()) {
                // Parameters now contain their type and name !
                pUmlMethod.addParam(parameter.getNameAsString() + ": " + parameter.getTypeAsString());
            }
            pUmlClass.addPUmlMethodList(pUmlMethod);
        }
        if (parserConfig.isShowComment()) {
            constructor.getComment().ifPresent(comment -> {
                pUmlMethod.setComment(comment.getContent());
            });
        }
    }

    @Override
    public void visit(MethodDeclaration method, PUml pUml) {
        if (!(pUml instanceof PUmlClass)) {
            super.visit(method, pUml);
            return;
        }
        PUmlClass pUmlClass = (PUmlClass) pUml;

        PUmlMethod pUmlMethod = new PUmlMethod();

        if (method.getModifiers().size() != 0) {
            for (Modifier modifier : method.getModifiers()) {
                String modifierStr = modifier.toString().trim();
                if (VisibilityUtils.isVisibility(modifierStr)) {
                    pUmlMethod.setVisibility(modifierStr);
                    break;
                }
            }
        }

        // Fixed: if the method is part of an interface and the visibility is the
        // default one, it should actually be public because interfaces have everything
        // in public by default
        boolean partOfInterface = pUmlClass.getClassType().equals("interface");
        if (partOfInterface && pUmlMethod.getVisibility().equals("default")) {
            pUmlMethod.setVisibility("public");
        }

        if (parserConfig.isMethodModifier(pUmlMethod.getVisibility())) {
            pUmlMethod.setStatic(method.isStatic());
            // Use a body for methods in interfaces if there is no abstract modifier
            pUmlMethod.setAbstract(method.isAbstract() || method.getBody().isEmpty());
            pUmlMethod.setReturnType(method.getTypeAsString());
            pUmlMethod.setName(method.getNameAsString());
            for (Parameter parameter : method.getParameters()) {
                // Parameters now contain their type and name !
                pUmlMethod.addParam(parameter.getNameAsString() + ": " + parameter.getTypeAsString());
            }
            pUmlClass.addPUmlMethodList(pUmlMethod);
        }

        if (parserConfig.isShowComment()) {
            method.getComment().ifPresent(comment -> {
                pUmlMethod.setComment(comment.getContent());
            });
        }
    }

    @Override
    public void visit(EnumConstantDeclaration enumConstantDeclaration, PUml pUml) {
        if (!(pUml instanceof PUmlClass)) {
            super.visit(enumConstantDeclaration, pUml);
            return;
        }
        PUmlClass pUmlClass = (PUmlClass) pUml;
        PUmlField pUmlField = new PUmlField();

        pUmlField.setName(enumConstantDeclaration.getNameAsString());
        pUmlField.setType("");
        pUmlField.setVisibility("public");
        pUmlClass.addPUmlFieldList(pUmlField);

        if (parserConfig.isShowComment()) {
            enumConstantDeclaration.getComment().ifPresent(comment -> {
                pUmlField.setComment(comment.getContent());
            });
        }
    }

    private Optional<NodeList<ImportDeclaration>> parseImport(Node node, PUmlClass pUmlClass) {
        if (node instanceof CompilationUnit cu) {
            return Optional.ofNullable(cu.getImports());
        } else if (node instanceof ClassOrInterfaceDeclaration || node instanceof RecordDeclaration) {
            pUmlClass.setClassName(((TypeDeclaration<?>) node).getNameAsString() + "$" + pUmlClass.getClassName());

            Node parentNode = node.getParentNode().get();
            if (parentNode instanceof CompilationUnit) {
                PUmlRelation pUmlRelation = new PUmlRelation();
                pUmlRelation.setTarget(getPackageNamePrefix(pUmlClass.getPackageName()) + pUmlClass.getClassName());
                pUmlRelation.setSource(getPackageNamePrefix(pUmlClass.getPackageName()) + pUmlClass.getClassName().substring(0, pUmlClass.getClassName().lastIndexOf("$")));
                pUmlRelation.setRelation("+--");	// Fixed: Intern class should have an association of +-- not +..
                pUmlView.addPUmlRelation(pUmlRelation);
            }
            parseImport(parentNode, pUmlClass);
        }
        return Optional.empty();
    }


    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public ParserConfig getParserConfig() {
        return parserConfig;
    }
}
