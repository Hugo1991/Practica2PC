package pc2;

/**
 * @author Hugo Fernandez Visier
 * 
 * Este fichero contendra tres clases:
 * DownloadFile. sera la clase principal
 * FileDownloader. Descargará el fichero
 * MergeFile. Unira los ficheros .part
 */
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

/**
 * 
 * Clase principal, con los siguientes atributos: 
 * -RUTADESCARGA, que sera una constante que tenga la ruta de descargas. 
 * -fichero sera el nombre del fichero a leer.
 * - merge sera el cyclicBarrier que ejecutara la clase Runnable MergeFile cuando se completen las descargas. 
 * -semMaxDescargas será un semaforo que controlará el numero máximo de descargas (exclusion mutua). 
 * -latchDescarga sera el encargado de controlar que se descargue el siguiente fichero
 * unicamente cuando se completen todas las descargas del anterior.
 * 
 * Y metodos: 
 * startDownload(): Lee el fichero de descargas y crea los hilos
 * deleteFiles(): elimina los ficheros .part 
 * getListaFichero(): devuelve los ficheros de un directorio 
 * setFichero(): asigna el nombre del fichero a la variable, utilizado en el main para asignar el nombre del fichero
 * getSemMaxDescargas(): devuelve el semaforo semMaxDescargas necesario para otra clase 
 * getLatchDescarga(): devuelve el CountDownLatch utilizado necesario para otra clase
 * 
 */
public class FileDownloader {
	private static final String RUTADESCARGA = "downloads";
	private String fichero;
	private CyclicBarrier merge;
	private static Semaphore semMaxDescargas;
	private static CountDownLatch latchDescarga;

	/**
	 * Constructor que asigna el numero maximo de descargas simultaneas
	 * 
	 * @param numMaxDescargas
	 *            indica el numero maximo de descargas
	 */
	public FileDownloader(int numMaxDescargas) {
		super();
		semMaxDescargas = new Semaphore(numMaxDescargas);
	}

	/**
	 * recorre el fichero de descargas, por cada linea construye el objeto
	 * latchDescarga con el numero de partes que aparece en el fichero,
	 * Construye el objeto cyclicBarrier con el numero de partes, cuando este
	 * ejecute todas las partes, se ejecutará la clase MergeFile crea un hilo
	 * DonwloadFile por cada parte mientras no descargue todos los ficheros de
	 * esa parte, latchDescarga se queda esperando lee la siguiente linea del
	 * fichero
	 */
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
	public static void deleteFiles(String dir, String fileStart) {
		for (String fileName : getListaFicheros(dir, fileStart))
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
	static String[] getListaFicheros(String dir, String fileStart) {
		String[] files = new File(dir)
				.list((path, name) -> Pattern.matches(fileStart + Pattern.quote(".") + "part.*", name));
		Arrays.sort(files);
		return files;
	}

	/**
	 * Asigna el nombre del fichero de clase
	 * 
	 * @param fichero
	 *            nombre del fichero
	 */
	public void setFichero(String fichero) {
		this.fichero = fichero;
	}

	public static Semaphore getSemMaxDescargas() {
		return semMaxDescargas;
	}

	public static CountDownLatch getLatchDescarga() {
		return latchDescarga;
	}
} // FIN CLASE DOWNLOADFILE

/**
 * 
 * @author Hugo
 *
 *         Clase encarga de descargar los fichero
 */
class DownloadFile implements Runnable {
	String url = null;
	String path = null;
	CyclicBarrier merge = null;

	/**
	 * Constructor que asigna los valores a las variables de clase
	 * 
	 * @param url
	 *            Direccion URL a descargar
	 * @param path
	 *            Directorio donde descargar los ficheros con el nombre del
	 *            fichero
	 * @param merge
	 *            Objeto cyclicBarrier para la espera de que no se ejecute la
	 *            clase MergeFile sin haber descargado todas las partes
	 */
	public DownloadFile(String url, String path, CyclicBarrier merge) {
		this.url = url;
		this.path = path;
		this.merge = merge;
		new Thread(this).start();
	}

	/**
	 * Descarga un fichero a traves de la direccion url en el fichero, este
	 * metodo es de exclusion mutua gracias al semaforo, por lo que no se podran
	 * descargar mas ficheros mientras existan ya las descargas
	 * 
	 * @param url
	 *            Direccion URL a descargar
	 * @param path
	 *            Ruta con el nombre del fichero a descargar
	 */
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

	/**
	 * Hace una llamada al metodo de descargar fichero, y quita un permiso a la
	 * descarga cada vez que se descargue un fichero
	 */
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
} // FIN CLASE FILEDOWNLOADER

/**
 * @author Hugo 
 * Clase encargada de unir los archivos
 * 
 */
class MergeFile implements Runnable {
	String fichero = null;
	String dir = null;

	/**
	 * Constructor de clase que asigna los valores que se le pasa a las
	 * variables de clase
	 * 
	 * @param dir
	 *            nombre del directorio a unir
	 * @param fileStart
	 *            Nombre del fichero a unir
	 */
	public MergeFile(String dir, String fileStart) {
		fichero = fileStart;
		this.dir = dir;
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

	public void mergeFile(String dir, String fileStart) {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			File ofile = new File(dir + "/" + fileStart);
			if (ofile.exists())
				ofile.delete();
			int bytesRead = 0;
			fos = new FileOutputStream(ofile, true);
			for (String fileName : FileDownloader.getListaFicheros(dir, fileStart)) {
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

	/**
	 * Muestra por pantalla que se ha descargado el fichero, hace la llamada al
	 * metodo mergeFile y llama al metodo deleteFiles
	 */
	@Override
	public void run() {
		System.out.println("Descargado fichero " + fichero);
		mergeFile(dir, fichero);
		FileDownloader.deleteFiles(dir, fichero);
	}
} // FIN CLASE MERGEFILE