package pc2;

public class principal {
	public static void main(String[] args) {
		FileDownloader fichero = new FileDownloader(2);
		fichero.setFichero("resources/entrada.txt");
		fichero.startDownload();

	}
}
