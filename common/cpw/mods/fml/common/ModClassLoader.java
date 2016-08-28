/*
 * The FML Forge Mod Loader suite.
 * Copyright (C) 2012 cpw
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package cpw.mods.fml.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.common.asm.ASMTransformer;
import cpw.mods.fml.common.asm.transformers.AccessTransformer;
import cpw.mods.fml.common.modloader.BaseModProxy;
import cpw.mods.fml.relauncher.RelaunchClassLoader;
import java.util.HashMap;

/**
 * A simple delegating class loader used to load mods into the system
 *
 *
 * @author cpw
 *
 */
public class ModClassLoader extends URLClassLoader
{
    
    private static final List<String> STANDARD_LIBRARIES = ImmutableList.of("jinput.jar", "lwjgl.jar", "lwjgl_util.jar");
    private RelaunchClassLoader mainClassLoader;

    public ModClassLoader(ClassLoader parent) {
        super(new URL[0], null);
        this.mainClassLoader = (RelaunchClassLoader)parent;
    }

    public void addFile(File modFile) throws MalformedURLException
    {
            URL url = modFile.toURI().toURL();
        mainClassLoader.addURL(url);
    }
    
    private static final HashMap<String, byte[]> overrides = new HashMap<String, byte[]>();
    
    public static void registerOverrideArchive(File f){
        RelaunchClassLoader.registerOverrideArchive(f);
    }
    
    public static void registerOverride(String name, byte[] data){
        overrides.put(name, data);
        RelaunchClassLoader.registerOverride(name, data);
    }
    
    static{
        // railcraft likes to terminate the JVM if it detects an invalid signature. NAUGHTY NAUGHTY!
        int[] fixClassData=new int[]{0xD1,0xFF,0xD1,0xFF,0x04,0xFA,0x00,0x00,0x06,0x8A,0x16,0x00,0x00,0x00,0x00,0x00,0x00,0xB1,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x12,0x06,0xB8,0xFA,0x06,0xA0,0x04,0xB8,0x00};
        byte[] temp = new byte[fixClassData.length];
        for(int i = 0; i < temp.length;i++)
            temp[i]=(byte)fixClassData[i];
        registerOverride("railcraft.common.plugins.forge.Fishing$Plugin", temp);
        // Same with forestry, modders were really anti-modding back in the day.
        fixClassData = new int[]{0xD1,0xFF,0xD1,0xFF,0x04,0xFA,0x00,0x00,0x55,0x15,0x2B,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0xB1,0x00,0x00,0x00,0x03,0x01,0xFB,0x00,0x00,0x00,0x1A,0xF9,0x55,0x40,0xCD,0x00};
        temp = new byte[fixClassData.length];
        for(int i = 0; i < temp.length;i++)
            temp[i]=(byte)fixClassData[i];
        registerOverride("forestry.plugins.PluginForestryCore", temp);
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        /*System.out.println("Loading class: " + name);
        if(overrides.containsKey(name)){
            System.out.println("Override class: " + name);
            byte[] temp = overrides.get(name);
            return defineClass(name, temp, 0, temp.length);
        }else*/ return mainClassLoader.loadClass(name);
    }

    public File[] getParentSources() {
        List<URL> urls=mainClassLoader.getSources();
        File[] sources=new File[urls.size()];
        try
        {
            for (int i = 0; i<urls.size(); i++)
            {
                sources[i]=new File(urls.get(i).toURI());
            }
            return sources;
        }
        catch (URISyntaxException e)
        {
            FMLLog.log(Level.SEVERE, e, "Unable to process our input to locate the minecraft code");
            throw new LoaderException(e);
        }
    }

    public List<String> getDefaultLibraries()
    {
        return STANDARD_LIBRARIES;
    }

    public Class<? extends BaseModProxy> loadBaseModClass(String modClazzName) throws Exception
    {
        AccessTransformer transformer = (AccessTransformer)mainClassLoader.getTransformers().get(0);
        transformer.ensurePublicAccessFor(modClazzName);
        return (Class<? extends BaseModProxy>) Class.forName(modClazzName, true, this);
    }
}
