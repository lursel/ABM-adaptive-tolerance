package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import events.EventManager;
import migscape.World;

/**
 * Migrationscape version 2.2
 * A version of the Schelling segregation model with adaptive tolerance.
 * Author: Linda Urselmans
 * University of Essex
 *  **/
@SuppressWarnings("serial")
public class MigFrame extends JFrame
{
	Grid grid;
	Grid gridF;
	UserControls ui;
	Parameters para;


	public MigFrame(World world, EventManager event, int[] rules)
	{
		super("MigScape 3.0");	// creates frame, the constructor uses a string argument for the frame title
		JPanel mainPanel = new JPanel(new GridLayout(5, 5));
		grid = new Grid(world, false);
		gridF = new Grid(world, true);
		ui = new UserControls(grid, event, rules);
		para = new Parameters(event, rules);

		mainPanel.add(grid, BorderLayout.CENTER);
		mainPanel.add(ui, BorderLayout.NORTH);
		mainPanel.add(para, BorderLayout.SOUTH);
		mainPanel.setSize(world.getSizeX()*10, world.getSizeY()*10);
		grid.setSize(new Dimension(world.getSizeX()*10, world.getSizeY()*10));
		gridF.setSize(new Dimension(world.getSizeX()*10, world.getSizeY()*10));
		ui.setSize(new Dimension(world.getSizeY()*10, 50));
		para.setSize(new Dimension(world.getSizeY()*10, 50));
		this.add(grid, BorderLayout.CENTER);
		this.add(gridF, BorderLayout.EAST);
		this.add(ui, BorderLayout.NORTH);
		this.add(para, BorderLayout.SOUTH);
		this.validate();
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);  // specifies what happens when user closes the frame. exit_on_close means the program will stop
		grid.setBackground(Color.WHITE);
		gridF.setBackground(Color.WHITE);
		ui.setBackground(Color.WHITE);
		para.setBackground(Color.WHITE);
	}

	public UserControls getUserControls()
	{
		return ui;
	}

	public Grid getGrid()
	{
		return grid;
	}
	
	public Parameters getParameters()
	{
		return para;
	}

	public void open()
	{
		setVisible(true);
		pack();

		Timer t = new Timer(10, new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				grid.repaint();
				gridF.repaint();
			}

		});
		t.start();

	}
}
