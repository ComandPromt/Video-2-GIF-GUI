package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.elements.PlayBin;
import org.json.JSONArray;
import org.json.JSONObject;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.sun.jna.Platform;

import lib.SimpleVideoComponent;
import net.bramp.ffmpeg.FFmpeg;

public class PlayerFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private String carpeta;

	static String directorioActual;

	static String os = System.getProperty("os.name");

	static String separador;

	private JTextField size = new JTextField();

	private static JSlider positionSlider = new JSlider();

	private JComboBox<String> posicionWatermark = new JComboBox<String>();

	private JComboBox<String> colorWatermark = new JComboBox<String>();

	JCheckBox watermark;
	JCheckBox prev;
	JComboBox<String> calidad;

	JCheckBox guardarMismaCarpeta;

	private JPanel contentPane;

	private JCheckBox blur;

	private boolean reproducir = false;

	private double segundosInicio;

	private double segundosFin;

	private PlayBin playbin;

	private final JFileChooser fileChooser = new JFileChooser();

	private JTextField duracionVideo;

	private JTextField anchoVideoTxt;

	private JTextField largoVideoTxt;

	private JTextField rate;

	private String video;

	private int anchoVideo, largoVideo;

	private float frameRate;

	private static JLabel duracion;

	private static JLabel tiempo;

	private static long segundosActual;

	private static long duration;

	private static double position = 0;

	final JLabel playPauseButton = new JLabel("");

	private JTextField inicio;

	private JTextField textoWatermark;
	private static JTextField fin;
	private JTextField brillo_1;
	private JTextField color_1;
	private JTextField brillo_2;
	private JTextField color_2;

	public static String saberSeparador(String os) {
		if (os.equals("Linux")) {
			return "/";
		} else {
			return "\\";
		}
	}

	private String saberCarpetaGif(int filtro) {

		carpeta = directorioActual + "output" + separador;

		if (filtro == 2 || (filtro == 3 && guardarMismaCarpeta.isSelected())) {
			carpeta = video.substring(0, video.lastIndexOf(separador) + 1);
		}

		return carpeta;

	}

	private void ponerDuracionFin(boolean filtro) {

		String positionVideo = calcularPosicionVideo();

		segundosInicio = convertirASegundos(inicio.getText());

		segundosFin = convertirASegundos(positionVideo);

		if (segundosInicio == segundosFin || (segundosFin > 0f && (segundosInicio > segundosFin))) {

			segundosFin = convertirASegundos(duracion.getText());

			positionVideo = duracion.getText();

			duracionVideo.setText("");

		}

		if (filtro) {
			segundosFin = convertirASegundos(fin.getText());
		}

		fin.setText(positionVideo);

		ponerDuracionGif();

	}

	private void ponerDuracionGif() {

		DecimalFormat df = new DecimalFormat("0.000");

		String resultado = "" + df.format(segundosFin - segundosInicio);

		if (resultado.contains("-")) {
			resultado = "";
		}

		duracionVideo.setText(resultado);

	}

	double convertirASegundos(String duracionVideo) {

		double horas, minutos, segundos;

		try {

			horas = Double.parseDouble(duracionVideo.substring(0, duracionVideo.indexOf(":")));

			if (horas > 0) {
				horas *= 3600f;
			}

			minutos = Double.parseDouble(
					duracionVideo.substring(duracionVideo.indexOf(":") + 1, duracionVideo.lastIndexOf(":")));

			if (minutos > 0) {
				minutos *= 60f;
			}

			segundos = Double
					.parseDouble(duracionVideo.substring(duracionVideo.lastIndexOf(":") + 1, duracionVideo.length()));
		} catch (Exception e) {
			horas = 0;
			minutos = 0;
			segundos = 0;
		}

		return horas + minutos + segundos;
	}

	private void play(boolean filtro) {

		try {

			duration = playbin.queryDuration(TimeUnit.NANOSECONDS);

			if (reproducir || duration > 0) {

				boolean playing = playbin.isPlaying();

				if (playing) {

					playbin.pause();
				}

				else {
					playbin.play();
				}

				ponerTiempos(filtro);

				playPauseButton.setIcon(new ImageIcon(getClass().getResource("/play-pause.png")));
			}
		}

		catch (Exception e) {
		}

	}

	private void abrirVideo() throws ImageProcessingException, IOException {

		int returnValue = fileChooser.showOpenDialog(contentPane);

		reproducir = true;

		if (returnValue == JFileChooser.APPROVE_OPTION) {

			video = fileChooser.getSelectedFile().toURI().toString();

			video = video.replace("%20", " ");

			video = video.replace("file:", "");

			video = video.trim();

			playbin.stop();

			InputStream inputstream = new FileInputStream(video);

			Metadata metadata = ImageMetadataReader.readMetadata(inputstream);

			String etiqueta = "";

			for (Directory directory : metadata.getDirectories()) {

				for (com.drew.metadata.Tag tag : directory.getTags()) {

					etiqueta = tag.toString();

					etiqueta = etiqueta.replace(" pixels", "");

					if (etiqueta.contains("[MP4 Video] Width - ")) {

						anchoVideo = Integer
								.parseInt(etiqueta.substring(etiqueta.indexOf("Width - ") + 8, etiqueta.length()));

					}

					if (etiqueta.contains("[MP4 Video] Height - ")) {

						largoVideo = Integer
								.parseInt(etiqueta.substring(etiqueta.indexOf("Height - ") + 9, etiqueta.length()));

					}

					if (etiqueta.contains("[MP4 Video] Frame Rate - ")) {
						etiqueta = etiqueta.replace(",", ".");
						frameRate = Float.parseFloat(
								etiqueta.substring(etiqueta.indexOf("Frame Rate - ") + 13, etiqueta.length()));

					}
				}
			}

			anchoVideoTxt.setText("" + anchoVideo);

			largoVideoTxt.setText("" + largoVideo);

			rate.setText("" + frameRate);

			playbin.setURI(fileChooser.getSelectedFile().toURI());

			playbin.play();

		}

		verTiempos();

		play(true);

		play(true);

	}

	static LinkedList<Object> saberTiempo() {

		LinkedList<Object> tiempo = new LinkedList<Object>();

		position = positionSlider.getValue() / 1000.0;

		long segundos = duration / 1000000000;

		tiempo.add(position);

		tiempo.add(segundos);

		return tiempo;

	}

	private String calcularPosicionVideo() {

		String positionVideo = "";

		if (!tiempo.getText().isEmpty()) {

			double segundos = Double.parseDouble(tiempo.getText()
					.substring(tiempo.getText().lastIndexOf(":") + 1, tiempo.getText().length()).trim());

			segundos += position;

			positionVideo = tiempo.getText().substring(0, tiempo.getText().lastIndexOf(":") + 1) + segundos;
		}

		positionVideo = positionVideo.replace(" ", "");

		return positionVideo;

	}

	private void inicializar() {

		inicio.setText("00:00:0.0");

		ponerDuracionInicio(false);

		inicio.setText("00:00:0.0");

	}

	static String calcularSegundosActual(Object obj1, Object obj2) {

		double inicio = (double) obj1;

		long segundos = (long) obj2;

		segundosActual = Math.round((inicio / segundos) * Math.pow(segundos, 2));

		return calcularTiempo(segundosActual);

	}

	private static String readAll(Reader rd) throws IOException {

		StringBuilder sb = new StringBuilder();

		int cp;

		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}

		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException {

		InputStream is = new URL(url).openStream();

		BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

		String jsonText = readAll(rd);
		is.close();

		return new JSONObject(jsonText);

	}

	public static JSONObject apiImagenes(String parametros) throws IOException {

		JSONObject json = readJsonFromUrl("https://apiperiquito.herokuapp.com/recibo-json.php?imagenes=" + parametros);

		return json;
	}

	public static String extraerNombreArchivo(String extension) throws IOException {

		JSONObject json = apiImagenes("archivo." + extension);

		JSONArray imagenesBD = json.getJSONArray("imagenes_bd");

		return imagenesBD.get(0).toString();
	}

	public static void abrirCarpeta(String ruta) throws IOException {

		if (ruta != null && !ruta.equals("") && !ruta.isEmpty()) {

			try {

				if (os.contentEquals("Linux")) {
					Runtime.getRuntime().exec("xdg-open " + ruta);
				}

				else {
					Runtime.getRuntime().exec("cmd /c explorer " + "\"" + ruta + "\"");
				}

			}

			catch (IOException e) {

			}
		}

	}

	public static String calcularTiempo(long segundos) {

		int minutos = 0;

		int horas = 0;

		if (segundos == 60) {
			minutos = 1;
			segundos = 0;
		}

		minutos = (int) (segundos / 60);

		int calculoSegundos = 0;

		calculoSegundos = 60 * minutos;

		segundos -= calculoSegundos;

		if (minutos == 60) {
			horas = 1;
			minutos = 0;
			segundos = 0;
		}

		if (minutos > 60) {

			if (minutos % 60 == 0) {
				horas = minutos / 60;
				minutos = 0;
				segundos = 0;
			}

			else {

				int contador = 0;

				int horaProxima = 120;

				int siguienteHora = 0;

				while (contador == 0) {

					if (minutos < horaProxima) {
						contador = horaProxima;
					}

					else {

						siguienteHora = horaProxima + 60;

						if (minutos > horaProxima && minutos < siguienteHora) {
							contador = siguienteHora;
						}

						horaProxima = siguienteHora;

					}
				}

				horas = minutos / 60;

				minutos = 60 - (horaProxima - minutos);

			}

		}

		String ceroHoras = "";
		String ceroMinutos = "";

		String ceroSegundos = "";

		if (horas <= 9) {
			ceroHoras = "0";
		}

		if (minutos <= 9) {
			ceroMinutos = "0";
		}

		if (segundos <= 9) {
			ceroSegundos = "0";
		}

		return ceroHoras + horas + " : " + ceroMinutos + minutos + " : " + ceroSegundos + segundos;

	}

	private void verTiempos() {

		duration = playbin.queryDuration(TimeUnit.NANOSECONDS);

		if (duration > 0 && video != null) {

			try {

				InputStream inputstream = new FileInputStream(video);

				Metadata metadata = ImageMetadataReader.readMetadata(inputstream);

				String etiqueta = "";

				String resultado = "";

				for (Directory directory : metadata.getDirectories()) {

					for (com.drew.metadata.Tag tag : directory.getTags()) {

						etiqueta = tag.toString();

						if (etiqueta.contains("Duration")) {
							resultado = etiqueta.substring(etiqueta.indexOf("-") + 1, etiqueta.length());

						}

					}

				}

				resultado = resultado.trim();

				convertirANanoSegundos(resultado, true);

			}

			catch (Exception e) {

			}

		}

		if (prev.isSelected())

		{
			prev.setSelected(false);

			playbin.seek(0);

		}

	}

	private long convertirANanoSegundos(String duracionVideo, boolean filtro) {

		long nanosegundos;

		nanosegundos = (long) convertirASegundos(duracionVideo) * 1000000000;

		if (filtro) {

			duration = nanosegundos;

			tiempo.setText(calcularSegundosActual(0D, nanosegundos));

		}

		return nanosegundos;

	}

	static LinkedList<Object> ponerTiempos(boolean filtro) {

		LinkedList<Object> cuenta;

		cuenta = saberTiempo();

		tiempo.setText(calcularSegundosActual(cuenta.get(0), cuenta.get(1)));

		duracion.setText("" + calcularTiempo((long) cuenta.get(1)));

		if (filtro) {
			fin.setText("" + calcularTiempo((long) cuenta.get(1)));
		}

		return cuenta;
	}

	private void verVideo() {

		if (duration > 0 && (prev.isSelected() || positionSlider.getValueIsAdjusting())) {

			verTiempos();

		}

	}

	public static void initialize(boolean windows) throws Exception {

		System.setProperty("awt.useSystemAAFontSettings", "lcd");

		System.setProperty("swing.aatext", "true");

		if (windows) {
			System.setProperty("gstreamer.GstNative.nameFormats", "%s-1.0-0|%s-1.0|%s-0|%s|lib%s|lib%s-0");
		}

		for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {

			if ("Nimbus".equals(info.getName())) {

				UIManager.setLookAndFeel(info.getClassName());

				break;

			}
		}

	}

	public static void main(String[] args) throws IOException {

		positionSlider = new JSlider(0, 1000);

		positionSlider.setBorder(null);

		Gst.init();

		separador = saberSeparador(os);

		directorioActual = new File(".").getCanonicalPath() + separador;

		File directorio = new File(directorioActual + "output");

		directorio.mkdir();

		try {

			initialize(Platform.isWindows());

			PlayerFrame frame = new PlayerFrame();

			frame.setVisible(true);

			if (args.length > 0) {
				frame.openFile(args[0]);
			}

			frame.pack();

			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

			// make the frame half the height and width

			frame.setSize(700, 710);

			// center the jframe on screen
			frame.setLocationRelativeTo(null);

			frame.setVisible(true);

		}

		catch (Exception e) {

		}

	}

	private void ponerDuracionInicio(boolean filtro) {

		if (!inicio.getText().isEmpty() && !fin.getText().isEmpty()) {

			try {

				String positionVideo = "";

				segundosInicio = convertirASegundos(inicio.getText());

				segundosFin = convertirASegundos(fin.getText());

				if (segundosInicio == segundosFin || (segundosFin > 0f && (segundosInicio > segundosFin))) {

					inicializar();
				}

				ponerDuracionGif();

				if (filtro) {
					positionVideo = calcularPosicionVideo();

				}

				inicio.setText(positionVideo);

			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public PlayerFrame() {
		setIconImage(
				Toolkit.getDefaultToolkit().getImage(PlayerFrame.class.getResource("/imagenes/video_2_frame.png")));

		addKeyListener(new KeyAdapter() {

			@Override

			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}

			}

		});

		setTitle("Easy Video 2 GIF");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();

		setJMenuBar(menuBar);

		JMenu mnNewMenu = new JMenu("Abrir");
		mnNewMenu.setFont(new Font("Dialog", Font.PLAIN, 16));
		mnNewMenu.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/abrir.png")));
		menuBar.add(mnNewMenu);

		JMenuItem mntmNewMenuItem = new JMenuItem("Video (CTRL+O)");
		mntmNewMenuItem.setFont(new Font("Dialog", Font.PLAIN, 16));
		mntmNewMenuItem.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/video_2_frame.png")));
		mnNewMenu.add(mntmNewMenuItem);

		JSeparator separator = new JSeparator();
		mnNewMenu.add(separator);

		JMenuItem mntmNewMenuItem_1 = new JMenuItem("Carpeta de salida (programa)");
		mntmNewMenuItem_1.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				try {
					abrirCarpeta(saberCarpetaGif(1));
				} catch (Exception e1) {

				}
			}
		});
		mntmNewMenuItem_1.setFont(new Font("Dialog", Font.PLAIN, 16));
		mntmNewMenuItem_1.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/folder.png")));
		mnNewMenu.add(mntmNewMenuItem_1);

		JSeparator separator_1 = new JSeparator();
		mnNewMenu.add(separator_1);

		JMenuItem mntmNewMenuItem_2 = new JMenuItem("Carpeta del vídeo");
		mntmNewMenuItem_2.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {

				if (!duracion.getText().isEmpty()) {

					try {

						abrirCarpeta(saberCarpetaGif(2));
					}

					catch (Exception e1) {

					}

				}

			}

		});

		mntmNewMenuItem_2.setFont(new Font("Dialog", Font.PLAIN, 16));
		mntmNewMenuItem_2.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/folder.png")));
		mnNewMenu.add(mntmNewMenuItem_2);

		mntmNewMenuItem.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				try {
					abrirVideo();
				} catch (Exception e1) {
				}
			}

		});

		contentPane = new JPanel();

		contentPane.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}

			}

		});

		contentPane.setForeground(Color.WHITE);

		contentPane.setBackground(Color.DARK_GRAY);

		setContentPane(contentPane);

		final SpringLayout sl_contentPane = new SpringLayout();
		sl_contentPane.putConstraint(SpringLayout.WEST, posicionWatermark, 37, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, size, -14, SpringLayout.NORTH, colorWatermark);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, colorWatermark, -16, SpringLayout.NORTH, posicionWatermark);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, posicionWatermark, -24, SpringLayout.SOUTH, contentPane);

		contentPane.setLayout(sl_contentPane);

		final String[] videoExts = new String[] { "asf", "avi", "3gp", "mp4", "mov", "flv", "mpg", "ts", "mkv", "webm",
				"mxf", "ogg" };

		fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Video File", videoExts));

		JLabel openFileButton = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.WEST, openFileButton, 6, SpringLayout.EAST, positionSlider);
		sl_contentPane.putConstraint(SpringLayout.EAST, openFileButton, -21, SpringLayout.EAST, contentPane);

		openFileButton.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {

				try {
					abrirVideo();
				} catch (Exception e1) {

				}

			}

		});

		openFileButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK), "open");

		openFileButton.getActionMap().put("open", new AbstractAction("open") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {

				int returnValue = fileChooser.showOpenDialog(contentPane);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					playbin.stop();
					playbin.setURI(fileChooser.getSelectedFile().toURI());
					playbin.play();
				}

			}

		});

		openFileButton.setToolTipText("");

		openFileButton.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/open-file.png")));

		contentPane.add(openFileButton);

		sl_contentPane.putConstraint(SpringLayout.WEST, positionSlider, 218, SpringLayout.WEST, contentPane);

		sl_contentPane.putConstraint(SpringLayout.EAST, positionSlider, -45, SpringLayout.EAST, contentPane);

		positionSlider.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {

				verVideo();

			}

		});

		positionSlider.setValue(0);

		positionSlider.setBackground(Color.DARK_GRAY);

		contentPane.add(positionSlider);

		new Timer(50, new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				if (playbin == null || positionSlider == null)

					return;

				if (!positionSlider.getValueIsAdjusting() && playbin.isPlaying()) {

					long dur = playbin.queryDuration(TimeUnit.NANOSECONDS);

					long pos = playbin.queryPosition(TimeUnit.NANOSECONDS);

					if (dur > 0) {

						double relPos = (double) pos / dur;

						positionSlider.setValue((int) (relPos * 1000));

					}

					if (dur == pos && dur > 0) {

						playbin.seek(0);

						playbin.stop();
					}

				}

			}

		}).start();

		SimpleVideoComponent videoOutput = new SimpleVideoComponent();
		sl_contentPane.putConstraint(SpringLayout.NORTH, openFileButton, 6, SpringLayout.SOUTH, videoOutput);
		sl_contentPane.putConstraint(SpringLayout.EAST, openFileButton, 0, SpringLayout.EAST, videoOutput);
		sl_contentPane.putConstraint(SpringLayout.NORTH, positionSlider, 15, SpringLayout.SOUTH, videoOutput);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, videoOutput, -269, SpringLayout.SOUTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.NORTH, videoOutput, 10, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, videoOutput, 184, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, videoOutput, -10, SpringLayout.EAST, contentPane);
		contentPane.add(videoOutput);

		JButton btnNewButton_1_1 = new JButton("<|");
		btnNewButton_1_1.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}
		});

		btnNewButton_1_1.addMouseListener(new MouseAdapter() {
			@Override

			public void mousePressed(MouseEvent e) {

				if (inicio.getText().isEmpty()) {
					inicializar();
				}

				ponerDuracionInicio(true);

				ponerDuracionGif();

			}

		});
		btnNewButton_1_1.setHorizontalAlignment(SwingConstants.LEFT);
		btnNewButton_1_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(btnNewButton_1_1);

		JButton btnNewButton_1_1_1 = new JButton("|>");
		sl_contentPane.putConstraint(SpringLayout.NORTH, btnNewButton_1_1_1, 0, SpringLayout.NORTH, btnNewButton_1_1);

		btnNewButton_1_1_1.addKeyListener(new KeyAdapter() {

			@Override

			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}

			}

		});

		btnNewButton_1_1_1.addMouseListener(new MouseAdapter() {

			@Override

			public void mousePressed(MouseEvent e) {

				ponerDuracionFin(false);
				ponerDuracionGif();
			}

		});
		btnNewButton_1_1_1.setHorizontalAlignment(SwingConstants.LEFT);
		btnNewButton_1_1_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(btnNewButton_1_1_1);

		JLabel lblDuracin = new JLabel("Duración GIF");

		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin, 10, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin, -5, SpringLayout.WEST, videoOutput);
		lblDuracin.setHorizontalAlignment(SwingConstants.CENTER);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblDuracin, 10, SpringLayout.NORTH, contentPane);
		lblDuracin.setFont(new Font("Dialog", Font.PLAIN, 18));
		lblDuracin.setForeground(Color.WHITE);
		contentPane.add(lblDuracin);

		anchoVideoTxt = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.WEST, anchoVideoTxt, 37, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, anchoVideoTxt, -26, SpringLayout.WEST, videoOutput);
		anchoVideoTxt.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {

				try {
					if (Integer.parseInt(anchoVideoTxt.getText()) <= 0) {
						anchoVideoTxt.setText("" + anchoVideo);
					}
				} catch (Exception e1) {
					anchoVideoTxt.setText("" + anchoVideo);
				}
			}
		});

		largoVideoTxt = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.WEST, largoVideoTxt, 37, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, largoVideoTxt, -26, SpringLayout.WEST, videoOutput);
		largoVideoTxt.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					if (Integer.parseInt(largoVideoTxt.getText()) <= 0) {
						largoVideoTxt.setText("" + largoVideo);
					}
				} catch (Exception e1) {
					largoVideoTxt.setText("" + largoVideo);
				}
			}
		});

		duracionVideo = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.WEST, duracionVideo, 37, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, duracionVideo, -26, SpringLayout.WEST, videoOutput);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, duracionVideo, 31, SpringLayout.SOUTH, lblDuracin);

		duracionVideo.setHorizontalAlignment(SwingConstants.CENTER);
		duracionVideo.setEditable(false);
		sl_contentPane.putConstraint(SpringLayout.NORTH, duracionVideo, 6, SpringLayout.SOUTH, lblDuracin);
		contentPane.add(duracionVideo);
		duracionVideo.setColumns(10);

		JLabel lblDuracin_1 = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblDuracin_1, 7, SpringLayout.SOUTH, duracionVideo);
		lblDuracin_1.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/width.png")));
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin_1, 10, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin_1, -5, SpringLayout.WEST, videoOutput);
		lblDuracin_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblDuracin_1.setForeground(Color.WHITE);
		lblDuracin_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblDuracin_1);

		sl_contentPane.putConstraint(SpringLayout.NORTH, anchoVideoTxt, 6, SpringLayout.SOUTH, lblDuracin_1);
		anchoVideoTxt.setHorizontalAlignment(SwingConstants.CENTER);
		anchoVideoTxt.setColumns(10);
		contentPane.add(anchoVideoTxt);

		JLabel lblDuracin_1_1 = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblDuracin_1_1, 155, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, anchoVideoTxt, -6, SpringLayout.NORTH, lblDuracin_1_1);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, largoVideoTxt, 31, SpringLayout.SOUTH, lblDuracin_1_1);
		lblDuracin_1_1.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/height.png")));
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin_1_1, 10, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin_1_1, -5, SpringLayout.WEST, videoOutput);
		lblDuracin_1_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblDuracin_1_1.setForeground(Color.WHITE);
		lblDuracin_1_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblDuracin_1_1);

		largoVideoTxt.setHorizontalAlignment(SwingConstants.CENTER);
		sl_contentPane.putConstraint(SpringLayout.NORTH, largoVideoTxt, 6, SpringLayout.SOUTH, lblDuracin_1_1);
		largoVideoTxt.setColumns(10);
		contentPane.add(largoVideoTxt);

		JLabel lblDuracin_1_1_1 = new JLabel("Rate (FPS)");
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblDuracin_1_1_1, 12, SpringLayout.SOUTH, largoVideoTxt);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin_1_1_1, 0, SpringLayout.WEST, lblDuracin);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin_1_1_1, -5, SpringLayout.WEST, videoOutput);
		lblDuracin_1_1_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblDuracin_1_1_1.setForeground(Color.WHITE);
		lblDuracin_1_1_1.setFont(new Font("Dialog", Font.PLAIN, 18));
		contentPane.add(lblDuracin_1_1_1);

		rate = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblDuracin_1_1_1, -6, SpringLayout.NORTH, rate);
		sl_contentPane.putConstraint(SpringLayout.WEST, rate, 37, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, rate, 0, SpringLayout.EAST, duracionVideo);

		rate.addKeyListener(new KeyAdapter() {

			@Override

			public void keyReleased(KeyEvent e) {

				try {

					if (Integer.parseInt(rate.getText()) <= 0) {
						rate.setText("10");
					}

				}

				catch (Exception e1) {
					rate.setText("10");
				}

			}

		});

		rate.setText("10");
		rate.setHorizontalAlignment(SwingConstants.CENTER);
		rate.setColumns(10);
		contentPane.add(rate);

		JLabel lblDuracin_1_1_1_1 = new JLabel("Calidad");
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblDuracin_1_1_1_1, 342, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin_1_1_1_1, 37, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin_1_1_1_1, -33, SpringLayout.WEST, videoOutput);
		sl_contentPane.putConstraint(SpringLayout.NORTH, rate, -46, SpringLayout.NORTH, lblDuracin_1_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, rate, -18, SpringLayout.NORTH, lblDuracin_1_1_1_1);
		lblDuracin_1_1_1_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblDuracin_1_1_1_1.setForeground(Color.WHITE);
		lblDuracin_1_1_1_1.setFont(new Font("Dialog", Font.PLAIN, 18));
		contentPane.add(lblDuracin_1_1_1_1);

		calidad = new JComboBox<String>();
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblDuracin_1_1_1_1, -6, SpringLayout.NORTH, calidad);
		sl_contentPane.putConstraint(SpringLayout.WEST, calidad, 37, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, calidad, 0, SpringLayout.EAST, duracionVideo);
		calidad.setFont(new Font("Dialog", Font.PLAIN, 16));
		sl_contentPane.putConstraint(SpringLayout.NORTH, calidad, 377, SpringLayout.NORTH, contentPane);
		calidad.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}
		});

		contentPane.add(calidad);

		calidad.addItem("Baja");

		calidad.addItem("Alta");

		prev = new JCheckBox("Prev");
		prev.setForeground(Color.WHITE);
		prev.setBackground(Color.DARK_GRAY);
		sl_contentPane.putConstraint(SpringLayout.NORTH, prev, 86, SpringLayout.SOUTH, btnNewButton_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.WEST, prev, 510, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, prev, -28, SpringLayout.SOUTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, prev, -107, SpringLayout.EAST, contentPane);
		prev.setIcon(null);
		prev.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}
		});

		prev.addMouseListener(new MouseAdapter() {

			@Override

			public void mousePressed(MouseEvent e) {

				try {

					if (!inicio.getText().isEmpty() && !fin.getText().isEmpty() && segundosInicio < segundosFin) {

						verVideo();

					}

				}

				catch (Exception e1) {

				}

			}

		});

		prev.setFont(new Font("Dialog", Font.PLAIN, 21));

		contentPane.add(prev);

		JButton btnConvertir = new JButton("");

		btnConvertir.addKeyListener(new KeyAdapter() {

			@Override

			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}

		});

		sl_contentPane.putConstraint(SpringLayout.NORTH, btnConvertir, 0, SpringLayout.NORTH, prev);
		sl_contentPane.putConstraint(SpringLayout.WEST, btnConvertir, 14, SpringLayout.EAST, prev);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, btnConvertir, 0, SpringLayout.SOUTH, prev);

		btnConvertir.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {

				if (!inicio.getText().isEmpty() && !fin.getText().isEmpty()) {

					try {

						saberCarpetaGif(3);

						FFmpeg ffmpeg = new FFmpeg();

						LinkedList<String> comando = new LinkedList<String>();

						comando.add("-ss");

						comando.add("" + inicio.getText());

						comando.add("-t");

						comando.add(fin.getText());

						comando.add("-r");

						comando.add(rate.getText());

						comando.add("-i");

						comando.add(video);

						if (calidad.getSelectedIndex() == 0) {

							comando.add("-pix_fmt");

							comando.add("rgb24");

							comando.add("-s");

							comando.add(anchoVideoTxt.getText() + "x" + largoVideoTxt.getText());

						}

						if (calidad.getSelectedIndex() == 0 && !watermark.isSelected() && blur.isSelected()) {

							comando.add("-filter_complex");

							comando.add("[0:v]boxblur=luma_power=" + brillo_1.getText() + ":luma_radius="
									+ brillo_2.getText() + ":chroma_power=" + color_1.getText() + ":chroma_radius="
									+ color_2.getText() + "[blurred]");

							comando.add("-map");

							comando.add("[blurred]");

						}

						if (calidad.getSelectedIndex() == 1) {

							comando.add("-filter_complex");

							comando.add(
									"[0:v] fps=15,scale=w=720:h=-1,split [a][b];[a] palettegen=stats_mode=single [p];[b][p] paletteuse=new=1");

						}

						if (calidad.getSelectedIndex() == 0 && watermark.isSelected()) {

							comando.add("-i");

							comando.add(directorioActual + "watermark.png");

							comando.add("-filter_complex");

							String x = "";

							String y = "";

							String color = "";

							switch (posicionWatermark.getSelectedIndex()) {

							case 0:

								x = "(text_w)/2";

								y = "(text_h)/2";

								break;

							case 1:

								x = "(w-text_w)/2";

								y = "5";

								break;

							case 2:

								x = "(w-text_w)-5";

								y = "(text_h)/2";

								break;

							case 3:

								x = "5";

								y = "(h-text_h)/2";

								break;

							case 4:

								x = "(w-text_w)/2";

								y = "(h-text_h)/2";

								break;

							case 5:

								x = "(w-text_w)-5";

								y = "(h-text_h)/2";

								break;

							case 6:

								x = "5";

								y = "(h-text_h)-5";

								break;

							case 7:

								x = "(w-text_w)/2";

								y = "(h-text_h)-5";

								break;

							case 8:

								x = "(w-text_w)-5";

								y = "(h-text_h)-5";

								break;

							}

							switch (colorWatermark.getSelectedIndex()) {

							case 0:

								color = "black";

								break;

							case 1:

								color = "white";

								break;

							case 2:

								color = "red";

								break;

							case 3:

								color = "blue";

								break;

							case 4:

								color = "yellow";

								break;
							case 5:

								color = "lime";

								break;

							case 6:

								color = "pink";

								break;

							case 7:

								color = "violet";

								break;

							case 8:

								color = "gray";

								break;

							case 9:

								color = "cyan";

								break;

							case 10:

								color = "darkblue";

								break;

							case 11:

								color = "lightblue";

								break;

							case 12:

								color = "Purple";

								break;

							case 13:

								color = "Magenta";

								break;

							case 14:

								color = "Silver";

								break;

							case 15:

								color = "orange";

								break;

							case 16:

								color = "brown";
								break;

							case 17:

								color = "maroon";

								break;

							case 18:
								color = "green";

								break;
							case 19:

								color = "olive";

								break;

							case 20:

								color = "aquamarine";

								break;

							case 21:

								color = "gold";

								break;

							case 22:

								color = "darkorange";

								break;

							case 23:

								color = "#CFB53B";

								break;

							case 24:
								color = "chocolate";
								break;

							case 25:
								color = "#D4AF37";
								break;

							case 26:
								color = "turquoise";
								break;

							case 27:
								color = "teal";
								break;

							case 28:
								color = "seagreen";
								break;

							case 29:
								color = "#78866B";
								break;

							case 30:
								color = "#CD7F32";
								break;

							case 31:
								color = "#F3E5AB";
								break;

							}

							comando.add("[0]fps=" + rate.getText() + ",scale=" + anchoVideoTxt.getText() + ":"
									+ largoVideoTxt.getText()
									+ "[bg];[bg][1]overlay=main_w-overlay_w-10:main_h-overlay_h-10:format=auto,drawtext=text='"
									+ textoWatermark.getText() + "':fontsize=" + size.getText() + ":fontcolor=" + color
									+ ":x=" + x + ":y=" + y + ",split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse");
						}

						comando.add("-y");

						String archivo = carpeta + extraerNombreArchivo("test.gif");

						comando.add(archivo);

						ffmpeg.run(comando);

						abrirCarpeta("file://" + archivo);

					}

					catch (Exception e) {

					}

				}

			}

		});

		btnConvertir.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/video_2_frame.png")));
		btnConvertir.setFont(new Font("Dialog", Font.BOLD, 14));
		contentPane.add(btnConvertir);
		colorWatermark.setFont(new Font("Dialog", Font.PLAIN, 14));
		colorWatermark.setEnabled(false);
		colorWatermark.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}
		});

		Colors[] colores = Colors.values();

		LinkedList<String> listaColores = new LinkedList<String>();

		for (int i = 0; i < colores.length; i++) {
			listaColores.add("" + colores[i]);
		}

		for (int x = 0; x < listaColores.size(); x++) {
			colorWatermark.addItem(listaColores.get(x));
		}

		contentPane.add(colorWatermark);

		tiempo = new JLabel("");

		sl_contentPane.putConstraint(SpringLayout.NORTH, btnNewButton_1_1, 6, SpringLayout.SOUTH, tiempo);
		sl_contentPane.putConstraint(SpringLayout.WEST, btnNewButton_1_1, 0, SpringLayout.WEST, tiempo);
		sl_contentPane.putConstraint(SpringLayout.NORTH, tiempo, 6, SpringLayout.SOUTH, positionSlider);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, tiempo, -217, SpringLayout.SOUTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, tiempo, 92, SpringLayout.EAST, lblDuracin_1_1_1_1);

		tiempo.setFont(new Font("Dialog", Font.PLAIN, 12));

		tiempo.setForeground(Color.WHITE);

		contentPane.add(tiempo);

		duracion = new JLabel("");

		sl_contentPane.putConstraint(SpringLayout.NORTH, duracion, 37, SpringLayout.SOUTH, videoOutput);
		sl_contentPane.putConstraint(SpringLayout.EAST, duracion, -43, SpringLayout.EAST, contentPane);
		duracion.setForeground(Color.WHITE);
		contentPane.add(duracion);

		inicio = new JTextField();

		inicio.setText("00:00:0.0");

		inicio.addKeyListener(new KeyAdapter() {

			@Override

			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}

				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					String tiempoInicio = inicio.getText();

					inicio.setText(tiempoInicio);
					ponerDuracionInicio(true);
					inicio.setText(tiempoInicio);
				}

			}

			@Override

			public void keyReleased(KeyEvent e) {

				if (inicio.getText().trim().isEmpty()) {

					inicio.setText("00:00:0.0");

				}

			}

		});

		sl_contentPane.putConstraint(SpringLayout.WEST, inicio, 4, SpringLayout.EAST, btnNewButton_1_1);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, inicio, -181, SpringLayout.SOUTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.NORTH, inicio, 28, SpringLayout.SOUTH, positionSlider);
		contentPane.add(inicio);
		inicio.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel("");

		lblNewLabel_1.addMouseListener(new MouseAdapter() {

			@Override

			public void mousePressed(MouseEvent e) {

				fin.setText(duracion.getText());

				ponerDuracionFin(false);

			}

		});

		sl_contentPane.putConstraint(SpringLayout.EAST, inicio, -82, SpringLayout.WEST, lblNewLabel_1);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblNewLabel_1, -6, SpringLayout.WEST, btnNewButton_1_1_1);
		lblNewLabel_1.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/flag.png")));
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_1.setForeground(Color.WHITE);
		lblNewLabel_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblNewLabel_1);

		JLabel lblNewLabel = new JLabel("");
		lblNewLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {

				inicializar();

			}

		});
		sl_contentPane.putConstraint(SpringLayout.NORTH, size, 26, SpringLayout.SOUTH, lblNewLabel);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel_1, 0, SpringLayout.SOUTH, lblNewLabel);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel, 0, SpringLayout.WEST, videoOutput);
		lblNewLabel.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/start.png")));
		contentPane.add(lblNewLabel);

		watermark = new JCheckBox("WaterMark");

		watermark.addKeyListener(new KeyAdapter() {

			@Override

			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}

			}

		});

		sl_contentPane.putConstraint(SpringLayout.NORTH, watermark, 11, SpringLayout.SOUTH, calidad);
		sl_contentPane.putConstraint(SpringLayout.WEST, watermark, 20, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, watermark, -23, SpringLayout.WEST, videoOutput);

		watermark.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent arg0) {

				if (watermark.isSelected()) {

					size.setEnabled(true);

					posicionWatermark.setEnabled(true);

					colorWatermark.setEnabled(true);

					textoWatermark.setEnabled(true);
				}

				else {

					textoWatermark.setEnabled(false);

					size.setEnabled(false);

					posicionWatermark.setEnabled(false);

					colorWatermark.setEnabled(false);

				}

			}
		});
		watermark.setBackground(Color.DARK_GRAY);
		watermark.setHorizontalAlignment(SwingConstants.CENTER);
		watermark.setForeground(Color.WHITE);
		watermark.setFont(new Font("Dialog", Font.PLAIN, 18));
		contentPane.add(watermark);

		textoWatermark = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.NORTH, textoWatermark, 13, SpringLayout.SOUTH, watermark);
		sl_contentPane.putConstraint(SpringLayout.WEST, textoWatermark, 37, SpringLayout.WEST, contentPane);
		textoWatermark.setEnabled(false);
		textoWatermark.setToolTipText("Inserta el texto para la marca de agua");
		textoWatermark.setHorizontalAlignment(SwingConstants.CENTER);
		textoWatermark.setColumns(10);
		contentPane.add(textoWatermark);

		JLabel label = new JLabel("");

		label.addMouseListener(new MouseAdapter() {

			@Override

			public void mousePressed(MouseEvent e) {
				reproducir = false;

				play(false);
			}

		});

		sl_contentPane.putConstraint(SpringLayout.NORTH, label, 0, SpringLayout.NORTH, openFileButton);
		sl_contentPane.putConstraint(SpringLayout.EAST, label, -6, SpringLayout.WEST, positionSlider);
		label.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/play-pause.png")));

		contentPane.add(label);
		size.setEnabled(false);
		size.setFont(new Font("Dialog", Font.PLAIN, 16));

		size.addKeyListener(new KeyAdapter() {

			@Override

			public void keyReleased(KeyEvent e) {

				try {

					if (Integer.parseInt(size.getText()) <= 0) {
						size.setText("24");
					}

				} catch (Exception e1) {
					size.setText("24");
				}

			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}
		});

		size.setText("24");
		size.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(size);
		posicionWatermark.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}
		});
		posicionWatermark.setEnabled(false);

		posicionWatermark.setFont(new Font("Dialog", Font.PLAIN, 16));
		contentPane.add(posicionWatermark);

		posicionWatermark.addItem("ARRIBA - IZQUIERDA");
		posicionWatermark.addItem("ARRIBA - CENTRO");
		posicionWatermark.addItem("ARRIBA - DERECHA");
		posicionWatermark.addItem("MEDIO - IZQUIERDA");
		posicionWatermark.addItem("MEDIO - CENTRO");
		posicionWatermark.addItem("MEDIO - DERECHA");
		posicionWatermark.addItem("ABAJO - IZQUIERDA");
		posicionWatermark.addItem("ABAJO - CENTRO");
		posicionWatermark.addItem("ABAJO - DERECHA");

		JLabel lblNewLabel_2 = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.SOUTH, textoWatermark, -28, SpringLayout.NORTH, lblNewLabel_2);
		sl_contentPane.putConstraint(SpringLayout.WEST, colorWatermark, 6, SpringLayout.EAST, lblNewLabel_2);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel_2, 0, SpringLayout.WEST, duracionVideo);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel_2, 0, SpringLayout.SOUTH, colorWatermark);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblNewLabel_2, -595, SpringLayout.EAST, contentPane);
		lblNewLabel_2.setHorizontalAlignment(SwingConstants.RIGHT);
		lblNewLabel_2.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/color.png")));
		contentPane.add(lblNewLabel_2);

		JLabel lblNewLabel_3 = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel_3, -21, SpringLayout.NORTH, colorWatermark);
		sl_contentPane.putConstraint(SpringLayout.WEST, size, 13, SpringLayout.EAST, lblNewLabel_3);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel_3, 157, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel, -10, SpringLayout.NORTH, lblNewLabel_3);
		lblNewLabel_3.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/size_Text.png")));
		contentPane.add(lblNewLabel_3);

		fin = new JTextField();
		fin.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}

				if (e.getKeyCode() == KeyEvent.VK_ENTER) {

					String tiempoInicio = fin.getText();

					fin.setText(tiempoInicio);

					ponerDuracionFin(true);

					fin.setText(tiempoInicio);

				}

			}

			@Override
			public void keyReleased(KeyEvent e) {

				if (fin.getText().trim().isEmpty()) {

					fin.setText(duracion.getText());

				}

			}

		});

		sl_contentPane.putConstraint(SpringLayout.EAST, btnConvertir, 0, SpringLayout.EAST, fin);
		sl_contentPane.putConstraint(SpringLayout.EAST, btnNewButton_1_1_1, -6, SpringLayout.WEST, fin);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, fin, 0, SpringLayout.SOUTH, btnNewButton_1_1);
		sl_contentPane.putConstraint(SpringLayout.NORTH, fin, 0, SpringLayout.NORTH, btnNewButton_1_1);
		sl_contentPane.putConstraint(SpringLayout.WEST, fin, 569, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, fin, -25, SpringLayout.EAST, contentPane);
		fin.setColumns(10);
		contentPane.add(fin);

		blur = new JCheckBox("Desenfoque");
		sl_contentPane.putConstraint(SpringLayout.NORTH, blur, 18, SpringLayout.SOUTH, inicio);
		sl_contentPane.putConstraint(SpringLayout.EAST, blur, 0, SpringLayout.EAST, lblNewLabel_1);

		blur.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent arg0) {

				if (blur.isSelected()) {
					brillo_1.setEnabled(true);
					brillo_2.setEnabled(true);
					color_1.setEnabled(true);
					color_2.setEnabled(true);
				}

				else {

					brillo_1.setEnabled(false);
					brillo_2.setEnabled(false);
					color_1.setEnabled(false);
					color_2.setEnabled(false);
				}

			}

		});
		sl_contentPane.putConstraint(SpringLayout.WEST, blur, 6, SpringLayout.EAST, size);
		blur.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}
		});
		blur.setHorizontalAlignment(SwingConstants.CENTER);
		blur.setForeground(Color.WHITE);
		blur.setFont(new Font("Dialog", Font.PLAIN, 17));
		blur.setBackground(Color.DARK_GRAY);
		contentPane.add(blur);

		brillo_1 = new JTextField();
		brillo_1.setEnabled(false);

		brillo_1.addKeyListener(new KeyAdapter() {

			@Override

			public void keyReleased(KeyEvent e) {

				try {

					if (Integer.parseInt(brillo_1.getText()) <= 0) {
						brillo_1.setText("5");
					}

				}

				catch (Exception e1) {
					brillo_1.setText("5");
				}

			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}
		});

		brillo_1.setText("5");
		brillo_1.setHorizontalAlignment(SwingConstants.CENTER);
		brillo_1.setFont(new Font("Dialog", Font.PLAIN, 16));
		contentPane.add(brillo_1);
		brillo_1.setColumns(10);

		JLabel lblNewLabel_4 = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.EAST, colorWatermark, -6, SpringLayout.WEST, lblNewLabel_4);
		sl_contentPane.putConstraint(SpringLayout.EAST, posicionWatermark, -6, SpringLayout.WEST, lblNewLabel_4);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel_4, 272, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel_4, 0, SpringLayout.SOUTH, posicionWatermark);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblNewLabel_4, -365, SpringLayout.EAST, contentPane);
		lblNewLabel_4.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/pintura.png")));
		contentPane.add(lblNewLabel_4);

		color_1 = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.SOUTH, brillo_1, -11, SpringLayout.NORTH, color_1);
		color_1.setEnabled(false);
		color_1.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {

					if (Integer.parseInt(color_1.getText()) <= 0) {
						color_1.setText("5");
					}

				} catch (Exception e1) {
					color_1.setText("5");
				}

			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}
		});
		sl_contentPane.putConstraint(SpringLayout.NORTH, color_1, -5, SpringLayout.NORTH, posicionWatermark);
		sl_contentPane.putConstraint(SpringLayout.WEST, color_1, 16, SpringLayout.EAST, lblNewLabel_4);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, color_1, 10, SpringLayout.SOUTH, prev);
		color_1.setText("5");
		color_1.setHorizontalAlignment(SwingConstants.CENTER);
		color_1.setFont(new Font("Dialog", Font.PLAIN, 16));
		color_1.setColumns(10);
		contentPane.add(color_1);

		JLabel lblNewLabel_6 = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel_6, 272, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, size, -6, SpringLayout.WEST, lblNewLabel_6);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblNewLabel_6, 56, SpringLayout.SOUTH, inicio);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel_6, -8, SpringLayout.NORTH, lblNewLabel_4);
		sl_contentPane.putConstraint(SpringLayout.WEST, brillo_1, 11, SpringLayout.EAST, lblNewLabel_6);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblNewLabel_6, -360, SpringLayout.EAST, contentPane);
		lblNewLabel_6.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/brillo.png")));
		contentPane.add(lblNewLabel_6);

		brillo_2 = new JTextField();
		brillo_2.setEnabled(false);
		brillo_2.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {

					if (Integer.parseInt(brillo_2.getText()) <= 0) {
						brillo_2.setText("3");
					}

				} catch (Exception e1) {
					brillo_2.setText("3");
				}

			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}
		});
		sl_contentPane.putConstraint(SpringLayout.EAST, brillo_1, -30, SpringLayout.WEST, brillo_2);
		sl_contentPane.putConstraint(SpringLayout.WEST, brillo_2, 434, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, brillo_2, -18, SpringLayout.WEST, prev);
		brillo_2.setText("3");
		brillo_2.setHorizontalAlignment(SwingConstants.CENTER);
		brillo_2.setFont(new Font("Dialog", Font.PLAIN, 16));
		brillo_2.setColumns(10);
		contentPane.add(brillo_2);

		JLabel lblNewLabel_7 = new JLabel("Power");
		sl_contentPane.putConstraint(SpringLayout.NORTH, brillo_1, 8, SpringLayout.SOUTH, lblNewLabel_7);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, blur, -7, SpringLayout.NORTH, lblNewLabel_7);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel_7, 11, SpringLayout.EAST, lblNewLabel_6);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblNewLabel_7, 0, SpringLayout.EAST, brillo_1);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblNewLabel_7, 21, SpringLayout.NORTH, size);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel_7, 0, SpringLayout.SOUTH, size);
		lblNewLabel_7.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_7.setFont(new Font("Dialog", Font.PLAIN, 16));
		lblNewLabel_7.setForeground(Color.WHITE);
		contentPane.add(lblNewLabel_7);

		JLabel lblNewLabel_5_1_1 = new JLabel("Radio");
		sl_contentPane.putConstraint(SpringLayout.NORTH, brillo_2, 9, SpringLayout.SOUTH, lblNewLabel_5_1_1);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblNewLabel_5_1_1, -15, SpringLayout.EAST, lblNewLabel_1);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel_5_1_1, 0, SpringLayout.WEST, brillo_2);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel_5_1_1, 0, SpringLayout.SOUTH, size);
		lblNewLabel_5_1_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_5_1_1.setForeground(Color.WHITE);
		lblNewLabel_5_1_1.setFont(new Font("Dialog", Font.PLAIN, 16));
		contentPane.add(lblNewLabel_5_1_1);

		color_2 = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.SOUTH, brillo_2, -11, SpringLayout.NORTH, color_2);
		color_2.setEnabled(false);
		color_2.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {

					if (Integer.parseInt(color_2.getText()) <= 0) {
						color_2.setText("3");
					}

				} catch (Exception e1) {
					color_2.setText("3");
				}

			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}
			}
		});
		sl_contentPane.putConstraint(SpringLayout.EAST, color_1, -30, SpringLayout.WEST, color_2);
		sl_contentPane.putConstraint(SpringLayout.EAST, color_2, -18, SpringLayout.WEST, prev);
		sl_contentPane.putConstraint(SpringLayout.WEST, color_2, 434, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.NORTH, color_2, -5, SpringLayout.NORTH, posicionWatermark);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, color_2, 10, SpringLayout.SOUTH, prev);
		color_2.setText("3");
		color_2.setHorizontalAlignment(SwingConstants.CENTER);
		color_2.setFont(new Font("Dialog", Font.PLAIN, 16));
		color_2.setColumns(10);
		contentPane.add(color_2);

		guardarMismaCarpeta = new JCheckBox("Guardar en la");
		guardarMismaCarpeta.setHorizontalAlignment(SwingConstants.CENTER);
		guardarMismaCarpeta.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play(false);
				}

			}
		});
		guardarMismaCarpeta.setForeground(Color.WHITE);
		guardarMismaCarpeta.setBackground(Color.DARK_GRAY);
		sl_contentPane.putConstraint(SpringLayout.NORTH, guardarMismaCarpeta, 19, SpringLayout.SOUTH,
				btnNewButton_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.WEST, guardarMismaCarpeta, 0, SpringLayout.WEST, btnNewButton_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, guardarMismaCarpeta, -19, SpringLayout.SOUTH, lblNewLabel_3);
		sl_contentPane.putConstraint(SpringLayout.EAST, guardarMismaCarpeta, -25, SpringLayout.EAST, contentPane);
		guardarMismaCarpeta.setFont(new Font("Dialog", Font.PLAIN, 16));
		contentPane.add(guardarMismaCarpeta);

		JLabel lblNewLabel_5 = new JLabel("misma carpeta");
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel_5, 21, SpringLayout.EAST, lblNewLabel_5_1_1);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblNewLabel_5, -25, SpringLayout.EAST, contentPane);
		lblNewLabel_5.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_5.setForeground(Color.WHITE);
		lblNewLabel_5.setFont(new Font("Dialog", Font.PLAIN, 16));
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblNewLabel_5, 0, SpringLayout.NORTH, lblNewLabel_6);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel_5, -14, SpringLayout.NORTH, prev);
		contentPane.add(lblNewLabel_5);

		playbin = new PlayBin("GstDumbPlayer");

		playbin.setVideoSink(videoOutput.getElement());

	}

	public void openFile(String file) {

		File f = new File(file);

		playbin.stop();

		playbin.setURI(f.toURI());

		playbin.play();

	}
}
