package cr.ac.ucr.ecci.ci1323.memory;

public class Instruction {

    private int[] instructionFields;

    public Instruction(int[] instructionFields) {
        this.instructionFields = instructionFields;
    }

    public int getField(int fieldNumber) {
        return this.instructionFields[fieldNumber];
    }

    public int getOperationCode() {
        return this.instructionFields[0];
    }

    public int[] getInstructionFields() {
        return this.instructionFields;
    }

    public String toString() {
        return instructionFields[0] + "\t" + instructionFields[1]+ "\t" + instructionFields[2]+ "\t" + instructionFields[3];
    }
}
