package pc2;

public class principal{
	public static void main(String[] args){
			FileDownloader fichero= new FileDownloader(3);
			fichero.setFichero("entrada.txt");
			try {
				fichero.startDownload();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
}
