package pc2;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FileDownloader{
	private final int numMaxDescargas;

	public FileDownloader(int numMaxDescargas){
		super();
		this.numMaxDescargas = numMaxDescargas;
	}

	public static void empezarDescarga(String ficheroEntrada){
		FileReader f;
		try{
			f = new FileReader(ficheroEntrada);
			BufferedReader b = new BufferedReader(f);
			String linea = b.readLine();
			while(!(linea == null)){
				String[] datos = linea.split("\\s+");
				for(int i = 0;i < Integer.parseInt(datos[2]);i++){
					Fichero.descargaFichero(datos[0] + "/" + datos[1] + ".part" + i,"Descargas/" + datos[1] + ".part" + i);
				}
				Fichero.mergeFile("Descargas",datos[1]);
				linea = b.readLine();
			}
		}catch(FileNotFoundException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
