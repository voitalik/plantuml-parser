package com.shuzijun.plantumlparser.core;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.visitor.VoidVisitor;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * 解析程序
 *
 * @author shuzijun
 */
public class ParserProgram {

    private ParserConfig parserConfig;

    public ParserProgram(ParserConfig parserConfig) {
        this.parserConfig = parserConfig;
    }

    public void execute() throws IOException {
        if(parserConfig.getLanguageLevel()!=null) {
            StaticJavaParser.getConfiguration().setLanguageLevel(parserConfig.getLanguageLevel());
        }else {
            StaticJavaParser.getConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8);
        }
        Set<File> files = this.parserConfig.getFilePaths();

        PUmlView pUmlView = new PUmlView();
        for (File file : files) {
            if (file.getPath().endsWith("java")){
                CompilationUnit compilationUnit = StaticJavaParser.parse(file);
                Optional<PackageDeclaration> packageDeclaration = compilationUnit.getPackageDeclaration();
                VoidVisitor<PUml> classNameCollector = new ClassVoidVisitor(
                        packageDeclaration.map(NodeWithName::getNameAsString).orElse(""), parserConfig);
                classNameCollector.visit(compilationUnit, pUmlView);
            }
        }

        try {
            if (this.parserConfig.getOutFilePath() == null) {
                System.out.println(pUmlView);
            } else {
                File outFile = new File(this.parserConfig.getOutFilePath());

                //Create parent folders if necessary
                File parent = outFile.getParentFile();    //if parent is null, the path only contains the name of the file
                if (parent != null && !parent.exists())
                    parent.mkdirs();
                FileUtils.write(outFile, pUmlView.toString(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            System.out.println(e + "\n" + "outFilePath=" + this.parserConfig.getOutFilePath());
            System.exit(2);
        }

    }

}
