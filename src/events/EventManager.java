package events;

import java.util.Observable;
/**
 * Migrationscape version 2.2
 * A version of the Schelling segregation model with adaptive tolerance.
 * Author: Linda Urselmans
 * University of Essex
 *  **/
public class EventManager extends Observable
{
	public void fireEvent(Object event)
	{
		setChanged();
		notifyObservers(event);
	}
}
