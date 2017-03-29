package migscape;
/**
 * Migrationscape version 2.2
 * A version of the Schelling segregation model with adaptive tolerance.
 * Author: Linda Urselmans
 * University of Essex
 *  **/
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

import events.AgentMovedEvent;

public class World extends Observable implements Observer
{

	int sizeX;
	int sizeY;
	Random prng = new Random();
	Tile theWorld[][];

	public World(int sizeX, int sizeY)
	{
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		theWorld = new Tile[sizeX][sizeY];

		for (int x = 0; x < sizeX; x++) //iterate through x
		{
			for (int y = 0; y < sizeY; y++) //then through y; the 2d array is done
			{
				theWorld[x][y] = new Tile(x, y);
				// for each x-y loop, create a new Tile with the Position of the current x/y in the loop
				// and these values are also the index positions of theWorld array. 
				// So theWorld[0][0] has a Tile with the position [0][0]
			}
		}

	}

	/**
	 * This method takes a Position as argument, then
	 * checks which Tile has the same Position as the
	 * Position argument, and returns that tile
	 * @parameter Position
	 * @return Tile
	 */
	public Tile getTile(int x, int y)
	{
		if (x >= sizeX || x < 0)
		{
			print("X index out of bounds. Supplied Index: " + x + ", Accepted value: 0 - " + (sizeX - 1)
					+ ". <<world.getTile>>");
			return null;
		}
		if (y >= sizeY || y < 0)
		{
			print("Y index out of bounds. Supplied Index: " + y + ", Accepted value: 0 - " + (sizeY - 1)
					+ ". <<world.getTile>>");
			return null;
		}
		return theWorld[x][y];
	}

	public void removeAgentFromTile(int x, int y)
	{
		if (x >= sizeX || x < 0)
		{
			print("X index out of bounds. Supplied Index: " + x + ", Accepted value: 0 - " + (sizeX - 1)
					+ ". <<world.removeAgentFromTile>>");
			return;
		}
		if (y >= sizeY || y < 0)
		{
			print("Y index out of bounds. Supplied Index: " + y + ", Accepted value: 0 - " + (sizeY - 1)
					+ ". <<world.removeAgentFromTile>>");
			return;
		}
		theWorld[x][y].removeAgent();
	}

	public boolean placeAgentOnTile(int x, int y, boolean isBlue, int isHappy, double T)
	{
		if (x >= sizeX || x < 0)
		{
			print("X index out of bounds. Supplied Index: " + x + ", Accepted value: 0 - " + (sizeX - 1)
					+ ". <<world.removeAgentFromTile>>");
			return false;
		}
		if (y >= sizeY || y < 0)
		{
			print("Y index out of bounds. Supplied Index: " + y + ", Accepted value: 0 - " + (sizeY - 1)
					+ ". <<world.removeAgentFromTile>>");
			return false;
		}
		//print("x: " + x + " y: " + y);
		if (theWorld[x][y].hasAgent)
		{
			print("Tile " + theWorld[x][y].getPosition()[0] + "," + theWorld[x][y].getPosition()[1] + " already has an agent.");
			return false;
		}
		return theWorld[x][y].addAgent(isBlue, isHappy, T);

	}

	public int getSizeX()
	{
		return sizeX;
	}

	public int getSizeY()
	{
		return sizeY;
	}

	public void removeBelongsToGroupFlag()
	{
		for (int x = 0; x < sizeX; x++)
		{
			for (int y = 0; y < sizeY; y++)
			{
				theWorld[x][y].belongsToGroup = false;
			}
		}
	}

	public void setBelongsToGroupFlag(int x, int y)
	{
		theWorld[x][y].belongsToGroup = true;
	}

	public void removeBelongsToInflux()
	{
		for (int x = 0; x < sizeX; x++)
		{
			for (int y = 0; y < sizeY; y++)
			{
				theWorld[x][y].belongsToInflux = false;
			}
		}
	}
	
	public void removeAllCandidateInfo()
	{
		for (int x = 0; x < sizeX; x++)
		{
			for (int y = 0; y < sizeY; y++)
			{
				theWorld[x][y].isBestStart = false;
				theWorld[x][y].isStartCandidate = false;
			}
		}
	}

	public void setBelongsToInflux(int x, int y)
	{
		theWorld[x][y].belongsToInflux = true;
	}
	
	
	static <T> void print(T p)
	{
		System.out.println(p);
	}

	@Override
	public void update(Observable o, Object arg)
	{
		if (arg instanceof AgentMovedEvent)
		{
		}
	}

}