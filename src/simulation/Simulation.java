package simulation;

import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import com.opencsv.CSVWriter;

import events.AgentMovedEvent;
import events.Pauser;
import events.SimulationFinishedEvent;
import events.SimulationStartedEvent;
import events.TickEnd;
import events.TickStart;
import gui.Grid;
import migscape.Agent;
import migscape.Tile;
import migscape.World;

/**
 * Migrationscape version 2.2
 * A version of the Schelling segregation model with adaptive tolerance.
 * Author: Linda Urselmans
 * University of Essex
 *  **/
public class Simulation extends Observable implements Observer, Runnable
{
	double[] data = { 0, 0, 0, 0 }; // same, different, empty, happiness
	int sizeX = -1; // world size X axis
	int sizeY = -1; // world size Y axis
	int numBlues = -1; // total number of Blues
	int numGreens = -1; // total number of Greens
	int g; // start density and green ratio
	int fd; //final density
	double meanBlue = 0;
	int numAgents; // total num of agents, blue + green
	int agentsTarget;
	int agentsStart;
	int agentsGreen;
	int agentsBlue;
	double mC = 0; // morans I of colour
	double mT = 0; // morans I of tolerance
	int maxTicks = 0; // total number of ticks (rounds)
	int agentPercent = -1; // percentage of the world covered by agents
	int noOfSims = -1; // number of simulations
	int w = -1; // max number of tiles that an agent will consider.
	double m = -1; // tolerance in/decrement
	Double globalMoveCounter;
	double globalHappiness = 0.0;
	double globalHappinessG = 0.0;
	double globalHappinessB = 0.0;
	double averageTB = 0.0;
	double averageTG = 0.0;
	int[] errorcounter = { 0, 0 }; // blue agent error. green agent error
	int[] totalNBHforGreens = { 0, 0, 0 };
	int[] totalNBHforBlues = { 0, 0, 0 };
	CSVWriter writer;
	int prefcMcounter = 0;
	String csvname = "";
	ArrayList<Agent> allAgents; // list of all agents		
	ArrayList<Tile> emptyTiles;
	World world;
	Random prng;
	Grid grid;
	Integer tick;
	Pauser pauser;
	int[] population = { 0, 0 }; // array storing numbers of blues [0] and greens[1]
	int[][] csvAppealValues;
	int totalInfluxMax;
	DecimalFormat df4 = new DecimalFormat("#.####");
	DecimalFormat df2 = new DecimalFormat("#.##");
	DecimalFormat df0 = new DecimalFormat("#");
	boolean utilityOn = true;
	int changedMind = 0;
	int influx = -1;
	private static final int SAMPLING_INTERVAL = 10;
	int fluxcounter = 0;
	private int NUM_FLUXES = -2;
	private ArrayList<int[]> TICKS_OF_FLUX = new ArrayList<int[]>();
	int current_size = -1;
	int maxfluxsize;
	int setupMode = -999;

	List<int[]> floodFillResults;
	List<int[]> influxPositions;
	List<String[]> values = new ArrayList<String[]>();
	List<Integer> ticksHappinessList = new ArrayList<Integer>();
	int[] thresholdDistribution = { 0, 0, 0, 0, 0 };
	String currentDirectory = "";

	/**
	 * This is the constructor. 
	 */
	public Simulation(int[] simPara, int[] mapPara, int[] influxPara, int[] addedPara, Random prngFromController, Pauser pauser) throws IOException
	{
		// non-parameters
		sizeX = 50; // change the grid size here.
		sizeY = 50;

		// parameters
		w = addedPara[0];
		m = addedPara[1]; //divide by 10 or 100 to decrease m (rate of change of tolerance)
		g = mapPara[0];
		fd = mapPara[1];
		int[] agents = calculateNatAndMigAgents(influxPara[0], g, fd); //returns: target, start, green, blue
		agentsTarget = agents[0]; // maximum number of agents that simulation has to reach
		agentsStart = agents[1];
		agentsGreen = agents[2];
		agentsBlue = agents[3];

		noOfSims = simPara[0];
		maxTicks = simPara[1];
		numAgents = agentsStart; 
		maxfluxsize = agentsTarget - agentsStart; // should be 1200 if targetagents is 2450 and numagents 1250
		totalInfluxMax = (sizeX * sizeY) - numAgents;

		if (influxPara[0] == 1) // if influx is turned on, set the influx pc of population
		{
			setupMode = 0;
			influx = influxPara[0];
			NUM_FLUXES = influxPara[1];
			TICKS_OF_FLUX = calculateInfluxWHEN(NUM_FLUXES);

			print("<<Simulation.constructor>> the ticks at which to flux are: ");
			for (int i = 0; i < TICKS_OF_FLUX.size(); i++)
			{
				System.out.print(TICKS_OF_FLUX.get(i)[0] + ", ");
			}
			print(" totalling " + TICKS_OF_FLUX.size() + " influxes.");

		}
		else
		{
			setupMode = 2;
			influx = influxPara[0];
		}

		prng = prngFromController;
		this.pauser = pauser;

		floodFillResults = new ArrayList<int[]>();
		influxPositions = new ArrayList<int[]>();
		if (sizeX * sizeY < numAgents)
		{
			print("<<Simulation.constructor>>There are too many agents for the size of the grid. Number of Agents: " + numAgents + ", Number of tiles: " + sizeX * sizeY);
			System.exit(-1);
		}
		allAgents = new ArrayList<Agent>();
		emptyTiles = new ArrayList<Tile>();
		world = new World(sizeX, sizeY);
		populateMap();
		grid = new Grid(world, false);
		collectEmptyTiles();
		globalMoveCounter = 0.0;
		collectPopulationData();
	}

	/**
	 * This is the ratio-calculation method from MigScape1.1, adapted to
	 * MigScape2.1. The difference is that tolerance levels are
	 * adaptive. 
	 * @param g
	 *            = ratio of greens in percentage of presumed 100% pop. So g =
	 *            30 means, at the end of the simulation, greens should
	 *            constitute 30% of the population.
	 * @param fd
	 *            = final density. Final density is given in % (0-100) and
	 *            converted into actual agent numbers. We calculate the number
	 *            of greens using fd and g, and then simply subtract finalagents
	 *            - startingagents to get the migrating agents, mg_agents.
	 */
	public int[] calculateNatAndMigAgents(int isInfluxOn, int g, int fd)
	{
		//print("<calculateNatAndMigAgents> g/fd are " + g + "/" + fd + ".");
		int[] agentnumbers = new int[4]; // contains fd_agents, sd_agents, gr_agents, bl_agents
		int onePC_agents = (sizeX * sizeY) / 100;// 1% = 25 agents.
		int fd_agents = 0;
		int sd_agents = 0;
		int gr_agents = 0;
		int bl_agents = 0;

		if (isInfluxOn == 1)
		{
			fd_agents = fd * onePC_agents; // i.e. 98 * 25 = 2450
			sd_agents = (fd_agents * g) / 100; // i.e. (2450 * 30)/100 = 735
			gr_agents = sd_agents;
			bl_agents = fd_agents - gr_agents; // i.e 2450 - 735 = 1715
		}

		else
		{
			fd_agents = fd * onePC_agents; // i.e. 98 * 25 = 2450
			gr_agents = (fd_agents * g) / 100;
			bl_agents = fd_agents - gr_agents;
			sd_agents = fd_agents;
			//this.g = fd; //green ratio set to final density to ensure correct csv name
		}

		agentnumbers[0] = fd_agents;
		agentnumbers[1] = sd_agents;
		agentnumbers[2] = gr_agents;
		agentnumbers[3] = bl_agents;

		//print("<calculateNatAndMigAgents> Influx is " + isInfluxOn + ".");
		//print("<calculateNatAndMigAgents> fd/sd/gr/bl: " + fd_agents + "/" + sd_agents + "/" + gr_agents + "/" + bl_agents + ".");
		return agentnumbers;
	}

	public double generateThreshold()
	{
		double thresh = -99;
		{
			Random rng = new Random();
			thresh = rng.nextDouble();
			thresh = thresh * 100;
			if (thresh < 0.0 || thresh > 100)
			{
				thresh = 50;
			}
		}
		return thresh;
	}

	/**
	 * Method to add the agents to the map.
	 * In MigScape2.1, we take a ratio argument for greens and a final density argument.
	 * 
	 * There are 2 major differences to account for:
	 * 1. Is influx on or not (determined by setupMode 0 and 2 respectively).
	 * 2. Are tolerance levels fixed or randomised (determined by TR0-1 and 2-3 respectively).
	 * 
	 * When Influx is OFF, the final density is also the starting density, and the number of greens is calculated from the final density.
	 * When Influx is ON, the number of greens is also the starting density, and the gap will be filled by blues. 
	 * Example:
	 * GR 90, FD 70
	 * INF OFF: SD = FD; Greens are 90% OF the 70%
	 * INF ON: SD = Greens; i.e. 90% of what will be 70%, the remaining 10% will migrate later.
	 **/
	public void populateMap() throws IOException
	{
		if (setupMode == 2)//influx OFF
		{

			for (int i = 0; i < agentsTarget; i++)
			{
				int x = prng.nextInt(sizeX);
				int y = prng.nextInt(sizeY);
				boolean isBlue = false; // default is, agents are green
				double newAgentTolerance = generateThreshold(); // uniform
				if (i > agentsGreen)
				{
					isBlue = true; //under SC, the number of greens and blues is determined at the start
				}
				allAgents.add(new Agent(x, y, isBlue, newAgentTolerance, 2));
				Agent agentPara = allAgents.get(i);

				Tile currTile = world.getTile(agentPara.posX, agentPara.posY);

				if (currTile.hasAgent() == true) // if the tile with the same position as the agent already has an agent, then ...
				{
					// print("<<populateMap>> Tile with Position " + positionPara.getX() + ", " + positionPara.getY() + " already exists.");
					boolean hasFoundTile = false;
					while (hasFoundTile == false)
					{
						x = prng.nextInt(world.getSizeX()); // generate x-value between 0 and 50 if world.SizeX=50
						y = prng.nextInt(world.getSizeY()); // generate y-value between 0 and 50 if world.SizeY=50

						Tile findRightTile = world.getTile(x, y); // random tile
						if (findRightTile.hasAgent() == false) // check if it has agent: has not? then
						{
							allAgents.get(i).setPosition(x, y); // give agent that position
							hasFoundTile = true;
						}
					}
				}
				world.placeAgentOnTile(agentPara.posX, agentPara.posY, isBlue, agentPara.isHappy, newAgentTolerance);
			}

		}
		else if (setupMode == 0) //influx ON
		{
			for (int i = 0; i < agentsStart; i++)
			{
				int x = prng.nextInt(sizeX);
				int y = prng.nextInt(sizeY);
				boolean isBlue = false; // default is, agents are green
				double newagenttolerance = generateThreshold(); // uniform

				allAgents.add(new Agent(x, y, isBlue, newagenttolerance, 2));
				Agent agentPara = allAgents.get(i);

				Tile currTile = world.getTile(agentPara.posX, agentPara.posY);

				if (currTile.hasAgent() == true) // if the tile with the same position as the agent already has an agent, then ...
				{
					// print("<<populateMap>> Tile with Position " + positionPara.getX() + ", " + positionPara.getY() + " already exists.");
					boolean hasFoundTile = false;
					while (hasFoundTile == false)
					{
						x = prng.nextInt(world.getSizeX()); // generate x-value between 0 and 50 if world.SizeX=50
						y = prng.nextInt(world.getSizeY()); // generate y-value between 0 and 50 if world.SizeY=50

						Tile findRightTile = world.getTile(x, y); // random tile
						if (findRightTile.hasAgent() == false) // check if it has agent: has not? then
						{
							allAgents.get(i).setPosition(x, y); // give agent that position
							hasFoundTile = true;
						}
					}
				}
				world.placeAgentOnTile(agentPara.posX, agentPara.posY, isBlue, agentPara.isHappy, newagenttolerance);
			}
		}

		//print("<populateMap> setupMode " + setupMode + ". targetAgents " + agentsTarget + ", startAgents " + agentsStart + ". g is " + g + ", greens is "
		//		+ df0.format(agentsTarget * (g / 100.00)));
	}

	public int countAgentsOnMap()
	{
		int agentcount = 0;
		int blucount = 0;
		int grecount = 0;
		int emptycount = 0;
		for (int x1 = 0; x1 < world.getSizeX(); x1++)
			for (int y1 = 0; y1 < world.getSizeY(); y1++)
			{
				Tile curr = world.getTile(x1, y1);
				if (curr.hasAgent())
				{
					agentcount++;
					if (curr.isAgentBlue())
					{
						blucount++;
					}
					else if (!curr.isAgentBlue())
					{
						grecount++;
					}
				}
				else
					emptycount++;
			}
		return emptycount;
	}

	/**Main method for placing migrants
	 * @param int[] pos**/
	public void findSpace(int[] pos)
	{
		if (influxPositions.size() < current_size)
		{
			// if the position does not have an agent
			if (world.getTile(pos[0], pos[1]).hasAgent() == false)
			{
				// and if the position is not already part of the influx
				if (world.getTile(pos[0], pos[1]).belongsToInflux == false)
				{

					// then we want that tile added to the influx:
					boolean moveOn = false;
					influxPositions.add(pos); // add tile to influx list
					world.setBelongsToInflux(pos[0], pos[1]); // tell the world the same thing
					moveOn = true;
					if (moveOn)
					{
						// here we get the positions of each neighbouring tile
						int[][] myLittleNeighbours = getNeighbourPositionsMoore1(pos);
						// shuffle the array so the agents don't all start with
						// the top left tile
						Collections.shuffle(Arrays.asList(myLittleNeighbours)); // shuffle tested & working 29 Sep 16

						// recursion! now the agent tries to find space to
						// settle at their most-appealing neighbour
						findSpace(checkMostAppealingNeighbour(pos));
					}
				}
				else
				{
					// what to do when the most appealing neighbour has already
					// influx? well.. better not add them in the first place
					print("<<findSpace>> " + pos[0] + ", " + pos[1] + " has no agent, but is part of the influx.");
				}
			}
			else
			{
				// the current tile already has an agent. so we use...
				// RECURSION! and move on to the next most appealing neighbour.
				findSpace(checkMostAppealingNeighbour(pos));
			}
		}
		else
		{
			// print("<<findSpace>> InfluxPositions.size() is not <= size. It's " +
			// influxPositions.size() + " and " + size);
		}
	}

	/**
	 * Method that deals with the influx of agents.<br>
	 * Making sure that the number of agents to be added does not exceed the
	 * total number of free tiles (if crowded) and that the number of agents to
	 * be added does not exceed the maximum number of those to be added (like,
	 * 150 at a time), it finds space starting from the best starting position
	 * that was determined earlier.
	 **/
	public void flux(Double dinfluxsize, int rx, int ry)
	{
		int[] pos = { rx, ry }; // store x and y in an integer array
		influxPositions = new ArrayList<int[]>(); // list of tiles (defined as x-y int array) that agents will be place on
		boolean isBlue = true;// migrants are blue
		double newAgentTolerance;

		while (influxPositions.size() < dinfluxsize) // while the list of positions is smaller than the max number of migrants to be added...
		{
			// print("<<flux>> going to findSpace now... influxsize: " + influxsize + ". influxPositions.size(): " + influxPositions.size());
			findSpace(pos); // find space! starting at the given position
		}
		// print("<<flux>> findSpace survived! influxsize: " + dinfluxsize + ". influxPositions.size(): " + influxPositions.size());

		if (influxPositions.size() == numBlues)
		{

		}
		// now that we have all the positions in the arraylist, we loop through the list, placing a new migrant at each position.
		for (int i = 0; i < influxPositions.size(); i++)
		{
			//because we have different type of influxes, they result in a different kind of blue population:
			newAgentTolerance = generateThreshold(); //1 uniform

			if (world.getTile(influxPositions.get(i)[0], influxPositions.get(i)[1]).addAgent(isBlue, 2, newAgentTolerance))
			{   // if the agent is indeed placed in the world..
				// ...then proceed to add that migrant to the migrant list, using the same position ofc
				allAgents.add(new Agent(influxPositions.get(i)[0], influxPositions.get(i)[1], isBlue, newAgentTolerance, 2));
			}
		}

		// clearing all the influx tiles (cyan tiles)
		world.removeBelongsToInflux();
		// clearing the influx position list
		influxPositions.clear();
	}

	/**
	 * ~~HOW TO TOGGLE OVERIDE~~<br>
	 * To override: comment out Files.createFile(pathToFile); and activate the
	 * boo variable + if statement.<br>
	 * To stop override: activate the Files.createFile(pathToFile); and comment
	 * out the boo + if statement.<br>
	 * Method for dealing with the creation of the csv files and directories<br>
	 * First create the name of the file, generated using moveRule, tick,
	 * maxtick, agents, percentage<br>
	 * Then we try to create the path to the file<br>
	 * Then check if file exists<br>
	 * If yes, delete existing file<br>
	 * and create a new file in the same spot
	 **/
	public void csv(int experimentNo)
	{
		df0.setRoundingMode(RoundingMode.HALF_EVEN);
		String influxString = "";

		if (influx == 1)
		{
			influxString = "ON" + NUM_FLUXES + "z" + current_size;
		}
		else
		{
			influxString = "OFF";
		}

		String longNameRan = "gr" + g + "fd" + fd + influxString + "w" + w + "m" + df4.format(m);
		csvname = longNameRan;
		String path = "output/";
		String csvName = path + longNameRan + ".csv";
		// String csvName = path + experimentNo + longNameRan + ".csv";
		currentDirectory = path + longNameRan;
		try
		{
			Path pathToFile = Paths.get(path + longNameRan + ".csv");
			// Path pathToFile = Paths.get(path + experimentNo + longNameRan + ".csv");
			Files.createDirectories(pathToFile.getParent());
			File f = new File(csvName);
			boolean boo = f.createNewFile(); // check if file exists: if boolean
											// is false, file already exists
			if (boo == false)
			{
				// f.delete(); //boolean is false so we delete the existing file...
				f = new File(csvName + 1); // ...so that we can write a new one instead
			}

			// Files.createFile(pathToFile);
			writer = new CSVWriter(new FileWriter(csvName, true), ','); // once all files and directories are dealt with, we create the filewriter
		}
		catch (IOException e)
		{
			print("<csv>: Could not open csv file \"" + csvName + "\" for writing.");
			print(e.getMessage());
			System.exit(-2);
		}
	}

	/** 
	 * Method for saving the tolerance levels of all agents at the end of each simulation.
	 * Because the csv has between 2000 and 2495 columns, it will be saved into a
	 * separate file in a separate folder, named the same + the affix "_tol". */
	public void csvTolerance()
	{
		List<Double> data = collectThresholdALL();
		String[] strdata = new String[data.size()];
		for (int i = 0; i < data.size(); i++)
		{
			strdata[i] = "" + data.get(i);
		}

		String path = "output/";
		String currFileName = path + csvname + "_tol.csv";
		currentDirectory = path + csvname + "_tol";
		try
		{
			Path pathToFile = Paths.get(path + csvname + ".csv");
			Files.createDirectories(pathToFile.getParent());
			File f = new File(currFileName);
			boolean boo = f.createNewFile(); // check if file exists: if boolean is false, file already exists
			if (boo == false)
			{
				// f.delete(); //boolean is false so we delete the existing file...
				f = new File(currFileName + 1); // ...so that we can write a new one instead
			}

			// Files.createFile(pathToFile);

			writer = new CSVWriter(new FileWriter(currFileName, true), ',');
			String[] wrap = new String[1]; //probably saving on GC
			for (String s : strdata)
			{
				wrap[0] = s;
				writer.writeNext(wrap);
			}
		}

		catch (IOException e)
		{
			print("<csvTolerance>: Could not open csv file \"" + currFileName + "\" for writing.");
			print(e.getMessage());
			System.exit(-2);
		}
		try
		{
			writer.close();
		}
		catch (IOException e)
		{
			print("<csvTolerance>: Could not close csv file.");
			throw new RuntimeException(e);
		}

	}

	public void run(int experimentNo)
	{
		int oldCM = 0;
		fireEvent(new SimulationStartedEvent(experimentNo, w, m, NUM_FLUXES, current_size));
		csv(experimentNo);
		for (tick = 0; tick < maxTicks; tick++)
		{
			oldCM = changedMind;
			changedMind = 0;
			globalMoveCounter = 0.0;
			prefcMcounter = 0;

			if (influx == 1 && fluxcounter < NUM_FLUXES) // after x ticks, influx
			{
				if (TICKS_OF_FLUX.get(fluxcounter)[0] == tick)
				{
					current_size = TICKS_OF_FLUX.get(fluxcounter)[1];
					if (w > emptyTiles.size())
					{
						// print("<<run>> w is larger than emptyTiles.size(): " + w + "> " + emptyTiles.size());
						w = emptyTiles.size();
						placeAgents(current_size);
						fluxcounter++;
					}
					else
					{
						placeAgents(current_size);
						fluxcounter++;
					}
				}
			}

			collectEmptyTiles();

			long seed = System.nanoTime(); // agents get shuffled every tick to avoid dis/advantages
			Collections.shuffle(allAgents, new Random(seed));

			invokeAgentActions();

			if ((tick % SAMPLING_INTERVAL) == 0)// after set amount of ticks: every 10 (if x divisible yields a 0)
			{
				collectData();
			}
			try
			{
				pauser.look();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			updateAppeal();
			countAppealData();
			//print("<<run>> Changed preferences this tick ("+tick+"): " + prefcMcounter);
			fireEvent(new TickEnd(tick, averageTB, averageTG, globalHappiness, mC, mT));
		}
		fireEvent(new SimulationFinishedEvent());

		try
		{
			writer.flush();
			writer.close();
		}
		catch (IOException e)
		{
			print("Error: Could not close csv file.");
			throw new RuntimeException(e);
		}

		//since the simulation ended, we collect the final tolerance
		csvTolerance();
	}

	/**
	 * Picking the best starting tile for new influxes to start. It loops
	 * through the world, collects the appeal info of all tiles without agents,
	 * and then loops through those tiles, picking the best.
	 * 
	 * Right now it's crude: favour all tiles with 4 or 5 appeal. If that leaves
	 * 0 tiles, go for 3 ratings instead. No catch-clauses for less than 3
	 * rating at the moment.
	 */
	public int[] collectBestStart()
	{
		int[] bestPos = { -1, -1 };
		List<int[]> prelimStartCandidates = new ArrayList<int[]>();
		List<int[]> bestStartCandidates = new ArrayList<int[]>();

		for (int x = 0; x < world.getSizeX(); x++)
		{
			for (int y = 0; y < world.getSizeY(); y++)
			{
				if (!world.getTile(x, y).hasAgent())
				{
					int[] bestSCInfo = { -3, -3, -3 };
					bestSCInfo[0] = x;
					bestSCInfo[1] = y;
					bestSCInfo[2] = world.getTile(x, y).getAppeal();
					prelimStartCandidates.add(bestSCInfo);
				}
			}
		}
		// looping through all candidates (should be all tiles without agents at this point
		for (int i = 0; i < prelimStartCandidates.size(); i++)
		{
			if (prelimStartCandidates.get(i)[2] == 4) // if the appealrating is greater or equal to 4, these are the best candidates
			{
				bestStartCandidates.add(prelimStartCandidates.get(i)); // and thus they get added to the final list of candidates
			}
		}

		if (bestStartCandidates.size() == 0) // rather crudely- if none of the tiles have a 4 rating, we go down to 3
		{
			for (int i = 0; i < prelimStartCandidates.size(); i++)
			{
				if (prelimStartCandidates.get(i)[2] >= 3)
				{
					bestStartCandidates.add(prelimStartCandidates.get(i));
				}
			}

			if (bestStartCandidates.size() == 0) // rather crudely- if none of the tiles have a 4 rating, we go down to 3
			{
				for (int i = 0; i < prelimStartCandidates.size(); i++)
				{
					if (prelimStartCandidates.get(i)[2] >= 2)
					{
						bestStartCandidates.add(prelimStartCandidates.get(i));
					}
				}
				if (bestStartCandidates.size() == 0) // rather crudely- if none of the tiles have a 4 rating, we go down to 3
				{
					for (int i = 0; i < prelimStartCandidates.size(); i++)
					{
						if (prelimStartCandidates.get(i)[2] >= 1)
						{
							bestStartCandidates.add(prelimStartCandidates.get(i));
						}
					}
				}
				else
				{
					//print("<<collectBestStart>>Just sod it. Seriously. CAN'T FIND BEST STARTING POINT");
				}
			}
		}

		if (bestStartCandidates.size() == 1)
		{
			bestPos[0] = bestStartCandidates.get(0)[0];
			bestPos[1] = bestStartCandidates.get(0)[1];
		}

		else if (bestStartCandidates.size() > 1)
		{
			Collections.shuffle(bestStartCandidates); // tested & working 29 Sep 16
			bestPos[0] = bestStartCandidates.get(0)[0];
			bestPos[1] = bestStartCandidates.get(0)[1];
		}
		if (bestPos[0] != -1)
		{
			for (int u = 1; u < bestStartCandidates.size(); u++)
			{
				world.getTile(bestStartCandidates.get(u)[0], bestStartCandidates.get(u)[1]).isStartCandidate = true;
			}
			world.getTile(bestStartCandidates.get(0)[0], bestStartCandidates.get(0)[1]).isBestStart = true;
		}
		else
		{
			//print("<<collectBestStart>> bestPosition not found. " + bestPos[0] + ", " + bestPos[1]);
			Tile randomTile = emptyTiles.get(prng.nextInt(emptyTiles.size()));
			world.getTile(randomTile.getPosition()[0], randomTile.getPosition()[1]).isBestStart = true;
			bestPos[0] = randomTile.getPosition()[0];
			bestPos[1] = randomTile.getPosition()[1];
			//print("<<collectBestStart>> random Tile selected:" + bestPos[0] + ", " + bestPos[1]);

		}
		return bestPos;
	}

	/**
	 * Method that oversees the placing of new agents.
	 * 1. Collect the best Start (call collectBestStart())
	 * 2. Flux going from best Start (calling flux())
	 * 
	 * @param fluxsize
	 **/
	public void placeAgents(int fluxsize)
	{
		Double influxsize = fluxsize * 1.0;
		if (countAgentsOnMap() >= influxsize) // check that the number of agents in total is greater or equal to fluxes.
		{
			influxPositions.clear();
			int[] bestPos = collectBestStart();
			flux(influxsize, bestPos[0], bestPos[1]); // number of agentsto be added, X and Y value of where the flux starts
		}
		else
		{
			print("<placeAgents>: " + allAgents.size() + " <= " + totalInfluxMax + "? Not enough empty space for remaining agents to be fluxed");
		}

		world.removeAllCandidateInfo();
	}

	/**
	 * The method that will go through all the tiles and that will call the
	 * floodfill on each tile
	 **/
	public void clusterSearch()
	{
		// so this is a list of lists. the first list contains the foundGroups.
		// the second list are the members of each group
		List<ArrayList<int[]>> foundGroups = new ArrayList<ArrayList<int[]>>();
		world.removeBelongsToGroupFlag();

		for (int x = 0; x < sizeX; x++)// looping through the world
		{
			for (int y = 0; y < sizeY; y++)
			{
				int currPos[] = { x, y };
				// we check if the tile has an agent AND whether that agent, if
				// exists, belongs to a group already.
				if (world.getTile(x, y).hasAgent() == true && world.getTile(x, y).belongsToGroup == false)
				{
					// if both conditions are satisfied, the floodfill results list (Position) is cleared
					floodFillResults.clear();
					floodfill(currPos, false); // here we call the floodfill method.

					if (floodFillResults.size() > 2) // Omitting empty lists here; minimum group size is THREE agents
					{
						foundGroups.add(new ArrayList<int[]>()); // add new list to the found group list. that list contains the agents

						for (int i = 0; i < floodFillResults.size(); i++)
						{
							// print("Group Member #" + i + ": " + floodFillResults.get(i)[0] + "," + floodFillResults.get(i)[1]);
							foundGroups.get(foundGroups.size() - 1).add(floodFillResults.get(i));
						}
					}
				}
			}
		}

	}

	/**
	 * Main method for implementing flood-fill.
	 * 
	 * @param Position
	 *            currentPosition (int x and int y)
	 * @param Boolean
	 *            searchNative (search for Natives yes or no)
	 **/
	public void floodfill(int[] pos, boolean searchBlue)
	{
		if (world.getTile(pos[0], pos[1]).hasAgent())// checking for agent to determine whether we need to flood-fill or not
		{
			boolean moveOn = false;
			if (searchBlue) // if searchBlue is enabled, search for blue groups
			{
				if (world.getTile(pos[0], pos[1]).isAgentBlue()) // check if agent is native
				{
					floodFillResults.add(pos); // if yes then add the position to the list of positions of flood-fill-results
					world.setBelongsToGroupFlag(pos[0], pos[1]); // the tile position will get the flag 'belongs to group'
					moveOn = true; // after thats done, we move on to the next tile
				}
			}
			else if (!world.getTile(pos[0], pos[1]).isAgentBlue()) // check if agent is green, i.e. go for green groups instead
			{
				floodFillResults.add(pos);
				world.setBelongsToGroupFlag(pos[0], pos[1]);
				moveOn = true;
			}

			if (moveOn) // if true
			{
				int[][] myLittleNeighbours = getNeighbourPositionsVNeu1(pos); // collect the neighbour positions: only strict < ^ > v

				for (int i = 0; i < myLittleNeighbours.length; i++) // loop through the neighbours
				{
					// we have stored the already searched positions in the
					// floodfillresults list, and here we check whether that
					// list contains any of the eight neighbour positions we
					// have just collected. one by one ofc.
					if (!world.getTile(myLittleNeighbours[i][0], myLittleNeighbours[i][1]).belongsToGroup)
					{
						floodfill(myLittleNeighbours[i], searchBlue);
						// if the neighbouring position has not previously been
						// added to the floodfill results, we perform floodfill
						// on that tile (recursion)
					}
				}
			}
		}
	}

	private int[][] getNeighbourPositionsMoore1(int[] currPos)
	{
		int[][] neighbourPositions = new int[8][2];
		// here, all positions get stored in the agentNeighbourPosition array;
		// starting INNER RING top left, clockwise.
		neighbourPositions[0][0] = currPos[0] - 1; // up left
		neighbourPositions[0][1] = currPos[1] - 1; // up left

		neighbourPositions[1][0] = currPos[0]; // up
		neighbourPositions[1][1] = currPos[1] - 1; // up

		neighbourPositions[2][0] = currPos[0] + 1; // up right
		neighbourPositions[2][1] = currPos[1] - 1; // up right

		neighbourPositions[3][0] = currPos[0] + 1; // right
		neighbourPositions[3][1] = currPos[1]; // right

		neighbourPositions[4][0] = currPos[0] + 1; // down right
		neighbourPositions[4][1] = currPos[1] + 1; // down right

		neighbourPositions[5][0] = currPos[0]; // down
		neighbourPositions[5][1] = currPos[1] + 1; // down

		neighbourPositions[6][0] = currPos[0] - 1; // down left
		neighbourPositions[6][1] = currPos[1] + 1; // down left

		neighbourPositions[7][0] = currPos[0] - 1; // left
		neighbourPositions[7][1] = currPos[1]; // left

		// going through the tiles here and check if X or Y are greater or
		// smaller than the maximum or minimum coordinates of the world.
		// if any condition applies, that X or Y coordinate will be set such
		// that the world wraps around. There are no edges
		for (int z = 0; z < neighbourPositions.length; z++)
		{
			if (neighbourPositions[z][0] >= sizeX)
			{
				neighbourPositions[z][0] = neighbourPositions[z][0] - sizeX;
			}
			else if (neighbourPositions[z][0] < 0)
			{
				neighbourPositions[z][0] = neighbourPositions[z][0] + sizeX;
			}
			if (neighbourPositions[z][1] >= sizeY)
			{
				neighbourPositions[z][1] = neighbourPositions[z][1] - sizeY;
			}
			else if (neighbourPositions[z][1] < 0)
			{
				neighbourPositions[z][1] = neighbourPositions[z][1] + sizeY;
			}
		}
		return neighbourPositions;
	}

	/**
	 * This is the main method for the agents.<br>
	 **/
	public void invokeAgentActions()
	{
		fireEvent(new TickStart());

		for (int i = 0; i < allAgents.size(); i++)
		{
			Agent currAgent = allAgents.get(i);
			int[] mooreNBHofAgent = checkNBH(currAgent.pos);
			updateHappiness(i, mooreNBHofAgent);
			updatePreferences(i, mooreNBHofAgent); // update preference before movement decision

			if (currAgent.isHappy == 0)// unhappy
			{
				agentMove(i);
			}

			else if (currAgent.isHappy == 1)// happy
			{
				if (prng.nextInt(100) == 72) // small chance of random movement.
				{
					moveRandomly(i);
				}

			}
			else
			{
				print("happyval invalid. Should be 0 or 1, is " + currAgent.isHappy);
				System.exit(0);
			}
			collectEmptyTiles();

		}
		fireEvent(new AgentMovedEvent());
	}

	/**
	 * Method for agents to update their preference for threshold of friends
	 * @param mooreNBHofAgent
	 */
	public void updatePreferences(int agentid, int[] mooreNBHofAgent)
	{
		df2.setRoundingMode(RoundingMode.HALF_EVEN);
		Agent currAgent = allAgents.get(agentid);
		int happy = currAgent.isHappy;
		boolean isAgentBlue = currAgent.isBlue;
		double prefMin = 5.00; //meaning always one same must be present
		double prefMax = 93.00; //meaning always one different is tolerated
		double oldPref = Double.valueOf(df2.format(currAgent.getThreshold())); // say, 75%, rounded to 2 decimal places
		double newPref = -44;
		double prefDecrement = -m;
		double prefIncrement = m;
		int same;
		int diff;

		if (isAgentBlue) //depending on the colour of the agent, the "differents" and "sames" are blue/green or vice versa
		{
			same = mooreNBHofAgent[0]; // moornbh: blue, green, empty
			diff = mooreNBHofAgent[1];
		}
		else
		{
			same = mooreNBHofAgent[1]; // moornbh: blue, green, empty
			diff = mooreNBHofAgent[0];
		}

		int tot = same + diff;
		if (tot == 0) // that must mean the agent is isolated.
		{
			oldPref = newPref;
		}
		else
		{

			if (diff > 0) // if there is at least one "different" neighbour for this  agent...
			{
				if (happy == 1) // agent is happy
				{
					newPref = oldPref + prefDecrement; // the decrement is applied: fewer neighbours need to be of the same colour now.
				}
				else if (happy == 0)
				{
					newPref = oldPref; // if the agent is unhappy the threshold doesn't change
				}
				else
				{
					print("<<updatePreferences>>happiness of this blue agent neither 0 nor 1. is " + happy);
				}
			}
			else if (diff == 0) // if there are no differents around, the agent gets more hostile.
			{
				if (happy == 1)
				{
					newPref = oldPref + prefIncrement;

				}
				else if (happy == 0)
				{
					errorcounter[0]++;
					print("<<updatePreferences>>logic error. no differents but agent unhappy?");
					print("<<updatePreferences>>Is " + newPref + " > " + prefMax + "? " + (newPref > prefMax));
					print("<<updatePreferences>>Is " + newPref + " < " + prefMin + "? " + (newPref < prefMin));
					print("<<updatePreferences>>isAgent blue? " + currAgent.isBlue + ". Is agent happy? " + currAgent.isHappy + ". oldPref " + oldPref);
					print("<<updatePreferences>>differents: " + diff + ". tot: " + tot + ". ");
					System.exit(0);
				}
				else
				{
					print("<<updatePreferences>>happiness of this blue agent neither 0 nor 1. is " + happy);
				}
			}
			else
			{
				print("<<updatePreferences>>the mooreNBH is wonky. Is " + mooreNBHofAgent[1]);
			}
		}
		if (newPref < prefMin)
		{
			newPref = prefMin;
		}
		else if (newPref > prefMax)
		{
			newPref = prefMax;
		}

		if (newPref < prefMin || newPref > prefMax)
		{
			print("<updatePreferences> newPref invalid. It's " + newPref + ". Min/Max: " + prefMin + "/" + prefMax);
			print("<<updatePreferences>>Is " + newPref + " > " + prefMax + "? " + (newPref > prefMax));
			print("<<updatePreferences>>Is " + newPref + " < " + prefMin + "? " + (newPref < prefMin));
			print("<<updatePreferences>>isAgent blue? " + currAgent.isBlue + ". Is agent happy? " + currAgent.isHappy + ". oldPref " + oldPref);
			print("<<updatePreferences>>differents: " + diff + ". tot: " + tot + ". ");
			System.exit(0);
		}

		if (newPref != oldPref)
		{
			currAgent.setThreshold(newPref);
			int[] apos = currAgent.getPosition();
			world.getTile(apos[0], apos[1]).setAgentAvgF(newPref);
		}
	}

	/**
	 * the agent movement method.<br>
	 * first, the new position gets determined by calling
	 * moveToABetterPlace().<br>
	 * second, the agent gets removed from current tile.<br>
	 * third, the agent gets added to the newly determined tile.<br>
	 */
	public void agentMove(int agentIndex)
	{
		int[] newPos = { -1, -1 };
		newPos = moveToABetterPlace(agentIndex);
		boolean placedSuccessfully = false;

		if (newPos[0] == -1 || newPos[1] == -1)
		{
			print("A new position could not be found for our agentIndex " + agentIndex + ". Not moving.");
			throw new IllegalArgumentException();
		}

		else if (newPos[0] == -44 && newPos[1] == -44)
		{
			changedMind++;
			// print("<<agentMove>>newPos is = -44,-44. That means no better tile was found. not moving now.");
		}
		else
		{
			world.removeAgentFromTile(allAgents.get(agentIndex).posX, allAgents.get(agentIndex).posY); // remove agent from tile
			placedSuccessfully = world.placeAgentOnTile(newPos[0], newPos[1], allAgents.get(agentIndex).isBlue, allAgents.get(agentIndex).getHappy(), allAgents.get(agentIndex).getThreshold());
			if (placedSuccessfully == false)
			{
				print("<<agentMove>>does emptyTiles contain the tile with the new position? " + emptyTiles.contains(world.getTile(newPos[0], newPos[1])));
				print("<<agentMove>>Trying to place agent number " + agentIndex + " at " + allAgents.get(agentIndex).posX + "," + allAgents.get(agentIndex).posY + " on tile " + newPos[0] + ","
						+ newPos[1] + " but that tile says: 'hasAgent?' - " + world.getTile(newPos[0], newPos[1]).hasAgent() + "!");
				throw new IllegalArgumentException();
			}
			emptyTiles.remove(world.getTile(newPos[0], newPos[1]));
			emptyTiles.add(world.getTile(allAgents.get(agentIndex).posX, allAgents.get(agentIndex).posY));
			allAgents.get(agentIndex).setPosition(newPos[0], newPos[1]);
			allAgents.get(agentIndex).moveCounter++;

			globalMoveCounter++;
		}
	}

	public int[] moveRandomly(int agentIndex)
	{
		int[] newPos = { -3, -3 };
		boolean placedSuccessfully = false;
		int rngMax = emptyTiles.size();
		Tile currRngTile = emptyTiles.get(prng.nextInt(rngMax));

		newPos[0] = currRngTile.getPosition()[0];
		newPos[1] = currRngTile.getPosition()[1];

		if (newPos[0] == -3 || newPos[1] == -3)
		{
			print("<<moveRandomly>>A new <random> position could not be found for our agentIndex " + agentIndex + ". Not moving.");
			throw new IllegalArgumentException();
		}
		else
		{
			world.removeAgentFromTile(allAgents.get(agentIndex).posX, allAgents.get(agentIndex).posY);
			placedSuccessfully = world.placeAgentOnTile(newPos[0], newPos[1], allAgents.get(agentIndex).isBlue, allAgents.get(agentIndex).isHappy, allAgents.get(agentIndex).getThreshold());
			if (placedSuccessfully == false)
			{
				print("<<moveRandomly>>does emptyTiles contain the tile with the new position? " + emptyTiles.contains(world.getTile(newPos[0], newPos[1])));
				print("<<moveRandomly>>Trying to place agent number " + agentIndex + " at " + allAgents.get(agentIndex).posX + "," + allAgents.get(agentIndex).posY + " on tile " + newPos[0] + ","
						+ newPos[1] + " but that tile says: 'hasAgent?' - " + world.getTile(newPos[0], newPos[1]).hasAgent() + "!");

				throw new IllegalArgumentException();

			}
		}
		emptyTiles.remove(world.getTile(newPos[0], newPos[1]));
		emptyTiles.add(world.getTile(allAgents.get(agentIndex).posX, allAgents.get(agentIndex).posY));
		allAgents.get(agentIndex).setPosition(newPos[0], newPos[1]);
		allAgents.get(agentIndex).moveCounter++;

		globalMoveCounter++;

		return newPos;
	}

	/**
	 * This main method for finding the best place to move to after agents
	 * determine they are unhappy works as follows:<br>
	 * of every empty tile on the map, we get the neighbourhood
	 * information via checkMooreNBH();<br>
	 * it returns the counts for blue, greens and empties. <br>
	 * then we take the ratio of each and store that information. the empty tile
	 * with the best ratio wins the betterPlace contest.<br>
	 * the logic varies depending on whether the agent itself is blue or
	 * green.<br>
	 * ### IF UTILITY ON: (Default: true)
	 * Abolished the ratio logic. Agents should still refer to their threshold,
	 * and any tile equal to or better than the demands should be deemed of
	 * equal value. The ratio logic of simply subtracting green - blue (or vice
	 * versa) is much more strict as it will always favour the absolute best of
	 * all possible considered (w) tiles. (traditional Schelling)
	 * 
	 * @return int[] position x,y
	 **/
	public int[] moveToABetterPlace(int agentNum)
	{
		// print("<<moveToABetterPlace>>moving to a better place now. emptyTiles.size() is " + emptyTiles.size() + ".");
		Agent curA = allAgents.get(agentNum);
		int betterPlace[] = { -1, -1 }; // future position agent moves to
		int[] consideredTiles = generateUniqueNumbers(w);
		int[] surroundingInfo = new int[3]; // 0=blue,// 1=green,// 2=empty
		double total;
		List<int[]> emptyTileInfo = new ArrayList<int[]>(); // x, y, same, different, utility

		// here we loop through the list of empty tiles. for each empty tile, we
		// get its mooreNBH information and store that in our emptyTileInfo list.
		if (consideredTiles.length <= emptyTiles.size())
		{
			for (int i = 0; i < consideredTiles.length; i++)
			{
				int[] allInfo = new int[5]; // 0=x, 1=y, 2=same, 3=different, 4=utility
				surroundingInfo = checkNBH(emptyTiles.get(consideredTiles[i]).position);
				allInfo[0] = emptyTiles.get(consideredTiles[i]).position[0]; // x coordinate
				allInfo[1] = emptyTiles.get(consideredTiles[i]).position[1]; // y coordinate

				if (curA.isBlue) // if agent is blue, ...
				{
					allInfo[2] = surroundingInfo[0]; // samecount is set to blue
					allInfo[3] = surroundingInfo[1]; // and differentcount is green
					total = (double) allInfo[2] + (double) allInfo[3];
					//print("<<moveToABetterPlace>>" + (double) allInfo[2] + "/"  + total + " >= " + (curA.getThreshold()/100) + "? " + ((double) allInfo[2] / total >= curA.getThreshold()/100));
					if ((double) allInfo[2] / total >= (curA.getThreshold() / 100))
					{
						allInfo[4] = 1; // this is less punishing. utility is set to 1 no matter how much better
										// the tile is, as long as it's over the threshold it's fine
					}
					else
						allInfo[4] = 0;
				}
				else // if agent is green, ...
				{
					allInfo[2] = surroundingInfo[1]; // then samecount is set to green
					allInfo[3] = surroundingInfo[0]; // and differentcount is blue
					total = (double) allInfo[2] + (double) allInfo[3];

					if ((double) allInfo[2] / total >= (curA.getThreshold() / 100))
					{
						allInfo[4] = 1;
					}
					else
						allInfo[4] = 0;
				}
				emptyTileInfo.add(allInfo);
			}
		}
		else
		{
			print("the number of considered tiles is higher than the number of empty tiles.");
			throw new IllegalArgumentException();
		}

		// now we have all neighbours and their appeal rating. now we need to get the highest one
		// first things first, put all appeals in an array to see whether there are duplicates etc
		int[] ratioArray = new int[emptyTileInfo.size()]; // array is as long as the surrounding list looping through the surrounding list
		for (int j = 0; j < emptyTileInfo.size(); j++)
		{
			ratioArray[j] = emptyTileInfo.get(j)[4];// adding all the appeals
		}

		// now we loop through our appeal array to find the highest value: [1, 1, 1, 0, 0, 1] then we want to get the 1s
		int bestUtility = 1; // utility = 1
		int duplicates = 0;
		for (int t = 0; t < ratioArray.length; t++)
		{
			// print("<<moveToABetterPlace>>is ratioArray[t] " + ratioArray[t] + " == " + " bestUtility " + bestUtility + "?");
			if (ratioArray[t] == bestUtility)
			{
				duplicates++;
			}
		}

		List<int[]> narrowedCandidates = new ArrayList<int[]>(); // list of positions of suitable tiles
		List<int[]> narrowedCrappyCandidates = new ArrayList<int[]>();

		// if the number of duplicates is not equal the number of tiles (i.e. not all tiles are the same)...
		if (duplicates < ratioArray.length)
		{
			for (int t = 0; t < emptyTileInfo.size(); t++) // loop through our tile list
			{
				if (emptyTileInfo.get(t)[4] == bestUtility) // and add only the ones that have the best ratio
				{
					int[] position = { emptyTileInfo.get(t)[0], emptyTileInfo.get(t)[1] };
					narrowedCandidates.add(position);
				}
				else
				{
					int[] position = { emptyTileInfo.get(t)[0], emptyTileInfo.get(t)[1] };
					narrowedCrappyCandidates.add(position);
				}
			}
		}
		else if (duplicates == ratioArray.length)
		// all tiles in the list are of the same value to the agent! we just add them all to the narrowed list
		{
			for (int l = 0; l < emptyTileInfo.size(); l++)
			{
				int[] position = { emptyTileInfo.get(l)[0], emptyTileInfo.get(l)[1] };
				narrowedCandidates.add(position);
			}
		}

		// now we have a list of narrowed-down candidates. if the list is larger
		// than 1, we have multiple tiles of equal value.
		// in that case, we randomize which tile gets picked by shuffling and
		// always picking the first element
		if (narrowedCandidates.size() > 1) // if we have more than one tile with the same highest influx, randomize
		{
			Collections.shuffle(narrowedCandidates, new Random(System.nanoTime())); // tested & working
			betterPlace = narrowedCandidates.get(0);
		}
		else if (narrowedCandidates.size() == 1) // if we have only one winner... bingo!
		{
			betterPlace = narrowedCandidates.get(0);
		}
		else // narrowedCandidates.size() is smaller than 1, i.e. none of the choices had utility = 1. we just pick any tile.
		{
			if (utilityOn == false)
			{
				Collections.shuffle(narrowedCrappyCandidates, new Random(System.nanoTime()));
				betterPlace = narrowedCrappyCandidates.get(0);
			}
			else if (utilityOn) // if on, agents will actually not move to a better place if there is none
			{
				betterPlace[0] = -44;
				betterPlace[1] = -44;
			}
		}
		return betterPlace;
	}

	/**
	 * Main method for agents determining their happiness.<br>
	 * The method takes the previously collected moore NBH info and loops
	 * through it, counting for differents and sames (changing depending on
	 * whether agent is blue or green)<br>
	 * the count gets compared to the threshold and the happiness is set.
	 */
	public void updateHappiness(int agentIndex, int[] surroundingInfo)
	{
		int amHappy = 2;

		Agent currAgent = allAgents.get(agentIndex);
		double threshold = currAgent.getThreshold(); // say, 95%
		double differentCount = 0.0;
		double sameCount = 0.0; //
		double emptyCount = 0.0;
		double total = 0.0;
		double max = 0.0;
		double situation = 0.0;
		if (currAgent.isBlue) // if the agent is blue...
		{
			sameCount = surroundingInfo[0]; // then the number of blues count as sames
			differentCount = surroundingInfo[1]; // and the number of greens as differents.
		}
		else // if the agent is not blue (i.e. green)
		{
			differentCount = surroundingInfo[0]; // the process is reversed. Blues are different and
			sameCount = surroundingInfo[1]; // greens are sames
		}
		emptyCount = surroundingInfo[2];
		total = sameCount + differentCount;
		max = total + emptyCount;

		// here comes the math. threshold (in percentage / 100, so 50% is .5 etc) times the number of differents.
		// if that number is greater than the sames, the agent is unhappy. else, happy.
		if (emptyCount == max) // special case: isolated agent. they are unhappy and want to move towards civilisation.
		{
			amHappy = 0;
		}
		else
		{
			situation = (sameCount / total) * 100;
			//print("<<updateHappiness>> (sameCount / total) * 100 is: " + df2.format(situation) + ". " + df2.format(situation) + " >= " + df2.format(threshold) + "? " + (situation >= threshold));
			if (situation >= threshold)
			{
				amHappy = 1; // happy
			}
			else
			{
				amHappy = 0; // unhappy
			}
		}

		if (amHappy != 2)
		{
			currAgent.isHappy = amHappy;
			world.getTile(currAgent.posX, currAgent.posY).setAgentIsHappy(amHappy);
		}
		else
		{
			print("<<updateHappiness>>amHappy is neither 0 or 1. Is " + amHappy);
		}
	}

	/**
	 * checks the surrounding X neighbourhood for a given tile.<br>
	 * explanation: <br>
	 * first retrieve the target tile's neighbours (int[X][2])<br>
	 * Then loop through those neighbours, counting agents and empty tiles. that
	 * gets stored:<br>
	 * output: int [3] <br>
	 * [0] number of blues<br>
	 * [1] number of greens<br>
	 * [2] number of empty tiles
	 * 
	 * @return int[3] bluec greenc emptyc
	 **/
	public int[] checkNBH(int[] targetTile)
	{
		Tile tile = world.getTile(targetTile[0], targetTile[1]);
		int[] tileNBHInfo = new int[3]; // store no. of blue, green and empty
		int[][] tileNeighbourPositions = null;
		int bluec = 0;
		int greenc = 0;
		int emptyc = 0;

		tileNeighbourPositions = getNeighbourPositionsMoore1(tile.getPosition());

		// loop through the X neighbours
		for (int i = 0; i < tileNeighbourPositions.length; i++)
		{
			Tile currT = world.getTile(tileNeighbourPositions[i][0], tileNeighbourPositions[i][1]);
			if (currT.hasAgent())
			{
				if (currT.isAgentBlue())
				{
					bluec++;
				}
				else
				{
					greenc++;
				}
			}
			else
			{
				emptyc++;
			}

		}
		tileNBHInfo[0] = bluec;
		tileNBHInfo[1] = greenc;
		tileNBHInfo[2] = emptyc;
		return tileNBHInfo;
	}

	public int getInfluxNbours(Tile tile)
	{
		int counter = 0;
		int[][] neighbours = null;

		neighbours = getNeighbourPositionsMoore1(tile.getPosition());

		for (int i = 0; i < neighbours.length; i++)
		{
			if (world.getTile(neighbours[i][0], neighbours[i][1]).belongsToInflux)
			{
				counter++;
			}
		}
		return counter;
	}

	/**
	 * Method for retrieving the most appealing neighbour.
	 * @param int[]  (position of current tile)
	 * @return int[] (position of the determined best neighbour)
	 * Long method, read comments
	 **/
	public int[] checkMostAppealingNeighbour(int[] pos)
	{
		// we create a List of integer arrays. The list concerns itself with the
		// surrounding tiles of a given tile. Each entry has 4 integers
		List<int[]> surroundingTiles = new ArrayList<int[]>();// 0=x, 1=y, 2=appeal, 3=influxNbours
		int[][] tempAgntsNeigPos = getNeighbourPositionsMoore1(pos); // we get the neighbour positions (x,y int) of the current tile
		int[] mostAppealingNeighbour = { -1, -1 }; // the position of the most appealing neighbour that we want to determine

		// looping through the neighbour positions
		for (int g = 0; g < tempAgntsNeigPos.length; g++)
		{
			// retrieving the associated tile of that position. [x][0] and
			// [x][1] are always the x and y positions
			Tile currTile = world.getTile(tempAgntsNeigPos[g][0], tempAgntsNeigPos[g][1]);
			// now we can work with the tile. checking whether the tile is free and doesn't belong to influx:
			if (!currTile.hasAgent() && !currTile.belongsToInflux)
			{
				// if so, we want to store the position, appeal rating and how
				// many influxes surround the tile- stored in a neat array
				int[] allStuff = new int[4];
				allStuff[0] = currTile.getPosition()[0];
				allStuff[1] = currTile.getPosition()[1];
				allStuff[2] = currTile.getAppeal();
				allStuff[3] = getInfluxNbours(currTile);
				// that array gets added to our surrounding-list.
				surroundingTiles.add(allStuff);
			}
		}

		// now we have a list of surrounding tiles that don't have agents and don't have an influx flag.
		if (surroundingTiles.size() == 1) // if the list contains only one entry, it's easy- the most appealing neighbour is the only list entry.
		{
			mostAppealingNeighbour = surroundingTiles.get(0);
		}

		// if it contains more than one entry, we need to sort by the highest appeal, which is the next criteria
		else if (surroundingTiles.size() > 1)
		{
			// now we have all neighbours and their appeal rating. now we need to get the highest one
			// (but not 5, as that indicates an agent on the tile.) first things
			// first, put all appeals in an array to see whether there are duplicates etc
			int[] appealArray = new int[surroundingTiles.size()]; // array is as long as the surrounding list

			// looping through the surrounding list...
			for (int j = 0; j < surroundingTiles.size(); j++)
			{
				// adding all the appeals
				appealArray[j] = surroundingTiles.get(j)[2];
			}

			// now we loop through our appeal array to find the highest value
			// lets assume: [0, 4, 2, 5, 4, 3, 4, 3] then we would want to get the 4
			int bestAppeal = -1;
			int duplicates = 0;
			for (int w = 0; w < appealArray.length; w++)
			{
				if (appealArray[w] > bestAppeal && appealArray[w] != 5) // appeal == 5 implicates agent on that tile
				{
					bestAppeal = appealArray[w];
				}

				if (appealArray[w] == bestAppeal)
				{
					duplicates++;
				}
			}

			//create a new list for all winning tiles with bestAppeal
			List<int[]> notsure = new ArrayList<int[]>();
			// if the number of duplicates is not equal to the array length, that means we have a winner.
			// we don't need to resort to influx-counts, we use the appeal
			if (!(duplicates == appealArray.length))
			{
				for (int t = 0; t < surroundingTiles.size(); t++)
				{
					if (surroundingTiles.get(t)[2] == bestAppeal)
					{
						int[] position = { surroundingTiles.get(t)[0], surroundingTiles.get(t)[1] };
						notsure.add(position);
					}
				}
			}

			// if all elements in the appeal array are the same, go prioritise
			// those tiles with the most influx neighbours
			else if ((duplicates == appealArray.length))
			{
				int[] influxArray = new int[surroundingTiles.size()];
				for (int j = 0; j < surroundingTiles.size(); j++)
				{
					influxArray[j] = surroundingTiles.get(j)[3]; // store all influx counters in an array
				}

				// then we try and find the best influx...
				int bestInflux = -1;
				for (int w = 0; w < influxArray.length; w++)
				{
					if (influxArray[w] > bestInflux)
					{
						bestInflux = influxArray[w];
					}
				}

				// and THEN we get the best influx tiles and add them to our notsure list.
				for (int t = 0; t < surroundingTiles.size(); t++)
				{
					if (surroundingTiles.get(t)[3] == bestInflux)
					{
						int[] position = { surroundingTiles.get(t)[0], surroundingTiles.get(t)[1] };
						notsure.add(position);
					}
				}
			}

			if (notsure.size() > 1) // if we have more than one tile with the same highest influx, randomize
			{
				Collections.shuffle(notsure); 
				mostAppealingNeighbour = notsure.get(0);
			}

			else if (notsure.size() == 1) // if we have only one winner... bingo!
			{
				mostAppealingNeighbour = notsure.get(0);
			}

			else // this should not happen. because we start out from an influx position, there should always be at least 1.
			{
				print("<<checkMostAppealingNeighbour>>There were no influxes or appeals recorded for any neighbours, notsureList is empty");
				print(notsure.get(0));
				System.exit(-2);
			}

		}

		// if there are no free tiles, we pick a random neighbour and try our luck. that way, the next influx is always "in the neighbourhood".
		else
		{
			//print("<<checkMostAppealingNeighbour>>no free tiles. surroundingTiles is empty. trying " + tempAgntsNeigPos[prng.nextInt(12)][0] + "," +
			//tempAgntsNeigPos[prng.nextInt(24)][1] + " now.");
			mostAppealingNeighbour = checkMostAppealingNeighbour(tempAgntsNeigPos[prng.nextInt(12)]);
		}

		return mostAppealingNeighbour;
	}


	public void initAppeal()
	{
		for (int x = 0; x < world.getSizeX(); x++)
		{
			for (int y = 0; y < world.getSizeY(); y++)
			{
				world.getTile(x, y).setAppeal(0); // by default, first set all tiles to 0 appeal
			}
		}
		csvAppealValues = new int[world.getSizeX()][world.getSizeY()];
		try
		{
			initGridAppeal(1);
		}
		catch (IOException e)
		{
			print("Something went wrong in initAppeal");
			e.printStackTrace();
		}
	}

	public void updateAppeal()
	{
		for (int x = 0; x < world.getSizeX(); x++)
		{
			for (int y = 0; y < world.getSizeY(); y++)
			{
				Tile currT = world.getTile(x, y);
				if (!currT.hasAgent())
				{
					int counter = 0;
					int[][] nbh = getNeighbourPositionsVNeu1(currT.getPosition());
					for (int i = 0; i < nbh.length; i++)
					{
						if (world.getTile(nbh[i][0], nbh[i][1]).hasAgent())
						{
							counter++;
						}
					}
					currT.setAppeal(counter);
				}
				else //if tiles has agent, appeal = 5 
				{
					currT.setAppeal(5);
				}
			}
		}
	}

	/**
	 * This is the method that creates a list. A LIST. OF EMPTY TILES.
	 */
	public void collectEmptyTiles()
	{
		emptyTiles.clear();
		for (int x = 0; x < world.getSizeX(); x++)
		{
			for (int y = 0; y < world.getSizeY(); y++)
			{
				Tile curT = world.getTile(x, y);

				if (!curT.hasAgent())
				{
					emptyTiles.add(curT);
				}
			}
		}
	}

	private int[][] getNeighbourPositionsVNeu1(int[] currPos)
	{
		int[][] neighbourPositionsFour = new int[4][2];

		neighbourPositionsFour[0][0] = currPos[0]; // up
		neighbourPositionsFour[0][1] = currPos[1] - 1; // up

		neighbourPositionsFour[1][0] = currPos[0] + 1; // right
		neighbourPositionsFour[1][1] = currPos[1]; // right

		neighbourPositionsFour[2][0] = currPos[0]; // down
		neighbourPositionsFour[2][1] = currPos[1] + 1; // down

		neighbourPositionsFour[3][0] = currPos[0] - 1; // left
		neighbourPositionsFour[3][1] = currPos[1]; // left

		for (int z = 0; z < 4; z++)
		{
			if (neighbourPositionsFour[z][0] >= sizeX)
			{
				neighbourPositionsFour[z][0] = neighbourPositionsFour[z][0] - sizeX;
			}
			else if (neighbourPositionsFour[z][0] < 0)
			{
				neighbourPositionsFour[z][0] = neighbourPositionsFour[z][0] + sizeX;
			}
			if (neighbourPositionsFour[z][1] >= sizeY)
			{
				neighbourPositionsFour[z][1] = neighbourPositionsFour[z][1] - sizeY;
			}
			else if (neighbourPositionsFour[z][1] < 0)
			{
				neighbourPositionsFour[z][1] = neighbourPositionsFour[z][1] + sizeY;
			}
		}
		return neighbourPositionsFour;
	}

	public void initGridAppeal(int optionSelect) throws IOException
	{
		if (optionSelect == 1) //randomize the appeal of the starting grid (default)
		{
			for (int x = 0; x < world.getSizeX(); x++)
			{
				for (int y = 0; y < world.getSizeY(); y++)
				{
					world.getTile(x, y).setAppeal(prng.nextInt(5));
				}
			}
		}

		else if (optionSelect == 3) //set the appeal to 0 for all tiles of the starting grid
		{
			for (int x = 0; x < world.getSizeX(); x++)
			{
				for (int y = 0; y < world.getSizeY(); y++)
				{
					world.getTile(x, y).setAppeal(0);;
				}
			}
		}
		else
		{
			System.out.println("no Grid initialization option selected.");
		}
		countAppealData();
	}

	public int[] collectPopulationData()
	{
		df2.setRoundingMode(RoundingMode.HALF_EVEN);
		int[] happiness = { 0, 0 };
		int[] greenhappy = { 0, 0 };
		int[] bluehappy = { 0, 0 };
		double[] avgF0 = { 0, 0 }; // blue, green
		population[0] = 0; // resetting the global population value since we use a counter
		population[1] = 0;
		for (int x = 0; x < world.getSizeX(); x++)
		{
			for (int y = 0; y < world.getSizeY(); y++)
			{
				Tile currentTile = world.getTile(x, y);
				boolean currentTileHasAgent = currentTile.hasAgent();
				if (currentTileHasAgent == true)
				{
					for (int i = 0; i < allAgents.size(); i++)
					{
						if (currentTile.getPosition()[0] == allAgents.get(i).getPosition()[0])
						{
							if (currentTile.getPosition()[1] == allAgents.get(i).getPosition()[1])
							{
								double t = allAgents.get(i).getThreshold();
								double tt = currentTile.getT();
								if (!(t == tt))
								{
									print("<<collectPopulationData>> t" + t + "/" + "tt" + tt + ",isB? " + allAgents.get(i).isBlue + ", H: " + allAgents.get(i).isHappy + ". Errors: " + errorcounter[0] + "/" + errorcounter[1]);
								}
							}
						}
					}

					if (currentTile.isAgentHappy() == 1)
					{
						happiness[1]++;
					}
					else
					{
						happiness[0]++;
					}

					boolean isCurrentAgentBlue = currentTile.isAgentBlue();
					if (isCurrentAgentBlue == true)
					{
						avgF0[0] += currentTile.getT();
						population[0]++;
						if (currentTile.isAgentHappy() == 1)
						{
							bluehappy[1]++;
						}
						else
						{
							bluehappy[0]++;
						}
					}
					else if (isCurrentAgentBlue == false)
					{
						avgF0[1] += currentTile.getT();
						population[1]++;
						if (currentTile.isAgentHappy() == 1)
						{
							greenhappy[1]++;
						}
						else
						{
							greenhappy[0]++;
						}
					}
					else
					{
						System.out.println("<<collectPopulationData>> Tile has agent but neither blue nor green. Something's gone wrong");
					}
				}
			}
		}

		if (allAgents.size() > 0)
		{
			numBlues = population[0];
			numGreens = population[1];
			averageTB = (avgF0[0] / numBlues);
			averageTG = (avgF0[1] / numGreens);

			// here we collect the global happiness. all happy agents divided by total number of agents = average happiness [0,1]
			globalHappiness = (double) happiness[1] / allAgents.size();
			globalHappinessB = (double) bluehappy[1] / numBlues;
			globalHappinessG = (double) greenhappy[1] / numGreens;
			meanBlue = numBlues * 1.0 / allAgents.size();
		}

		return population;
	}

	public int[] convertToColourArray()
	{
		int[] x = new int[sizeX * sizeY];
		int[] pos;
		for (int i = 1; i < sizeX * sizeY; i++)
		{
			pos = singleDimension(i);
			if (world.getTile(pos[0], pos[1]).hasAgent() && world.getTile(pos[0], pos[1]).isAgentBlue())
			{
				x[i] = 1;
			}
			else
				x[i] = 0;
		}
		return x;
	}

	public double[] convertToTolArray()
	{
		double[] x = new double[sizeX * sizeY];
		int[] pos;
		for (int i = 1; i < sizeX * sizeY; i++)
		{
			pos = singleDimension(i);
			if (world.getTile(pos[0], pos[1]).hasAgent())
			{
				x[i] = world.getTile(pos[0], pos[1]).getT();
			}
		}
		return x;
	}

	public void countAppealData()
	{
		int[] appealRatings = { 0, 0, 0, 0, 0, 0 };// 0 rating, 1 rating, 2
													// rating, 3 rating, 4
													// rating, 5 rating
		for (int x = 0; x < world.getSizeX(); x++)
		{
			for (int y = 0; y < world.getSizeY(); y++)
			{
				int currAppeal = world.getTile(x, y).getAppeal();
				if (currAppeal == 0)
				{
					appealRatings[0]++;
				}
				else if (currAppeal == 1)
				{
					appealRatings[1]++;
				}
				else if (currAppeal == 2)
				{
					appealRatings[2]++;
				}
				else if (currAppeal == 3)
				{
					appealRatings[3]++;
				}
				else if (currAppeal == 4)
				{
					appealRatings[4]++;
				}
				else if (currAppeal == 5)
				{
					appealRatings[5]++;
				}

				else
				{
					print("<<countAppealData>> Appeal rating not between 0 and 5, or -1:" + currAppeal);
				}

			}

		}
	}

	public List<Double> collectThresholdALL()
	{
		List<Double> allThresholds = new ArrayList<Double>();
		for (int i = 0; i < allAgents.size(); i++)
		{
			allThresholds.add(allAgents.get(i).getThreshold());
		}
		return allThresholds;
	}

	public List<Double> collectThresholdNat()
	{
		List<Double> natThresholds = new ArrayList<Double>();
		for (int i = 0; i < allAgents.size(); i++)
		{
			if (!allAgents.get(i).isBlue)
			{
				natThresholds.add(allAgents.get(i).getThreshold());
			}
		}
		return natThresholds;
	}

	public List<Double> collectThresholdMig()
	{
		List<Double> migThresholds = new ArrayList<Double>();
		for (int i = 0; i < allAgents.size(); i++)
		{
			if (allAgents.get(i).isBlue)
			{
				migThresholds.add(allAgents.get(i).getThreshold());
			}
		}
		return migThresholds;
	}

	/**    
	 */
	public double calculateMoransT(double average)
	{
		double[] x = convertToTolArray();
		double a = 0;
		double sumw = 0;
		double b = 0;
		double result = 0;
		double S = allAgents.size();

		for (int i = 1; i < x.length; i++)
		{
			for (int j = 1; j < x.length; j++)
			{
				a += ((x[i] - average) * (x[j] - average)) * w(i, j);
				sumw += w(i, j);
			}
			b += Math.pow(((double) x[i] - average), 2);
		}
		result = (S * a) / (sumw * b);
		//print("<<calculateMoransT>> result = (S*a)/(sumw*n)" + S + "*" + a + " / " + sumw + " * " + b + " = " + result);
		return result;
	}

	public double calculateMoransI(double avgBlue)
	{
		int[] x = convertToColourArray();
		double a = 0;
		double sumw = 0;
		double b = 0;
		double result = 0;
		double S = allAgents.size();

		for (int i = 1; i < x.length; i++)
		{
			for (int j = 1; j < x.length; j++)
			{
				a += ((x[i] - avgBlue) * (x[j] - avgBlue)) * w(i, j);
				sumw += w(i, j);

			}
			b += Math.pow(((double) x[i] - avgBlue), 2);
		}
		result = (S * a) / (sumw * b);
		return result;
	}

	/**calculates euclidian distance between two points i and j*/
	public double euclDist(int i, int j)
	{
		//first convert the 1-dimensional points into x,y
		int ix = singleDimension(i)[0];
		int iy = singleDimension(i)[1];
		int jx = singleDimension(j)[0];
		int jy = singleDimension(j)[1];

		double s = (Math.pow((ix - jx), 2)) + (Math.pow((iy - jy), 2));
		return s;
	}

	public int w(int i, int j) // takes two tiles and returns 1 if tiles are adjacent
	{
		// what distinguishes neighbouring tiles? They must be a maximum of one
		// row & one column away
		// so if rownumber OR columnumber difference is greater than 1 = can't
		// be adjacent
		// BUT the word wraps around so the column number has to be reset
		int isNB = 0;
		int ix = singleDimension(i)[0];
		int iy = singleDimension(i)[1];
		int jx = singleDimension(j)[0];
		int jy = singleDimension(j)[1];

		if (ix == sizeX - 1)
		{
			ix = ix - (sizeX - 1);
		}
		if (iy == sizeY - 1)
		{
			iy = iy - (sizeY - 1);
		}
		if (jx == sizeX - 1)
		{
			jx = jx - (sizeX - 1);
		}
		if (jy == sizeY - 1)
		{
			jy = jy - (sizeY - 1);
		}

		if (ix - jx > 1 || ix - jx < -1)
		{
			isNB = 0;
		}
		else if (iy - jy > 1 || iy - jy < -1)
		{
			isNB = 0;
		}
		else
		{
			isNB = 1;
		}
		return isNB;
	}

	public int[] singleDimension(int i)
	{
		int y = i / sizeY;
		int x = i % sizeX;
		int[] coords = { x, y };
		return coords;
	}

	public void collectData()
	{
		collectPopulationData();

		List<DescriptiveStatistics> allTdata = collectMoreTolData();
		DescriptiveStatistics tolstats = allTdata.get(0);
		DescriptiveStatistics ntolstats = allTdata.get(1);
		DescriptiveStatistics mtolstats = allTdata.get(2);

		double morans = calculateMoransI(meanBlue);
		double moranTol = calculateMoransT(tolstats.getMean());
		mC = morans;
		mT = moranTol;
		//print("<<collectData>> morans is " + morans + ", moranTol is " + moranTol);
		String moranStr = "" + morans;
		String moranTolStr = "" + moranTol;
		String globalHappyStr = "" + globalHappiness;
		String globalBlueHappy = "" + globalHappinessB;
		String globalGreenHappy = "" + globalHappinessG;
		String numAgentStr = "" + allAgents.size();
		String totalMoveStr = String.valueOf(globalMoveCounter);
		String numblue = String.valueOf(population[0]);
		String numgreen = String.valueOf(population[1]);
		String changedMindStr = "" + changedMind;

		String tolN = "" + tolstats.getN();
		String tolmean = "" + tolstats.getMean();
		String tolvar = "" + tolstats.getVariance();
		String tolstdDev = "" + tolstats.getStandardDeviation();
		String tolKurt = "" + tolstats.getKurtosis();
		String tolSkew = "" + tolstats.getSkewness();

		String ntolN = "" + ntolstats.getN();
		String ntolmean = "" + ntolstats.getMean();
		String ntolvar = "" + ntolstats.getVariance();
		String ntolstdDev = "" + ntolstats.getStandardDeviation();
		String ntolKurt = "" + ntolstats.getKurtosis();
		String ntolSkew = "" + ntolstats.getSkewness();

		String mtolN = "" + mtolstats.getN();
		String mtolmean = "" + mtolstats.getMean();
		String mtolvar = "" + mtolstats.getVariance();
		String mtolstdDev = "" + mtolstats.getStandardDeviation();
		String mtolKurt = "" + mtolstats.getKurtosis();
		String mtolSkew = "" + mtolstats.getSkewness();

		String theString[] = null;

		theString = new String[] { moranStr, moranTolStr, totalMoveStr, globalHappyStr, globalBlueHappy, globalGreenHappy, numAgentStr, numblue, numgreen, changedMindStr, tolN, tolmean, tolvar,
				tolstdDev, tolKurt, tolSkew, ntolN, ntolmean, ntolvar, ntolstdDev, ntolKurt, ntolSkew, mtolN, mtolmean, mtolvar, mtolstdDev, mtolKurt, mtolSkew };

		writer.writeNext(theString);
	}

	/**
	 * Method to generate a set of w unique numbers designed to help agents
	 * chose random empty tiles
	 **/
	public int[] generateUniqueNumbers(int w)
	{
		if (w > emptyTiles.size())
		{
			w = emptyTiles.size();
			// print("<<generateUniqueNumbers>> w is " + w + ", but empty Tiles remaining are " +
			// emptyTiles.size() + ". w = " + emptyTiles.size());
		}
		ArrayList<Integer> list = new ArrayList<Integer>();
		int[] listOfW = new int[w];
		for (int i = 0; i < emptyTiles.size(); i++)
		{
			list.add(new Integer(i));
		}
		Collections.shuffle(list, new Random(System.nanoTime()));

		for (int i = 0; i < w; i++)
		{
			listOfW[i] = list.get(i);
		}

		return listOfW;
	}

	public List<DescriptiveStatistics> collectMoreTolData()
	{
		List<DescriptiveStatistics> returnList = new ArrayList<>();
		DescriptiveStatistics tolstats = new DescriptiveStatistics();
		List<Double> toldata = collectThresholdALL();
		for (int t = 0; t < toldata.size(); t++)
		{
			tolstats.addValue(toldata.get(t));
		}

		DescriptiveStatistics natTolStats = new DescriptiveStatistics();
		List<Double> ntoldata = collectThresholdNat();
		for (int n = 0; n < ntoldata.size(); n++)
		{
			natTolStats.addValue(ntoldata.get(n));
		}

		DescriptiveStatistics migTolStats = new DescriptiveStatistics();
		List<Double> mtoldata = collectThresholdMig();
		for (int m = 0; m < mtoldata.size(); m++)
		{
			migTolStats.addValue(mtoldata.get(m));
		}
		returnList.add(tolstats);
		returnList.add(natTolStats);
		returnList.add(migTolStats);

		return returnList;
	}

	public Integer getTick()
	{
		return tick;
	}

	@Override
	public void update(Observable o, Object arg)
	{
	}

	public void fireEvent(Object event)
	{
		setChanged();
		notifyObservers(event);
	}

	public World getWorld()
	{
		return world;
	}

	public Simulation getSimulation()
	{
		return this;
	}

	public Agent getAgent(int[] posWeWant)
	{
		Agent ourAgent = null;

		for (int i = 0; i < allAgents.size(); i++)
		{
			if (allAgents.get(i).pos[0] == posWeWant[0] && allAgents.get(i).pos[1] == posWeWant[1])
			{
				ourAgent = allAgents.get(i);
				break;
			}
		}

		return ourAgent;
	}

	public Grid getGrid()
	{
		return grid;
	}

	public int randomizeAppeal()
	{
		Random rng = new Random();
		int appeal = rng.nextInt(5);
		return appeal;
	}

	/** Method to calculate HOW BIG an influx should be, given a percentage */
	public int calculateInfluxHOWBIG(int influxpercent, double numofagents)
	{
		double pc = influxpercent * 1.0;
		Double tempPc = (pc / 100);
		Double percentOfCurrentPop = numofagents * tempPc;
		int percentCurrentPop = percentOfCurrentPop.intValue();
		return percentCurrentPop;
	}

	/**
	 * Method to calculate WHEN influxes should occur, given a set amount of
	 * fluxes
	 */
	public ArrayList<int[]> calculateInfluxWHEN(int noofinfluxes)
	{
		Double maxticks = maxTicks * 1.0;
		//Double fivPCofmax = maxticks * 0.05; // say, of 1000 maxticks, 5% = 50 ticks
		Double tenPCofmax = maxticks * 0.1; // say, of 1000 maxticks, 10% = 100 ticks

		int starttick = tenPCofmax.intValue(); // starttick would be 100
		//int starttick = fivPCofmax.intValue(); // starttick would be 50
		int endtick = maxTicks - starttick; // endTick would be 1000-100 = 900
		int totalconsideredticks = endtick - starttick; // 900-100 = 800
		int ticknumber = totalconsideredticks / noofinfluxes; // so if influxes = 4, we'd have 800/4=200. so every 200 ticks, influx
		if (ticknumber < 1)
		{
			print("<<calculateInfluxWHEN>>" + totalconsideredticks + " < " + noofinfluxes + ". Too few ticks for even distribution.");
			System.exit(0);
		}

		current_size = maxfluxsize / noofinfluxes;
		int reach = current_size * noofinfluxes;
		int diff = maxfluxsize - reach;

		// List of int[], length is = noofinfluxes. so for influx 15, length is 15 x 2[actual tick, actual size]
		ArrayList<int[]> ticksAndSizes = new ArrayList<int[]>();
		for (int i = 0; i < noofinfluxes; i++)
		{
			int thetick = starttick + (ticknumber * i);
			int[] meh = { thetick, current_size };
			ticksAndSizes.add(meh);
		}

		// because the number of influxes is preset, the size of each influx has
		// to be calculated using maximum agents and number of influxes.
		// the problem is that due to divisions resulting in fractions (and
		// agents must be whole integers), the accuracy of the estimated
		// migrants to flux goes down the higher the number of fluxes is.
		// Accuracy is perfect for 1x, close enough for 4x, sometimes off for
		// x15 and wildly inaccurate for 100x. The differences are between 0.04
		// and 3.48% density. The threshold that should not be exceeded is
		// 0.24%.
		// that is less than a quarter of density different and can be rounded
		// down. I.e. 87.24% density is rounded down to 87%. But if the
		// actual density differs by more than that, the missing agents should
		// be added.

		if (diff > 6) // the difference of agents that should enter, and that do enter, should not exceed 6, which is 0.24% density
		{
			for (int p = 0; p < diff; p++)
			{
				int rnd = new Random().nextInt(ticksAndSizes.size());
				int[] data = new int[2];
				data = ticksAndSizes.get(rnd); // pick a random entry from the tick list
				data[1] = data[1] + 1; // add one agent to that entry. i.e.: tick 630, 9 agents -> tick 630, 10 agents
				ticksAndSizes.set(rnd, data);
				// print("<<calculateInfluxWHEN>>" + p + 1 + ") at tick " + ticksAndSizes.get(rnd)[0] + "
				// there are now " + data[1] + " agents instead of " + current_size + ".");
			}
		}
		return ticksAndSizes;
	}

	static <T> void print(T p)
	{
		System.out.println(p);
	}

	@Override
	public void run()
	{
	}
}