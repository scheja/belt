package edu.kit.aifb.belt.db;

import static org.junit.Assert.assertTrue;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import com.googlecode.javaewah.EWAHCompressedBitmap;

public class BitmapSizeTest {
	private static final int UNCOMPRESSED_SIZE = 10000;
	private static final int MAX_ONE_COUNT = 1000;
	private static final int SAMPLE_COUNT = 100;
	private static final int MAX_ONE_PROPORTION = 100;

	@Ignore
	@Test
	public void testBitmapSizes() {
		EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
		Random r = new Random();
		IntSet used = new IntRBTreeSet();

		for (int i = 0; i <= MAX_ONE_COUNT; i++) {
			int size = 0;
			int[] positions = new int[i];

			for (int j = 0; j < SAMPLE_COUNT; j++) {
				for (int k = 0; k < positions.length; k++) {
					int number;

					do {
						number = r.nextInt(UNCOMPRESSED_SIZE);
					} while (used.contains(number));

					used.add(number);
					positions[k] = number;
				}

				Arrays.sort(positions);

				for (int number : positions) {
					assertTrue(bitmap.set(number));
				}

				size += bitmap.sizeInBytes();
				bitmap.clear();
				used.clear();
			}

			System.out.println(i + "," + size / SAMPLE_COUNT);
		}
	}

	@Ignore
	@Test
	public void testBitmapSizesByOverallSize() throws FileNotFoundException {
		PrintStream out = System.out;
		System.setOut(new PrintStream(
				new FileOutputStream(new File("data.csv"))));

		EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
		Random r = new Random();
		IntSet used = new IntRBTreeSet();

		for (int x = 2; x <= MAX_ONE_PROPORTION; x++) {
			System.out.print("," + x);
		}

		System.out.println();

		for (int size = 1000; size <= 10000; size += 100) {
			System.out.print(size);
			System.err.println(size);

			for (int oneProportion = 2; oneProportion <= MAX_ONE_PROPORTION; oneProportion++) {
				int oneCount = oneProportion;
				int byteSize = 0;
				int[] positions = new int[oneCount];

				for (int i = 0; i < SAMPLE_COUNT; i++) {
					for (int k = 0; k < positions.length; k++) {
						int number;

						do {
							number = r.nextInt(UNCOMPRESSED_SIZE);
						} while (used.contains(number));

						used.add(number);
						positions[k] = number;
					}

					Arrays.sort(positions);
					for (int number : positions) {
						assertTrue(bitmap.set(number));
					}

					byteSize += bitmap.sizeInBytes();
					bitmap.clear();
					used.clear();
				}

				System.out.print("," + byteSize / SAMPLE_COUNT);
			}

			System.out.println();
		}

		System.out.close();
		System.setOut(out);
	}

	@Ignore
	@Test
	public void createSizeMap() {
		EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
		Random r = new Random();
		IntSet used = new IntRBTreeSet();

		System.out.print("{");

		for (int population = 0; population < 101; population++) {
			int byteSize = 0;
			int[] positions = new int[population];

			for (int size = 100; size < 10000; size += 100) {
				for (int i = 0; i < SAMPLE_COUNT; i++) {
					for (int k = 0; k < positions.length; k++) {
						int number;

						do {
							number = r.nextInt(UNCOMPRESSED_SIZE);
						} while (used.contains(number));

						used.add(number);
						positions[k] = number;
					}

					Arrays.sort(positions);
					for (int number : positions) {
						assertTrue(bitmap.set(number));
					}

					byteSize += bitmap.serializedSizeInBytes();
					bitmap.clear();
					used.clear();
				}
			}
			
			System.out.print(byteSize / SAMPLE_COUNT / 100);
			
			if (population < 100) {
				System.out.print(", ");
			}
		}
		
		System.out.println("}");
	}
}
