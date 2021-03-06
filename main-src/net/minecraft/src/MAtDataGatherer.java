package net.minecraft.src;

import java.util.ArrayList;
import java.util.List;

import eu.ha3.matmos.conv.ProcessorModel;
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

public class MAtDataGatherer
{
	final static String INSTANTS = "Instants";
	final static String DELTAS = "Deltas";
	final static String LARGESCAN = "LargeScan";
	final static String SMALLSCAN = "SmallScan";
	final static String LARGESCAN_THOUSAND = "LargeScanPerMil";
	final static String SMALLSCAN_THOUSAND = "SmallScanPerMil";
	final static String SPECIAL_LARGE = "SpecialLarge";
	final static String SPECIAL_SMALL = "SpecialSmall";
	final static String CONTACTSCAN = "ContactScan";
	final static String CONFIGVARS = "ConfigVars";
	
	final static String POTIONPOWER = "PotionEffectsPower";
	final static String POTIONDURATION = "PotionEffectsDuration";
	
	final static String CURRENTITEM_E = "CurrentItemEnchantments";
	final static String ARMOR1_E = "Armor1Enchantments";
	final static String ARMOR2_E = "Armor2Enchantments";
	final static String ARMOR3_E = "Armor3Enchantments";
	final static String ARMOR4_E = "Armor4Enchantments";
	
	final static int COUNT_WORLD_BLOCKS = 4096;
	final static int COUNT_INSTANTS = 128;
	final static int COUNT_CONFIGVARS = 256;
	
	final static int COUNT_POTIONEFFECTS = 32;
	final static int COUNT_ENCHANTMENTS = 64;
	
	final static int MAX_LARGESCAN_PASS = 10;
	private static final int ENTITYIDS_MAX = 256;
	
	private MAtMod mod;
	
	private MAtScanVolumetricModel largeScanner;
	private MAtScanVolumetricModel smallScanner;
	
	private MAtScanCoordsPipeline largePipeline;
	private MAtScanCoordsPipeline smallPipeline;
	
	private ProcessorModel relaxedProcessor;
	private ProcessorModel frequentProcessor;
	private ProcessorModel contactProcessor;
	private ProcessorModel configVarsProcessor;
	
	private MAtProcessorEnchantments enchantmentsCurrentItem;
	private MAtProcessorEnchantments enchantmentsArmor1;
	private MAtProcessorEnchantments enchantmentsArmor2;
	private MAtProcessorEnchantments enchantmentsArmor3;
	private MAtProcessorEnchantments enchantmentsArmor4;
	
	private MAtProcessorPotionQuality potionPowerProcessor;
	private MAtProcessorPotionQuality potionDurationProcessor;
	
	private MAtProcessorEntityDetector detect2;
	private MAtProcessorEntityDetector detect5;
	private MAtProcessorEntityDetector detect10;
	private MAtProcessorEntityDetector detect20;
	
	private List<ProcessorModel> additionalRelaxedProcessors;
	private List<ProcessorModel> additionalFrequentProcessors;
	
	private Data data;
	
	private int cyclicTick;
	
	private long lastLargeScanX;
	private long lastLargeScanY;
	private long lastLargeScanZ;
	private int lastLargeScanPassed;
	
	public MAtDataGatherer(MAtMod mAtmosHaddon)
	{
		this.mod = mAtmosHaddon;
		
	}
	
	private void resetRegulators()
	{
		this.lastLargeScanPassed = MAX_LARGESCAN_PASS;
		this.cyclicTick = 0;
	}
	
	public void load()
	{
		resetRegulators();
		
		this.additionalRelaxedProcessors = new ArrayList<ProcessorModel>();
		this.additionalFrequentProcessors = new ArrayList<ProcessorModel>();
		
		this.data = new Data();
		prepareSheets();
		
		this.largeScanner = new MAtScanVolumetricModel(this.mod);
		this.smallScanner = new MAtScanVolumetricModel(this.mod);
		
		this.largePipeline = new MAtPipelineIDAccumulator(this.mod, this.data, LARGESCAN, LARGESCAN_THOUSAND, 1000);
		this.smallPipeline = new MAtPipelineIDAccumulator(this.mod, this.data, SMALLSCAN, SMALLSCAN_THOUSAND, 1000);
		
		this.largeScanner.setPipeline(this.largePipeline);
		this.smallScanner.setPipeline(this.smallPipeline);
		
		this.relaxedProcessor = new MAtProcessorRelaxed(this.mod, this.data, INSTANTS, DELTAS);
		this.frequentProcessor = new MAtProcessorFrequent(this.mod, this.data, INSTANTS, DELTAS);
		this.contactProcessor = new MAtProcessorContact(this.mod, this.data, CONTACTSCAN, null);
		this.configVarsProcessor = new MAtProcessorCVARS(this.mod, this.data, CONFIGVARS, null);
		
		this.enchantmentsCurrentItem = new MAtProcessorEnchantments(this.mod, this.data, CURRENTITEM_E, null) {
			@Override
			protected ItemStack getItem(EntityPlayer player)
			{
				return player.inventory.getCurrentItem();
			}
		};
		this.enchantmentsArmor1 = new MAtProcessorEnchantments(this.mod, this.data, ARMOR1_E, null) {
			@Override
			protected ItemStack getItem(EntityPlayer player)
			{
				return player.inventory.armorInventory[0];
			}
		};
		this.enchantmentsArmor2 = new MAtProcessorEnchantments(this.mod, this.data, ARMOR2_E, null) {
			@Override
			protected ItemStack getItem(EntityPlayer player)
			{
				return player.inventory.armorInventory[1];
			}
		};
		this.enchantmentsArmor3 = new MAtProcessorEnchantments(this.mod, this.data, ARMOR3_E, null) {
			@Override
			protected ItemStack getItem(EntityPlayer player)
			{
				return player.inventory.armorInventory[2];
			}
		};
		this.enchantmentsArmor4 = new MAtProcessorEnchantments(this.mod, this.data, ARMOR4_E, null) {
			@Override
			protected ItemStack getItem(EntityPlayer player)
			{
				return player.inventory.armorInventory[3];
			}
		};
		
		this.potionPowerProcessor = new MAtProcessorPotionQuality(this.mod, this.data, POTIONPOWER, null) {
			@Override
			protected int getQuality(PotionEffect effect)
			{
				return effect.getAmplifier() + 1;
			}
		};
		this.potionDurationProcessor = new MAtProcessorPotionQuality(this.mod, this.data, POTIONDURATION, null) {
			@Override
			protected int getQuality(PotionEffect effect)
			{
				return effect.getDuration();
			}
		};
		
		this.detect2 =
			new MAtProcessorEntityDetector(this.mod, this.data, "Detect2", "Detect2_Deltas", 2, ENTITYIDS_MAX);
		this.detect5 =
			new MAtProcessorEntityDetector(this.mod, this.data, "Detect5", "Detect5_Deltas", 5, ENTITYIDS_MAX);
		this.detect10 =
			new MAtProcessorEntityDetector(this.mod, this.data, "Detect10", "Detect10_Deltas", 10, ENTITYIDS_MAX);
		this.detect20 =
			new MAtProcessorEntityDetector(this.mod, this.data, "Detect20", "Detect20_Deltas", 20, ENTITYIDS_MAX);
		
	}
	
	public Data getData()
	{
		return this.data;
		
	}
	
	public void tickRoutine()
	{
		if (this.cyclicTick % 64 == 0)
		{
			EntityPlayer player = this.mod.manager().getMinecraft().thePlayer;
			long x = (long) Math.floor(player.posX);
			long y = (long) Math.floor(player.posY);
			long z = (long) Math.floor(player.posZ);
			
			if (this.cyclicTick % 256 == 0)
			{
				if (this.lastLargeScanPassed >= MAX_LARGESCAN_PASS
					|| Math.abs(x - this.lastLargeScanX) > 16 || Math.abs(y - this.lastLargeScanY) > 8
					|| Math.abs(z - this.lastLargeScanZ) > 16)
				{
					this.lastLargeScanX = x;
					this.lastLargeScanY = y;
					this.lastLargeScanZ = z;
					this.lastLargeScanPassed = 0;
					this.largeScanner.startScan(x, y, z, 64, 32, 64, 8192, null);
					
				}
				else
				{
					this.lastLargeScanPassed++;
					
				}
				
			}
			this.smallScanner.startScan(x, y, z, 16, 8, 16, 2048, null);
			this.relaxedProcessor.process();
			
			for (ProcessorModel processor : this.additionalRelaxedProcessors)
			{
				processor.process();
			}
			
			this.data.flagUpdate();
			
		}
		//if (this.cyclicTick % 1 == 0) // XXX
		if (true)
		{
			this.contactProcessor.process();
			this.frequentProcessor.process();
			
			this.enchantmentsCurrentItem.process();
			this.enchantmentsArmor1.process();
			this.enchantmentsArmor2.process();
			this.enchantmentsArmor3.process();
			this.enchantmentsArmor4.process();
			
			this.potionPowerProcessor.process();
			this.potionDurationProcessor.process();
			
			this.detect2.process();
			this.detect5.process();
			this.detect10.process();
			this.detect20.process();
			
			for (ProcessorModel processor : this.additionalFrequentProcessors)
			{
				processor.process();
			}
			
			this.data.flagUpdate();
			
		}
		
		if (this.cyclicTick % 2048 == 0)
		{
			this.configVarsProcessor.process();
			
		}
		
		this.largeScanner.routine();
		this.smallScanner.routine();
		
		this.cyclicTick = this.cyclicTick + 1;
		
	}
	
	private void prepareSheets()
	{
		createSheet(LARGESCAN, COUNT_WORLD_BLOCKS);
		createSheet(LARGESCAN_THOUSAND, COUNT_WORLD_BLOCKS);
		
		createSheet(SMALLSCAN, COUNT_WORLD_BLOCKS);
		createSheet(SMALLSCAN_THOUSAND, COUNT_WORLD_BLOCKS);
		
		createSheet(CONTACTSCAN, COUNT_WORLD_BLOCKS);
		
		createSheet(INSTANTS, COUNT_INSTANTS);
		createSheet(DELTAS, COUNT_INSTANTS);
		
		createSheet(POTIONPOWER, COUNT_POTIONEFFECTS);
		createSheet(POTIONDURATION, COUNT_POTIONEFFECTS);
		
		createSheet(CURRENTITEM_E, COUNT_ENCHANTMENTS);
		createSheet(ARMOR1_E, COUNT_ENCHANTMENTS);
		createSheet(ARMOR2_E, COUNT_ENCHANTMENTS);
		createSheet(ARMOR3_E, COUNT_ENCHANTMENTS);
		createSheet(ARMOR4_E, COUNT_ENCHANTMENTS);
		
		createSheet(SPECIAL_LARGE, 2);
		createSheet(SPECIAL_SMALL, 1);
		
		createSheet(CONFIGVARS, COUNT_CONFIGVARS);
		
		createSheet("Detect2", ENTITYIDS_MAX);
		createSheet("Detect5", ENTITYIDS_MAX);
		createSheet("Detect10", ENTITYIDS_MAX);
		createSheet("Detect20", ENTITYIDS_MAX);
		createSheet("Detect2_Deltas", ENTITYIDS_MAX);
		createSheet("Detect5_Deltas", ENTITYIDS_MAX);
		createSheet("Detect10_Deltas", ENTITYIDS_MAX);
		createSheet("Detect20_Deltas", ENTITYIDS_MAX);
		
	}
	
	private void createSheet(String name, int count)
	{
		List<Integer> array = new ArrayList<Integer>();
		this.data.sheets.put(name, array);
		for (int i = 0; i < count; i++)
		{
			array.add(0);
			
		}
		
	}
	
}
