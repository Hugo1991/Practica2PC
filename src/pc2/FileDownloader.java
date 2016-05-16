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
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

public class FileDownloader {
	private static final String RUTADESCARGA = "downloads";
	private String fichero;
	private int numMaxDescargas;
	private CyclicBarrier merge;
	private static Semaphore semMaxDescargas;
	private static CountDownLatch latchDescarga;

	public FileDownloader(int numMaxDescargas) {
		super();
		this.numMaxDescargas = numMaxDescargas;
		semMaxDescargas = new Semaphore(this.numMaxDescargas);
	}

	@SuppressWarnings("resource")
	public void startDownload() {
		try {
			FileReader f = new FileReader(fichero);
			BufferedReader b = new BufferedReader(f);
			String linea = b.readLine();
			
			while (!(linea == null)) {
				String[] datos = linea.split("\\s+");
				String ruta = datos[0];
				String nombre = datos[1];
				int numPartes = Integer.parseInt(datos[2]);
				latchDescarga = new CountDownLatch(numPartes);
				String num = null;
				merge = new CyclicBarrier(numPartes, new MergeFile(RUTADESCARGA, nombre));
				for (int i = 0; i < numPartes; i++) {
					if (i < 10)
						num = "0" + i;
					else
						num = String.valueOf(i);
					String rutaOrigen = ruta + "/" + nombre + ".part" + i;
					String rutaDestino = RUTADESCARGA + "/" + nombre + ".part" + num;

					new DownloadFile(rutaOrigen, rutaDestino, merge);

				}
				
				latchDescarga.await();
				
				linea = b.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
	static void deleteFiles(String dir, String fileStart) {

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
	static String[] dameListaFicheros(String dir, String fileStart) {

		String[] files = new File(dir)
				.list((path, name) -> Pattern.matches(fileStart + Pattern.quote(".") + "part.*", name));
		Arrays.sort(files);
		return files;
	}

	public void setFichero(String fichero) {
		this.fichero = fichero;
	}

	public static Semaphore getSemMaxDescargas() {
		return semMaxDescargas;
	}

	public static CountDownLatch getLatchDescarga() {
		return latchDescarga;
	}
}

/**
 * Metodo encargado de unir las partes que forman los ficheros de un directorio
 * 
 * @param dir
 *            nombre del directorio
 * @param fileStart
 *            nombre del fichero a unir
 */

class MergeFile implements Runnable {
	String fichero = null;
	String dir = null;

	public MergeFile(String dir, String fileStart) {
		fichero = fileStart;
		this.dir = dir;
	}

	public void mergeFile(String dir, String fileStart) {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			File ofile = new File(dir + "/" + fileStart);
			if (ofile.exists())
				ofile.delete();
			int bytesRead = 0;
			fos = new FileOutputStream(ofile, true);
			for (String fileName : FileDownloader.dameListaFicheros(dir, fileStart)) {
				File f = new File(dir + "/" + fileName);
				fis = new FileInputStream(f);
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
		} catch (IOException e) {
			fis = null;
			fos = null;
		}

	}

	@Override
	public void run() {

		System.out.println("Descargado fichero "+fichero);
		mergeFile(dir, fichero);
		FileDownloader.deleteFiles(dir, fichero);
		

	}
}

class DownloadFile implements Runnable {
	String url = null;
	String path = null;
	CyclicBarrier merge = null;

	public DownloadFile(String url, String path, CyclicBarrier merge) {
		this.url = url;
		this.path = path;
		this.merge = merge;
		new Thread(this).start();
	}

	@Override
	public void run() {
		downloadFile(url, path);
		FileDownloader.getLatchDescarga().countDown();

		try {
			merge.await();
		} catch (InterruptedException e) {

			e.printStackTrace();
		} catch (BrokenBarrierException e) {

			e.printStackTrace();
		}

	}

	private void downloadFile(String url, String path) {
		try {
			FileDownloader.getSemMaxDescargas().acquire();
			URL website = new URL(url);
			File folder = new File(path.split("/")[0]);
			if (!folder.exists())
				folder.mkdir();
			InputStream in = website.openStream();
			Path pathOut = Paths.get(path);
			Files.copy(in, pathOut, StandardCopyOption.REPLACE_EXISTING);
			in.close();
			FileDownloader.getSemMaxDescargas().release();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}