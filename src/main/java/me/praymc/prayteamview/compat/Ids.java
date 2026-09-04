package me.praymc.prayteamview.compat;

//? if >=1.21.9 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}

public final class Ids {

    public static final String MOD_ID = "prayteamview";

    private Ids() {
    }

    //? if >=1.21.9 {
    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
    //?} elif >=1.21 {
    /*public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    *///?} else {
    /*public static ResourceLocation of(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
    *///?}
}
