package io.anuke.mindustry.ui;

import io.anuke.arc.Core;
import io.anuke.arc.collection.OrderedMap;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.mindustry.world.meta.StatCategory;
import io.anuke.mindustry.world.meta.StatValue;

public class ContentDisplay{

    public static void displayBlock(Table table, Block block){

        table.table(title -> {
            int size = 8 * 6;

            title.addImage(block.icon(Icon.large)).size(size);
            title.add("[accent]" + block.formalName).padLeft(5);
        });

        table.row();

        table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(8).padLeft(0).padRight(0).fillX();

        table.row();

        if(block.fullDescription != null){
            table.add(block.fullDescription).padLeft(5).padRight(5).width(400f).wrap().fillX();
            table.row();

            table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(8).padLeft(0).padRight(0).fillX();
            table.row();
        }

        BlockStats stats = block.stats;

        for(StatCategory cat : stats.toMap().keys()){
            OrderedMap<BlockStat, StatValue> map = stats.toMap().get(cat);

            if(map.size == 0) continue;

            table.add("$category." + cat.name()).color(Palette.accent).fillX();
            table.row();

            for(BlockStat stat : map.keys()){
                table.table(inset -> {
                    inset.left();
                    inset.add("[LIGHT_GRAY]" + stat.localized() + ":[] ");
                    map.get(stat).display(inset);
                }).fillX().padLeft(10);
                table.row();
            }
        }
    }

    public static void displayItem(Table table, Item item){

        table.table(title -> {
            title.addImage(item.getContentIcon()).size(8 * 6);
            title.add("[accent]" + item.localizedName()).padLeft(5);
        });

        table.row();

        table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();

        table.row();

        if(item.description != null){
            table.add(item.description).padLeft(5).padRight(5).width(400f).wrap().fillX();
            table.row();

            table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();
            table.row();
        }

        table.left().defaults().fillX();

        table.add(Core.bundle.format("item.explosiveness", (int) (item.explosiveness * 100 * 2f)));
        table.row();
        table.add(Core.bundle.format("item.flammability", (int) (item.flammability * 100 * 2f)));
        table.row();
        table.add(Core.bundle.format("item.radioactivity", (int) (item.radioactivity * 100 * 2f)));
        table.row();
        table.add(Core.bundle.format("item.fluxiness", (int) (item.fluxiness * 100 * 2f)));
        table.row();
    }

    public static void displayLiquid(Table table, Liquid liquid){

        table.table(title -> {
            title.addImage(liquid.getContentIcon()).size(8 * 6);
            title.add("[accent]" + liquid.localizedName()).padLeft(5);
        });

        table.row();

        table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();

        table.row();

        if(liquid.description != null){
            table.add(liquid.description).padLeft(5).padRight(5).width(400f).wrap().fillX();
            table.row();

            table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();
            table.row();
        }

        table.left().defaults().fillX();

        table.add(Core.bundle.format("item.explosiveness", (int) (liquid.explosiveness * 100 * 2f)));
        table.row();
        table.add(Core.bundle.format("item.flammability", (int) (liquid.flammability * 100 * 2f)));
        table.row();
        table.add(Core.bundle.format("liquid.heatcapacity", (int) (liquid.heatCapacity * 100)));
        table.row();
        table.add(Core.bundle.format("liquid.temperature", (int) (liquid.temperature * 100)));
        table.row();
        table.add(Core.bundle.format("liquid.viscosity", (int) (liquid.viscosity * 100)));
        table.row();
    }

    public static void displayMech(Table table, Mech mech){
        table.table(title -> {
            title.addImage(mech.getContentIcon()).size(8 * 6);
            title.add("[accent]" + mech.localizedName()).padLeft(5);
        });

        table.row();

        table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();

        table.row();

        if(mech.description != null){
            table.add(mech.description).padLeft(5).padRight(5).width(400f).wrap().fillX();
            table.row();

            table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();
            table.row();
        }

        table.left().defaults().fillX();

        if(Core.bundle.has("mech." + mech.name + ".weapon")){
            table.add(Core.bundle.format("mech.weapon", Core.bundle.get("mech." + mech.name + ".weapon")));
            table.row();
        }
        if(Core.bundle.has("mech." + mech.name + ".ability")){
            table.add(Core.bundle.format("mech.ability", Core.bundle.get("mech." + mech.name + ".ability")));
            table.row();
        }
        table.add(Core.bundle.format("mech.armor", mech.armor));
        table.row();
        table.add(Core.bundle.format("mech.itemcapacity", mech.itemCapacity));
        table.row();

        if(mech.drillPower > 0){
            table.add(Core.bundle.format("mech.minespeed", (int) (mech.mineSpeed * 10)));
            table.row();
            table.add(Core.bundle.format("mech.minepower", mech.drillPower));
            table.row();
        }
    }

    public static void displayUnit(Table table, UnitType unit){
        table.table(title -> {
            title.addImage(unit.getContentIcon()).size(8 * 6);
            title.add("[accent]" + unit.localizedName()).padLeft(5);
        });

        table.row();

        table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();

        table.row();

        if(unit.description != null){
            table.add(unit.description).padLeft(5).padRight(5).width(400f).wrap().fillX();
            table.row();

            table.addImage("white").height(3).color(Color.LIGHT_GRAY).pad(15).padLeft(0).padRight(0).fillX();
            table.row();
        }

        table.left().defaults().fillX();

        table.add(Core.bundle.format("unit.health", unit.health));
        table.row();
        table.add(Core.bundle.format("unit.speed", Strings.toFixed(unit.speed, 1)));
        table.row();
        table.row();
    }
}
