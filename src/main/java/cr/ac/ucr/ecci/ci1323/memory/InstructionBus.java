package cr.ac.ucr.ecci.ci1323.memory;

public class InstructionBus extends Bus {

    private volatile InstructionBlock[] instructionMemory;

    public InstructionBus(InstructionBlock[] instructionMemory) {
        super();
        this.instructionMemory = instructionMemory;
    }

    public InstructionBlock getInstructionBlock(int index) {
        return instructionMemory[index];
    }

    public void setInstructionBlock(int index, InstructionBlock instructionBlock) {
        this.instructionMemory[index] = instructionBlock;
    }

    public void setInstructionMemory(InstructionBlock[] instructionMemory) {
        this.instructionMemory = instructionMemory;
    }

    public Instruction getMemoryInstructionBlockInstruction(int blockNumber, int offset) {
        return this.getInstructionBlock(blockNumber).getInstruction(offset);
    }

    public InstructionBlock[] getInstructionMemory() {
        return this.instructionMemory;
    }

    public void printInstructionMemory() {
        System.out.println(instructionMemory.length);
        for (int i = 0; i < instructionMemory.length; i++) {
            this.instructionMemory[i].printBlock();
        }
    }
}
