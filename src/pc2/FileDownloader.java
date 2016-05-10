package pc2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

public class FileDownloader {
	private static final String RUTADESCARGA = "downloads";
	private String fichero;
	private int numMaxDescargas = 3;
	private static CountDownLatch latchDescarga;
	private static CountDownLatch latchMerge;
	private static CountDownLatch latchElimina;

	public FileDownloader(int numMaxDescargas) {
		super();
		this.numMaxDescargas = numMaxDescargas;
	}

	public void startDownload() throws InterruptedException {
		FileReader f;
		try {
			f = new FileReader(fichero);
			BufferedReader b = new BufferedReader(f);
			String linea = b.readLine();
			while (!(linea == null)) {
				latchElimina = new CountDownLatch(1);
				latchDescarga = new CountDownLatch(numMaxDescargas);
				String[] datos = linea.split("\\s+");
				String ruta = datos[0];
				String nombre = datos[1];
				int numPartes = Integer.parseInt(datos[2]);
				latchMerge = new CountDownLatch(numPartes);
				String num = "";
				for (int i = 0; i < numPartes; i++) {
					if (i < 10)
						num = "0" + i;
					else
						num = String.valueOf(i);
					String rutaOrigen = ruta + "/" + nombre + ".part" + i;
					String rutaDestino = RUTADESCARGA + "/" + nombre + ".part" + num;
					new Thread(new Runnable() {
						public void run() {
							try {
								downloadFile(rutaOrigen, rutaDestino);

							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}, "hilo" + i).start();
				}
				latchMerge.await();
				mergeFile(RUTADESCARGA, nombre);
				latchElimina.await();
				deleteFiles(RUTADESCARGA, nombre);
				linea = b.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void downloadFile(String url, String path) throws InterruptedException {
		try {
			URL website = new URL(url);
			File folder = new File(path.split("/")[0]);
			if (!folder.exists())
				folder.mkdir();
			InputStream in = website.openStream();
			Path pathOut = Paths.get(path);
			Files.copy(in, pathOut, StandardCopyOption.REPLACE_EXISTING);
			in.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		latchMerge.countDown();
	}

	/**
	 * Metodo encargado de unir las partes que forman los ficheros de un
	 * directorio
	 * 
	 * @param dir
	 *            nombre del directorio
	 * @param fileStart
	 *            nombre del fichero a unir
	 */
	private void mergeFile(String dir, String fileStart) {
		try {
			File ofile = new File(dir + "/" + fileStart);
			int bytesRead = 0;
			FileOutputStream fos = new FileOutputStream(ofile, true);
			for (String fileName : dameListaFicheros(dir, fileStart)) {
				File f = new File(dir + "/" + fileName);
				FileInputStream fis = new FileInputStream(f);
				byte[] fileBytes = new byte[(int) f.length()];
				bytesRead = fis.read(fileBytes, 0, (int) f.length());
				assert (bytesRead == fileBytes.length);
				assert (bytesRead == (int) f.length());
				fos.write(fileBytes);
				fos.flush();
				fileBytes = null;
				fis.close();
				fis = null;
			}
			fos.close();
			fos = null;
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		latchElimina.countDown();
	}

	/**
	 * Metodo encargado de eliminar los ficheros, hace un recorrido sobre el
	 * directorio buscando los archivos que se llamen con el nombre del fichero
	 * 
	 * @param dir
	 *            directorio de los ficheros
	 * @param fileStart
	 *            nombre del fichero a borrar
	 */
	private void deleteFiles(String dir, String fileStart) {
		for (String fileName : dameListaFicheros(dir, fileStart))
			new File(dir + "/" + fileName).delete();
	}

	/**
	 * Funcion encargada de devolver un listado de ficheros de un directorio y
	 * sobre un nombre de fichero
	 * 
	 * @param dir
	 *            directorio del fichero
	 * @param fileStart
	 *            nombre del fichero
	 * @return array de nombre de ficheros
	 */
	private String[] dameListaFicheros(String dir, String fileStart) {
		return new File(dir).list((path, name) -> Pattern.matches(fileStart + Pattern.quote(".") + "part.*", name));
	}

	public void setFichero(String fichero) {
		this.fichero = fichero;
	}
}
