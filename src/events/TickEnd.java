package events;
/**
 * Migrationscape version 2.2
 * A version of the Schelling segregation model with adaptive tolerance.
 * Author: Linda Urselmans
 * University of Essex
 *  **/
public class TickEnd
{
	int[] populationData;
	int tick;
	double[] BGthresh = {0,0};
	double globalhapp;
	double tmC;
	double tmT;
	public TickEnd(int newTick, double averageTB, double averageTG, double globalHappiness, double mC, double mT)
	{
		this.tick = newTick;
		//this.populationData = newPopulation;
		BGthresh[0] = averageTB;
		BGthresh[1] = averageTG;
		globalhapp = globalHappiness;
		tmC = mC;
		tmT = mT;
	}

	public int[] getPopulationData()
	{
		return populationData;
	}

	public int getTick()
	{
		return tick;
	}

	public double getMC()
	{
		return tmC;
	}
	
	public double getMT()
	{
		return tmT;
	}
	
	public double[] getBGThresh()
	{
		return BGthresh;
	}
	
	public double getGHapp()
	{
		return globalhapp;
	}
}
