package cr.ac.ucr.ecci.ci1323.control;

public class MissHandler extends Thread {

/*
    private InstructionBus instructionBus;
    private DataBus dataBus;*/
    private Context context;

    public MissHandler (/*InstructionBus instructionBus, DataBus dataBus,*/ Context context) {
        /*this.instructionBus = instructionBus;
        this.dataBus = dataBus;*/
        this.context = context;
    }

    @Override
    public void run() {
        System.out.println("Miss Handler ready! Context: ");
        this.context.print();
    }

}
