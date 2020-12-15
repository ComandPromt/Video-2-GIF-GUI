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
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
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

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.sun.jna.Platform;

import lib.SimpleVideoComponent;
import net.bramp.ffmpeg.FFmpeg;

public class PlayerFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private JTextField size = new JTextField();

	private static JSlider positionSlider = new JSlider();

	private JComboBox posicionWatermark = new JComboBox();

	private JComboBox colorWatermark = new JComboBox();

	private JPanel contentPane;

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
	private JTextField fin;
	private JTextField blur_1;
	private JTextField blur_2;

	private void ponerDuracionGif() {

		DecimalFormat df = new DecimalFormat("#.000");

		String resultado = "" + df.format(segundosFin - segundosInicio);

		if (resultado.indexOf(".") == 0) {
			resultado = "0" + resultado;
		}

		if (resultado.contains("-")) {
			resultado = "";
		}

		duracionVideo.setText(resultado);

	}

	double convertirASegundos(String duracionVideo) {

		double horas = Double.parseDouble(duracionVideo.substring(0, duracionVideo.indexOf(":")));

		if (horas > 0) {
			horas *= 3600f;
		}

		double minutos = Double
				.parseDouble(duracionVideo.substring(duracionVideo.indexOf(":") + 1, duracionVideo.lastIndexOf(":")));

		if (minutos > 0) {
			minutos *= 60f;
		}

		double segundos = Double
				.parseDouble(duracionVideo.substring(duracionVideo.lastIndexOf(":") + 1, duracionVideo.length()));

		return horas + minutos + segundos;
	}

	private void play() {

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

				ponerTiempos();

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

		verTiempos(false);

		play();

		play();

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

	static String calcularSegundosActual(Object obj1, Object obj2) {

		double inicio = (double) obj1;

		long segundos = (long) obj2;

		segundosActual = Math.round((inicio / segundos) * Math.pow(segundos, 2));

		return calcularTiempo(segundosActual);

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

	private void verTiempos(boolean adjust) {

		duration = playbin.queryDuration(TimeUnit.NANOSECONDS);

		if (duration > 0 && video != null) {

			try {

				String resultado;

				ProcessBuilder processBuilder = new ProcessBuilder("ffprobe", video);

				processBuilder.redirectErrorStream(true);

				Process process = processBuilder.start();

				StringBuilder processOutput = new StringBuilder();

				try (BufferedReader processOutputReader = new BufferedReader(
						new InputStreamReader(process.getInputStream()));) {

					String readLine;

					while ((readLine = processOutputReader.readLine()) != null) {

						if (readLine.contains("Duration")) {
							processOutput.append(readLine + System.lineSeparator());
						}

					}

				}

				resultado = processOutput.toString().trim();

				resultado = resultado.substring(resultado.indexOf("Duration") + 9, resultado.indexOf(","));

				resultado = resultado.trim();

				convertirANanoSegundos(resultado, true);

			}

			catch (Exception e) {
				e.printStackTrace();
			}

		}

		LinkedList<Object> cuenta = new LinkedList<Object>();

		cuenta = ponerTiempos();

		if (adjust)

		{

			// duration = (long) segundosFin;

			System.out.println("resultado: " + saberSegundosActual(inicio.getText(), duration / 1000000000));

			duration = (long) convertirASegundos(fin.getText()) * 1000000000;

			playbin.seek((long) (saberSegundosActual(inicio.getText(), duration / 1000000000) * duration),
					TimeUnit.NANOSECONDS);

		}

		else {
			System.out.println("duracion: " + duration);
			playbin.seek((long) ((double) cuenta.get(0) * duration), TimeUnit.NANOSECONDS);
		}

		if (adjust) {
			play();
		}

	}

	private double saberSegundosActual(String inicio, long segundosTotales) {

		double resultado = 0;

		resultado = convertirASegundos(inicio) / segundosTotales;

		return resultado;

	}

	private long convertirANanoSegundos(String duracionVideo, boolean filtro) {

		long nanosegundos;

		nanosegundos = (long) convertirASegundos(duracionVideo) * 1000000000;

		if (filtro) {

			duration = nanosegundos;

			tiempo.setText(calcularSegundosActual(0D, nanosegundos));

			duracion.setText("" + calcularTiempo((long) nanosegundos));

		}

		return nanosegundos;

	}

	static LinkedList<Object> ponerTiempos() {

		LinkedList<Object> cuenta;

		cuenta = saberTiempo();

		tiempo.setText(calcularSegundosActual(cuenta.get(0), cuenta.get(1)));

		duracion.setText("" + calcularTiempo((long) cuenta.get(1)));

		return cuenta;
	}

	private void verVideo(boolean adjust) {

		if (duration > 0 && (adjust || positionSlider.getValueIsAdjusting())) {

			verTiempos(adjust);

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

	public static void main(String[] args) {

		positionSlider = new JSlider(0, 1000);

		positionSlider.setBorder(null);

		Gst.init();

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

			frame.setSize(700, 680);

			// center the jframe on screen
			frame.setLocationRelativeTo(null);

			frame.setVisible(true);

		}

		catch (Exception e) {
			e.printStackTrace();
		}

	}

	public PlayerFrame() {
		setIconImage(
				Toolkit.getDefaultToolkit().getImage(PlayerFrame.class.getResource("/imagenes/video_2_frame.png")));

		addKeyListener(new KeyAdapter() {

			@Override

			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}

			}

		});

		setTitle("Easy Video 2 GIF");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();

		setJMenuBar(menuBar);

		JMenuItem mntmNewMenuItem = new JMenuItem("Abrir Video (CTRL+O)\n");

		mntmNewMenuItem.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				try {
					abrirVideo();
				} catch (ImageProcessingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		});

		menuBar.add(mntmNewMenuItem);

		JMenuItem mntmNewMenuItem_1 = new JMenuItem("Configuración");

		menuBar.add(mntmNewMenuItem_1);

		contentPane = new JPanel();

		contentPane.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}

			}

		});

		contentPane.setForeground(Color.WHITE);

		contentPane.setBackground(Color.DARK_GRAY);

		setContentPane(contentPane);

		final SpringLayout sl_contentPane = new SpringLayout();
		sl_contentPane.putConstraint(SpringLayout.EAST, colorWatermark, -419, SpringLayout.EAST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, size, 0, SpringLayout.WEST, positionSlider);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, size, -20, SpringLayout.NORTH, colorWatermark);
		sl_contentPane.putConstraint(SpringLayout.EAST, size, 0, SpringLayout.EAST, colorWatermark);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, colorWatermark, -16, SpringLayout.NORTH, posicionWatermark);
		sl_contentPane.putConstraint(SpringLayout.WEST, posicionWatermark, 37, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, posicionWatermark, 0, SpringLayout.EAST, colorWatermark);
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
				} catch (ImageProcessingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
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

				verVideo(false);

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
					play();
				}
			}
		});

		btnNewButton_1_1.addMouseListener(new MouseAdapter() {
			@Override

			public void mousePressed(MouseEvent e) {

				try {

					String positionVideo = calcularPosicionVideo();

					segundosInicio = convertirASegundos(positionVideo);

					if (segundosFin > 0f && (segundosInicio > segundosFin)) {

						segundosInicio = 0;

						positionVideo = "00:00:0.0";

						duracionVideo.setText("");

					}

					else {
						ponerDuracionGif();
					}

					inicio.setText(positionVideo);

				} catch (Exception e1) {
				}
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
					play();
				}

			}

		});

		btnNewButton_1_1_1.addMouseListener(new MouseAdapter() {

			@Override

			public void mousePressed(MouseEvent e) {

				String positionVideo = calcularPosicionVideo();

				segundosFin = convertirASegundos(positionVideo);

				if (segundosFin > 0f && (segundosInicio > segundosFin)) {

					segundosFin = convertirASegundos(duracion.getText());

					positionVideo = duracion.getText();

					duracionVideo.setText("");

				}

				else {
					ponerDuracionGif();
				}

				fin.setText(positionVideo);

			}

		});
		btnNewButton_1_1_1.setHorizontalAlignment(SwingConstants.LEFT);
		btnNewButton_1_1_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(btnNewButton_1_1_1);

		JLabel lblDuracin = new JLabel("Duración");
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin, 10, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin, -5, SpringLayout.WEST, videoOutput);
		lblDuracin.setHorizontalAlignment(SwingConstants.CENTER);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblDuracin, 10, SpringLayout.NORTH, contentPane);
		lblDuracin.setFont(new Font("Dialog", Font.PLAIN, 18));
		lblDuracin.setForeground(Color.WHITE);
		contentPane.add(lblDuracin);

		anchoVideoTxt = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.WEST, anchoVideoTxt, 37, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, anchoVideoTxt, -33, SpringLayout.WEST, videoOutput);
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
		sl_contentPane.putConstraint(SpringLayout.EAST, largoVideoTxt, -33, SpringLayout.WEST, videoOutput);
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
		sl_contentPane.putConstraint(SpringLayout.WEST, duracionVideo, -114, SpringLayout.EAST, anchoVideoTxt);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, duracionVideo, 31, SpringLayout.SOUTH, lblDuracin);
		sl_contentPane.putConstraint(SpringLayout.EAST, duracionVideo, 0, SpringLayout.EAST, anchoVideoTxt);

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

		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblDuracin_1_1_1, -6, SpringLayout.NORTH, rate);
		sl_contentPane.putConstraint(SpringLayout.WEST, rate, 0, SpringLayout.WEST, duracionVideo);
		rate.setHorizontalAlignment(SwingConstants.CENTER);
		rate.setColumns(10);
		contentPane.add(rate);

		JLabel lblDuracin_1_1_1_1 = new JLabel("Calidad");
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblDuracin_1_1_1_1, 342, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.NORTH, rate, -46, SpringLayout.NORTH, lblDuracin_1_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, rate, -18, SpringLayout.NORTH, lblDuracin_1_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblDuracin_1_1_1_1, 37, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblDuracin_1_1_1_1, 0, SpringLayout.EAST, duracionVideo);
		lblDuracin_1_1_1_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblDuracin_1_1_1_1.setForeground(Color.WHITE);
		lblDuracin_1_1_1_1.setFont(new Font("Dialog", Font.PLAIN, 18));
		contentPane.add(lblDuracin_1_1_1_1);

		JCheckBox optimizar = new JCheckBox("Optimizar");
		sl_contentPane.putConstraint(SpringLayout.WEST, optimizar, 6, SpringLayout.EAST, colorWatermark);
		optimizar.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}
			}
		});
		optimizar.setHorizontalAlignment(SwingConstants.CENTER);
		optimizar.setFont(new Font("Dialog", Font.PLAIN, 18));
		optimizar.setBackground(Color.DARK_GRAY);
		optimizar.setForeground(Color.WHITE);
		contentPane.add(optimizar);

		JComboBox comboBox = new JComboBox();
		comboBox.setFont(new Font("Dialog", Font.PLAIN, 16));
		sl_contentPane.putConstraint(SpringLayout.NORTH, comboBox, 377, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblDuracin_1_1_1_1, -6, SpringLayout.NORTH, comboBox);
		sl_contentPane.putConstraint(SpringLayout.WEST, comboBox, 0, SpringLayout.WEST, duracionVideo);
		sl_contentPane.putConstraint(SpringLayout.EAST, comboBox, 0, SpringLayout.EAST, duracionVideo);
		comboBox.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}
			}
		});

		contentPane.add(comboBox);

		comboBox.addItem("Baja");

		comboBox.addItem("Alta");

		JButton btnNewButton = new JButton("");
		sl_contentPane.putConstraint(SpringLayout.EAST, optimizar, -6, SpringLayout.WEST, btnNewButton);
		sl_contentPane.putConstraint(SpringLayout.WEST, btnNewButton, 510, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, btnNewButton, -107, SpringLayout.EAST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.NORTH, btnNewButton, 0, SpringLayout.NORTH, colorWatermark);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, btnNewButton, -4, SpringLayout.SOUTH, posicionWatermark);
		btnNewButton.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/view.png")));
		btnNewButton.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}
			}
		});

		btnNewButton.addMouseListener(new MouseAdapter() {

			@Override

			public void mousePressed(MouseEvent e) {

				try {

					if (!inicio.getText().isEmpty() && !fin.getText().isEmpty() && segundosInicio < segundosFin) {

						verVideo(true);

					}

				}

				catch (Exception e1) {

					e1.printStackTrace();

				}

			}

		});

		btnNewButton.setFont(new Font("Dialog", Font.BOLD, 14));
		contentPane.add(btnNewButton);

		JButton btnConvertir = new JButton("");
		sl_contentPane.putConstraint(SpringLayout.NORTH, btnConvertir, 0, SpringLayout.NORTH, btnNewButton);
		sl_contentPane.putConstraint(SpringLayout.WEST, btnConvertir, 14, SpringLayout.EAST, btnNewButton);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, btnConvertir, 0, SpringLayout.SOUTH, btnNewButton);

		btnConvertir.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {

				if (!inicio.getText().isEmpty() && !fin.getText().isEmpty()) {

					try {

						// -vf "fps=10,scale=320:-1:flags=lanczos" -c:v pam -f image2pipe - | convert

						// -delay 10 - -loop 0 -layers optimize output.gif

						String calidad = "scale=" + anchoVideoTxt.getText() + ":" + largoVideoTxt.getText();

						String archivo = "/home/yeah/output.gif";

						FFmpeg ffmpeg = new FFmpeg();

						LinkedList<String> comando = new LinkedList<String>();

						comando.add("-ss");

						comando.add("" + inicio.getText());

						comando.add("-t");

						comando.add(fin.getText());

						comando.add("-i");

						comando.add(video);

						comando.add("-r");

						comando.add(rate.getText());

						if (!optimizar.isSelected()) {

							comando.add("-pix_fmt");

							comando.add("rgb24");

							comando.add("-s");

							comando.add(anchoVideoTxt.getText() + "x" + largoVideoTxt.getText());

						}

						if (optimizar.isSelected()) {

							comando.add("-filter_complex");

							comando.add(
									"[0:v] fps=15,scale=w=720:h=-1,split [a][b];[a] palettegen=stats_mode=single [p];[b][p] paletteuse=new=1");

							comando.add("-y");

							comando.add(archivo);

							ffmpeg.run(comando);

						}

						else {

							comando.add("-y");

							comando.add(archivo);

							ffmpeg.run(comando);
						}

						System.out.println("\n----------------------\n CONVERSION TERMINADA");

					}

					catch (Exception e) {
						e.printStackTrace();
					}

				}

			}

		});

		btnConvertir.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/video_2_frame.png")));
		btnConvertir.setFont(new Font("Dialog", Font.BOLD, 14));
		contentPane.add(btnConvertir);
		colorWatermark.setFont(new Font("Dialog", Font.PLAIN, 16));
		colorWatermark.setEnabled(false);
		colorWatermark.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					play();
				}
			}
		});

		colorWatermark.addItem("NEGRO");

		colorWatermark.addItem("BLANCO");

		colorWatermark.addItem("ROJO");

		colorWatermark.addItem("AZUL");

		colorWatermark.addItem("AMARILLO");

		colorWatermark.addItem("VERDE");

		colorWatermark.addItem("ROSA");

		colorWatermark.addItem("VIOLETA");

		colorWatermark.addItem("GRIS");

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
		sl_contentPane.putConstraint(SpringLayout.WEST, inicio, 4, SpringLayout.EAST, btnNewButton_1_1);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, inicio, -181, SpringLayout.SOUTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.NORTH, inicio, 28, SpringLayout.SOUTH, positionSlider);
		contentPane.add(inicio);
		inicio.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.EAST, inicio, -82, SpringLayout.WEST, lblNewLabel_1);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblNewLabel_1, -6, SpringLayout.WEST, btnNewButton_1_1_1);
		lblNewLabel_1.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/flag.png")));
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel_1.setForeground(Color.WHITE);
		lblNewLabel_1.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblNewLabel_1);

		JLabel lblNewLabel = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel_1, 0, SpringLayout.SOUTH, lblNewLabel);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel, 0, SpringLayout.WEST, videoOutput);
		lblNewLabel.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/start.png")));
		contentPane.add(lblNewLabel);

		JCheckBox watermark = new JCheckBox("WaterMark");
		sl_contentPane.putConstraint(SpringLayout.NORTH, watermark, 11, SpringLayout.SOUTH, comboBox);

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
		sl_contentPane.putConstraint(SpringLayout.WEST, watermark, 20, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, watermark, 10, SpringLayout.EAST, duracionVideo);
		watermark.setHorizontalAlignment(SwingConstants.CENTER);
		watermark.setForeground(Color.WHITE);
		watermark.setFont(new Font("Dialog", Font.PLAIN, 18));
		contentPane.add(watermark);

		textoWatermark = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.NORTH, textoWatermark, 13, SpringLayout.SOUTH, watermark);
		textoWatermark.setEnabled(false);
		textoWatermark.setToolTipText("Inserta el texto para la marca de agua");
		sl_contentPane.putConstraint(SpringLayout.WEST, textoWatermark, 0, SpringLayout.WEST, duracionVideo);
		textoWatermark.setHorizontalAlignment(SwingConstants.CENTER);
		textoWatermark.setColumns(10);
		contentPane.add(textoWatermark);

		JLabel label = new JLabel("");

		label.addMouseListener(new MouseAdapter() {

			@Override

			public void mousePressed(MouseEvent e) {
				reproducir = false;

				play();
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

		});

		size.setText("24");
		size.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(size);
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
		posicionWatermark.addItem("ABAJO - DENTRO");
		posicionWatermark.addItem("ABAJO - DERECHA");

		JLabel lblNewLabel_2 = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.WEST, colorWatermark, 6, SpringLayout.EAST, lblNewLabel_2);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel_2, 37, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblNewLabel_2, -581, SpringLayout.EAST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel_2, -16, SpringLayout.NORTH, posicionWatermark);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, textoWatermark, -28, SpringLayout.NORTH, lblNewLabel_2);
		lblNewLabel_2.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/color.png")));
		contentPane.add(lblNewLabel_2);

		JLabel lblNewLabel_3 = new JLabel("");
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel, -10, SpringLayout.NORTH, lblNewLabel_3);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel_3, 6, SpringLayout.EAST, textoWatermark);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lblNewLabel_3, -21, SpringLayout.NORTH, colorWatermark);
		lblNewLabel_3.setIcon(new ImageIcon(PlayerFrame.class.getResource("/imagenes/size_Text.png")));
		contentPane.add(lblNewLabel_3);

		fin = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.EAST, btnConvertir, 0, SpringLayout.EAST, fin);
		sl_contentPane.putConstraint(SpringLayout.EAST, btnNewButton_1_1_1, -6, SpringLayout.WEST, fin);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, fin, 0, SpringLayout.SOUTH, btnNewButton_1_1);
		sl_contentPane.putConstraint(SpringLayout.NORTH, fin, 0, SpringLayout.NORTH, btnNewButton_1_1);
		sl_contentPane.putConstraint(SpringLayout.WEST, fin, 569, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, fin, -25, SpringLayout.EAST, contentPane);
		fin.setColumns(10);
		contentPane.add(fin);

		JCheckBox chckbxDesenfoque = new JCheckBox("Desenfoque");
		sl_contentPane.putConstraint(SpringLayout.NORTH, optimizar, 47, SpringLayout.SOUTH, chckbxDesenfoque);
		sl_contentPane.putConstraint(SpringLayout.EAST, chckbxDesenfoque, 0, SpringLayout.EAST, optimizar);
		chckbxDesenfoque.setHorizontalAlignment(SwingConstants.CENTER);
		chckbxDesenfoque.setForeground(Color.WHITE);
		chckbxDesenfoque.setFont(new Font("Dialog", Font.PLAIN, 18));
		chckbxDesenfoque.setBackground(Color.DARK_GRAY);
		contentPane.add(chckbxDesenfoque);

		blur_1 = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.WEST, blur_1, 6, SpringLayout.EAST, chckbxDesenfoque);
		sl_contentPane.putConstraint(SpringLayout.EAST, blur_1, -108, SpringLayout.EAST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.NORTH, chckbxDesenfoque, 5, SpringLayout.NORTH, blur_1);

		blur_1.addKeyListener(new KeyAdapter() {

			@Override

			public void keyReleased(KeyEvent e) {

				try {

					if (Integer.parseInt(blur_1.getText()) <= 0) {
						blur_1.setText("4");
					}

				}

				catch (Exception e1) {
					blur_1.setText("4");
				}

			}

		});
		blur_1.setText("4");
		blur_1.setHorizontalAlignment(SwingConstants.CENTER);
		sl_contentPane.putConstraint(SpringLayout.NORTH, blur_1, 28, SpringLayout.SOUTH, btnNewButton_1_1_1);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, blur_1, -116, SpringLayout.SOUTH, contentPane);
		blur_1.setFont(new Font("Dialog", Font.PLAIN, 16));
		contentPane.add(blur_1);
		blur_1.setColumns(10);

		blur_2 = new JTextField();
		sl_contentPane.putConstraint(SpringLayout.EAST, blur_2, -25, SpringLayout.EAST, contentPane);

		blur_2.addKeyListener(new KeyAdapter() {

			@Override

			public void keyReleased(KeyEvent e) {

				try {

					if (Integer.parseInt(blur_2.getText()) <= 0) {
						blur_2.setText("3");
					}

				}

				catch (Exception e1) {
					blur_2.setText("3");
				}

			}

		});

		sl_contentPane.putConstraint(SpringLayout.NORTH, blur_2, -38, SpringLayout.SOUTH, size);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, blur_2, 0, SpringLayout.SOUTH, size);
		blur_2.setText("3");
		blur_2.setHorizontalAlignment(SwingConstants.CENTER);
		blur_2.setFont(new Font("Dialog", Font.PLAIN, 16));
		blur_2.setColumns(10);
		contentPane.add(blur_2);

		JLabel lblNewLabel_4 = new JLabel(":");
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblNewLabel_4, 37, SpringLayout.SOUTH, fin);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblNewLabel_4, 583, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, blur_2, 3, SpringLayout.EAST, lblNewLabel_4);
		sl_contentPane.putConstraint(SpringLayout.EAST, lblNewLabel_4, 12, SpringLayout.EAST, blur_1);
		lblNewLabel_4.setForeground(Color.WHITE);
		lblNewLabel_4.setFont(new Font("Dialog", Font.BOLD, 16));
		contentPane.add(lblNewLabel_4);

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
