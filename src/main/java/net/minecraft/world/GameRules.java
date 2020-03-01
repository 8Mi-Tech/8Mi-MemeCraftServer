package net.minecraft.world;

import java.util.HashMap;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;

public class GameRules
{
    private final HashMap<String, Value> rules = new HashMap<>();

    public GameRules()
    {
        this.addGameRule("doFireTick", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("mobGriefing", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("keepInventory", "false", ValueType.BOOLEAN_VALUE);
        this.addGameRule("doMobSpawning", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("doMobLoot", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("doTileDrops", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("doEntityDrops", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("commandBlockOutput", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("naturalRegeneration", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("doDaylightCycle", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("logAdminCommands", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("showDeathMessages", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("randomTickSpeed", "3", ValueType.NUMERICAL_VALUE);
        this.addGameRule("sendCommandFeedback", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("reducedDebugInfo", "false", ValueType.BOOLEAN_VALUE);
        this.addGameRule("spectatorsGenerateChunks", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("spawnRadius", "10", ValueType.NUMERICAL_VALUE);
        this.addGameRule("disableElytraMovementCheck", "false", ValueType.BOOLEAN_VALUE);
        this.addGameRule("maxEntityCramming", "24", ValueType.NUMERICAL_VALUE);
        this.addGameRule("doWeatherCycle", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("doLimitedCrafting", "false", ValueType.BOOLEAN_VALUE);
        this.addGameRule("maxCommandChainLength", "65536", ValueType.NUMERICAL_VALUE);
        this.addGameRule("announceAdvancements", "true", ValueType.BOOLEAN_VALUE);
        this.addGameRule("gameLoopFunction", "-", ValueType.FUNCTION);
    }

    public void addGameRule(String key, String value, ValueType type)
    {
        this.rules.put(key, new Value(value, type));
    }

    public void setOrCreateGameRule(String key, String ruleValue)
    {
        Value gamerules$value = this.rules.get(key);

        if (gamerules$value != null)
        {
            gamerules$value.setValue(ruleValue);
        }
        else
        {
            this.addGameRule(key, ruleValue, ValueType.ANY_VALUE);
        }
    }

    public String getString(String name)
    {
        Value gamerules$value = this.rules.get(name);
        return gamerules$value != null ? gamerules$value.getString() : "";
    }

    public boolean getBoolean(String name)
    {
        Value gamerules$value = this.rules.get(name);
        return gamerules$value != null ? gamerules$value.getBoolean() : false;
    }

    public int getInt(String name)
    {
        Value gamerules$value = this.rules.get(name);
        return gamerules$value != null ? gamerules$value.getInt() : 0;
    }

    public NBTTagCompound writeToNBT()
    {
        NBTTagCompound nbttagcompound = new NBTTagCompound();

        for (String s : this.rules.keySet())
        {
            Value gamerules$value = this.rules.get(s);
            nbttagcompound.setString(s, gamerules$value.getString());
        }

        return nbttagcompound;
    }

    public void readFromNBT(NBTTagCompound nbt)
    {
        for (String s : nbt.getKeySet())
        {
            this.setOrCreateGameRule(s, nbt.getString(s));
        }
    }

    public String[] getRules()
    {
        Set<String> set = this.rules.keySet();
        return (String[])set.toArray(new String[set.size()]);
    }

    public boolean hasRule(String name)
    {
        return this.rules.containsKey(name);
    }

    public boolean areSameType(String key, ValueType otherValue)
    {
        Value gamerules$value = this.rules.get(key);
        return gamerules$value != null && (gamerules$value.getType() == otherValue || otherValue == ValueType.ANY_VALUE);
    }

    static class Value
        {
            private String valueString;
            private boolean valueBoolean;
            private int valueInteger;
            private double valueDouble;
            private final ValueType type;

            public Value(String value, ValueType type)
            {
                this.type = type;
                this.setValue(value);
            }

            public void setValue(String value)
            {
                this.valueString = value;
                this.valueBoolean = Boolean.parseBoolean(value);
                this.valueInteger = this.valueBoolean ? 1 : 0;

                try
                {
                    this.valueInteger = Integer.parseInt(value);
                }
                catch (NumberFormatException var4)
                {
                    ;
                }

                try
                {
                    this.valueDouble = Double.parseDouble(value);
                }
                catch (NumberFormatException var3)
                {
                    ;
                }
            }

            public String getString()
            {
                return this.valueString;
            }

            public boolean getBoolean()
            {
                return this.valueBoolean;
            }

            public int getInt()
            {
                return this.valueInteger;
            }

            public ValueType getType()
            {
                return this.type;
            }
        }

    public static enum ValueType
    {
        ANY_VALUE,
        BOOLEAN_VALUE,
        NUMERICAL_VALUE,
        FUNCTION;
    }
}