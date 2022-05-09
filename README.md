# ABM-adaptive-tolerance
Agent-based model (Schelling) including migration of agents. Agents' tolerance is adaptive.

This is the Java code for the agent-based model.
The main method is located in Simulation.SomController.
Most of the model mechanics reside in Simulation.Simulation.

This project includes a User Interface which can be toggled.

## How you can adjust the model:
The *SimController* class is the controller for the simulation. 
Here, enter all the parameters. If you want to try different settings and scenarios, this is the only class you need to touch.
The *Simulation* class is the main class in which the simulation logic resides, and which makes use of all other classes. Only change things here if you want to change the simulation algorithms.

### SimController model adjustments:
Go to the _public static void main_ method. Here, you can turn the UI on and off- _isUIOFF_ is set to false, therefore the UI is on.
The _runrule_ is the parameter that determines whether you want to manually enter the simulation parameters, or whether you want to have them picked randomly.
The _sinm_ parameter is the number of simulations. If it is greater than 1, you run the model more than once.

Now go to the _public static void runRule_ method.
Rule == 0 means that you want to enter the parameters manually. Here is what they mean:
_simPara_: here you can change one number (set to 100). This determines how many ticks, i.e. how many time steps the simulation runs for.
_mapPara_ (set to 50, 99). The first number, 50, is the ratio of green to blue agents. If you want 75% green agents, set the number to 75. The final density, number 99, is how full the grid should be when all migration has finished. 99 is 99% of the map covered.
_Influx Para_ (set to 0, 0). Here you control whether migration (called influx in the code) occurs at all. If you leave it at 0, the first number, no new agents will enter the grid. Influxcount is the number of migration waves. 1 means one big wave in which all migrants arrive, 100 means that they arrive in 100 small waves.
_addedPara_ (set to 30, 1). They are the parameters w = 30 and m =1. w is the number of tiles that are considered when finding empty space to migrate into. Increasing this decreases performance of the model. The parameter m = 1 sets the rate of change of tolerance for the agents. The tolerance is set to 5% as a minimum to 93% maximum respectively. Therefore, m=1 equals 1%. 

### Further adjustments
In the Simulation class there are some additional adjustments you can make:
If the value of m=1 is too big for you, you can decrease it by dividing it with a number. This happens in the public Simulation() part of the simulation. The line is: 	m = addedPara[1]; // divide by 10 or 100 to decrease m (rate of change of tolerance)
You may notice the simulation pausing after 10 ticks. This is because it is calculating and collection all the data for analysis. You can turn this off by commenting out the line: collectData(); in the method public void run(). CollectData() also writes csv files to your system. They are stored in the folder of the java project under “output”.

