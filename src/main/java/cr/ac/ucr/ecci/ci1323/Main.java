package cr.ac.ucr.ecci.ci1323;

import cr.ac.ucr.ecci.ci1323.controller.SimulationController;

/**
 * Creates a new SimulationController and starts the simulation of the MIPS processor.
 *
 * @author Josue Leon Sarkis, Elias Calderon, Daniel Montes de Oca
 */
public class Main {

    public static void main(String[] args) {
        SimulationController simulationController = new SimulationController();
        simulationController.runSimulation();

    }

}
