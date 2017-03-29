package events;
/**
 * Migrationscape version 2.2
 * A version of the Schelling segregation model with adaptive tolerance.
 * Author: Linda Urselmans
 * University of Essex
 *  **/
public class Pauser
{

	private boolean isPaused = false;

	public synchronized void pause()
	{
		isPaused = true;
	}

	public synchronized void resume()
	{
		isPaused = false;
		notifyAll();
	}

	public synchronized void look() throws InterruptedException
	{
		while (isPaused == true)
		{
			wait();
		}
	}

}