package pc2;

import java.util.Scanner;

public class principal {
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		System.out.println("Introduzca el fichero de entrada");
		String ficheroEntrada = new Scanner(System.in).nextLine();
		System.out.println("Introduzca el numero de descargas maximo");
		int numMaxDescargas=new Scanner(System.in).nextInt();
		FileDownloader fichero = new FileDownloader(numMaxDescargas);
		fichero.setFichero(ficheroEntrada);
		fichero.startDownload();

	}
}
