package cr.ac.ucr.ecci.ci1323.core;

import cr.ac.ucr.ecci.ci1323.cache.DataCache;
import cr.ac.ucr.ecci.ci1323.cache.InstructionCache;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.controller.SimulationController;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;

abstract class AbstractCore extends Thread {

    protected DataCache dataCache;
    protected InstructionCache instructionCache;
    protected Context currentContext;
    protected SimulationController simulationController;
    protected int maxQuantum;
    protected int currentQuantum;

    protected AbstractCore (int maxQuantum, Context startingContext, SimulationController simulationController) {

        this.maxQuantum = maxQuantum;
        this.simulationController = simulationController;
        this.currentContext = startingContext;
        this.currentQuantum = 0;
    }

    protected void executeDADDI (Instruction instruction) {
        this.getRegisters()[instruction.getField(2)] = this.getRegisters()[instruction.getField(1)]
                + instruction.getField(3);
    }

    protected void executeDADD (Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                + this.getRegisters()[instruction.getField(2)];
    }

    protected void executeDSUB (Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                - this.getRegisters()[instruction.getField(2)];
    }

    protected void executeDMUL (Instruction instruction) {
        this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                * this.getRegisters()[instruction.getField(2)];
    }

    protected void executeDDIV (Instruction instruction) {
        try {
            this.getRegisters()[instruction.getField(3)] = this.getRegisters()[instruction.getField(1)]
                    / this.getRegisters()[instruction.getField(2)];
        } catch (ArithmeticException e) {
            System.err.println("Executing DDIV error: Division by zero.");
            System.exit(1);
        }
    }

    protected void executeBEQZ (Instruction instruction) {
        if (this.getRegisters()[instruction.getField(1)] == 0) {
            int newPC = this.getPC() + 4 * instruction.getField(3);
            this.setPC(newPC);
        }
    }

    protected void executeBNEZ (Instruction instruction) {
        if (this.getRegisters()[instruction.getField(1)] != 0) {
            int newPC = this.getPC() + 4 * instruction.getField(3);
            this.setPC(newPC);
        }
    }

    protected void executeJAL (Instruction instruction) {
        this.getRegisters()[31] = this.getPC();
        this.setPC(instruction.getField(3));
    }

    protected void executeJR (Instruction instruction) {
        this.setPC(this.getRegisters()[instruction.getField(1)]);
    }

    private int[] getRegisters() {
        return this.currentContext.getRegisters();
    }

    private int getPC() {
        return this.currentContext.getProgramCounter();
    }

    private void setPC(int newPC) {
        this.currentContext.setProgramCounter(newPC);
    }

}
