package eu.ha3.matmos.conv;

import java.util.List;

import eu.ha3.matmos.engine.Data;

/*
            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE 
                    Version 2, December 2004 

 Copyright (C) 2004 Sam Hocevar <sam@hocevar.net> 

 Everyone is permitted to copy and distribute verbatim or modified 
 copies of this license document, and changing it is allowed as long 
 as the name is changed. 

            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE 
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION 

  0. You just DO WHAT THE FUCK YOU WANT TO. 
*/

public abstract class ProcessorModel
{
	private Data data;
	
	private String normalName;
	private String deltaName;
	
	private List<Integer> normalSheet;
	private List<Integer> deltaSheet;
	
	public ProcessorModel(Data dataIn, String normalNameIn, String deltaNameIn)
	{
		this.data = dataIn;
		this.normalName = normalNameIn;
		this.deltaName = deltaNameIn;
		
	}
	
	public Data data()
	{
		return this.data;
		
	}
	
	protected abstract void doProcess();
	
	public void process()
	{
		this.normalSheet = data().sheets.get(this.normalName);
		if (this.deltaName != null)
		{
			this.deltaSheet = data().sheets.get(this.deltaName);
		}
		
		doProcess();
		
	}
	
	public void setValue(int index, int newValue)
	{
		int previousValue = this.normalSheet.get(index);
		this.normalSheet.set(index, newValue);
		
		if (this.deltaName != null)
		{
			this.deltaSheet.set(index, newValue - previousValue);
		}
		
	}
	
}
