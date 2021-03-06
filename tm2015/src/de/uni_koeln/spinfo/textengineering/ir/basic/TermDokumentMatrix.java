package de.uni_koeln.spinfo.textengineering.ir.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TermDokumentMatrix implements InformationRetrieval {

	private boolean[][] matrix;
	private Map<String, Integer> positions;

	public TermDokumentMatrix(Corpus corpus) {

		long start = System.currentTimeMillis();
		System.out.println("Erstelle Matrix ...");

		List<String> works = corpus.getWorks();
		List<String> terms = getTerms(works);
		positions = getPositions(terms);// 'Zeilennummern' der Terme
		matrix = new boolean[terms.size()][works.size()];
		/*
		 * Falls beim Erstellen der Matrix der Speicher überläuft, kann man dem
		 * Prozess mit der Angabe des Parameters "-Xmx256m" (unter Open Run
		 * Dialog, Arguments, VM Arguments) mehr Speicher zuweisen, hier z.B.
		 * 256 MB; per default nimmt sich jeder Java-Prozess 64 MB.
		 */
		System.out.println("Größe der Matrix: " + terms.size() + " X "
				+ works.size());

		for (int spalte = 0; spalte < works.size(); spalte++) {
			String[] tokens = works.get(spalte).split("\\s+");
			for (int j = 0; j < tokens.length; j++) {
				String t = tokens[j];// das aktuelle Token
				int zeile = positions.get(t);// Zeilennummer des Tokens
				matrix[zeile][spalte] = true;
			}
		}
		System.out.println("Matrix erstellt, Dauer: "
				+ (System.currentTimeMillis() - start) + " ms.");

		// printMatrix(terms);// optionale Ausgabe der Matrix
	}

	/*
	 * Legt die 'Zeilennummern' der Terme in eine Map (für schnellen Zugriff).
	 */
	private Map<String, Integer> getPositions(List<String> terms) {
		Map<String, Integer> pos = new HashMap<String, Integer>();
		for (int i = 0; i < terms.size(); i++) {
			pos.put(terms.get(i), i);
		}
		return pos;
	}

	/*
	 * Ermittelt die Terme aller Werke. Das Set wird abschließend in eine Liste
	 * umgewandelt, da der Listen-Zugriff über get(index) sowohl das Mappen der
	 * Positionen als auch das Ausgeben der Matrix in der printMatrix-Methode
	 * erleichtert.
	 */
	private List<String> getTerms(List<String> works) {
		Set<String> allTerms = new HashSet<String>();
		for (String work : works) {
			List<String> termsInCurrentWork = Arrays.asList(work.split("\\s+"));
			allTerms.addAll(termsInCurrentWork);// füge zu Gesamtliste hinzu
		}
		return new ArrayList<String>(allTerms);
	}

	/*
	 * Optionale Ausgabe der Matrix
	 */
	@SuppressWarnings("unused")
	private void printMatrix(List<String> terms) {
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				System.out.print((matrix[i][j]) ? "1 " : "0 ");
			}
			System.out.println(terms.get(i) + " ");
		}
	}

	/*
	 * Die eigentliche Suche.
	 */
	@Override
	public Set<Integer> search(String query) {

		long start = System.currentTimeMillis();
		List<String> queries = Arrays.asList(query.split(" "));
		Set<Integer> result = new HashSet<Integer>();

		for (String q : queries) {
			// erstmal die Zeile ermitteln:
			Integer zeilennummer = positions.get(q);
			// und dann Spalte für Spalte (Werk für Werk) nachsehen:
			for (int i = 0; i < matrix[0].length; i++) {
				/*
				 * Bei nicht vorhandenen Wörtern können 'Nullpointer' auftreten.
				 * Werden diese wie hier explizit vermieden, können an dieser
				 * Stelle bspw. auch Suchalternativen angezeigt werden
				 * ("Meinten Sie ...") - was allerdings voraussetzt, dass diese
				 * zunächst auf Grundlage der Query ermittelt werden. Mehr dazu
				 * beim Thema "Tolerant Retrieval".
				 */
				if (zeilennummer == null) {
					System.out.println("Term " + q + " nicht gefunden");
					break;
				}
				// die boolesche Matrix enthält ein 'true' für jeden Treffer:
				if (matrix[zeilennummer][i]) {
					result.add(i);
					//Hier behandeln wir die Suchwörter als ODER-verknüpft!
				}
			}
		}
		System.out.println("Suchdauer: " + (System.currentTimeMillis() - start)
				+ " ms.");
		return result;
	}

	/*
	 * Alternative Umsetzung der Matrix-Suche unter Verwendung von BitSets.
	 */
	public Set<Integer> booleanSearch(String query) {

		long start = System.currentTimeMillis();
		List<String> queries = Arrays.asList(query.split(" "));
		Set<Integer> result = new HashSet<Integer>();

		// Wir erstellen ein BitSet für das erste Suchwort (= dessen Zeile):
		BitSet bitSet = bitSetFor(matrix[positions.get(queries.get(0))]);
		// ... und dann für alle weiteren Suchwörter:
		for (String q : queries) {
			// Die boolschen Operationen bekommen wir nun einfach geschenkt:
			bitSet.and(bitSetFor(matrix[positions.get(q)]));
		}
		// jetzt nur noch die 'true'-Positionen aus resultierendem BitSet holen:
		for (int i = 0; i < matrix[0].length; i++) {
			if (bitSet.get(i))
				result.add(i);
		}
		System.out.println("Suchdauer: " + (System.currentTimeMillis() - start)
				+ " ms.");
		return result;
	}

	/*
	 * Erzeugt ein BitSet aus dem übergebenen boolean[] (bei uns: den Zeilen).
	 */
	private BitSet bitSetFor(boolean[] bs) {
		BitSet set = new BitSet(bs.length);
		for (int i = 0; i < bs.length; i++) {
			if (bs[i])
				set.set(i);
		}
		return set;
	}

}
