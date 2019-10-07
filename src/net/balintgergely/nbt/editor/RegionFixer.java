package net.balintgergely.nbt.editor;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.function.Predicate;

import net.balintgergely.nbt.Compound;
import net.balintgergely.nbt.NamedTag;
import net.balintgergely.nbt.Region;
import net.balintgergely.nbt.Region.Chunk;

public class RegionFixer {
	public static final int MAX_VERSION = 1976;
	public static void main(String[] atgs) throws IOException{
		Integer mx = Integer.valueOf(MAX_VERSION);
		@SuppressWarnings("unlikely-arg-type")
		Predicate<NamedTag<?>> clearBiomes = (NamedTag<?> tag) -> tag.getName().equals("Biomes");
		try(Scanner sc = new Scanner(System.in)){
			File directory = new File(sc.nextLine());
			for(File file : directory.listFiles()){
				System.out.println("Working on "+file);
				try(Region region = new Region(file, true)){
					int count = 0,modif = 0;
					for(Chunk ch : region){
						count++;
						Compound comp = (Compound)(ch.getValue().get(0).getValue());
						@SuppressWarnings("unchecked")
						NamedTag<Integer> version = (NamedTag<Integer>)comp.forName("DataVersion");
						int dataVersion = version.getValue();
						if(dataVersion > MAX_VERSION){
							modif++;
							version.setValue(mx);
							comp = (Compound)comp.forName("Level").getValue();
							comp.removeIf(clearBiomes);
						}
						ch.save();
					}
					System.out.println("Modified "+modif+"/"+count+" chunks.");
				}
			}
		}
	}
}
