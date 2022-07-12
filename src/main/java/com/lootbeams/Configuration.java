package com.lootbeams;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Configuration {
	public static final Gson GSON;

	static {
		GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.setPrettyPrinting()
				.create();
	}

	public boolean allItems = true;
	public boolean onlyEquipment = false;
	public boolean onlyRare = false;
	public List<String> whitelist = new ArrayList<>();
	public List<String> blacklist = new ArrayList<>();
	public List<String> colorOverrides = new ArrayList<>();

	public boolean renderNameColor = true;
	public boolean renderRarityColor = true;
	public float beamRadius = 1;
	public float beamHeight = 1;
	public float beamYOffset = 0;
	public float beamAlpha = 0.85f;
	public float renderDistance = 24.0f;

	public boolean borders = true;
	public boolean renderNametags = true;
	public boolean renderNametagsOnlook = true;
	public boolean renderStackcount = true;
	public float nametagLookSensitivity = 0.018f;
	public float nametagTextAlpha = 1;
	public float nametagBackgroundAlpha = 0.5f;
	public float nametagScale = 1.0f;
	public float nametagYOffset = 0.75f;
	public boolean dmclootCompatRarity = true;
	public List<String> customRarities = new ArrayList<>();
	public boolean whiteRarities = false;

	public static Configuration load(){
		File configFile = FabricLoader.getInstance().getConfigDir().resolve(LootBeams.MODID + ".json").toFile();
		if (configFile.isFile()) {
			try {
				Configuration fromJson = GSON.fromJson(new FileReader(configFile), Configuration.class);
				if (fromJson == null) fromJson = new Configuration();
				return fromJson;
			} catch (IOException e) {
				LootBeams.LOGGER.warn("cannot load config file", e);
			}
		}
		else{
			Configuration inst = new Configuration();
			inst.save();
			return inst;
		}
		return new Configuration();
	}

	public void save(){
		File configFile = FabricLoader.getInstance().getConfigDir().resolve(LootBeams.MODID + ".json").toFile();
		try (JsonWriter writer = new JsonWriter(new FileWriter(configFile))) {
			GSON.toJson(this, Configuration.class, writer);
		} catch (IOException e) {
			LootBeams.LOGGER.warn("cannot save config file", e);
		}
	}

	public static TextColor getColorFromItemOverrides(Item i) {
		List<String> overrides = LootBeams.config.colorOverrides;
		if (overrides.size() > 0) {
			for (String unparsed : overrides.stream().filter((s) -> (!s.isEmpty())).toList()) {
				String[] configValue = unparsed.split("=");
				if (configValue.length == 2) {
					String nameIn = configValue[0];
					ResourceLocation registry = ResourceLocation.tryParse(nameIn.replace("#", ""));
					TextColor colorIn = null;
					try {
						colorIn = TextColor.parseColor(configValue[1]);
					} catch (Exception e) {
						LootBeams.LOGGER.error(String.format("Color overrides error! \"%s\" is not a valid hex color for \"%s\"", configValue[1], nameIn));
						return null;
					}

					//Modid
					if (!nameIn.contains(":")) {
						if (Registry.ITEM.getKey(i).getNamespace().equals(nameIn)) {
							return colorIn;
						}

					}

					if (registry != null) {
						//Tag
						if (nameIn.startsWith("#")) {
							Optional<HolderSet.Named<Item>> tag = Registry.ITEM.getTags().filter(pair -> pair.getFirst().location().equals(registry))
									.findFirst().map(Pair::getSecond);
//							Optional<HolderSet.Named<Item>> tag = Registry.ITEM.getTag(TagKey.create(Registry.ITEM_REGISTRY, registry));
							if(tag.isPresent() && tag.get().contains(Registry.ITEM.getHolder(Registry.ITEM.getResourceKey(i).get()).get())){
								return colorIn;
							}
						}

						//Item
						Optional<Item> registryItem = Registry.ITEM.getOptional(registry);
						if (registryItem.isPresent() && registryItem.get().asItem() == i.asItem()) {
							return colorIn;
						}

					}
				}
			}
		}
		return null;
	}
}
