package cr.ac.ucr.ecci.ci1323.memory;

public class InstructionBlock {

    private volatile Instruction[] instructions;

    public InstructionBlock(Instruction[] instructions) {
        this.instructions = instructions;
    }

    public Instruction getInstruction(int index) {
        return instructions[index];
    }

    public void printBlock() {
        for (int i = 0; i < instructions.length; i++) {
            if (instructions[i] != null)
                System.out.print(instructions[i].toString() + "\t");
        }
    }
}
