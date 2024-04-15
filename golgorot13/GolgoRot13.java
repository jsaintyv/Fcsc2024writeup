package fcsc2024;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Scanner;

public class GolgoRot13 {
	public record Change(long time, boolean value) {}
	
	public record Communication(BitSet bs, int nbTic) {
		public String toData() {
			int x = 0;
			StringBuilder sb = new StringBuilder();
			while (x < nbTic) {
				x++;
				int b = 0;
				for (int i = 0; i < 8; i++) {
					if (bs.get(x)) {
						b = b | (1 << i);
					}
					x++;
				}
				x++;
				sb.append((char) b);
			}
			return sb.toString();
		}
	}
	
	public static Communication toBitSignal(long gap, List<Change> changes) {
		BitSet result = new BitSet();
		int index = 0;
		for (int x = 0; x < (changes.size() - 2); x++) {
			Change current = changes.get(x);
			long diff = changes.get(x + 1).time - changes.get(x).time;
			long count = diff / gap;
			for (long y = 0; y < count; y++) {
				result.set(index, current.value);
				index++;
			}
		}		
		// System.out.println(index);
		return new Communication(result, index);
	}

	public static boolean containsChar(String row) {
		int count = 0;
		for (int x = 0; x < row.length(); x++) {
			if (Character.isDigit(row.charAt(x)) || Character.isAlphabetic(row.charAt(x))) {
				count++;
			}
		}
		return count > 10;
	}

	public static void main(String[] args) {
		//analyseCom("!");
		analyseCom("\"");
	}

	private static void analyseCom(String d) {			
		Scanner in = new Scanner(GolgoRot13.class.getResourceAsStream("res/golgrot13.vcd"));
		while (!in.nextLine().startsWith("#")) {
		}
		in.nextLine();
		in.nextLine();
		in.nextLine();
		in.nextLine();
		in.nextLine();

		List<Change> changes = new ArrayList<>();
		while (in.hasNextLine()) {
			String rowTime = in.nextLine();
			String valueTime = in.nextLine();

			if (valueTime.endsWith(d)) {
				changes.add(new Change(Long.parseLong(rowTime.substring(1)), valueTime.charAt(0) == '1'));
			}
		}		
		for (long horloge = 1000; horloge < 8000; horloge += 5) {
			Communication com =toBitSignal(horloge, changes); 			
			String data = com.toData();
			if (containsChar(data)) {				
				System.out.println("Holorge:" + horloge + "ns " +  data);				
			}
		}
	}	
}
