package simulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import events.EventManager;
import events.Pauser;
import events.TickPause;
import gui.MigFrame;

/**
 * Migrationscape version 2.2
 * A version of the Schelling segregation model with adaptive tolerance.
 * Author: Linda Urselmans
 * University of Essex
 *  **/

public class SimController extends Observable implements Runnable, Observer
{

	static Simulation simulation;
	Pauser pauser = new Pauser();
	static int tempLine[] = { -1, -1, -1, -1, -1, -1 };
	static int NUM_SIMULATIONS = -1;
	static int TICKS = -1;
	static int START_DENSITY = -1;
	static int FINAL_DENSITY = -1;
	static int INFLUX = -1; // 0 = OFF
	static int INFLUX_PC = -1;
	static int POPULATING_RULE = -1;
	static int T1 = -1;
	static int T2 = -1;
	static int W = -1;
	static int NB = 3;
	static double M = -1;
	static ArrayList<int[]> setupList;
	static int[] modi;
	static int THRESHOLD_RULE; // 0 normal, 1 uniform, 2 F1F2, 3 F0
	MigFrame frame;
	private Thread game;
	static EventManager event;
	static SimController controller;
	boolean isPaused = false;
	static boolean isUIOFF = false;
	static int[] simPara;
	static int[] mapPara;
	static int[] influxPara;
	static int[] addedPara;
	static int sd1 = 0;
	static int sd2 = 0;
	private static int tick;

	public SimController(int[] simPara, int[] mapPara, int[] influxPara, int[] addedPara)
	{
		event = new EventManager();
		NUM_SIMULATIONS = simPara[0];
		TICKS = simPara[1];
		START_DENSITY = mapPara[0];
		FINAL_DENSITY = mapPara[1];
		INFLUX = influxPara[0];
		INFLUX_PC = influxPara[1];
		POPULATING_RULE = 0; // 0=random, 1=segregation
		W = addedPara[0];
		M = addedPara[1];
		modi = new int[] { START_DENSITY, INFLUX, INFLUX_PC, THRESHOLD_RULE };
	}

	public static void runRule(int rule, int sims) throws IOException
	{

		for (int i = 0; i < sims; i++)
		{
			if (rule == 0) //manual setup, for testing
			{
				simPara = new int[] { sims, 100 }; //numSim, numTicks 
				mapPara = new int[] { 50, 99 }; //green ratio, final-density
				influxPara = new int[] { 0, 0 }; //influxONOFF, influxcount
				addedPara = new int[] { 30, 1 }; // w, m 
			}
			else if (rule == 1) //random effects setup. randomise parameters.
			{
				int numrep = sims; //number of times a new setup gets created
				int numtick = 1000;
				int influx = -2;
				int influxcount = -2;
				int sd = randInt(2, 98); //starting density or rather, number of greens
				int td = randInt(75, 98); //target density
				int m = randInt(1, 40); // 10Oct16: changed to max of 40% rather than 99% for theoretical reasons
				int[] infcoptions = { 1, 4, 15, 100 };
				int isInfluxOn = randInt(0, 4);
				int w = randInt(25, 125);

				if (isInfluxOn != 0) // influx ON should be more likely. 4 influx possibilities and 1 non-possibility.
				{
					influx = 1; //ON
					influxcount = infcoptions[randInt(0, 3)]; // <- always pick 0th elements as array gets shuffled beforehand
				}
				else
				{
					influx = 0; //OFF
					influxcount = 0; // <- set to 0
				}

				simPara = new int[] { numrep, numtick }; //numSim, numTicks
				mapPara = new int[] { sd, td }; //starting-density, final-density
				influxPara = new int[] { influx, influxcount }; //influxONOFF, influxcount
				addedPara = new int[] { w, m }; // w, m. (considered tiles, tolerance de/increment
			}
			else
			{
				System.out.println("<<runRule>> runRule not 1 or 2. It's " + rule);
			}
			tick = i;
			long startTime = System.currentTimeMillis();
			SimController controller = new SimController(simPara, mapPara, influxPara, addedPara);
			event.addObserver(controller);

			String[] inf = { "OFF", "ON" };
			Date now = new Date();
			String startstr = i + 1 + "/" + simPara[0] + " sim started at " + now + ". Params: " + "GR" + mapPara[0] + "/FD" + FINAL_DENSITY + "/I-" + inf[INFLUX] + "/" + influxPara[1] + "/M" + M
					+ "/W" + W;
			System.out.println(startstr);

			controller.run();

			long estimatedTime = System.currentTimeMillis() - startTime;
			long minutes = (estimatedTime / 1000) / 60;
			int seconds = (int) (estimatedTime / 1000) % 60;
			now = new Date();
			System.out.println(i + 1 + "/" + simPara[0] + " ended at " + now + ". Duration: " + minutes + "m" + seconds + "s" + " (" + estimatedTime + ")");

		}
		System.exit(0);
	}

	public static void main(String[] args) throws IOException
	{
		isUIOFF = false; //true=UI is off; false = UI is on
		int runrule = 0; //0=manual, 1=random
		int simn = 1; //number of simulations
		runRule(runrule, simn);
	}

	@Override
	public void run()
	{
		Random prng = new Random();
		try
		{
			simulation = new Simulation(simPara, mapPara, influxPara, addedPara, prng, pauser);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		game = new Thread(simulation);
		simulation.getWorld().addObserver(simulation);
		simulation.addObserver(simulation.getWorld());

		if (isUIOFF)
		{
			game.start();
			simulation.initAppeal();
			simulation.run(tick);

		}

		else
		{
			MigFrame frame = new MigFrame(simulation.getWorld(), event, modi);
			frame.open();
			simulation.addObserver(frame.getUserControls());
			simulation.addObserver(frame.getParameters());
			game.start();
			simulation.initAppeal();
			simulation.run(tick);
			frame.dispose();
		}
	}


	@Override
	public void update(Observable o, Object arg)
	{
		if (arg instanceof TickPause)
		{
			setPause();
		}
	}

	public void fireEvent(Object event)
	{
		setChanged();
		notifyObservers(event);
	}

	public void setPause()
	{
		if (isPaused == true)
		{
			pauser.resume();
			isPaused = false;
		}
		else
		{
			pauser.pause();
			isPaused = true;
		}
	}

	public static int randInt(int min, int max)
	{

		Random rand = new Random();
		// nextInt is normally exclusive of the top value, so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;
		return randomNum;
	}
}
