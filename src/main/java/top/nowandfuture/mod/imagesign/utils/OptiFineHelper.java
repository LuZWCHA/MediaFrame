package top.nowandfuture.mod.imagesign.utils;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Field;

@OnlyIn(Dist.CLIENT)
public class OptiFineHelper
{
    private static Boolean loaded = null;

    public static boolean isLoaded()
    {
        if(loaded == null)
        {
            try
            {
                Class.forName("optifine.Installer");
                loaded = true;
            }
            catch(ClassNotFoundException e)
            {
                loaded = false;
            }
        }
        return loaded;
    }

    static Field countResetDisplayListsField;
    static Class shadersClazz = null;
    static Field shaderLoaded = null;
    public static int getResetDisplayListsCount(){
        if(countResetDisplayListsField == null){
            if(shadersClazz == null) {
                try {
                    shadersClazz = Class.forName("net.optifine.shaders.Shaders");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            try {
                countResetDisplayListsField = shadersClazz.getField("countResetDisplayLists");
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        try {
            return countResetDisplayListsField.getInt(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static boolean isShaderLoaded(){
        if(!isLoaded()){
            return false;
        }
        if(shadersClazz == null) {
            try {
                shadersClazz = Class.forName("net.optifine.shaders.Shaders");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if(shaderLoaded == null) {
            try {
                shaderLoaded = shadersClazz.getField("shaderPackLoaded");
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }


        try {
            return shaderLoaded.getBoolean(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
}
