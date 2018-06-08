package cr.ac.ucr.ecci.ci1323.core;

public class Context {

    private int programCounter;
    private int[] registers;
    private int executionTics;

    public Context() {

    }

    public int getProgramCounter() {
        return programCounter;
    }

    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
    }

    public int[] getRegisters() {
        return registers;
    }

    public void setRegisters(int[] registers) {
        this.registers = registers;
    }

    public int getExecutionTics() {
        return executionTics;
    }

    public void setExecutionTics(int executionTics) {
        this.executionTics = executionTics;
    }
}
