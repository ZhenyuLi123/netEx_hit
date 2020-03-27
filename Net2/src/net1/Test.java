package net1;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Test {
	public static void main(String[] args) {
		InputStream inputFile;
		OutputStream writeFile;

		try {
			inputFile = new FileInputStream("readtest.txt"); //∂¡»°read.txt
			writeFile = new FileOutputStream("receivetest.txt"); 

			byte[] b = new byte[8];

			inputFile.read(b, 1, b.length - 1);
			writeFile.write(b, 1, b.length - 1);

			inputFile.read(b, 1, b.length - 1);
			writeFile.write(b, 1, b.length - 1);

			inputFile.read(b, 1, b.length - 1);
			writeFile.write(b, 1, b.length - 1);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
