package demo.project.gradle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExampleTest {

    @Test
    public void test() {
        final ExampleClass exampleClazz = new ExampleClass();
        exampleClazz.calleeMethod();
        assertNotNull(exampleClazz);
    }

}
