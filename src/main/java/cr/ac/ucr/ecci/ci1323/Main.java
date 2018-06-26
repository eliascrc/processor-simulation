package cr.ac.ucr.ecci.ci1323;

import cr.ac.ucr.ecci.ci1323.controller.SimulationController;

public class Main {

    public static void main(String[] args) {

        try {

            for (int i = 0; i < 1; i++) {
                SimulationController simulationController = new SimulationController(2);
                simulationController.runSimulation();
                Thread.sleep(500);
            }

            for (int i = 0; i < 1; i++) {
                SimulationController simulationController = new SimulationController(13);
                simulationController.runSimulation();
                Thread.sleep(500);
            }

            for (int i = 0; i < 1; i++) {
                SimulationController simulationController = new SimulationController(600);
                simulationController.runSimulation();
                Thread.sleep(500);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
