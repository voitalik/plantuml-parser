package com.shuzijun.plantumlparser.core;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.javaparser.ParserConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestCore {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    private static List<String> getUmlAsLines(ParserConfig parserConfig, ByteArrayOutputStream os) throws IOException {
        ParserProgram parserProgram = new ParserProgram(parserConfig);
        parserProgram.execute();
        return removeEmptyLines(os.toString().lines());
    }

    private static List<String> removeEmptyLines(Stream<String> lines) {
        return lines.filter(Predicate.not(String::isEmpty)).toList();
    }

    private static File getFile(String path) {
        URL url = ParserProgram.class.getClassLoader().getResource(path);
        if (url == null) {
            throw new IllegalStateException();
        }
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Test interface")
    void testInterface() throws Exception {
        ParserConfig parserConfig = new ParserConfig();
        parserConfig.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        parserConfig.addFieldModifier("public");
        parserConfig.addMethodModifier("public");
        parserConfig.setShowComment(true);
        parserConfig.setShowConstructors(true);
        parserConfig.setShowDefaultConstructors(true);
        runTest(parserConfig, "Interface.java", "Interface_expected1.txt");
    }

    @Test
    @DisplayName("Display constant initializers")
    void testConstantValue() throws Exception {
        ParserConfig parserConfig = new ParserConfig();
        parserConfig.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        parserConfig.addFieldModifier("public");
        parserConfig.setShowConstantValues(true);
        runTest(parserConfig, "Interface.java", "Interface_expected2.txt");
    }

    @Test
    @DisplayName("Display enum instances and constructor")
    void testEnumOutput() throws Exception {
        ParserConfig parserConfig = new ParserConfig();
        parserConfig.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        parserConfig.addFieldModifier("*");
        parserConfig.addMethodModifier("*");
        parserConfig.setShowConstructors(true);
        runTest(parserConfig, "Enum.java", "Enum_expected1.txt");
    }

    @Test
    @DisplayName("Handle multiple fields")
    void testMultipleFields() throws Exception {
        ParserConfig parserConfig = new ParserConfig();
        parserConfig.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        parserConfig.addFieldModifier("*");
        runTest(parserConfig, "MultipleFields.java", "MultipleFields_expected.txt");
    }

    private void runTest(ParserConfig parserConfig, String actualPath, String expectedPath) throws IOException {
        String baseFolder = "test_core/";
        File actualFile = getFile(baseFolder + actualPath);
        parserConfig.addFilePath(actualFile.getAbsolutePath());

        List<String> actualLines = getUmlAsLines(parserConfig, outputStream);

        File expectedFile = getFile(baseFolder + expectedPath);
        List<String> expectedLines = removeEmptyLines(Files.readAllLines(expectedFile.toPath()).stream());

        assertLinesMatch(expectedLines, actualLines);
    }
}
