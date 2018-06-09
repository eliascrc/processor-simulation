package cr.ac.ucr.ecci.ci1323.memory;

public class Instruction {

    int[] instructionFields;

    Instruction(int[] instructionFields) {
        this.instructionFields = instructionFields;
    }

    public int[] getInstructionFields() {
        return this.instructionFields;
    }
}
