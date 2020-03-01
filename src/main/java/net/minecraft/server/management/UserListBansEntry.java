package net.minecraft.server.management;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.Date;
import java.util.UUID;

public class UserListBansEntry extends UserListEntryBan<GameProfile>
{
    public UserListBansEntry(GameProfile profile)
    {
        this(profile, (Date)null, (String)null, (Date)null, (String)null);
    }

    public UserListBansEntry(GameProfile profile, Date startDate, String banner, Date endDate, String banReason)
    {
        super(profile, startDate, banner, endDate, banReason);
    }

    public UserListBansEntry(JsonObject json)
    {
        super(toGameProfile(json), json);
    }

    protected void onSerialization(JsonObject data)
    {
        if (this.getValue() != null)
        {
            data.addProperty("uuid", ((GameProfile)this.getValue()).getId() == null ? "" : ((GameProfile)this.getValue()).getId().toString());
            data.addProperty("name", ((GameProfile)this.getValue()).getName());
            super.onSerialization(data);
        }
    }

    private static GameProfile toGameProfile(JsonObject json)
    {
        // Spigot start
        // this whole method has to be reworked to account for the fact Bukkit only accepts UUID bans and gives no way for usernames to be stored!
        UUID uuid = null;
        String name = null;
        if (json.has("uuid"))
        {
            String s = json.get("uuid").getAsString();

            try
            {
                uuid = UUID.fromString(s);
            }
            catch (Throwable var4)
            {
            }
        }
        if (json.has("name"))
        {
            name = json.get("name").getAsString();
        }
        if (uuid != null || name != null)
        {
            return new GameProfile(uuid, name);
        }
        else
        {
            return null;
        }
        // Spigot End
    }
}