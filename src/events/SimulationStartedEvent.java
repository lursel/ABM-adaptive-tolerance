package events;
/**
 * Migrationscape version 2.2
 * A version of the Schelling segregation model with adaptive tolerance.
 * Author: Linda Urselmans
 * University of Essex
 *  **/
public class SimulationStartedEvent
{
	int simNumber;
	int w;
	double m;
	int iN; //number of influxes
	int iS; //size of each influx

	public SimulationStartedEvent(int i, int we, double me, int inno, int insz)
	{
		simNumber = i;
		w = we;
		m = me;
		iN = inno;
		iS = insz;
	}

	public int getSimNumber()
	{
		//System.out.println("simNumber:" + simNumber + ". I'm in the SimulationStartedEvent class.");
		return simNumber;
	}



	public int getW()
	{
		return w;
	}

	public double getM()
	{
		return m;
	}
	
	public int getiN()
	{
		return iN;
	}
	
	public int getiS()
	{
		return iS;
	}
}
