package pc2;

public class principal{
	public static void main(String[] args) {
		FileDownloader fichero = new FileDownloader(2);
		fichero.setFichero("entrada.txt");

		fichero.startDownload();

	}
}
