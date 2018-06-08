package cr.ac.ucr.ecci.ci1323.control;

public class Context {

    private int programCounter;
    private int[] registers;
    private int executionTics;

    public Context(int programCounter, int[] registers) {
        this.programCounter = programCounter;
        this.registers = registers;
        this.executionTics = 0;
    }

    public void print() {
        System.out.print("Context: PC = " + this.programCounter
                + ", Registers = { ");
        for (int register : this.registers) {
            System.out.print(register + ", ");
        }
        System.out.println("} , Tics = " + this.executionTics);
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
