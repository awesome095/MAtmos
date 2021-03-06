package net.minecraft.src;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.Minecraft;
import paulscode.sound.SoundSystem;
import eu.ha3.matmos.conv.CustomVolume;
import eu.ha3.matmos.conv.ReplicableSoundRelay;
import eu.ha3.mc.haddon.PrivateAccessException;

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

public class MAtSoundManagerMaster implements ReplicableSoundRelay, CustomVolume
{
	// XXX Implement me: Does not do anything and sndcomms is down
	
	private MAtMod mod;
	
	private float volume;
	final private float defSoundVolume = 1F;
	
	private int nbTokens;
	private Map<String, String> soundequivalences;
	
	private float settingsVolume;
	
	private Random random;
	
	public MAtSoundManagerMaster(MAtMod mAtmosHaddon)
	{
		this.mod = mAtmosHaddon;
		
		this.volume = this.defSoundVolume;
		
		this.nbTokens = 0;
		this.soundequivalences = new HashMap<String, String>();
		
		this.settingsVolume = 0F;
		this.random = new Random();
		
	}
	
	public SoundSystem sndSystem()
	{
		return this.mod.getSoundCommunicator().getSoundSystem();
		
	}
	
	@Override
	public float getVolume()
	{
		return this.volume;
		
	}
	
	@Override
	public void routine()
	{
		updateSettingsVolume();
	}
	
	@Override
	public void cacheSound(String path)
	{
		getSound(path);
		
	}
	
	@SuppressWarnings("static-access")
	public String getSound(String soundPath)
	{
		if (this.soundequivalences.containsKey(soundPath))
			return this.soundequivalences.get(soundPath);
		
		File soundFile = new File(this.mod.manager().getMinecraft().getMinecraftDir() + "/resources", soundPath);
		
		// FIXME DO IT BETTER
		String path = new StringBuilder().append(soundPath).toString();
		int j = path.indexOf("/");
		int t = path.indexOf(".");
		String quant = path.substring(j + 1, t);
		String dotted = quant.replaceAll("/", ".");
		dotted = dotted.replaceAll("0", "");
		dotted = dotted.replaceAll("1", "");
		dotted = dotted.replaceAll("2", "");
		dotted = dotted.replaceAll("3", "");
		dotted = dotted.replaceAll("4", "");
		dotted = dotted.replaceAll("5", "");
		dotted = dotted.replaceAll("6", "");
		dotted = dotted.replaceAll("7", "");
		dotted = dotted.replaceAll("8", "");
		dotted = dotted.replaceAll("9", "");
		
		this.soundequivalences.put(soundPath, dotted);
		
		if (!soundFile.exists())
		{
			MAtMod.LOGGER.warning("File " + soundPath + " is missing " + " (" + dotted + ")");
		}
		
		return dotted;
		
	}
	
	@Override
	public void playSound(String path, float volume, float pitch, int meta)
	{
		Minecraft mc = this.mod.manager().getMinecraft();
		float nx = (float) mc.thePlayer.posX;
		float ny = (float) mc.thePlayer.posY;
		float nz = (float) mc.thePlayer.posZ;
		
		String equivalent = getSound(path);
		
		float soundEffectiveVolume = getVolume() * volume;
		
		if (soundEffectiveVolume <= 0)
			return;
		
		if (meta > 0)
		{
			double angle = this.random.nextFloat() * 2 * Math.PI;
			nx = nx + (float) (Math.cos(angle) * meta);
			ny = ny + this.random.nextFloat() * meta * 0.2F - meta * 0.01F;
			nz = nz + (float) (Math.sin(angle) * meta);
			
			this.mod.getSoundCommunicator().playSound(equivalent, nx, ny, nz, soundEffectiveVolume, pitch, 0, 0F);
		}
		else
		{
			// NOTE: playSoundFX from Minecraft SoundManager
			//   does NOT work (actually, only works for stereo sounds).
			// Must use playSoundFX Proxy
			//   which will play the sound 2048 blocks above the player...
			//   ...and that somehow does the trick!
			
			ny = ny + 2048;
			this.mod.getSoundCommunicator().playSound(equivalent, nx, ny, nz, soundEffectiveVolume, pitch, 0, 0F);
			
		}
	}
	
	@Override
	public synchronized int getNewStreamingToken()
	{
		int token = this.nbTokens;
		this.nbTokens = this.nbTokens + 1;
		
		return token;
	}
	
	@Override
	public synchronized boolean setupStreamingToken(int token, String path, float volume, float pitch)
	{
		// Master NEVER manages stream playback.
		
		return true;
	}
	
	@Override
	public synchronized void startStreaming(int token, float fadeDuration, int timesToPlay)
	{
		// Master NEVER manages stream playback.
	}
	
	@Override
	public synchronized void stopStreaming(int token, float fadeDuration)
	{
		// Master NEVER manages stream playback.
	}
	
	@Override
	public synchronized void pauseStreaming(int token, float fadeDuration)
	{
		// Master NEVER manages stream playback.
	}
	
	@Override
	public synchronized void eraseStreamingToken(int token)
	{
		// Master NEVER manages stream playback.
	}
	
	@Override
	public void setVolume(float modifier)
	{
		this.volume = modifier;
		
	}
	
	private void updateSettingsVolume()
	{
		Minecraft mc = this.mod.manager().getMinecraft();
		
		if (this.settingsVolume != mc.gameSettings.soundVolume)
		{
			this.settingsVolume = mc.gameSettings.soundVolume;
		}
	}
	
	public float getSettingsVolume()
	{
		return this.settingsVolume;
	}
	
	public SoundPoolEntry getSoundPoolEntryOf(String path)
	{
		try
		{
			// soundPoolSounds
			return ((SoundPool) this.mod.util().getPrivateValueLiteral(
				net.minecraft.src.SoundManager.class, this.mod.manager().getMinecraft().sndManager, "b", 1))
				.getRandomSoundFromSoundPool(getSound(path));
		}
		catch (PrivateAccessException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public SoundSystem getSoundSystem()
	{
		return this.mod.getSoundCommunicator().getSoundSystem();
	}
	
	public MAtSoundManagerChild createChild()
	{
		return new MAtSoundManagerChild(this);
	}
	
}
