package demo.project.gradle;

public class Callee {

    protected void method1() {
        innerMethod();
    }

    private void innerMethod() {
        try {
            Thread.sleep(1);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

}
